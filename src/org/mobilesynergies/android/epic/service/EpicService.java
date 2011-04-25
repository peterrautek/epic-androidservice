package org.mobilesynergies.android.epic.service;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.mobilesynergies.android.epic.service.Manifest.permission;
import org.mobilesynergies.android.epic.service.core.AdministrationInterface;
import org.mobilesynergies.android.epic.service.core.ApplicationInterface;

import org.mobilesynergies.android.epic.service.core.Preferences;
import org.mobilesynergies.android.epic.service.core.ServiceStatusWidget;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceStateChangeManager;

import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceAdministrationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceApplicationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.IncomingMessageCallbackImpl;
import org.mobilesynergies.android.epic.service.interfaces.ParameterMapImpl;
import org.mobilesynergies.epic.client.EpicClient;
import org.mobilesynergies.epic.client.EpicClientException;
import org.mobilesynergies.epic.client.EpicNetworkConnectivityCallback;
import org.mobilesynergies.epic.client.IncomingMessageCallback;
import org.mobilesynergies.epic.client.NetworkNode;
import org.mobilesynergies.epic.client.PresenceCallback;
import org.mobilesynergies.epic.client.remoteui.EpicCommandInfo;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * The service running as a background process on Android. 
 * It offers two interfaces: 
 * <ul>
 * <li>An interface for 3rd party applications to bind to (see {@link EpicServiceApplicationInterface}). 
 * <li>An interface for activities that configure the service or show the status of the service (see {@link EpicServiceAdministrationInterface}).
 * </ul>
 * The EpicService class is responsible to establish a connection to the epic network when possible. It handles changes in network (i.e., internet) availability and manages the EPIC client.
 * 
 * @author Peter Rautek
 *
 */


public class EpicService extends Service {


	private static String CLASS_TAG = EpicService.class.getSimpleName();
	private ServiceStatusWidget mWidget = new ServiceStatusWidget();
	private HashMap<String, String> mMapSessionIdToPeer = new HashMap<String, String>(); 

	/*
	 * private class StateChangeHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int state = msg.what;
			mServiceStateObject.changeState(state);			
			Preferences.log(EpicService.this, "StateChangeHandler", "Status changed: " + mServiceStateObject.getStateAsHumanReadableString(state));
			mWidget.update(EpicService.this, mServiceStateObject.getStateAsHumanReadableString());
		}		
	}
	 */


	EpicServiceStateChangeManager mServiceStateChangeManager = new EpicServiceStateChangeManager();

	/** 
	 * Thread that is run whenever the service needs to connect to the xmpp server
	 */
	//private ConnectionThread mConnectionThread = null; 


	//private StateChangeHandler mStateChangeHandler = new StateChangeHandler();

	/**
	 * The implementation of the application interface
	 */
	private final IEpicServiceApplicationInterface mApplicationInterface = (IEpicServiceApplicationInterface) new ApplicationInterface(this);

	/**
	 * The implementation of the administration interface
	 */
	private final IEpicServiceAdministrationInterface mAdministrationInterface = new AdministrationInterface(this); 



	/**
	 * Identifying the version number of the epic standard that is implemented with this service
	 */
	public static final int EPIC_SERVICE_VERSION_NUMBER = 1;



	/**
	 * A XmppClient handling the interaction with the XmppServer.
	 */
	private EpicClient mEpicClient = null;  

	/**
	 * Called when the service is created
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Preferences.log(this, CLASS_TAG, "service created");
		//first check if the user is already registered
		if(Preferences.isRegistered(this)){
			//registered -> try to connect with the network
			handleStateChanges.sendEmptyMessage(0);
		}/* else{
			Intent registerIntent = new Intent();
			registerIntent.setClassName(this.getPackageName(), LoginActivity.class.getCanonicalName());
		}*/
	}





	IncomingMessageCallback mEpicMessageListener = new IncomingMessageCallback() {

		@Override
		public boolean handleMessage(String from, String action, String sessionid, String packageName, String className, ParameterMap data) {

			if((action==null)||(action.length()==0)){
				return false;
			}

			mMapSessionIdToPeer.put(sessionid, from);

			Bundle b = new Bundle();
			b.putString("action", action);
			b.putString("session", sessionid);
			b.putString("class", className);
			b.putString("package", packageName);

			Intent intent = new Intent();
			if(data!=null){
				b.putParcelable("data", new ParameterMapImpl(data));
				/*Set<String> keys = message.keySet();
				Iterator<String> iterKeys = keys.iterator();
				while(iterKeys.hasNext()){
					String key = iterKeys.next();
					b.putString(key, message.getString(key));
				}*/

			}
			intent.putExtras(b);

			//since the service does not run in a task, we have to start a new task
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if(action.equalsIgnoreCase("launch")){
				intent.setClassName(packageName, className);
			} else {
				intent.setAction(action);
			}

			try{
				startActivity(intent);
			} catch(ActivityNotFoundException e) {
				//we cannot resolve it 
				//we can also not automatically install it 
				//-> we point the user to the market
				Intent marketIntent = new Intent();
				marketIntent.setAction(Intent.ACTION_VIEW);
				Uri uri = Uri.parse("http://market.android.com/search?q=pname:"+packageName);
				marketIntent.setData(uri);
				marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(marketIntent);
			}
			return true;
		}
	};

	private boolean isConnectedToInternet(){

		ConnectivityManager cm =  (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		boolean isconnected = false;
		if(info!=null){
			isconnected = info.isConnected();
		}
		return isconnected;		
	}

	/** Called when the service is started.
	 * The service is either started by an application (respectively the user), or by the NetworkConnectivityStatusReceiver.
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		Preferences.log(this, CLASS_TAG, "service started");
		//first check if the user is already registered
		if(Preferences.isRegistered(this)){
			//registered -> try to connect with the network
			handleStateChanges.sendEmptyMessage(0);
		} else{
			//not doing anything - the onstart might be called because the network connectivity changed
			//it would be very disturbing if the user had to register only because of internet connectivity change
		}
	}

	@Override
	public void onDestroy() {
		Preferences.log(this, CLASS_TAG, "service destroyed");
	}


	Handler handleStateChanges = new Handler() {


		Timer mConnectionTimer = null;
		Timer mAuthenticationTimer = null;

		EpicServiceState mState = new EpicServiceState();

		public void handleMessage(Message msg) {
			stateChange();
		};

		private void stateChange() {

			int oldState = mState.state;
			int newState = currentState(); 

			String logmessage = EpicServiceState.getStateAsHumanReadableString(newState);

			mWidget.update(EpicService.this, logmessage);

			if(newState==oldState){
				//nothing to do!
				Preferences.log(EpicService.this, CLASS_TAG, "oldstate == newstate");
				//return;
			}

			//the state changed

			//log the new state 
			logmessage = "New State: " + logmessage;
			Preferences.log(EpicService.this, CLASS_TAG, logmessage);

			//cancel timers
			/*
			if(mConnectionTimer!=null)
			{
				mConnectionTimer.cancel();
				mConnectionTimer = null;
			}
			if(mAuthenticationTimer!=null)
			{
				mAuthenticationTimer.cancel();
				mAuthenticationTimer = null;
			}*/

			//send the new state to state listeners
			mServiceStateChangeManager.sendStateChangeToListeners(mState.state);

			//set the new state
			mState.state = newState;

			//handle the new state
			switch(newState){
			case EpicServiceState.INITIALIZING:
				initEpicClient();
				break;
			case EpicServiceState.NONETWORKCONNECTION:
				//we lost internet connection 
				//nothing to do
				//the NetworkConnectivityStatusReceiver will trigger a state change
				break;
			case EpicServiceState.NETWORKCONNECTION:
				//we either just got internet connection or lost server connection
				//anyway we will schedule (re)connection with the server
				/*if(mConnectionTimer!=null)
				{
					mConnectionTimer.cancel();
				}
				mConnectionTimer = new Timer();
				TimerTask connectiontask = new ConnectionTask();
				mConnectionTimer.schedule(connectiontask, 0, 600000);*/
				if( ! mEpicClient.isConnected()) {
					String servername = Preferences.getConfiguredServerName(EpicService.this);
					String servicename = Preferences.getConfiguredServiceName(EpicService.this);
					try {
						if(mEpicClient.establishConnection(servername, servicename)){
							Preferences.log(EpicService.this, "ConnectionThread", "succeeded.");
						} else {
							Preferences.log(EpicService.this, "ConnectionThread", "failed.");
						}
					} catch (EpicClientException e) {
						Preferences.log(EpicService.this, "ConnectionThread", "failed with exception: " +e.getMessage());
					}
				}
				handleStateChanges.sendEmptyMessage(0);

				break;
			case EpicServiceState.SERVERCONNECTION:
				/*if(mAuthenticationTimer!=null)
				{
					mAuthenticationTimer.cancel();
				}
				mAuthenticationTimer = new Timer();
				TimerTask authenticationtask = new AuthenticationTask();
				mAuthenticationTimer.schedule(authenticationtask, 0, 600000);*/

				if( ! mEpicClient.isConnectedToEpicNetwork()) {

					String username = Preferences.getUserName(EpicService.this);
					String password = Preferences.getUserPassword(EpicService.this);

					try {
						String resource = Preferences.getConfiguredDeviceName(EpicService.this);
						if(mEpicClient.authenticateUser(username, password, resource)){
							Preferences.log(EpicService.this, "AuthenticationThread", "succeeded.");
						} else {
							Preferences.log(EpicService.this, "AuthenticationThread", "failed.");
						}
					} catch (EpicClientException e) {
						Preferences.log(EpicService.this, "AuthenticationThread", "failed with exception: " +e.getMessage());
					}
				}

				handleStateChanges.sendEmptyMessage(0);
				break;
			case EpicServiceState.EPICNETWORKCONNECTION:
				//got epic network connection - nothing to do!  
				break;
			}



		}


		private int currentState() {

			if(mEpicClient==null){
				return EpicServiceState.INITIALIZING;
			}

			int state = EpicServiceState.NONETWORKCONNECTION;

			if(isConnectedToInternet()){
				state = EpicServiceState.NETWORKCONNECTION;
			}
			if(isConnectedToServer()){
				state = EpicServiceState.SERVERCONNECTION;
			}
			if(isConnectedToEpicNetwork()){
				state = EpicServiceState.EPICNETWORKCONNECTION;
			}
			return state;
		}


		private void initEpicClient() {
			//create a new client
			mEpicClient = new EpicClient();
			Preferences.log(EpicService.this, CLASS_TAG, "creating new epic client");
			//register the connectivity callback
			mEpicClient.registerEpicNetworkConnectivityCallback(new EpicNetworkConnectivityCallback() {
				/**
				 * connectivity to the epic network changed (server side)
				 */
				@Override
				public void onConnectivityChanged(boolean hasConnectivity) {
					handleStateChanges.sendEmptyMessage(0);
				}
			});

			mEpicClient.registerEpicMessageCallback(mEpicMessageListener);
			handleStateChanges.sendEmptyMessage(0);
		}


		/**
		 * Thread that will try to connect to the server 
		 */
		/*class ConnectionTask extends TimerTask {


			public void run(){
				if( mEpicClient.isConnected()) {
					return;
				}

				String servername = Preferences.getConfiguredServerName(EpicService.this);
				String servicename = Preferences.getConfiguredServiceName(EpicService.this);

				try {
					if(mEpicClient.establishConnection(servername, servicename)){
						Preferences.log(EpicService.this, "ConnectionThread", "succeeded.");
					} else {
						Preferences.log(EpicService.this, "ConnectionThread", "failed.");
					}
				} catch (EpicClientException e) {
					Preferences.log(EpicService.this, "ConnectionThread", "failed with exception: " +e.getMessage());
				}
				handleStateChanges.sendEmptyMessage(0);

			}
		};*/

		/**
		 * Thread that will try to authenticate at the server 
		 */
		/*class AuthenticationTask extends TimerTask{

			public void run(){
				//this thread constantly checks if the client is connected
				//and tries to establish a connection if necessary 
				if( mEpicClient.isConnectedToEpicNetwork()) {
					return;
				}

				String username = Preferences.getUserName(EpicService.this);
				String password = Preferences.getUserPassword(EpicService.this);

				try {
					String resource = Preferences.getConfiguredDeviceName(EpicService.this);
					if(mEpicClient.authenticateUser(username, password, resource)){
						Preferences.log(EpicService.this, "AuthenticationThread", "succeeded.");
					} else {
						Preferences.log(EpicService.this, "AuthenticationThread", "failed.");
					}
				} catch (EpicClientException e) {
					Preferences.log(EpicService.this, "AuthenticationThread", "failed with exception: " +e.getMessage());
				}

				handleStateChanges.sendEmptyMessage(0);
			}
		};*/









	};


	/**
	 * Overriding the android.os.Service function onBind. 
	 * 
	 * @return Returns the desired interface to the calling activity 
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// Select the interface to return.  
		if (intent.getAction().equals("org.mobilesynergies.EPIC_SERVICE")) {
			return (IBinder) mApplicationInterface;
		}
		if (IEpicServiceAdministrationInterface.class.getName().equals(intent.getAction())) {
			return (IBinder) mAdministrationInterface;
		}
		return null;
	}

	/** 
	 * The application can register a callback to be informed about changes of service availability.
	 * The callback is called either when the connection to the xmpp server was established and the user authentication suceeded or when the connection to the xmpp server was lost or the user logged out.
	 * @param callback The callback that will be notified about changes
	 */
	public void registerServiceStatusChangeCallback(IServiceStatusChangeCallback callback) {
		mServiceStateChangeManager.addServiceStatusCallback(callback);
	}


	/**
	 * @param sessionToken
	 * @param messageCallback
	 */
	public void registerMessageCallback(String application,
			IncomingMessageCallbackImpl messageCallback) {

		int iPermissionReceiveMessagesStatus = checkCallingOrSelfPermission(permission.receivemessages);
		if(iPermissionReceiveMessagesStatus==PackageManager.PERMISSION_DENIED){
			Log.w(CLASS_TAG, "The calling application is missing the permission" + permission.receivemessages+".");
			return;
		}

		//TODO
	}

	public void unregisterMessageCallback(String application) {
		//TODO		
	}



	/**

	 * @param application
	 * @param object
	 */
	public void sendMessage(String application, Bundle object) {
		int iPermissionSendMessagesStatus = checkCallingOrSelfPermission(permission.sendmessages);
		if(iPermissionSendMessagesStatus==PackageManager.PERMISSION_DENIED){
			Log.w(CLASS_TAG, "The calling application is missing the permission"+ permission.sendmessages+".");
			return;
		}
		if(mEpicClient==null){
			return;
		}
		//TODO sendMessage(sessionToken, object);
	}

	public void sendMessage(String action, String sessionid, ParameterMapImpl parameter) {

		int iPermissionSendMessagesStatus = checkCallingOrSelfPermission(permission.sendmessages);
		if(iPermissionSendMessagesStatus==PackageManager.PERMISSION_DENIED){
			Log.w(CLASS_TAG, "The calling application is missing the permission"+ permission.sendmessages+".");
			return;
		}
		if(mEpicClient==null){
			return;
		}
		String receiver = mMapSessionIdToPeer.get(sessionid);
		mEpicClient.sendMessage(receiver, action, sessionid, parameter);

	}


	public NetworkNode[] getNetworkNodes() throws EpicClientException {
		if(mEpicClient==null){
			return null;
		}
		return mEpicClient.getNetworkNodes();		
	}


	public boolean isConnectedToEpicNetwork() {
		if(mEpicClient==null){
			return false;
		}
		return mEpicClient.isConnectedToEpicNetwork();
	}

	public boolean isConnectedToServer() {
		if(mEpicClient==null){
			return false;
		}
		return mEpicClient.isConnected();
	}



	public EpicCommandInfo[] getEpicCommands(String epicNode) throws EpicClientException {
		if(mEpicClient==null){
			return null;
		}
		return mEpicClient.getEpicCommands(epicNode);
	}


	public String executeRemoteCommand(String epicNode, String command,
			String sessionId, ParameterMapImpl parametersIn,
			ParameterMapImpl parametersOut) throws EpicClientException {
		if(mEpicClient==null){
			return null;
		}

		return mEpicClient.executeRemoteCommand(epicNode, command,
				sessionId, parametersIn, parametersOut);
	}


	public void registerPresenceCallback(PresenceCallback callback) {
		if(mEpicClient==null){
			return;
		}
		mEpicClient.registerPresenceCallback(callback);
	}





}