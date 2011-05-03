package org.mobilesynergies.android.epic.service.application;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;

import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.core.ApplicationActivity;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;


import android.app.ListActivity;
import android.content.Intent;
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

public class BrowserHistoryProviderActivity extends ApplicationActivity{

	private static final String CLASS_TAG = BrowserHistoryProviderActivity.class.getSimpleName();
	private static final String EPIC_ACTION = "org.epic.action.ListBrowserHistory";
	
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
		
		setContentView(R.layout.browserhistory);
		Intent callingIntent = getIntent();
		Uri uri = callingIntent.getData();
		mSessionId = uri.getLastPathSegment();
		mData = callingIntent.getExtras();
		
	}

	private Bundle getHistoryMostVisits(int start, int size) {
		String[] projection = new String[] {
				Browser.BookmarkColumns.DATE, 
				Browser.BookmarkColumns.TITLE,
				Browser.BookmarkColumns.URL,
				Browser.BookmarkColumns.VISITS				
		};
		
		//WHERE DATE
		String selection = Browser.BookmarkColumns.VISITS;
		String selectionargs[] = null;

		Cursor mCur = managedQuery(android.provider.Browser.BOOKMARKS_URI,
				projection, selection, selectionargs,  Browser.BookmarkColumns.VISITS + " DESC"
		);

		if(!mCur.moveToPosition(start))
		{
			return null;
		}
				
		int dateIdx = mCur.getColumnIndex(Browser.BookmarkColumns.DATE);
		//int faviconIdx = mCur.getColumnIndex(Browser.BookmarkColumns.FAVICON);
		int titleIdx = mCur.getColumnIndex(Browser.BookmarkColumns.TITLE);
		int urlIdx = mCur.getColumnIndex(Browser.BookmarkColumns.URL);
		int visitsIdx = mCur.getColumnIndex(Browser.BookmarkColumns.VISITS);
		String keyString = "entry";
		Bundle entries = new Bundle();
		for(int i = 0; i<size; i++) {
			
			String date = mCur.getString(dateIdx);
			String title = mCur.getString(titleIdx);
			String url = mCur.getString(urlIdx);
			String visits = mCur.getString(visitsIdx);
			
			if((url.length()>0)&&(title.length()>0)){
			
				Bundle entry = new Bundle();
				if(url.length()>0){
					entry.putString("url", url);
				}
				if(title.length()>0){				
					entry.putString("title", title);
				}
				if(date.length()>0){
					entry.putString("date", date);
				}
				if(visits.length()>0){
					entry.putInt("visits", Integer.parseInt(visits));
				}				
				entries.putBundle(keyString+i, entry);
			}
			if(!mCur.moveToNext())
			{
				i=size;
			}
		}
		
		return entries;
	}

	public Bundle getHistoryMostRecent(int start, int length){

		String[] projection = new String[] {
				Browser.BookmarkColumns.DATE, 
				Browser.BookmarkColumns.TITLE,
				Browser.BookmarkColumns.URL,
				Browser.BookmarkColumns.VISITS				
		};
		
		//WHERE DATE
		String selection = Browser.BookmarkColumns.DATE;
		String selectionargs[] = null;

		Cursor mCur = managedQuery(android.provider.Browser.BOOKMARKS_URI,
				projection, selection, selectionargs,  Browser.BookmarkColumns.DATE + " DESC"
		);
		if(!mCur.moveToPosition(start))
		{
			return null;
		}
				
		int dateIdx = mCur.getColumnIndex(Browser.BookmarkColumns.DATE);
		//int faviconIdx = mCur.getColumnIndex(Browser.BookmarkColumns.FAVICON);
		int titleIdx = mCur.getColumnIndex(Browser.BookmarkColumns.TITLE);
		int urlIdx = mCur.getColumnIndex(Browser.BookmarkColumns.URL);
		int visitsIdx = mCur.getColumnIndex(Browser.BookmarkColumns.VISITS);
		Bundle entries = new Bundle();
		String keyString = "entry";
		for(int i = 0; i<length; i++) {
			
			String date = mCur.getString(dateIdx);
			String title = mCur.getString(titleIdx);
			String url = mCur.getString(urlIdx);
			String visits = mCur.getString(visitsIdx);
			
			
			if((url.length()>0)&&(title.length()>0)){
			
				Bundle entry = new Bundle();
				if(url.length()>0){
					entry.putString("url", url);
				}
				if(title.length()>0){				
					entry.putString("title", title);
				}
				if(date.length()>0){
					entry.putString("date", date);
				}
				if(visits.length()>0){
					entry.putInt("visits", Integer.parseInt(visits));
				}				
				entries.putBundle(keyString+i, entry);
			}
			if(!mCur.moveToNext())
			{
				i=length;
			}
		}
		
		return entries;
	}


	@Override
	protected void onConnected() {
		Log.d(CLASS_TAG, "connected to the service");
		try {
			int state = mEpicService.getState();
			
			if(state == EpicServiceState.EPICNETWORKCONNECTION){
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
		String order = "recent";
		if(mData!=null){
			start = mData.getInt("start", 0);
			size = mData.getInt("size", 10);
			String o = mData.getString("order"); 
			if(o!=null){
				order = o;
			}
		}
		Bundle data = null;
		
		if(order.equalsIgnoreCase("visits")){
			data = getHistoryMostVisits(start, size);
		} else {
			data = getHistoryMostRecent(start, size);
		}
		
		
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