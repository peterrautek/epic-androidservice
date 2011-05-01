package org.mobilesynergies.android.epic.service;

import java.util.HashMap;

import org.mobilesynergies.android.epic.service.Manifest.permission;
import org.mobilesynergies.android.epic.service.administration.ConfigurationDatabase;
import org.mobilesynergies.android.epic.service.administration.ServiceConfigurationActivity;
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
import org.mobilesynergies.epic.client.remoteui.Parameter;

import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
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

	
	EpicServiceStateChangeManager mServiceStateChangeManager = new EpicServiceStateChangeManager();

	
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
	
	protected static final int STATECHANGE_OK = 0;
	protected static final int STATECHANGE_NONETWORK = -1;
	protected static final int STATECHANGE_NOSERVERCONNECTION = -2;
	protected static final int STATECHANGE_AUTHFAIL = -3;
	protected static final int STATECHANGE_XMPPERROR = -4;
	protected static final int STATECHANGE_STOP = -5;
	



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
			handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
		} else{
			//the onstart might be called because the network connectivity changed
			//it would be very disturbing if the user had to register only because of internet connectivity change
			//however we inform the widget and the statechangelisteners that the user is not registered yet:
			int state = mState.updateState(EpicService.this, mEpicClient); 
			String logmessage = EpicServiceState.getStateAsHumanReadableString(state);
			mWidget.update(EpicService.this, logmessage);
			mServiceStateChangeManager.sendStateChangeToListeners(state);

		}
	}





	IncomingMessageCallback mEpicMessageListener = new IncomingMessageCallback() {

		@Override
		public boolean handleMessage(String from, String action, String sessionid, String packageName, String className, Parameter data) {

			if((action==null)||(action.trim().length()==0)){
				return false;
			}
			
			
			
			// perform intent resolution
			PackageManager packageManager = getPackageManager();
			// create the intent 
			final Intent mainIntent = new Intent();
			mainIntent.addCategory(Intent.CATEGORY_DEFAULT);
			
			if( ! action.equalsIgnoreCase("org.epic.action.LaunchApplication")){
				mainIntent.setAction(action);
				Uri datauri = Uri.parse("epic://"+action);
				mainIntent.setData(datauri);
			}
				
			if((packageName!=null)&&(packageName.trim().length()>0)){
				if((className!=null)&&(className.trim().length()>0)){
					mainIntent.setClassName(packageName, className);
				} else {
					mainIntent.setPackage(packageName);
				}
			}
			
			
			//try to resolve the intent and get the package and class name
			ComponentName componentname  = mainIntent.resolveActivity(packageManager);
			
			if(componentname==null){
				//ignore this intent
				return true;
			}
			
			packageName = componentname.getPackageName();
			className = componentname.getClassName();
			
			// check for permission configuration
			int iPermission = ConfigurationDatabase.PERMISSION_UNKNOWN;
			ConfigurationDatabase permissionDatabase = new ConfigurationDatabase(EpicService.this);
			iPermission = permissionDatabase.getPermissionValue(ConfigurationDatabase.getUniqueId(packageName, className));
			

			// execute permission model
			if(iPermission==ConfigurationDatabase.PERMISSION_UNKNOWN){
				iPermission=ServiceConfigurationActivity.PERMISSION_DEFAULT;
			}
			
			if(iPermission==ConfigurationDatabase.PERMISSION_DISALLOW){
				return true;
			}
			
			if(iPermission==ConfigurationDatabase.PERMISSION_ASK){
				//TODO
				return true;
			}
			
			// start activity
			
				
			mMapSessionIdToPeer.put(sessionid, from);
			Bundle b = new Bundle();
			if(data!=null){
				String type = data.getType();
				String xml = data.asXml("data");
				Log.d(CLASS_TAG, xml);
				ParameterMapImpl map = new ParameterMapImpl();
				map.putParameter("data", data);
				//b.putParcelable("data", map);
				//TODO put data in the bundle
				//Parcel p = Parcel.obtain();
				//Map map = data.getMap();
				//if(map!=null){
					//p.writeMap(map);
				//}
			//	Parcelable p1 = new Parcelable();
		//		b.putParcel("data", p);
				
				/*Set<String> keys = message.keySet();
				Iterator<String> iterKeys = keys.iterator();
				while(iterKeys.hasNext()){
					String key = iterKeys.next();
					b.putString(key, message.getString(key));
				}*/

			}
			b.putString("action", action);
			b.putString("session", sessionid);
			b.putString("class", className);
			b.putString("package", packageName);
			
			mainIntent.putExtras(b);

			//since the service does not run in a task, we have to start a new task
			mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			
			try{
				startActivity(mainIntent);
			} catch(ActivityNotFoundException e) {
				//we cannot resolve it 
				//we cannot automatically install it 
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


	/** Called when the service is started.
	 * The service is either started by an application (respectively the user), or by the NetworkConnectivityStatusReceiver.
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		Preferences.log(this, CLASS_TAG, "service started");
		//first check if the user is already registered
		if(Preferences.isRegistered(this)){
			//registered -> try to connect with the network
			handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
		} else{
			//the onstart might be called because the network connectivity changed
			//it would be very disturbing if the user had to register only because of internet connectivity change
			//however we inform the widget and the statechangelisteners that the user is not registered yet:
			int state = mState.updateState(EpicService.this, mEpicClient); 
			String logmessage = EpicServiceState.getStateAsHumanReadableString(state);
			mWidget.update(EpicService.this, logmessage);
			mServiceStateChangeManager.sendStateChangeToListeners(state);

		}
	}
	
	
	
	@Override
	public void onDestroy() {
		Preferences.log(this, CLASS_TAG, "service destroyed");
		/*if(mEpicClient!=null){
			mEpicClient.disconnect();
		}*/		
		
		

	}

	EpicServiceState mState = new EpicServiceState();

	Handler handleStateChanges = new Handler() {

		public void handleMessage(Message msg) {
			//if other ok messages are pending we delete them
			this.removeMessages(STATECHANGE_OK);
			
			if(msg.what==STATECHANGE_OK){
				stateChange();
			} else {
				switch(msg.what){
				case STATECHANGE_STOP:{
					int state = EpicServiceState.STOPPED;
					mState.setState(state);
					String logmessage = EpicServiceState.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					if(mEpicClient!=null){
						mEpicClient.disconnect();
					}
					mEpicClient = null;
					stopSelf();
					break;
				}
				case STATECHANGE_XMPPERROR:{
					int state = EpicServiceState.ERROR_XMPPERROR;
					mState.setState(state);
					String logmessage = EpicServiceState.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					if(mEpicClient!=null){
						mEpicClient.disconnect();
					}
					//try to recover
					//mState.setState(EpicServiceState.UNINITIALIZED);
					//handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
					break;
				}
				case STATECHANGE_NONETWORK:{
					//inform the widget and the state change listeners
					int state = EpicServiceState.ERROR_NONETWORK;
					mState.setState(state);
					String logmessage = EpicServiceState.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					//wait for the NetworkConnectivityStatusReceiver to change the state again
					}
					break;
				case STATECHANGE_AUTHFAIL:{
					//inform the widget and the state change listeners
					int state = EpicServiceState.ERROR_AUTHFAIL;
					mState.setState(state);
					String logmessage = EpicServiceState.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					//wait for the MainActivity to start the service again
					}
					break;
				case STATECHANGE_NOSERVERCONNECTION:{
					//inform the widget and the state change listeners
					int state = EpicServiceState.ERROR_NOSERVER;
					mState.setState(state);
					String logmessage = EpicServiceState.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					//wait for the MainActivity to start the service again
					}
					break;
				}
			}
		};

		private void stateChange() {

			int oldState = mState.getState();

			//set the new state
			int state = mState.updateState(EpicService.this, mEpicClient); 

			String logmessage = EpicServiceState.getStateAsHumanReadableString(state);

			mWidget.update(EpicService.this, logmessage);

			if(state==oldState){
				//nothing to do!
				Preferences.log(EpicService.this, CLASS_TAG, "oldstate == newstate");
				//return;
			}

			//the state changed

			//log the new state 
			logmessage = "New State: " + logmessage;
			Preferences.log(EpicService.this, CLASS_TAG, logmessage);


			//send the new state to state listeners
			mServiceStateChangeManager.sendStateChangeToListeners(state);


			//handle the new state
			switch(state){
			case EpicServiceState.INITIALIZING:
				initEpicClient();
				break;
			//case EpicServiceState.NONETWORKCONNECTION:
				//we lost internet connection
				//handleStateChanges.sendEmptyMessage(STATECHANGE_NONETWORK);
				//the NetworkConnectivityStatusReceiver will trigger a state change
				//break;
			case EpicServiceState.NETWORKCONNECTION:
				//we either just got internet connection or lost server connection
				//anyway we will schedule (re)connection with the server
				if( ! mEpicClient.isConnected()) {
					String servername = Preferences.getConfiguredServerName(EpicService.this);
					String servicename = Preferences.getConfiguredServiceName(EpicService.this);
					try {
						if(mEpicClient.establishConnection(servername, servicename)){
							Preferences.log(EpicService.this, "ConnectionThread", "succeeded.");
							handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
						} else {
							Preferences.log(EpicService.this, "ConnectionThread", "failed.");
							handleStateChanges.sendEmptyMessage(STATECHANGE_NOSERVERCONNECTION);
						}
					} catch (EpicClientException e) {
						Preferences.log(EpicService.this, "ConnectionThread", "failed with exception: " +e.getMessage());
						handleStateChanges.sendEmptyMessage(STATECHANGE_NOSERVERCONNECTION);
					}
				}
				break;
			case EpicServiceState.SERVERCONNECTION:
				if( ! mEpicClient.isConnectedToEpicNetwork()) {

					String username = Preferences.getUserName(EpicService.this);
					String password = Preferences.getUserPassword(EpicService.this);

					try {
						String resource = Preferences.getConfiguredDeviceName(EpicService.this);
						if(mEpicClient.authenticateUser(username, password, resource)){
							Preferences.log(EpicService.this, "AuthenticationThread", "succeeded.");
							handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
						} else {
							Preferences.log(EpicService.this, "AuthenticationThread", "failed.");
							handleStateChanges.sendEmptyMessage(STATECHANGE_AUTHFAIL);
						}
					} catch (EpicClientException e) {
						Preferences.log(EpicService.this, "AuthenticationThread", "failed with exception: " +e.getMessage());
						handleStateChanges.sendEmptyMessage(STATECHANGE_AUTHFAIL);
					}
				}
				break;
			case EpicServiceState.EPICNETWORKCONNECTION:
				//got epic network connection - nothing to do!  
				break;
			}
		}


		private void initEpicClient() {
			//create a new client
			mEpicClient = new EpicClient();
			Preferences.log(EpicService.this, CLASS_TAG, "creating new epic client");
			//register the connectivity callback
			mEpicClient.registerEpicNetworkConnectivityCallback(new EpicNetworkConnectivityCallback() {
				

				@Override
				public void onConnectionClosed() {
					handleStateChanges.sendEmptyMessage(STATECHANGE_STOP);
				}

				@Override
				public void onConnectionClosedOnError() {
					handleStateChanges.sendEmptyMessage(STATECHANGE_XMPPERROR);
				}
			});

			mEpicClient.registerEpicMessageCallback(mEpicMessageListener);
			handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
		}


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
	
	public void stop(){
		handleStateChanges.sendEmptyMessage(STATECHANGE_STOP);
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

	public int getState() {
		return mState.getState();
	}





}