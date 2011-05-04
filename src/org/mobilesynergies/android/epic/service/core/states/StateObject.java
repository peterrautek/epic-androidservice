package org.mobilesynergies.android.epic.service.core.states;


public class StateObject {

	public static final int UNKNOWN = -99;
	public static final int NOUSERCREDENTIALS = -5;
	public static final int UNINITIALIZED = -4;
	public static final int INITIALIZING = -3;
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
	
	public void setState(int state) {
		this.mState = state;
	}
	
	public static String getStateHint(int state) {
		switch(state) {
		case ERROR_AUTHFAIL:
			return "check your username and password and enter them again";
		case ERROR_NONETWORK:
			return "try to connect to the internet";
		case ERROR_NOSERVER:
			return "check the servername (Login->Menu). maybe it is wrong or the server is not working";
		case ERROR_XMPPERROR:
			return "please wait. if the service does not reconnect, stop and restart it.";
		case NOUSERCREDENTIALS:
			return "please provide your username and password";
		case UNINITIALIZED:
			return "please wait";
		case INITIALIZING:
			return "please wait";
		//case EpicServiceState.NONETWORKCONNECTION:
			//return "no network connection";
		case NETWORKCONNECTION:
			return "the service just got internet connection. please wait.";
		case SERVERCONNECTION:
			return "the service connected to the epic server. please wait.";
		case EPICNETWORKCONNECTION:
			return "enjoy!";
		case STOPPED:
			return "to use the service again please start it!";
		}
	
		return "unknown";
	}

	
	

	public static String getStateAsHumanReadableString(int state) {
		switch(state) {
		case ERROR_AUTHFAIL:
			return "user credentials wrong";
		case ERROR_NONETWORK:
			return "no network connection";
		case ERROR_NOSERVER:
			return "server not reachable";
		case ERROR_XMPPERROR:
			return "a communication error occured";
		case NOUSERCREDENTIALS:
			return "no user credentials provided";
		case UNINITIALIZED:
			return "uninitialized";
		case INITIALIZING:
			return "initializing";
		case NETWORKCONNECTION:
			return "connected to the internet";
		case SERVERCONNECTION:
			return "connected to the epic server";
		case EPICNETWORKCONNECTION:
			return "connected to the epic network";
		case STOPPED:
			return "stopped";
		}

		return "unknown";
	}
}
