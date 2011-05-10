package org.mobilesynergies.android.epic.service;

import java.util.concurrent.ConcurrentHashMap;

import org.mobilesynergies.android.epic.service.Manifest.permission;
import org.mobilesynergies.android.epic.service.administration.ConfigurationDatabase;
import org.mobilesynergies.android.epic.service.administration.ServiceConfigurationActivity;
import org.mobilesynergies.android.epic.service.core.AdministrationInterface;
import org.mobilesynergies.android.epic.service.core.ApplicationInterface;
import org.mobilesynergies.android.epic.service.core.Preferences;
import org.mobilesynergies.android.epic.service.core.ServiceStatusWidget;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceStateChangeManager;
import org.mobilesynergies.android.epic.service.core.states.StateObject;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceAdministrationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceApplicationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.remoteui.BundleAdapter;
import org.mobilesynergies.epic.client.EpicClient;
import org.mobilesynergies.epic.client.EpicClientException;
import org.mobilesynergies.epic.client.EpicNetworkConnectivityCallback;
import org.mobilesynergies.epic.client.IncomingMessageCallback;
import org.mobilesynergies.epic.client.NetworkNode;
import org.mobilesynergies.epic.client.PresenceCallback;
import org.mobilesynergies.epic.client.remoteui.EpicCommandInfo;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
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

	/**
	 * Class identification string
	 */
	private static final String CLASS_TAG = EpicService.class.getSimpleName();
	/**
	 * The widget that shows the state of the EpicService
	 */
	private ServiceStatusWidget mWidget = new ServiceStatusWidget();
	
	/**
	 * A map storing unique session ids with their associated peer (i.e., the jid of the sender)
	 */
	private static final ConcurrentHashMap<String, String> mMapSessionIdToPeer = new ConcurrentHashMap<String, String>(); 


	/**
	 * Sends notifications (about state changes of the service) to activities that are bound via the application interface or the administration interface
	 */
	private static final EpicServiceStateChangeManager mServiceStateChangeManager = new EpicServiceStateChangeManager();

	
	/**
	 * The implementation of the application interface
	 */
	private final IEpicServiceApplicationInterface.Stub mApplicationInterface = new ApplicationInterface(this);

	/**
	 * The implementation of the administration interface
	 */
	private final IEpicServiceAdministrationInterface.Stub mAdministrationInterface = new AdministrationInterface(this); 



	/**
	 * Identifying the version number of the epic standard that is implemented with this service
	 */
	public static final int EPIC_SERVICE_VERSION_NUMBER = 1;

	/**
	 * State change constant: the state changed in an expected way
	 */
	protected static final int STATECHANGE_OK = 0;
	/**
	 * State change constant: the state changed because there is no network connection
	 */
	protected static final int STATECHANGE_NONETWORK = -1;
	/**
	 * State change constant: the state changed because connection to the server is impossible
	 */
	protected static final int STATECHANGE_NOSERVERCONNECTION = -2;
	/**
	 * State change constant: the state changed because the authentication failed 
	 */
	protected static final int STATECHANGE_AUTHFAIL = -3;
	/**
	 * State change constant: the state changed because there was an xmpp stream error
	 */
	protected static final int STATECHANGE_XMPPERROR = -4;
	/**
	 * State change constant: the state changed because the service was stopped by the user
	 */
	protected static final int STATECHANGE_STOP = -5;
	
	/**
	 * The state of the EpicService
	 */
	private static final StateObject mState = new StateObject();
	

	/**
	 * The EpicClient handling the interaction with the XmppServer
	 */
	private static final EpicClient mEpicClient = new EpicClient();;  

	/**
	 * Called when the service is created
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(CLASS_TAG, "service onCreate()" );
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
		
		Log.d(CLASS_TAG, "service onDestroy()" );
		Preferences.log(this, CLASS_TAG, "service created");
		//first check if the user is already registered
		if(Preferences.isRegistered(this)){
			//registered -> try to connect with the network
			handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
		} else{
			//the onstart might be called because the network connectivity changed
			//it would be very disturbing if the user had to register only because of internet connectivity change
			//however we inform the widget and the statechangelisteners that the user is not registered yet:
			int state = EpicServiceState.updateState(mState, EpicService.this, mEpicClient); 
			String logmessage = StateObject.getStateAsHumanReadableString(state);
			mWidget.update(EpicService.this, logmessage);
			mServiceStateChangeManager.sendStateChangeToListeners(state);

		}
	}


	/**
	 * Callback that is registered with the EpicClient. It is receiving messages.
	 */
	private final IncomingMessageCallback mEpicMessageListener = new IncomingMessageCallback() {

		/**
		 * Is called when a new message arrived. The sender is requesting the execution of an epic action. 
		 * Either the epic action is org.epic.action.LaunchApplication and packageName and className are specified, or a different epic action is specified (then the className and the packageName is optional)
		 *  
		 * @param from The jid of the sender
		 * @param action The epic action that shall be performed
		 * @param sessionid The unique id for this communication session or null if no response is necessary 
		 * @param packageName The name of the package that implements the epic action or null if no specific implementation of the epic action is required
		 * @param className The name of the class that implements the epic action or null if no specific implementation of the epic action is required
		 * @param data The data that shall be sent to the application that is performing the epic action, or null if no data is required
		 */
		@Override
		public boolean handleMessage(String from, String action, String sessionid, String packageName, String className, ParameterMap data) {

			if((action==null)||(action.trim().length()==0)){
				return false;
			}
			
			// perform intent resolution
			PackageManager packageManager = getPackageManager();
			// create the intent 
			final Intent mainIntent = new Intent();
			mainIntent.addCategory(Intent.CATEGORY_DEFAULT);
			
			if( action.equalsIgnoreCase("org.epic.action.LaunchApplication")){
				//the application is not a specific epic application
				if((packageName!=null)&&(packageName.trim().length()>0)){
					if((className!=null)&&(className.trim().length()>0)){
						mainIntent.setClassName(packageName, className);
					} else {
						mainIntent.setPackage(packageName);
					}
				}
			} else {
				//the application is a specific epic application
				mainIntent.setAction(action);
				Uri datauri = Uri.parse("epic://"+action+"/"+sessionid);
				mainIntent.setData(datauri);
				
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
			//iPermission = permissionDatabase.getPermissionValue(ConfigurationDatabase.getUniqueId(packageName, className));
			iPermission = permissionDatabase.getPermissionValue(packageName);
			

			// execute permission model
			if(iPermission==ConfigurationDatabase.PERMISSION_UNKNOWN){
				iPermission=ServiceConfigurationActivity.PERMISSION_DEFAULT;
			}
			
			if(iPermission==ConfigurationDatabase.PERMISSION_DISALLOW){
				return true;
			}
			
			if(iPermission==ConfigurationDatabase.PERMISSION_ASK){
				Intent configureintent = new Intent(ServiceConfigurationActivity.INTENTACTION);
				generateNotification("Change permission of package: "+ packageName, "Remote lanch blocked", configureintent);
				return true;
			}
			
			// start activity
			mMapSessionIdToPeer.put(sessionid, from);
			
			if(data!=null){
				//String xml = data.asXml("data");
				//Log.d(CLASS_TAG, xml);
				Bundle dataBundle = BundleAdapter.makeBundle(data);
				mainIntent.putExtras(dataBundle);
				
			}

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


	/** 
	 * Called when the service is started.
	 * The service is either started by an application (respectively the user), or by the NetworkConnectivityStatusReceiver.
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		Log.d(CLASS_TAG, "service onStart()" );
		Preferences.log(this, CLASS_TAG, "service started");
		//first check if the user is already registered
		if(Preferences.isRegistered(this)){
			//registered -> try to connect with the network
			handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
		} else{
			//the onstart might be called because the network connectivity changed
			//it would be very disturbing if the user had to register only because of internet connectivity change
			//however we inform the widget and the statechangelisteners that the user is not registered yet:
			int state = EpicServiceState.updateState(mState, EpicService.this, mEpicClient); 
			String logmessage = StateObject.getStateAsHumanReadableString(state);
			mWidget.update(EpicService.this, logmessage);
			mServiceStateChangeManager.sendStateChangeToListeners(state);

		}
	}
	
	
	private void generateNotification(String msg, String title, Intent intent) {
		int icon = R.drawable.notification_icon;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, title, when);
		notification.setLatestEventInfo(this, title, msg,
				PendingIntent.getActivity(this, 0, intent, 0));
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		
		NotificationManager nm =
			(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(0, notification);
		playNotificationSound();

		
	}
	
	private void playNotificationSound( ) {
		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		if (uri != null) {
			Ringtone rt = RingtoneManager.getRingtone(this, uri);
			rt.setStreamType(AudioManager.STREAM_NOTIFICATION);
			if (rt != null) rt.play();
		}
	}
	
	
	@Override
	public void onDestroy() {
		Log.d(CLASS_TAG, "service onDestroy()" );
		Preferences.log(this, CLASS_TAG, "service destroyed");
	}

	/**
	 * The handler that is called for all (asynchronous) events that potentially could change the state.
	 */
	Handler handleStateChanges = new Handler() {

		/**
		 * Handles the state change.
		 * @param msg A message that must use the correct STATECHANGE constant in the what field (msg.what). All other data of the message is ignored. 
		 */
		public void handleMessage(Message msg) {
			//if other ok messages are pending we delete them
			this.removeMessages(STATECHANGE_OK);
			
			if(msg.what==STATECHANGE_OK){
				stateChange();
			} else {
				switch(msg.what){
				case STATECHANGE_STOP:{
					int state = StateObject.STOPPED;
					mState.setState(state);
					String logmessage = StateObject.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					if(mEpicClient!=null){
						mEpicClient.disconnect();
					}
					
					stopSelf();
					//Log.d(CLASS_TAG, "service stopSelf!!!" );
					break;
				}
				case STATECHANGE_XMPPERROR:{
					int state = StateObject.ERROR_XMPPERROR;
					mState.setState(state);
					String logmessage = StateObject.getStateAsHumanReadableString(state);
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
					int state = StateObject.ERROR_NONETWORK;
					mState.setState(state);
					String logmessage = StateObject.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					//wait for the NetworkConnectivityStatusReceiver to change the state again
					}
					break;
				case STATECHANGE_AUTHFAIL:{
					//inform the widget and the state change listeners
					int state = StateObject.ERROR_AUTHFAIL;
					mState.setState(state);
					String logmessage = StateObject.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					//wait for the MainActivity to start the service again
					}
					break;
				case STATECHANGE_NOSERVERCONNECTION:{
					//inform the widget and the state change listeners
					int state = StateObject.ERROR_NOSERVER;
					mState.setState(state);
					String logmessage = StateObject.getStateAsHumanReadableString(state);
					mWidget.update(EpicService.this, logmessage);
					mServiceStateChangeManager.sendStateChangeToListeners(state);
					//wait for the MainActivity to start the service again
					}
					break;
				}
			}
		};

		/**
		 * Is called of the state change is expected (not an error state) 
		 */
		private void stateChange() {

			int oldState = mState.getState();

			//set the new state
			int state = EpicServiceState.updateState(mState, EpicService.this, mEpicClient); 

			String logmessage = StateObject.getStateAsHumanReadableString(state);

			mWidget.update(EpicService.this, logmessage);
			//send the new state to state listeners
			mServiceStateChangeManager.sendStateChangeToListeners(state);


			if(state==oldState){
				Preferences.log(EpicService.this, CLASS_TAG, "oldstate == newstate");
			}

			//the state changed

			//log the new state 
			logmessage = "New State: " + logmessage;
			Preferences.log(EpicService.this, CLASS_TAG, logmessage);

			//handle the new state
			switch(state){
			case StateObject.INITIALIZING:
				//initEpicClient();
				handleStateChanges.sendEmptyMessage(STATECHANGE_OK);
				break;
			case StateObject.NETWORKCONNECTION:
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
			case StateObject.SERVERCONNECTION:
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
			case StateObject.EPICNETWORKCONNECTION:
				//got epic network connection - nothing to do!  
				break;
			}
		}


		/** 
		 * Initializes a new EpicClient. Must only be called once!
		 
		private void initEpicClient() {
			//create a new client
			
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
		}*/


	};


	/**
	 * Overriding the android.os.Service function onBind. 
	 * Activities can either bind to the application interface by launching a intent with action org.mobilesynergies.EPIC_SERVICE,
	 * or to the administration interface by launching an intent with the action set to the class name of the administration interface.
	 * 
	 * @return Returns the desired interface to the calling activity or null if no appropriate action was specified. 
	 */
	@Override
	public IBinder onBind(Intent intent) {
		// Select the interface to return
		// The application interface
		if (intent.getAction().equals("org.mobilesynergies.EPIC_SERVICE")) {
			return mApplicationInterface;
		}
		// The administration interface
		if (IEpicServiceAdministrationInterface.class.getName().equals(intent.getAction())) {
			return mAdministrationInterface;
		}

		// Unknown action 
		return null;
	}

	/** 
	 * The application can register a callback to be informed about changes of service state.
	 *
	 * @param callback The callback that will be notified about changes
	 */
	public void registerServiceStatusChangeCallback(IServiceStatusChangeCallback callback) {
		Log.d(CLASS_TAG, "service adding statuscallback: "+callback);
		int state = mState.getState();
		Log.d(CLASS_TAG, "the state at this point is: "+StateObject.getStateAsHumanReadableString(state));
		mServiceStateChangeManager.addServiceStatusCallback(callback);
	}


	/**
	 * Call this function to stop the service. Available only to the administration interface 
	 */
	public void stop(){
		handleStateChanges.sendEmptyMessage(STATECHANGE_STOP);
	}

	/**
	 * Send one (of potentially many) reply message to a caller for a performed epic action.
	 * The caller is identified by the sessionid only. If the session id is invalid the message will be ignored.
	 * An activity that wants to send a message needs to specify the appropriate permission in its AndroidManifest file.
	 *
	 * @param action The action that was or is being performed
	 * @param sessionId The session id received from the caller 
	 * @param data The data that is the result of the action
	 */
	public void sendMessage(String action, String sessionId, Bundle data) {
		Log.w(CLASS_TAG, "Trying to send message with sessionId"+ sessionId+".");
		int iPermissionSendMessagesStatus = checkCallingOrSelfPermission(permission.sendmessages);
		if(iPermissionSendMessagesStatus==PackageManager.PERMISSION_DENIED){
			Log.w(CLASS_TAG, "The calling application is missing the permission"+ permission.sendmessages+".");
			return;
		}

		if(mEpicClient==null){
			return;
		}
		
		if(sessionId==null){
			return;
		}
		
		String receiver = mMapSessionIdToPeer.get(sessionId);
		if(receiver==null){
			return;
		}
		ParameterMap map = BundleAdapter.makeParameterMap(data);
		mEpicClient.sendMessage(receiver, action, sessionId, map);
	}


	/**
	 * Retrieve the network nodes that are currently connected to the network
	 * @return The known other nodes
	 * @throws EpicClientException 
	 */
	public NetworkNode[] getNetworkNodes() throws EpicClientException {
		if(mEpicClient==null){
			return null;
		}
		return mEpicClient.getNetworkNodes();		
	}


	/**
	 * Get the commands that are announced by an different node
	 * @param epicNode The other node
	 * @return Infos about the commands
	 * @throws EpicClientException
	 */
	public EpicCommandInfo[] getEpicCommands(String epicNode) throws EpicClientException {
		if(mEpicClient==null){
			return null;
		}
		return mEpicClient.getEpicCommands(epicNode);
	}


	/**
	 * 
	 * @param epicNode
	 * @param command
	 * @param sessionId
	 * @param parametersIn
	 * @param parametersOut
	 * @return
	 * @throws EpicClientException
	 */
	public String executeRemoteCommand(String epicNode, String command,
			String sessionId, Bundle parametersIn,
			Bundle parametersOut) throws EpicClientException {
		if(mEpicClient==null){
			return null;
		}

		ParameterMap indata = BundleAdapter.makeParameterMap(parametersIn);
		ParameterMap outdata = BundleAdapter.makeParameterMap(parametersOut);
		
		return mEpicClient.executeRemoteCommand(epicNode, command,
				sessionId, indata, outdata);
	}

	/**
	 * Register a callback for presence information.
	 * @param callback 
	 */
	public void registerPresenceCallback(PresenceCallback callback) {
		if(mEpicClient==null){
			return;
		}
		mEpicClient.registerPresenceCallback(callback);
	}

	/**
	 * Get the state of the EpicService
	 * @return The state of the EpicService
	 */
	public int getState() {
		int state = mState.getState();
		Log.d(CLASS_TAG, "Service was asked for state: "+StateObject.getStateAsHumanReadableString(state));
		return state;
		
	}





}