package org.mobilesynergies.android.epic.service.core;

import org.mobilesynergies.android.epic.service.EpicService;
import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.administration.ExploreEpicNetwork;
import org.mobilesynergies.android.epic.service.administration.LogActivity;
import org.mobilesynergies.android.epic.service.administration.ServiceAdministrationActivity;
import org.mobilesynergies.android.epic.service.core.IntentIntegrator;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceStateChangeManager;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceAdministrationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceApplicationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.ParameterMapImpl;
import org.mobilesynergies.epic.client.EpicClient;
import org.mobilesynergies.epic.client.EpicClientException;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity
 *   
 * @author Peter
 */

public class MainActivity extends Activity{

	
	protected static final int MESSAGEID_SERVER = 1;
	protected static final int MESSAGEID_AUTH = 2;
	protected static final int MESSAGEID_SUCCESS = 3;
	protected static final int MESSAGEID_PROCESSCRASHED = 4;
	protected static final int MESSAGEID_AUTHENTICATED = 5;
	protected static final int MESSAGEID_INTERNETCONNECTED = 6;
	protected static final int MESSAGEID_SERVERCONNECTED = 7;
	protected static final int MESSAGEID_UNCONNECTED = 8;
	protected static final int MESSAGEID_UNKNOWN = 9;
	protected static final int MESSAGEID_SERVICENOTRUNNING = 10;

	Handler handlerUpdateUi = new Handler(){
		public void handleMessage(android.os.Message msg) {
			updateUI();
		};
	};
	
	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		
		updateUI();
		testLogin();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(mIsBound){
	        // Detach our existing connection.
	        unbindService(mServiceConnection);
	        mIsBound = false;
		}
	}

	private final static int MENUID_EXPLORENETWORK = 1;
	private static final int MENUID_LOG = 2;

	public boolean onCreateOptionsMenu(Menu menu){

		menu.add(0,MENUID_EXPLORENETWORK,0,"Explore Network");
		menu.add(0,MENUID_LOG,0,"Log messages");

		return true;

	}


	public boolean onOptionsItemSelected (MenuItem item){
		int id = item.getItemId();
		switch(id){
		case MENUID_EXPLORENETWORK:
			Intent exploreintent = new Intent(ExploreEpicNetwork.INTENTACTION);
			startActivity(exploreintent);
			break;
		
		case MENUID_LOG:
			Intent logintent = new Intent(LogActivity.INTENTACTION);
			startActivity(logintent);
			break;
		}
		return true;
	}


	private void updateUI() {
		
		Button blogin = (Button) findViewById(R.id.buttonLogIn);
		
		blogin.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LoginActivity.INTENTACTION);
				
				startActivityForResult(intent, 0);
			}
		});
		
		TextView tvUser = (TextView) findViewById(R.id.textviewusername);
		String username = Preferences.getUserName(this);
		if(username.length()>0){
			tvUser.setText("Welcome "+username);
		} else {
			tvUser.setText("Welcome! Please log in!");
		}


	
	}
	
	

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		handlerUpdateUi.sendEmptyMessage(0);
		testLogin();
	}




	private void testLogin() {
		//TODO if connected -> return
		
		String username = Preferences.getUserName(this);
		String password = Preferences.getUserPassword(this);
		
		if((username.length()==0)||(password.length()==0)){
			Toast.makeText(this, "Please provide your username and password (press Login).", Toast.LENGTH_LONG).show();
		}else {
			Thread tTestLogin = new Thread(){

			@Override
			public void run() {
				EpicClient client = new EpicClient();
				boolean isConnected = false;
				try {
					String server = Preferences.getConfiguredServerName(MainActivity.this);
					String service = Preferences.getConfiguredServiceName(MainActivity.this);
					int port = Preferences.getConfiguredPort(MainActivity.this);
					isConnected = client.establishConnection(server, port, service);
				} catch (EpicClientException e) {
					e.printStackTrace();
				}
				if(!isConnected){
					mToastHandler.sendEmptyMessage(MESSAGEID_SERVER);
				}	else {
					String username = Preferences.getUserName(MainActivity.this);
					String password = Preferences.getUserPassword(MainActivity.this);
					String device = Preferences.getConfiguredDeviceName(MainActivity.this);
					boolean isAuth = false;
					
					try {
						isAuth = client.authenticateUser(username, password, device);
						
					} catch (EpicClientException e) {
						e.printStackTrace();
					}
					if(!isAuth){
						mToastHandler.sendEmptyMessage(MESSAGEID_AUTH);
						
					}	else {
						mToastHandler.sendEmptyMessage(MESSAGEID_SUCCESS);
						mServiceStartHandler.sendEmptyMessage(0);
					}

				}
				client.disconnect();
				
				
			}
			
		};
		
		tTestLogin.start();
		}
	}


	Handler mServiceStartHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			//String strServiceName = IEpicServiceApplicationInterface.class.getName();
			Intent intent = new Intent("org.mobilesynergies.EPIC_SERVICE");
			
			if(bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)){
				
			} else {
				Toast.makeText(MainActivity.this, "Could not start the epic service process!", Toast.LENGTH_LONG).show();
			}
		}
			
	};
	
	
	Handler mToastHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			String message = "";
			switch(msg.what){
			case MESSAGEID_SERVER:
				message = "The connection to the server failed. Make sure you are connected to the internet and your settings are correct (settings can be adjusted under Login->Menu).";
				break;
			case MESSAGEID_AUTH:
				message = "The username or password are not correct. Make sure you use the same credentials as registered on the website (press Login to adjust credentials).";
				break;
			case MESSAGEID_SUCCESS:
				message = "Login verified!";
				break;
			case MESSAGEID_PROCESSCRASHED:
				message = "The epic service process crashed unexpectedly.";
				break;
			case MESSAGEID_AUTHENTICATED:
				message = "The epic service logged in. Success!";
				try {
					mEpicService.sendMessage("lalalal", null, new ParameterMapImpl());
				} catch (RemoteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case MESSAGEID_INTERNETCONNECTED:
				message = "The epic service is connected to the internet connection.";
				break;
			case MESSAGEID_SERVERCONNECTED:
				message = "The epic service is connected to the server.";
				break;
			case MESSAGEID_UNCONNECTED:
				message = "The epic service is not connected (probably no internet connection).";
				break;
			case MESSAGEID_UNKNOWN:
				message = "Service is in an unknown state.";
				break;
			case MESSAGEID_SERVICENOTRUNNING:
				message = "Failed to start the epic service process.";
				break;

			
			}
			Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
		};
		
	};
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		

		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			Toast.makeText(MainActivity.this, "The epic service process was started sucessfully!", Toast.LENGTH_LONG).show();
			mEpicService = (IEpicServiceApplicationInterface) IEpicServiceApplicationInterface.Stub.asInterface(service);
			mIsBound = true;
			try {
				mEpicService.registerServiceStatusChangeCallback(mServiceStatusChangeCallback);
			} catch (RemoteException e) {
				MainActivity.this.mToastHandler.sendEmptyMessage(MESSAGEID_SERVICENOTRUNNING);
				e.printStackTrace();
			}
			//onConnected();
			
		}

		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			mEpicService = null;
			mIsBound=false;
			//onDisconnected();
			MainActivity.this.mToastHandler.sendEmptyMessage(MESSAGEID_PROCESSCRASHED);
		}
	};
	
	
	private IServiceStatusChangeCallback mServiceStatusChangeCallback = new IServiceStatusChangeCallback(){

		@Override
		public void onServiceStatusChanged(int status)
				throws RemoteException {
			if(status==EpicServiceState.EPICNETWORKCONNECTION) {
				//onConnectedToEpicNetwork();
				MainActivity.this.mToastHandler.sendEmptyMessage(MESSAGEID_AUTHENTICATED);
			}
			else if(status==EpicServiceState.NETWORKCONNECTION) {
				//onConnectedToEpicNetwork();
				MainActivity.this.mToastHandler.sendEmptyMessage(MESSAGEID_INTERNETCONNECTED);
			}
			else if(status==EpicServiceState.SERVERCONNECTION) {
				//onConnectedToEpicNetwork();
				MainActivity.this.mToastHandler.sendEmptyMessage(MESSAGEID_SERVERCONNECTED);
			} 
			else if(status==EpicServiceState.NONETWORKCONNECTION) {
				//onConnectedToEpicNetwork();
				MainActivity.this.mToastHandler.sendEmptyMessage(MESSAGEID_UNCONNECTED);
			} 
			else {
				//onConnectedToEpicNetwork();
				MainActivity.this.mToastHandler.sendEmptyMessage(MESSAGEID_UNKNOWN);
			} 


		}

		@Override
		public IBinder asBinder() {
			return null;
		}
		
	};

	
	
	protected IEpicServiceApplicationInterface mEpicService = null;
	boolean mIsBound = false;



}