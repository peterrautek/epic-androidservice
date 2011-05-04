package org.mobilesynergies.android.epic.service.core;

import org.mobilesynergies.android.epic.service.R;

import org.mobilesynergies.android.epic.service.administration.LogActivity;
import org.mobilesynergies.android.epic.service.administration.ServiceConfigurationActivity;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.android.epic.service.core.states.StateObject;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceAdministrationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceApplicationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;

import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main entry point for the user. 
 * Shows status of the service and allows to start and stop the service.
 * Allows the user to start the LoginActivity and the ServiceConfigurationActivity 
 *   
 * @author Peter
 */

public class MainActivity extends Activity{


	private static final int MENUID_CONFIGURE = 1;
	private static final int MENUID_LOG = 2;

	protected static final int MESSAGEID_PROCESSCRASHED = -333;
	protected static final int MESSAGEID_SERVICENOTRUNNING = -444;

	private static final int REQUESTCODE_LOGINACTIVITY = 123;
	private static final String CLASS_TAG = "MainActivity";   

	private static String EULA = "EULA - End-User Software License Agreement for the 'EPIC Service'-Android application.\n PLEASE CAREFULLY READ THE FOLLOWING LEGAL AGREEMENT (\"AGREEMENT\") FOR THE LICENSE OF THE EPIC SERVICE ANDROID APPLICATION (\"SOFTWARE\"). BY USING THE SOFTWARE, YOU (EITHER AN INDIVIDUAL OR A SINGLE ENTITY) CONSENT TO BE BOUND BY AND BECOME A PARTY TO THIS AGREEMENT. IF YOU DO NOT AGREE TO ALL OF THE TERMS OF THIS AGREEMENT, YOU MUST NOT USE THE SOFTWARE.\n\n1.License Grant.\nThe EPIC SERVICE APPLICATION, belonging to Peter Rautek, grants to you a non-exclusive, non-transferable License to use the Software for beta testing purposes (personal or business) provided you do not remove any of the original proprietary, trademark or copyright markings or notices placed upon or contained with the Software.\n\n2. Term.\nThis Agreement is effective for an interim period whilst the software remains in a beta state. \n\n3. Fees.\nThere is no License fee for the Software whilst being used on a non-profit basis by individuals, non-profit organizations or businesses for beta testing purposes.\n\n4. Warranty Disclaimer.\nTHE SOFTWARE IS PROVIDED ON AN \"AS IS\" BASIS. TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, PETER RAUTEK DISCLAIMS ALL WARRANTIES, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NONINFRINGEMENT. YOU ASSUME RESPONSIBILITY FOR SELECTING THE SOFTWARE TO ACHIEVE YOUR INTENDED RESULTS, AND FOR THE USE OF, AND RESULTS OBTAINED FROM THE SOFTWARE. PETER RAUTEK MAKES NO WARRANTY THAT THE SOFTWARE WILL BE FREE FROM DEFECTS OR THAT THE SOFTWARE WILL MEET YOUR REQUIREMENTS. SOME JURISDICTIONS DO NOT ALLOW LIMITATIONS ON IMPLIED WARRANTIES, SO THE ABOVE LIMITATION MAY NOT APPLY TO YOU.\n\n5. Limitation of Liability.\nTO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, UNDER NO CIRCUMSTANCES AND UNDER NO LEGAL THEORY, WHETHER IN TORT, CONTRACT, OR OTHERWISE, SHALL PETER RAUTEK OR ITS SUPPLIERS OR RESELLERS BE LIABLE TO YOU OR TO ANY OTHER PERSON FOR ANY INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES OF ANY CHARACTER ARISING OUT OF THE USE OF OR INABILITY TO USE THE SOFTWARE, INCLUDING, WITHOUT LIMITATION, DAMAGES FOR LOSS OF GOODWILL, COMPUTER FAILURE OR MALFUNCTION, WORK STOPPAGE OR FOR ANY AND ALL OTHER DAMAGES OR LOSSES. IN NO EVENT WILL PETER RAUTEK BE LIABLE FOR ANY DAMAGES IN EXCESS OF THE LIST PRICE PETER RAUTEK CHARGES FOR A LICENSE TO THE SOFTWARE, EVEN IF PETER RAUTEK SHALL HAVE BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES. SOME JURISDICTIONS DO NOT ALLOW THE EXCLUSION OR LIMITATION OF INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THIS LIMITATION AND EXCLUSION MAY NOT APPLY TO YOU.\n\n6. Further Limitation of Liability.\nTHE SOFTWARE IS NOT DESIGNED FOR USE IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE PERFORMANCE. PETER RAUTEK EXPRESSLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR HIGH-RISK ACTIVITIES. YOU AGREE THAT PETER RAUTEK WILL NOT BE LIABLE FOR ANY CLAIMS OR DAMAGES ARISING FROM THE USE OF THE SOFTWARE IN SUCH APPLICATIONS.\n\n7. Miscellaneous.\nThis Agreement is governed by the law of the Austrian Republic and the parties agree that the sole location and venue for any litigation which may arise hereunder shall be Austria. This Agreement sets forth all rights for the user of the Software and is the entire agreement between the parties. This Agreement may not be modified except by a written addendum issued by a duly authorized representative of Peter Rautek. No provision hereof shall be deemed waived unless such waiver shall be in writing and signed by Peter Rautek or a duly authorized representative of Peter Rautek. If any provision of this Agreement is held illegal or unenforceable by a court having jurisdiction, such provision shall be modified to the extent necessary to render it enforceable without losing its intent, or severed from this Agreement if no such modification is possible, and the remainder of this Agreement shall continue in full force and effect. The parties confirm that it is their wish that this Agreement has been written in the English language only.";

	private int mCurrentState = StateObject.UNKNOWN;


	Handler handlerUpdateUi = new Handler(){
		public void handleMessage(android.os.Message msg) {
			updateUi(mCurrentState);
		};
	};

	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		boolean firstrun = Preferences.isFirstRun(MainActivity.this);
		if (firstrun) {
			new AlertDialog.Builder(MainActivity.this)
			.setTitle("Welcome to the EPIC Service!")
			.setMessage("EPIC is a protocol that lets your phone talk to other devices (like your PC). " +
					"To use this software you must agree to the following End User Licence Agreement (EULA)." +
					"\n\n\n"+EULA).setNeutralButton("Agree", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Preferences.setIsFirstRun(MainActivity.this, false);
						}
					})
					.show();
		}

		TextView tvHelp = (TextView)findViewById(R.id.TextViewHelp);
		tvHelp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				String text = MainActivity.this.getInternalStateAndHintMessage(mCurrentState);
				new AlertDialog.Builder(MainActivity.this)
				.setTitle("Help")
				.setMessage(text).setNeutralButton("OK", null)
				.show();
			}
		});

		TextView tvMailUrl = (TextView)findViewById(R.id.TextViewMailUrl);
		tvMailUrl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new AlertDialog.Builder(MainActivity.this)
				.setTitle("Send link via email")
				.setMessage("You need to register and login from your PC to try the EPIC demo applications.").setNeutralButton("Send me the link!", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						Intent i = new Intent(Intent.ACTION_SEND);
						i.setType("message/rfc822");
						i.putExtra(Intent.EXTRA_SUBJECT, "EPIC: link to the webpage!");
						i.putExtra(Intent.EXTRA_TEXT, "Go to: http://www.mobilesynergies.org/. Enjoy the EPIC services! ");
						startActivity(Intent.createChooser(i, "Email link"));  
					}
				})
				.show();
			}
		});


	}

	@Override
	protected void onResume() {
		super.onResume();
		updateUi(mCurrentState);
		startEpicService();
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
		//menu.add(0,MENUID_LOG,0,"Log messages");

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


	private void updateUi(int state) {
		Log.d(CLASS_TAG, "got informed of state: " + StateObject.getStateAsHumanReadableString(mCurrentState));
		mCurrentState=state;
		if(mEpicService!=null){
			try {
				mCurrentState = mEpicService.getState();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Log.d(CLASS_TAG, "switched to state: " + StateObject.getStateAsHumanReadableString(mCurrentState));

		TextView tvState = (TextView) findViewById(R.id.textviewState);
		tvState.setText(getInternalStateMessage(mCurrentState));

		TextView tvAction = (TextView) findViewById(R.id.TextViewActionArea);
		tvAction.setText("");

		Button buttonAction = (Button) findViewById(R.id.buttonAction);

		switch(state){
		case MESSAGEID_PROCESSCRASHED:{
			tvAction.setText("Please restart the service!");
			buttonAction.setText("Start Service");
			buttonAction.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					handleEpicServiceStart.sendEmptyMessage(0);
				}
			});
			break;
		} 
		case MESSAGEID_SERVICENOTRUNNING:{
			buttonAction.setText("Start Service");
			tvAction.setText("The service was stopped! To make use of it again please restart the service!");
			buttonAction.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					handleEpicServiceStart.sendEmptyMessage(0);
				}
			});
			break;
		}
		case StateObject.EPICNETWORKCONNECTION:{
			tvAction.setText("If you don't want to use the EPIC service anymore, you might stop it! You will not be able to use the service from your PC anymore!");
			buttonAction.setText("Stop Service");
			buttonAction.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					handleEpicServiceStop.sendEmptyMessage(0);
				}
			});
			break;
		} 
		case StateObject.NOUSERCREDENTIALS:{
			tvAction.setText("Please login!");
			buttonAction.setText("Login");
			buttonAction.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(LoginActivity.INTENTACTION);
					startActivityForResult(intent, REQUESTCODE_LOGINACTIVITY);
				}
			});
			break;
		}
		case StateObject.ERROR_AUTHFAIL:{
			tvAction.setText("Your username or password is incorrect! Please try to login again!");
			buttonAction.setText("Login");
			buttonAction.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(LoginActivity.INTENTACTION);
					startActivityForResult(intent, REQUESTCODE_LOGINACTIVITY);
				}
			});
			break;
		}
		case StateObject.ERROR_NOSERVER:{
			tvAction.setText("The server is not reachable. Maybe your internet connection is broken, or server is not working. Also the server name, service name or server port might be wrongly configured! Please check your internet connection, and try to configure the server settings correctly!");
			buttonAction.setText("Login");
			buttonAction.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(LoginActivity.INTENTACTION);
					startActivityForResult(intent, REQUESTCODE_LOGINACTIVITY);
				}
			});
			break;
		}
		default:{
			if(mEpicService==null){
				buttonAction.setText("Start Service");
				tvAction.setText("Start the service to make use of it!");
				buttonAction.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						handleEpicServiceStart.sendEmptyMessage(0);
					}
				});
			} else {

				tvAction.setText("The EPIC service is not working correctly! You might try to stop and restart it!");
				buttonAction.setText("Stop Service");
				buttonAction.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						handleEpicServiceStop.sendEmptyMessage(0);
					}
				});




			}


		}
		} 


	}


	Handler handleEpicServiceStop = new Handler(){
		public void handleMessage(android.os.Message msg) {
			stopEpicService();
		};
	};

	void stopEpicService(){
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
		mStateChangeHandler.sendEmptyMessage(MESSAGEID_SERVICENOTRUNNING);
	}



	Handler handleEpicServiceStart = new Handler(){
		public void handleMessage(android.os.Message msg) {
			startEpicService();	
		};
	};


	private void startEpicService() {
		Log.d(CLASS_TAG, "start service..." );
		//String strServiceName = IEpicServiceApplicationInterface.class.getName();
		Intent intent = new Intent("org.mobilesynergies.android.epic.service.interfaces.IEpicServiceAdministrationInterface");
		if(bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)){
			Log.d(CLASS_TAG, "bind service!!!" );
		} else {
			mStateChangeHandler.sendEmptyMessage(MESSAGEID_SERVICENOTRUNNING);
			Log.d(CLASS_TAG, "bind service failed!!!" );
		}

	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		handlerUpdateUi.sendEmptyMessage(0);
		if(requestCode==REQUESTCODE_LOGINACTIVITY){
			stopEpicService();
			//restart the service
			startEpicService();
		}
	}


	Handler mStateChangeHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			mCurrentState = msg.what;
			updateUi(mCurrentState);
		};

	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {


		public void onServiceConnected(ComponentName className, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service.  We are communicating with our
			// service through an IDL interface, so get a client-side
			// representation of that from the raw service object.
			//Toast.makeText(MainActivity.this, "The epic service process was started sucessfully!", Toast.LENGTH_LONG).show();
			Log.d(CLASS_TAG, "service connected!!!" );
			mEpicService = (IEpicServiceAdministrationInterface) IEpicServiceAdministrationInterface.Stub.asInterface(service);
			mIsBound = true;
			try {
				Log.d("MainActivity", "registering status change callback " + mServiceStatusChangeCallback);
				mEpicService.registerServiceStatusChangeCallback(mServiceStatusChangeCallback);
				int state = mEpicService.getState();
				MainActivity.this.mStateChangeHandler.sendEmptyMessage(state);
				
			} catch (RemoteException e) {
				MainActivity.this.mStateChangeHandler.sendEmptyMessage(MESSAGEID_SERVICENOTRUNNING);
				e.printStackTrace();
			}


		}



		public void onServiceDisconnected(ComponentName className) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			Log.d(CLASS_TAG, "service disconnected!!!" );
			mEpicService = null;
			mIsBound=false;
			//onDisconnected();
			if(mCurrentState!=StateObject.STOPPED){
				MainActivity.this.mStateChangeHandler.sendEmptyMessage(MESSAGEID_PROCESSCRASHED);
			}
		}
	};





	private MyServiceStatusChangeCallback mServiceStatusChangeCallback = new MyServiceStatusChangeCallback();

	private class MyServiceStatusChangeCallback extends IServiceStatusChangeCallback.Stub{

		@Override
		public void onServiceStatusChanged(int state)	throws RemoteException {
			Log.d(CLASS_TAG, "got informed of state change" );
			//this stuff doesn't work currently for a service that runs in its own process
			MainActivity.this.mStateChangeHandler.sendEmptyMessage(state);
			
		}
		

		
	};



	protected IEpicServiceAdministrationInterface mEpicService = null;
	boolean mIsBound = false;

	protected String getInternalStateAndHintMessage(int internalState) {
		String statemessage = "";
		String hintmessage = "";
		if(internalState == MESSAGEID_PROCESSCRASHED){
			statemessage = "the service process crashed unexpectedly";
			hintmessage = "please try to restart the service";
		} else if (internalState == MESSAGEID_SERVICENOTRUNNING){
			statemessage = "epic service not running";
			hintmessage = "try to restart the service";
		} else {
			statemessage = StateObject.getStateAsHumanReadableString(internalState);
			hintmessage = StateObject.getStateHint(internalState); 
		}
		String message = "The EPIC service is in the following state:\n"+statemessage+" \nHint: "+hintmessage;
		if(internalState == StateObject.EPICNETWORKCONNECTION){
			String username = Preferences.getUserName(MainActivity.this);
			message = message + "\nYou are logged in as: "+username;
		}
		return message;
	}

	protected String getInternalStateMessage(int internalState) {
		String statemessage = "State: ";
		if(internalState == MESSAGEID_PROCESSCRASHED){
			statemessage = statemessage + "the epic service process crashed unexpectedly";
		} else if (internalState == MESSAGEID_SERVICENOTRUNNING){
			statemessage = statemessage + "the epic service process is not running";
		} else {
			statemessage = statemessage + StateObject.getStateAsHumanReadableString(internalState);
		}

		if(internalState == StateObject.EPICNETWORKCONNECTION){
			String username = Preferences.getUserName(MainActivity.this);
			statemessage = statemessage + "\nYou are logged in as: "+username;
		}
		return statemessage;
	}




}