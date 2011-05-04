package org.mobilesynergies.android.epic.service.application;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.core.ApplicationActivity;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.android.epic.service.core.states.StateObject;


import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Browser;
import android.util.Log;
import android.view.Window;
import android.widget.ArrayAdapter;

/**
 * Sends the browser history information to the requesting jid
 * 
 * @author Peter
 */

public class ListEpicActionsActivity extends ApplicationActivity{

	private static final String CLASS_TAG = ListEpicActionsActivity.class.getSimpleName();
	private static final String EPIC_ACTION = "org.epic.action.ListActions";
	
	/** The session id identifies the caller.
	 */
	private String mSessionId = "";
	
	/** The data that is sent with the request
	 * 
	 */
	private Bundle mData = null;


	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.listactions);
		Intent callingIntent = getIntent(); 
		mData = callingIntent.getExtras();
		
		Uri uri = callingIntent.getData();
		if(uri!=null){
			mSessionId  = uri.getLastPathSegment();
		}
		
	}

	private Bundle getList(int start, int size) {
		
		
		Intent intent = new Intent();
		// query for intents that specify a data field with scheme 'epic'
		Uri uri = Uri.parse("epic://www.mobilesynergies.org/");
		intent.setData(uri);
		
		PackageManager pm = getPackageManager();
		List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER);
		
		//check boundary condition
		if(start>=list.size()){
			return new Bundle();
		}
		
		Iterator<ResolveInfo> iter = list.iterator();
		int counter = 0;
		// proceed to the start index
		while(counter<start){
			iter.next();
			counter++;
		}
		
		Bundle entries = new Bundle();
		String keyString = "entry";
		// add stuff to the list
		while((counter<start+size)&&(iter.hasNext())){
			ResolveInfo info = iter.next();
			String packageName = info.activityInfo.packageName;
			String className = info.activityInfo.name;
			String action = null;
			if(info.filter!=null){
				action = info.filter.getAction(0);
			}
			
			Bundle data = new Bundle();
			data.putString("package", packageName);
			data.putString("class", className);
			if(action!=null){
				data.putString("action", action);
			}
			entries.putBundle(keyString+counter, data);
			counter++;
		}
		return entries;
	}



	@Override
	protected void onConnected() {
		Log.d(CLASS_TAG, "connected to the service");
		try {
			int state = mEpicService.getState();
			
			if(state == StateObject.EPICNETWORKCONNECTION){
				sendMessage();
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


	@Override
	protected void onConnectedToEpicNetwork() {
		sendMessage();
	}


	@Override
	protected void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	
	void sendMessage(){
		int start = 0;
		int size = 10;

		if(mData!=null){
			start = mData.getInt("start", 0);
			size = mData.getInt("size", 10);
		}
		Bundle data = null;
		data = getList(start, size);
		try {			  
			mEpicService.sendMessage(EPIC_ACTION, mSessionId, data);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		TimerTask t = new TimerTask(){

			@Override
			public void run() {
				handleFinish.sendEmptyMessage(0);
			}
			
		};
		Timer timer = new Timer();
		//wait some time before closing the application (allowing the user to see the screen of this app)
		timer.schedule(t, 2500);
		
	}
	
	Handler handleFinish = new Handler(){
		public void handleMessage(android.os.Message msg) {
			finish();
		};
	};


	


}