package org.mobilesynergies.android.epic.service.application;

import java.util.ArrayList;
import java.util.Iterator;

import org.mobilesynergies.android.epic.service.core.ApplicationActivity;
import org.mobilesynergies.android.epic.service.core.states.EpicServiceState;
import org.mobilesynergies.android.epic.service.interfaces.ParameterMapImpl;
import org.mobilesynergies.epic.client.remoteui.ArrayParameter;
import org.mobilesynergies.epic.client.remoteui.Parameter;
import org.mobilesynergies.epic.client.remoteui.ParameterMap;
import org.mobilesynergies.epic.client.remoteui.StringParameter;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.Browser;
import android.util.Log;
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
	private ParameterMapImpl mData = null;


	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Intent callingIntent = getIntent(); 
		Bundle b = callingIntent.getExtras();
		if(b!=null){
			mSessionId  = b.getString("session");
			mData  =(ParameterMapImpl) b.getParcelable("data");
		}
		
	}

	private ArrayParameter getHistoryMostVisits(int start, int size) {
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
		ArrayList<Parameter> array = new ArrayList<Parameter>();
		for(int i = 0; i<size; i++) {
			
			String date = mCur.getString(dateIdx);
			String title = mCur.getString(titleIdx);
			String url = mCur.getString(urlIdx);
			String visits = mCur.getString(visitsIdx);
			
			if((url.length()>0)&&(title.length()>0)){
			
				ParameterMap entry = new ParameterMap();
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
				array.add(entry);
			}
			if(!mCur.moveToNext())
			{
				i=size;
			}
		}
		ArrayParameter arrayparameter = new ArrayParameter(array);
		return arrayparameter;
	}

	public ArrayParameter getHistoryMostRecent(int start, int length){

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
		ArrayList<Parameter> array = new ArrayList<Parameter>();
		for(int i = 0; i<length; i++) {
			
			String date = mCur.getString(dateIdx);
			String title = mCur.getString(titleIdx);
			String url = mCur.getString(urlIdx);
			String visits = mCur.getString(visitsIdx);
			
			if((url.length()>0)&&(title.length()>0)){
			
				ParameterMap entry = new ParameterMap();
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
				array.add(entry);
			}
			if(!mCur.moveToNext())
			{
				i=length;
			}
		}
		ArrayParameter arrayparameter = new ArrayParameter(array);
		return arrayparameter;
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
			order = mData.getString("order");
		}
		ArrayParameter entries = null;
		if(order.equalsIgnoreCase("visits")){
			entries = getHistoryMostVisits(start, size);
		} else {
			entries = getHistoryMostRecent(start, size);
		}
		
		ParameterMapImpl map = new ParameterMapImpl();
		map.putParameter("entries", entries);
		try {			  
			mEpicService.sendMessage(EPIC_ACTION, mSessionId, map);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finish();
	
	}


	


}