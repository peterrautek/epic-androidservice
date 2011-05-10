package org.mobilesynergies.android.epic.service.core;

import org.mobilesynergies.android.epic.service.EpicService;
import org.mobilesynergies.android.epic.service.interfaces.IEpicServiceApplicationInterface;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;

import android.os.Bundle;
import android.os.RemoteException;


/**
 * This class implements the application interface. It is the public interface allowing the usage of the EPIC service as a message oriented middleware.    
 *  
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
	public void registerServiceStatusChangeCallback(IServiceStatusChangeCallback callback) throws RemoteException {
		mEpicServiceContext.registerServiceStatusChangeCallback(callback);
	}

	/* not implemented yet
	@Override
	public void registerMessageCallback(String application,  
			IIncomingMessageCallback messageCallback)
	throws RemoteException {
		mEpicServiceContext.registerMessageCallback(application,(IncomingMessageCallbackImpl) messageCallback);
	}
	
	@Override
	public void unregisterMessageCallback(String application) throws RemoteException {
		mEpicServiceContext.unregisterMessageCallback(application);
	}*/

	
	@Override
	public int getState() throws RemoteException {
		return mEpicServiceContext.getState();
	}



	@Override
	public void sendMessage(String action, String sessionid, Bundle data)
	throws RemoteException {
		mEpicServiceContext.sendMessage(action, sessionid, data);

	}







}