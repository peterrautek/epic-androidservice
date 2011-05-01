package org.mobilesynergies.android.epic.service.core;

import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.administration.LogActivity;
import org.mobilesynergies.android.epic.service.administration.ServiceConfigurationActivity;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceApplicationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;

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
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity
 *   
 * @author Peter
 */

public class MainActivity extends Activity{


	private static final int MENUID_CONFIGURE = 1;
	private static final int MENUID_LOG = 2;

	protected static final int MESSAGEID_PROCESSCRASHED = -333;
	protected static final int MESSAGEID_SERVICENOTRUNNING = -444;

	private static final int REQUESTCODE_LOGINACTIVITY = 123;   

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
	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUI();
		startService();
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


	public boolean onCreateOptionsMenu(Menu menu){

		menu.add(0,MENUID_CONFIGURE,0,"Configure Application Permissions");
		menu.add(0,MENUID_LOG,0,"Log messages");

		return true;

	}


	public boolean onOptionsItemSelected (MenuItem item){
		int id = item.getItemId();
		switch(id){
		case MENUID_CONFIGURE:
			Intent configureintent = new Intent(ServiceConfigurationActivity.INTENTACTION);
			startActivity(configureintent);
			break;

		case MENUID_LOG:
			Intent logintent = new Intent(LogActivity.INTENTACTION);
			startActivity(logintent);
			break;
		}
		return true;
	}


	private void updateUI() {
		mState = (TextView) findViewById(R.id.textviewusername);
		Button blogin = (Button) findViewById(R.id.buttonLogIn);
		blogin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(LoginActivity.INTENTACTION);

				startActivityForResult(intent, REQUESTCODE_LOGINACTIVITY);
			}
		});
	}



	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		handlerUpdateUi.sendEmptyMessage(0);
		if(requestCode==REQUESTCODE_LOGINACTIVITY){

			
			
			
			if(mIsBound){
				try {
					mEpicService.stop();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				// Detach our existing connection.
				unbindService(mServiceConnection);
				mIsBound = false;
			}
			
			mEpicService=null;
		}
		//restart the service
		startService();
	}




	private void startService() {

		if(mEpicService==null){
			//connect to the service
			mServiceStartHandler.sendEmptyMessage(0);
		} else {
			//we can get the state from the service
			try {
				int state = mEpicService.getState();
				mStateChangeHandler.sendEmptyMessage(state);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				mStateChangeHandler.sendEmptyMessage(MESSAGEID_SERVICENOTRUNNING);
			}

		}

	}


	Handler mServiceStartHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			//String strServiceName = IEpicServiceApplicationInterface.class.getName();
			Intent intent = new Intent("org.mobilesynergies.EPIC_SERVICE");
			if(bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)){

			} else {
				mStateChangeHandler.sendEmptyMessage(MESSAGEID_SERVICENOTRUNNING);
			}
		}

	};


	Handler mStateChangeHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			String statemessage = "";
			String hintmessage = "";
			if(msg.what == MESSAGEID_PROCESSCRASHED){
				statemessage = "the epic service process crashed unexpectedly";
				hintmessage = "please try to restart the service";
			} else if (msg.what == MESSAGEID_SERVICENOTRUNNING){
				statemessage = "failed to start the epic service process";
				hintmessage = "please try to restart the service";
			} else {
				statemessage = EpicServiceState.getStateAsHumanReadableString(msg.what);
				hintmessage = EpicServiceState.getStateHint(msg.what); 
			}

			String message = "State: "+statemessage+" \nHint: "+hintmessage;

			if(msg.what == EpicServiceState.EPICNETWORKCONNECTION){
				String username = Preferences.getUserName(MainActivity.this);
				message = message + "\nYou are logged in as: "+username;
			}

			mState.setText(message);

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
				int state = mEpicService.getState();
				MainActivity.this.mStateChangeHandler.sendEmptyMessage(state);
			} catch (RemoteException e) {
				MainActivity.this.mStateChangeHandler.sendEmptyMessage(MESSAGEID_SERVICENOTRUNNING);
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
			MainActivity.this.mStateChangeHandler.sendEmptyMessage(MESSAGEID_PROCESSCRASHED);
		}
	};






	private IServiceStatusChangeCallback mServiceStatusChangeCallback = new IServiceStatusChangeCallback(){

		@Override
		public void onServiceStatusChanged(int state)	throws RemoteException {
			MainActivity.this.mStateChangeHandler.sendEmptyMessage(state);
		}

		@Override
		public IBinder asBinder() {
			return null;
		}

	};


	TextView mState = null;
	protected IEpicServiceApplicationInterface mEpicService = null;
	boolean mIsBound = false;



}