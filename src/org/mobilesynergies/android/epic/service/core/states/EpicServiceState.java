package org.mobilesynergies.android.epic.service.core.states;

import org.mobilesynergies.android.epic.service.EpicService;
import org.mobilesynergies.android.epic.service.core.Preferences;
import org.mobilesynergies.epic.client.EpicClient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class EpicServiceState {

	public static int updateState(StateObject state, EpicService service, EpicClient client){
		state.setState(tryToGetGoodState(service, client));
		return state.getState();
	}

	private static boolean isConnectedToInternet(Context context){
		ConnectivityManager cm =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		boolean isconnected = false;
		if(info!=null){
			isconnected = info.isConnected();
		}
		return isconnected;		
	}
	
	public static boolean isConnectedToEpicNetwork(EpicClient client) {
		if(client==null){
			return false;
		}
		return client.isConnectedToEpicNetwork();
	}

	public static boolean isConnectedToServer(EpicClient client) {
		if(client==null){
			return false;
		}
		return client.isConnected();
	}


	public static boolean hasProvidedCredentials(Context context){
		String user = Preferences.getUserName(context);
		String password = Preferences.getUserPassword(context);
		
		if((user==null)||(password==null)||(user.length()==0)||(password.length()==0)){
			return false;
		}
		
		return true;

	}
	
	
	private static int tryToGetGoodState(EpicService service, EpicClient client) {

		if(! hasProvidedCredentials(service)){
			return StateObject.NOUSERCREDENTIALS;
		}
		
		if(client==null){
			return StateObject.INITIALIZING;
		}

		int state = StateObject.UNKNOWN;

		if(isConnectedToInternet(service)){
			state = StateObject.NETWORKCONNECTION;
		} else {
			return StateObject.ERROR_NONETWORK;
		}
		
		if(isConnectedToServer(client)){
			state = StateObject.SERVERCONNECTION;
		} 
		
		if(isConnectedToEpicNetwork(client)){
			state = StateObject.EPICNETWORKCONNECTION;
		}
		
		return state;
	}

	
	
}
