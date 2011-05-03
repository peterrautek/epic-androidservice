package org.mobilesynergies.android.epic.service.core;


import org.mobilesynergies.android.epic.service.EpicService;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceAdministrationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IPresenceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.NetworkNodeImpl;
import org.mobilesynergies.android.epic.service.interfaces.EpicCommandInfoImpl;

import org.mobilesynergies.epic.client.EpicClientException;


import org.mobilesynergies.epic.client.NetworkNode;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.epic.client.PresenceCallback;

import android.os.Bundle;
import android.os.RemoteException;

/**
 * 
 * This is the administration interface of the epic service. 
 * It provides methods for 
 *  - service configuration, 
 *  - service availability, 
 *  - epic network explorarion,
 *  - remote command execution 
 *  - etc.
 *
 * @author Peter
 */

public class AdministrationInterface extends IEpicServiceAdministrationInterface.Stub  implements PresenceCallback{

	private static String CLASS_TAG = AdministrationInterface.class.getSimpleName(); 
	private EpicService mEpicServiceContext = null;
	private IPresenceStatusChangeCallback mPresenceCallback = null;

	
	public AdministrationInterface(EpicService context) {
		mEpicServiceContext = context;
	}


	
	@Override
	public void registerPresenceStatusChangeCallback(
			IPresenceStatusChangeCallback callback) throws RemoteException {
		mPresenceCallback  = callback;
		mEpicServiceContext.registerPresenceCallback(this);
	}


	@Override
	public void onPresenceChanged(NetworkNode node) {
		if(mPresenceCallback!=null) {
			try {
				NetworkNodeImpl nodeImpl = new NetworkNodeImpl(node);
				mPresenceCallback.onPresenceStatusChanged(nodeImpl);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}


	@Override
	public int getVersion() throws RemoteException {
		return EpicService.EPIC_SERVICE_VERSION_NUMBER;
	}


	@Override
	public void registerServiceStatusChangeCallback(
			IServiceStatusChangeCallback callback) throws RemoteException {
			mEpicServiceContext.registerServiceStatusChangeCallback(callback);
	}



	@Override
	public String executeRemoteCommand(String epicNode, String command, String sessionId, Bundle parametersIn, Bundle parametersOut) throws RemoteException {
		String id = sessionId;
		if(mEpicServiceContext==null) {
			return null;
		}
		try {
			id = mEpicServiceContext.executeRemoteCommand(epicNode, command, sessionId, parametersIn, parametersOut);
		} catch (EpicClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return id;
	}



	@Override
	public NetworkNodeImpl[] getNetworkNodes() throws RemoteException {
		if(mEpicServiceContext==null) {
			return null;
		}
		
		NetworkNode[] nodes = null;
		try {
			nodes = mEpicServiceContext.getNetworkNodes();
		} catch (EpicClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return NetworkNodeImpl.asNetworkNodesImplArray(nodes);
	}


	@Override
	public EpicCommandInfoImpl[] getRemoteCommands(String epicNode) throws RemoteException {
		if(mEpicServiceContext==null) {
			return null;
		}
		
		try {
			return EpicCommandInfoImpl.asEpicCommandArray(mEpicServiceContext.getEpicCommands(epicNode));
		} catch (EpicClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;		
	}



	@Override
	public int getState() throws RemoteException {
		return mEpicServiceContext.getState();
		
	}



	

}
