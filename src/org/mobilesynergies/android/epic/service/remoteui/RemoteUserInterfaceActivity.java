package org.mobilesynergies.android.epic.service.remoteui;

import org.mobilesynergies.android.epic.service.administration.ServiceAdministrationActivity;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.epic.client.remoteui.Parameter;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.RelativeLayout.LayoutParams;

public class RemoteUserInterfaceActivity extends ServiceAdministrationActivity {
	
	private static final String CLASS_TAG = RemoteUserInterfaceActivity.class.getName();
	
	public static final String EXTRAS_NODE_FULLADDRESS = "address";
	public static final String EXTRAS_COMMAND_ID = "commandid";
	public static final String EXTRAS_COMMAND_HUMANREADABLENAME = "commandname";

	protected static final int ID_MAINVIEW = 41321234;
	
	private String mAddress;
	private String mCommand;
	private Bundle mCurrentDataBundle = null;
	private String mSessionId = null;

	protected UiGenerator mCurrentUiGenerator;
	
	
	/*ProgressDialog dd = null;
		
	private Handler handlerDisableUi = new Handler(){
		public void handleMessage(Message msg) {
			if(msg.what==0){
				//disable UI
				View view = findViewById(ID_MAINVIEW);
				
				if(view!=null){
					view.setEnabled(false);
					dd = ProgressDialog.show(RemoteCommandActivity.this, "aaaa", "sdfsd", true, false);
				}
				
			} else if (msg.what==1){
				//enable UI
				
				View view = findViewById(ID_MAINVIEW);
				if(view!=null){
					view.setEnabled(true);
					if(dd!=null){
						dd.dismiss();
					}
				}
				
				
				
			}
				
			
		};
	
	};*/
	
	private Handler handlerInitStage = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(mCurrentDataBundle==null) {
				//TODO change the ui to some error or some finished screen
				return;
			}
			
			View view = mCurrentUiGenerator.initializeUi(RemoteUserInterfaceActivity.this, BundleAdapter.makeParameterMap(mCurrentDataBundle));
			view.setId(ID_MAINVIEW);
						
			
			LinearLayout linearLayoutButtons = new LinearLayout(RemoteUserInterfaceActivity.this);
			
			linearLayoutButtons.setId(1234567890);
			linearLayoutButtons.setOrientation(LinearLayout.HORIZONTAL);

			Button buttonCancel = new Button(RemoteUserInterfaceActivity.this);
			buttonCancel.setMinimumWidth(100);
			buttonCancel.setText("Cancel");
			buttonCancel.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					mCurrentDataBundle = null;
					mCurrentUiGenerator = null;
					finish();					
				}
				
			});
			linearLayoutButtons.addView(buttonCancel);
			
			Button buttonOk = new Button(RemoteUserInterfaceActivity.this);
			buttonOk.setMinimumWidth(100);
			buttonOk.setText("Submit");
			buttonOk.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					ParameterMap map = null;					
					try {
						map = mCurrentUiGenerator.getValues();
						executeCommand(map);
					} catch (Exception e) {
						Toast.makeText(RemoteUserInterfaceActivity.this, "An unrecoverable error occured during the execution of this command.", Toast.LENGTH_LONG).show();
						e.printStackTrace();
						finish();
					}
				}
			});
			linearLayoutButtons.addView(buttonOk);
			
			RelativeLayout layoutMain = new RelativeLayout(RemoteUserInterfaceActivity.this);
			
			LayoutParams bottomParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		    bottomParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		    layoutMain.addView(linearLayoutButtons, bottomParams);
		    
		    LayoutParams midParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		    midParams.addRule(RelativeLayout.ABOVE, linearLayoutButtons.getId());
		    layoutMain.addView(view, midParams);
		    
			setContentView(layoutMain);		
						
						
		}
	};

	
	private Handler handlerChangeStage = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if(mCurrentDataBundle==null) {
				//TODO change the ui to some error or some finished screen
				return;
			}
			
			//mCurrentUiGenerator = new UiGenerator();
			try {
				mCurrentUiGenerator.updateUi(BundleAdapter.makeParameterMap(mCurrentDataBundle));
			} catch (Exception e) {
				Toast.makeText(RemoteUserInterfaceActivity.this, "An unrecoverable error occured during the execution of this command.", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
						
						
		}
	};

	
	
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//TODO set contentview to some screen telling the user that the ui is not ready yet
		//maybe also show a progress bar
		Intent callingIntent = getIntent(); 
		if(callingIntent!=null){
			mAddress = callingIntent.getStringExtra(EXTRAS_NODE_FULLADDRESS);
			mCommand = callingIntent.getStringExtra(EXTRAS_COMMAND_ID);
			String name = callingIntent.getStringExtra(EXTRAS_COMMAND_HUMANREADABLENAME);
			this.setTitle(name);
		}
		
		mCurrentUiGenerator = new UiGenerator();
		mCurrentUiGenerator.setSubmitActionListener(new SubmitActionListener(){

			@Override
			public void onSubmitAction(String variable) {
				
				try {
					Parameter parameter = mCurrentUiGenerator.getValue(variable);
					ParameterMap map = new ParameterMap();
					map.putParameter(variable, parameter);
					executeCommand(map);
				} catch (Exception e) {
					Toast.makeText(RemoteUserInterfaceActivity.this, "An unrecoverable error occured during the execution of this command.", Toast.LENGTH_LONG).show();
					e.printStackTrace();
					finish();
				}					
			}

			@Override
			public void onSubmitAction() {
				ParameterMap map = null;
				try {
					map = mCurrentUiGenerator.getValues();
					executeCommand(map);
				} catch (Exception e) {
					Toast.makeText(RemoteUserInterfaceActivity.this, "An unrecoverable error occured during the execution of this command.", Toast.LENGTH_LONG).show();
					e.printStackTrace();
					finish();
				}					
			}
			
		});

		
	}

	@Override
	protected void onConnected() {
		int state = EpicServiceState.UNKNOWN;
		try {
			state = mEpicService.getState();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(EpicServiceState.EPICNETWORKCONNECTION == state){
			executeCommand(null);
		}
	}

	@Override
	protected void onConnectedToEpicNetwork() {
		executeCommand(null);		
	}
	
	private void executeCommand(ParameterMap map){
		
		//handlerDisableUi.sendEmptyMessage(0);
		//mCurrentParameterMap = null;
		//mCurrentUiGenerator = null;
		
		if((mAddress==null)||(mCommand==null)){
			return;
		}
		
		if(map==null){
			//initialize the view
			try {
				mCurrentDataBundle = new Bundle();
				mSessionId  = mEpicService.executeRemoteCommand(mAddress, mCommand, mSessionId, null, mCurrentDataBundle);
				handlerInitStage.sendEmptyMessage(0);
			} catch (RemoteException e) {
				Toast.makeText(this, "The command could not be initialized", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
			
			
		} else {
		
			try {
				mCurrentDataBundle = new Bundle();
				Bundle data = BundleAdapter.makeBundle(map);
				mSessionId  = mEpicService.executeRemoteCommand(mAddress, mCommand, mSessionId, data, mCurrentDataBundle);
				handlerChangeStage.sendEmptyMessage(0);
			} catch (RemoteException e) {
				Toast.makeText(this, "The command could not be executed", Toast.LENGTH_LONG).show();
				//TODO change ui!
				e.printStackTrace();
			}
		}
		
		//handlerDisableUi.sendEmptyMessage(1);
	}

	@Override
	protected void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

}
