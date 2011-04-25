package org.mobilesynergies.android.epic.service.core.states;

import java.util.HashSet;
import java.util.Iterator;

import org.mobilesynergies.android.epic.service.interfaces.IApplicationStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.IIncomingMessageCallback;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;

import android.os.Bundle;
import android.os.RemoteException;

/**
 * 
 * 
 * State object 
 * @author Peter  
 *
 */
public class EpicServiceState {
	
	public static final int STATE_UNCONNECTED = 1;
	public static final int STATE_INTERNETCONNECTED = 2;
	public static final int STATE_SERVERCONNECTED= 3;
	public static final int STATE_AUTHENTICATED = 4; 
	
	/**
	 * Stores the callbacks that wish to be informed about changes in service availability.
	 */
	private HashSet<IServiceStatusChangeCallback> mSetServiceStatusCallbacks = new HashSet<IServiceStatusChangeCallback>();


	private int mState = STATE_UNCONNECTED;
		
	public void changeState(int newState){
		if(mState!=newState){
			mState = newState;
			
			Iterator<IServiceStatusChangeCallback> iter = mSetServiceStatusCallbacks.iterator();
			while(iter.hasNext()){
				IServiceStatusChangeCallback s = iter.next();
				try {
					s.onServiceStatusChanged(newState);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public void addServiceStatusCallback(IServiceStatusChangeCallback callback) {
		mSetServiceStatusCallbacks.add(callback);		
	}

	public String getStateAsHumanReadableString(int state) {
		switch(state) {
			case STATE_UNCONNECTED:
				return "not connected";
			case STATE_INTERNETCONNECTED:
				return "connected to the internet";
			case STATE_SERVERCONNECTED:
				return "connected to the epic server";
			case STATE_AUTHENTICATED:
				return "connected to the epic network";
		}
		return "unknown";
	}

	public String getStateAsHumanReadableString() {
		return getStateAsHumanReadableString(mState);
	}
		
	

}