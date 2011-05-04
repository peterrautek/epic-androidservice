package org.mobilesynergies.android.epic.service.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;


/**
 * A broadcast receiver that informs the epic service about network connectivity updates.
 * 
 * @author Peter
 *
 */
public class NetworkConnectivityStatusReceiver extends BroadcastReceiver {
	
	public static String BOOLEAN_IS_CONNECTED = "isconnected";
	
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		Bundle bExtra = intent.getExtras();
		NetworkInfo info = bExtra.getParcelable(ConnectivityManager.EXTRA_NETWORK_INFO);
		
		boolean isconnected = false;
		
		if(info!=null){
			isconnected = info.isConnected();
		}
		
		
		Intent serviceIntent = new Intent("org.mobilesynergies.EPIC_SERVICE");
		Bundle data = new Bundle();
		data.putBoolean(BOOLEAN_IS_CONNECTED, isconnected);
		serviceIntent.putExtras(data);
		context.startService(serviceIntent);
		
	}
}