package org.mobilesynergies.android.epic.service;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.mobilesynergies.android.epic.service.Manifest.permission;
import org.mobilesynergies.android.epic.service.core.AdministrationInterface;
import org.mobilesynergies.android.epic.service.core.ApplicationInterface;
import org.mobilesynergies.android.epic.service.core.LoginActivity;
import org.mobilesynergies.android.epic.service.core.Preferences;
import org.mobilesynergies.android.epic.service.core.ServiceStatusWidget;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
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

	private class StateChangeHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			int state = msg.what;
			mServiceStateObject.changeState(state);			
			Preferences.log(EpicService.this, "StateChangeHandler", "Status changed: " + mServiceStateObject.getStateAsHumanReadableString(state));

			mWidget.update(EpicService.this, mServiceStateObject.getStateAsHumanReadableString());
		}		
	}

	

	/**
	 * Thread that will try to connect to the server 
	 */
	private class ConnectionThread extends Thread {

		private String mServer = null;
		//default timeout between consecutive checks is set to 10 minutes
		//private int mTimeoutBetweenChecks = 600000;
		//default timeout between successive tries is set to 5 seconds
		private int mTimeoutBetweenTries = 5000;
		private String mUsername = "";
		private String mPassword = "";
		private String mService = "";
		private boolean mStopped = false;


		public ConnectionThread(String server, String username, String password, String service, StateChangeHandler stateChangeHandler) {
			mServer = server;
			mService = service;
			mUsername = username;
			mPassword = password;
			mStateChangeHandler = stateChangeHandler;

		}

		public void setTimeoutBetweenTries(int iTimeoutBetweenTries){
			if(iTimeoutBetweenTries>0){
				mTimeoutBetweenTries = iTimeoutBetweenTries;
			}
		}

		private int getClientState(){
			if(mEpicClient.isConnectedToEpicNetwork()){
				return EpicServiceState.STATE_AUTHENTICATED;
			}

			if(mEpicClient.isConnected()){
				return EpicServiceState.STATE_SERVERCONNECTED; 
			}

			if(isConnectedToInternet()){
				return EpicServiceState.STATE_INTERNETCONNECTED;
			}


			return EpicServiceState.STATE_UNCONNECTED;
		}

		public void run(){
			//this thread constantly checks if the client is connected
			//and tries to establish a connection if necessary 
			while((!mStopped)&&(!mEpicClient.isConnectedToEpicNetwork())) {
				Preferences.log(EpicService.this, "ConnectionThread", "Starting an iteration...");
				if( ! mEpicClient.isConnected()) {
					//try to connect
					try {
						Preferences.log(EpicService.this, "ConnectionThread", "Trying to connect to epic server...");
						if(mEpicClient.establishConnection(mServer, mService)){
							Preferences.log(EpicService.this, "ConnectionThread", "success.");
							if(mStateChangeHandler!=null){
								mStateChangeHandler.sendEmptyMessage(getClientState());
							} 
						} else {
							Preferences.log(EpicService.this, "ConnectionThread", "failed.");
							if(mStateChangeHandler!=null){
								mStateChangeHandler.sendEmptyMessage(getClientState());
							} 

						}
					} catch (EpicClientException e) {
						Preferences.log(EpicService.this, "ConnectionThread", "failed with exception: " +e.getMessage());
						if(mStateChangeHandler!=null){
							mStateChangeHandler.sendEmptyMessage(getClientState());
						} 

					}
				}  

				if(mEpicClient.isConnected()) {
					if(!mEpicClient.isConnectedToEpicNetwork()){
						//try to log in
						Preferences.log(EpicService.this, "ConnectionThread", "Trying to login...");
						try {
							String resource = Preferences.getConfiguredDeviceName(EpicService.this);
								
							if(mEpicClient.authenticateUser(mUsername, mPassword, resource)){
								Preferences.log(EpicService.this, "ConnectionThread", "success.");
								if(mStateChangeHandler!=null){
									mStateChangeHandler.sendEmptyMessage(getClientState());
								} 

							} else {
								Preferences.log(EpicService.this, "ConnectionThread", "failed.");
								if(mStateChangeHandler!=null){
									mStateChangeHandler.sendEmptyMessage(getClientState());
								} 

							}
						} catch (EpicClientException e) {
							Preferences.log(EpicService.this, "ConnectionThread", "failed with exception: "+e.getMessage());
							if(mStateChangeHandler!=null){

								mStateChangeHandler.sendEmptyMessage(getClientState());
							} 

						}
					} 
				} 


				if(mEpicClient.isConnectedToEpicNetwork()){
					//everything is fine -> kill the thread
					Preferences.log(EpicService.this, "ConnectionThread", "everything is fine stopping the thread.");


				} else {
					//not connected to epic network -> sleep until next try
					Preferences.log(EpicService.this, "ConnectionThread", "still not connected, sleeping for some time.");
					try{
						Log.d(CLASS_TAG, "Will try again to connect to the server in " + mTimeoutBetweenTries + " milliseconds.");
						sleep(mTimeoutBetweenTries);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}


		public void stopThread() throws InterruptedException{
			Preferences.log(EpicService.this, "ConnectionThread", "thread was stopped.");
			mStopped=true;
			this.join();
		}

	};

	/** 
	 * Thread that is run whenever the service needs to connect to the xmpp server
	 */
	private ConnectionThread mConnectionThread = null; 


	private StateChangeHandler mStateChangeHandler = new StateChangeHandler();

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
	 * A state object that defers the implementation of all state dependent methods of the service.   
	 */
	private EpicServiceState mServiceStateObject = new EpicServiceState();

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
		Preferences.deleteLog(this);
		//first check if the user is already registered
		if(Preferences.isRegistered(this)){
			//registered -> try to connect with the network
			changeConnectivityState();
		} else{
			Intent registerIntent = new Intent();
			registerIntent.setClassName(this.getPackageName(), LoginActivity.class.getCanonicalName());
		}
	}


	void startNewConnectionThread(){

		if(mConnectionThread!=null){
			try {
				Preferences.log(this, CLASS_TAG, "stopping the connection thread and waiting for it to finish (join).");
				mConnectionThread.stopThread();
				Preferences.log(this, CLASS_TAG, "connection thread finished");
			} catch (InterruptedException e) {
				Preferences.log(this, CLASS_TAG, "stopping the connection thread failed");
				e.printStackTrace();
			}
		}
		if(mEpicClient!=null){
			mEpicClient.disconnect();
			Preferences.log(this, CLASS_TAG, "disconnecting the epic client");
		}

		initEpicClient();
		
		String username = Preferences.getUserName(this);
		String password = Preferences.getUserPassword(this);
		String servername = Preferences.getConfiguredServerName(this);
		String servicename = Preferences.getConfiguredServiceName(this);
			
		mConnectionThread = new ConnectionThread(servername, username, password, servicename, mStateChangeHandler);
		Preferences.log(this, CLASS_TAG, "starting new connection thread");
		mConnectionThread.start();
	}



	private void initEpicClient() {
		//create a new client
		mEpicClient = new EpicClient();
		Preferences.log(this, CLASS_TAG, "creating new epic client");
		//register the connectivity callback
		mEpicClient.registerEpicNetworkConnectivityCallback(new EpicNetworkConnectivityCallback() {
			/**
			 * connectivity to the epic network changed (server side)
			 */
			@Override
			public void onConnectivityChanged(boolean hasConnectivity) {
				changeConnectivityState();
				String internetConn = isConnectedToInternet() ? "connected" : "disconnected";
				String epicConn = isConnectedToEpicNetwork() ? "connected" : "disconnected";

				Preferences.log(EpicService.this, "EpicNetworkConnectivityCallback", "connectivity changed. internet: " + internetConn + ", epic: " + epicConn);				
			}

		
		});


		//register applications that come "pre-installed" with the epic service
		//register the remote clip board application
		mEpicClient.registerEpicMessageCallback(mEpicMessageListener);


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
		//first check if the user is already registered
		if(Preferences.isRegistered(this)){
			//registered -> try to connect with the network
			changeConnectivityState();
		} else{
			//not doing anything - the onstart might be called because the network connectivity changed
			//it would be very disturbing if the user had to register only because of internet connectivity change
		}
	}
	

	private void changeConnectivityState() {
		
		boolean bHasInternetConnectivity = isConnectedToInternet();
		boolean bHasEpicNetworkConnectivity = false; 
		if(mEpicClient!=null){
			bHasEpicNetworkConnectivity = mEpicClient.isConnectedToEpicNetwork();
		}

		if(bHasInternetConnectivity){
			if(bHasEpicNetworkConnectivity){
				//is already connected -> change state to authenticated
				mStateChangeHandler.sendEmptyMessage(EpicServiceState.STATE_AUTHENTICATED);
				Log.d(CLASS_TAG, "Internet connection changed to connected, epic client is connected!");
			} else {
				//connected to the internet
				//no epic connection; all statefull entities are gone 
				//-> change state to internet connected
				mStateChangeHandler.sendEmptyMessage(EpicServiceState.STATE_INTERNETCONNECTED);
				//we need to reconnect
				startNewConnectionThread();
				Log.d(CLASS_TAG, "Internet connection changed to connected, starting new epic client !");
			}
		} else {
			//change the state to unconnected 
			mStateChangeHandler.sendEmptyMessage(EpicServiceState.STATE_UNCONNECTED);
			//do nothing else but wait for internet connectivity
			if(bHasEpicNetworkConnectivity){
				Log.d(CLASS_TAG, "Internet connection changed to disconnected, epic client is connected!");
			} else {
				Log.d(CLASS_TAG, "Internet connection changed to disconnected, epic client is disconnected!");
			}
		}	
	}


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
		mServiceStateObject.addServiceStatusCallback(callback);
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