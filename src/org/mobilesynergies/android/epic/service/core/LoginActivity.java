package org.mobilesynergies.android.epic.service.core;

import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.administration.ServiceAdministrationActivity;
import org.mobilesynergies.android.epic.service.core.IntentIntegrator;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Allows the user to enter the username and password either manually or via scanning a QrCode. 
 * The QrCode must encode the user credentials in the form username:password@xmppservice
 * The Menu options of this activity allow the configuration of the xmpp server, the xmpp service name and the port of the xmpp server.
 *   
 * @author Peter
 */
public class LoginActivity extends Activity{

	protected static final String INTENTACTION = "org.mobilesynergies.android.epic.service.login";

	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.login);

		updateUI();
		//showCustomDialog(DIALOG_DEVICE);
	}

	Dialog mDialog = null;
	int mDialogId = -1;
	/*	@Override
	protected Dialog onCreateDialog(int id) {
		Context mContext = getApplicationContext();
		mDialog = new Dialog(mContext);
		mDialog.setContentView(R.layout.dialogwithedittext);
		return mDialog;//super.onCreateDialog(id);
	}
	 */
	static final int DIALOG_SERVER = 1;
	static final int DIALOG_SERVICE = 2;
	static final int DIALOG_DEVICE = 3;
	static final int DIALOG_PORT = 4;
	protected static final String CLASS_TAG = "LoginActivity";


	protected void showCustomDialog(int id) {

		if(mDialog!=null)
		{
			mDialog.dismiss();
		}
		
		mDialog = new Dialog(this);
		mDialog.setContentView(R.layout.dialogwithedittext);
		mDialogId = id;

		String message = "";
		switch(id){
		case DIALOG_SERVER:{
			mDialog.setTitle("Configure Servername or IP");
			message = Preferences.getConfiguredServerName(LoginActivity.this);
			break;
		}
		case DIALOG_SERVICE:{
			mDialog.setTitle("Configure Service Name");	
			message = Preferences.getConfiguredServiceName(LoginActivity.this);
			break;
		}
		case DIALOG_PORT:{
			mDialog.setTitle("Configure XMPP Server Port");	
			message = String.valueOf(Preferences.getConfiguredPort(LoginActivity.this));
			break;
		}
		case DIALOG_DEVICE:{
			mDialog.setTitle("Configure Device Name");	
			message = Preferences.getConfiguredDeviceName(LoginActivity.this);
			break;
		}
		};

		EditText edittext = (EditText) mDialog.findViewById(R.id.menu_edittext);
		edittext.setText(message);

		edittext.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				EditText me = (EditText)v;
				String text = me.getText().toString().trim();
				String message = "Saved ";
				switch(mDialogId){
				case DIALOG_SERVER:{
					Preferences.setConfiguredServerName(LoginActivity.this, text);
					message = message + "server name: "+text;
					break;
				}
				case DIALOG_SERVICE:{
					Preferences.setConfiguredServiceName(LoginActivity.this, text);
					message = message + "service name: "+text;
					break;
				}
				case DIALOG_PORT:{
					Preferences.setConfiguredPort(LoginActivity.this, Integer.parseInt(text));
					message = message + "port: "+text;
					break;
				}
				case DIALOG_DEVICE:{
					Preferences.setConfiguredDeviceName(LoginActivity.this, text);
					message = message + "device name: "+text;
					break;
				}
				}
				Log.d(CLASS_TAG, message);
				return false;
			}
		});
		mDialog.show();
	}


	public boolean onCreateOptionsMenu(Menu menu){

		menu.add(0,DIALOG_SERVER,0,"Configure Server Name");
		menu.add(0,DIALOG_PORT,0,"Configure Port");
		menu.add(0,DIALOG_SERVICE,0,"Configure Service Name");
		menu.add(0,DIALOG_DEVICE,0,"Configure Device Name");


		return true;

	}


	public boolean onOptionsItemSelected (MenuItem item){
		int id = item.getItemId();
		showCustomDialog(id);
		return true;
	}

	@Override
	protected void onPause() {
		if(mDialog!=null)
		{
			mDialog.dismiss();
		}

		super.onPause();
	}

	private void updateUI() {

		Button bsave = (Button) findViewById(R.id.buttondone);
		Button bqrcode = (Button) findViewById(R.id.button_qrcode);
		
		EditText etusername = (EditText) findViewById(R.id.edittextusername);
		EditText etpassword = (EditText) findViewById(R.id.edittextpassword);


		bqrcode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				//the activity calling the initiate scan MUST NOT be of type
				//SingleInstance. otherwise the qr-code scanning doesn't work!
				//documented under known issues (17.04.2011) at
				//http://code.google.com/p/zxing/wiki/ScanningViaIntent
				IntentIntegrator.initiateScan(LoginActivity.this);

			}
		});

		bsave.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				EditText etusername = (EditText) findViewById(R.id.edittextusername);
				EditText etpassword = (EditText) findViewById(R.id.edittextpassword);

				String user = etusername.getText().toString();
				String password = etpassword.getText().toString();
				Preferences.setUserName(LoginActivity.this, user);
				Preferences.setUserPassword(LoginActivity.this, password);
				setResult(Activity.RESULT_OK);
				finish();
			}
		});


		


		String user = Preferences.getUserName(this);
		String password = Preferences.getUserPassword(this);

		if(user.length()>0){
			etusername.setText(user);
		}
		if(password.length()>0){
			etpassword.setText(password);
		}


	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {

		/*if(resultCode!=Activity.RESULT_OK)
			return;
		 */
		if(IntentIntegrator.REQUEST_CODE!=requestCode)
			return;

		if(resultCode==RESULT_CANCELED)
			return;

		IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);


		if (scanResult != null) {
			String result = scanResult.getContents();
			
			if (result == null){
				Toast.makeText(this, "Scanning the QrCode didn't work. Please try entering the username and password manually.", Toast.LENGTH_LONG).show();
			}else {
				Toast.makeText(this, result, Toast.LENGTH_LONG).show();

				String[] userandpasswordandservice = result.split("@");
				if((userandpasswordandservice!=null)&&(userandpasswordandservice.length<2)){
					
					Toast.makeText(this, "Scanning the QrCode didn't work. Please try entering the username and password manually. Cause: Invalid id (no @ symbol found)", Toast.LENGTH_LONG).show();
				} else {
					Preferences.setConfiguredServiceName(this, userandpasswordandservice[1]);
					String[] userandpassword = userandpasswordandservice[0].split(":");
					if((userandpassword!=null)&&(userandpassword.length!=2)){
						Toast.makeText(this, "Scanning the QrCode didn't work. Please try entering the username and password manually. Cause: invalid id (no : symbol found)", Toast.LENGTH_LONG).show();
					} else {
						EditText etusername = (EditText) findViewById(R.id.edittextusername);
						EditText etpassword = (EditText) findViewById(R.id.edittextpassword);
						etusername.setText(userandpassword[0]);
						etpassword.setText(userandpassword[1]);
						Preferences.setUserName(this, userandpassword[0]);
						Preferences.setUserPassword(this, userandpassword[1]);
					}
				}
			}
		}
		else {
			Toast.makeText(this, "No scan result returned! Please try again or enter the username and password manually.", Toast.LENGTH_LONG).show();
		}

	}


}