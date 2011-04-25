package org.mobilesynergies.android.epic.service.core.states;

public class EpicServiceState {

	public static final int UNINITIALIZED = -4;
	public static final int INITIALIZING = -3;
	public static final int NONETWORKCONNECTION = -2;
	public static final int NETWORKCONNECTION = -1;
	public static final int SERVERCONNECTION = 0;
	public static final int EPICNETWORKCONNECTION = 1;
		
	public int state = UNINITIALIZED;
	
	public static String getStateAsHumanReadableString(int state) {
		switch(state) {
		case EpicServiceState.UNINITIALIZED:
			return "uninitialized";
		case EpicServiceState.INITIALIZING:
			return "initializing";
		case EpicServiceState.NONETWORKCONNECTION:
			return "no network connection";
		case EpicServiceState.NETWORKCONNECTION:
			return "connected to the internet";
		case EpicServiceState.SERVERCONNECTION:
			return "connected to the epic server";
		case EpicServiceState.EPICNETWORKCONNECTION:
			return "connected to the epic network";
		}
		return "unknown";
	}

	
}
