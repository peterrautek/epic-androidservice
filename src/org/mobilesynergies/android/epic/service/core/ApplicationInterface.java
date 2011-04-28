package org.mobilesynergies.android.epic.service.core;

import org.mobilesynergies.android.epic.service.EpicService;

import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceApplicationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IncomingMessageCallbackImpl;
import org.mobilesynergies.android.epic.service.interfaces.ParameterMapImpl;

import org.mobilesynergies.android.epic.service.interfaces.IIncomingMessageCallback;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;

import android.os.RemoteException;


/**
 * This class implements the application interface. It is the public interface allowing the usage of the EPIC service as a message oriented middleware.    
 * All functions run in the thread of the caller and block it.
 * Therefore the functions will call handlers in the EpicService thread. 
 */
public class ApplicationInterface extends IEpicServiceApplicationInterface.Stub {
	
	private static String CLASS_TAG = ApplicationInterface.class.getSimpleName(); 
	
	private EpicService mEpicServiceContext = null;

	public ApplicationInterface(EpicService context)
	{
		mEpicServiceContext = context;
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
	public void registerMessageCallback(String application,  
			IIncomingMessageCallback messageCallback)
	throws RemoteException {
		mEpicServiceContext.registerMessageCallback(application,(IncomingMessageCallbackImpl) messageCallback);
	}
	

	@Override
	public void sendMessage(String action, String sessionid, ParameterMapImpl map)
	throws RemoteException {
		mEpicServiceContext.sendMessage(action, sessionid, map);
	}

	
	@Override
	public void unregisterMessageCallback(String application) throws RemoteException {
		mEpicServiceContext.unregisterMessageCallback(application);
	}
	
	@Override
	public void stop(){
		mEpicServiceContext.stop();
	}

	@Override
	public int getState() throws RemoteException {
		return mEpicServiceContext.getState();
	}
	
	





}