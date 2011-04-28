package org.mobilesynergies.android.epic.service.core.states;

import org.mobilesynergies.android.epic.service.EpicService;
import org.mobilesynergies.android.epic.service.core.MainActivity;
import org.mobilesynergies.android.epic.service.core.Preferences;
import org.mobilesynergies.epic.client.EpicClient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class EpicServiceState {

	public static final int UNKNOWN = -99;
	public static final int NOUSERCREDENTIALS = -5;
	public static final int UNINITIALIZED = -4;
	public static final int INITIALIZING = -3;
	//public static final int NONETWORKCONNECTION = -2;
	public static final int NETWORKCONNECTION = -1;
	public static final int SERVERCONNECTION = 0;
	public static final int EPICNETWORKCONNECTION = 1;
	public static final int STOPPED = 2;
	
	public static final int ERROR_NONETWORK = -98;
	public static final int ERROR_AUTHFAIL = -97;
	public static final int ERROR_NOSERVER = -96;
	public static final int ERROR_XMPPERROR = -95;
	
	
		
	private int mState = UNKNOWN;
	
	public int getState(){
		return mState;
	}
	
	public int updateState(EpicService service, EpicClient client){
		mState = tryToGetGoodState(service, client);
		return mState;
	}

	
	public static String getStateHint(int state) {
		switch(state) {
		case EpicServiceState.ERROR_AUTHFAIL:
			return "check your username and password and enter them again";
		case EpicServiceState.ERROR_NONETWORK:
			return "try to connect to the internet";
		case EpicServiceState.ERROR_NOSERVER:
			return "check the servername (Login->Menu). maybe it is wrong or the server is not working";
		case EpicServiceState.ERROR_XMPPERROR:
			return "please wait. if the service does not reconnect, stop and restart it.";
		case EpicServiceState.NOUSERCREDENTIALS:
			return "please provide your username and password";
		case EpicServiceState.UNINITIALIZED:
			return "please wait";
		case EpicServiceState.INITIALIZING:
			return "please wait";
		//case EpicServiceState.NONETWORKCONNECTION:
			//return "no network connection";
		case EpicServiceState.NETWORKCONNECTION:
			return "the service just got internet connection. please wait.";
		case EpicServiceState.SERVERCONNECTION:
			return "the service connected to the epic server. please wait.";
		case EpicServiceState.EPICNETWORKCONNECTION:
			return "enjoy!";
		case EpicServiceState.STOPPED:
			return "to use the service again please start it!";
		}
	
		return "unknown";
	}

	
	

	public static String getStateAsHumanReadableString(int state) {
		switch(state) {
		case EpicServiceState.ERROR_AUTHFAIL:
			return "user credentials wrong";
		case EpicServiceState.ERROR_NONETWORK:
			return "no network connection";
		case EpicServiceState.ERROR_NOSERVER:
			return "server not reachable";
		case EpicServiceState.ERROR_XMPPERROR:
			return "a communication error occured";
		case EpicServiceState.NOUSERCREDENTIALS:
			return "no user credentials provided";
		case EpicServiceState.UNINITIALIZED:
			return "uninitialized";
		case EpicServiceState.INITIALIZING:
			return "initializing";
		case EpicServiceState.NETWORKCONNECTION:
			return "connected to the internet";
		case EpicServiceState.SERVERCONNECTION:
			return "connected to the epic server";
		case EpicServiceState.EPICNETWORKCONNECTION:
			return "connected to the epic network";
		case EpicServiceState.STOPPED:
			return "stopped";
		}
		

		return "unknown";
	}

	private boolean isConnectedToInternet(Context context){

		ConnectivityManager cm =  (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cm.getActiveNetworkInfo();
		boolean isconnected = false;
		if(info!=null){
			isconnected = info.isConnected();
		}
		return isconnected;		
	}
	
	public boolean isConnectedToEpicNetwork(EpicClient client) {
		if(client==null){
			return false;
		}
		return client.isConnectedToEpicNetwork();
	}

	public boolean isConnectedToServer(EpicClient client) {
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
	
	
	private int tryToGetGoodState(EpicService service, EpicClient client) {

		if(! hasProvidedCredentials(service)){
			return EpicServiceState.NOUSERCREDENTIALS;
		}
		
		if(client==null){
			return EpicServiceState.INITIALIZING;
		}

		int state = EpicServiceState.UNKNOWN;

		if(isConnectedToInternet(service)){
			state = EpicServiceState.NETWORKCONNECTION;
		} else {
			return EpicServiceState.ERROR_NONETWORK;
		}
		
		if(isConnectedToServer(client)){
			state = EpicServiceState.SERVERCONNECTION;
		} 
		
		if(isConnectedToEpicNetwork(client)){
			state = EpicServiceState.EPICNETWORKCONNECTION;
		}
		
		return state;
	}

	public void setState(int state) {
		this.mState = state;
	}

	
	
	
}
