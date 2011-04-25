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
 * Manages callbacks that need to be informed if a state change occurs
 * @author Peter  
 *
 */
public class EpicServiceStateChangeManager {

	/**
	 * Stores the callbacks that wish to be informed about changes in service availability.
	 */
	private HashSet<IServiceStatusChangeCallback> mSetServiceStatusCallbacks = new HashSet<IServiceStatusChangeCallback>();

	public void sendStateChangeToListeners(int newState){

		Iterator<IServiceStatusChangeCallback> iter = mSetServiceStatusCallbacks.iterator();
		while(iter.hasNext()){
			IServiceStatusChangeCallback s = iter.next();
			if(s==null)
			{
				iter.remove();
			} else {
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

	
	



}