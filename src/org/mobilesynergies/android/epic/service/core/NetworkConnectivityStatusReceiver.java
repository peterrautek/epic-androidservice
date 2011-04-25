package org.mobilesynergies.android.epic.service.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;


/**
 * Informs the epic service about network connectivity updates.
 * 
 * @author rautek
 *
 */
public class NetworkConnectivityStatusReceiver extends BroadcastReceiver {
	
	 
	//private static String CLASS_TAG =  NetworkConnectivityStatusReceiver.class.getSimpleName();
	
	
	public static String BOOLEAN_IS_CONNECTED = "isconnected";
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Bundle bExtra = intent.getExtras();
		//boolean noConnectivity = bExtra.getBoolean(ConnectivityManager.EXTRA_NO_CONNECTIVITY);
		//boolean isFailover = bExtra.getBoolean(ConnectivityManager.EXTRA_IS_FAILOVER);
		NetworkInfo info = bExtra.getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
		//NetworkInfo other = bExtra.getParcelable(ConnectivityManager.EXTRA_OTHER_NETWORK_INFO);
		//String extra = bExtra.getString(ConnectivityManager.EXTRA_EXTRA_INFO);
		//String reason = bExtra.getString(ConnectivityManager.EXTRA_REASON);
		
		boolean isconnected = false;
		/*
		Log.d(CLASS_TAG, "noConnectivity = "+noConnectivity);
		Log.d(CLASS_TAG, "isFailover = "+isFailover);
		Log.d(CLASS_TAG, "extra = "+extra);
		Log.d(CLASS_TAG, "reason = "+reason);
*/
		if(info!=null){
			isconnected = info.isConnected();
			//String type = info.getTypeName();

			/*Log.d(CLASS_TAG, "info = "+info.toString());
			Log.d(CLASS_TAG, "type = "+type);
			Log.d(CLASS_TAG, "isConnected = "+isconnected);*/
		}
		
		
		Intent serviceIntent = new Intent("org.mobilesynergies.EPIC_SERVICE");
		Bundle data = new Bundle();
		data.putBoolean(BOOLEAN_IS_CONNECTED, isconnected);
		serviceIntent.putExtras(data);
		context.startService(serviceIntent);
		//Log.d(CLASS_TAG, "component name: "+cn.flattenToString());
		
	}
}