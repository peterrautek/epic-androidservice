package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.IApplicationStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.IIncomingMessageCallback;
import org.mobilesynergies.android.epic.service.interfaces.IRemoteCommandCallback;
import org.mobilesynergies.android.epic.service.interfaces.ParameterMapImpl;

interface IEpicServiceApplicationInterface {
	
		int getVersion();
		
		void registerServiceStatusChangeCallback(in IServiceStatusChangeCallback callback);
		
		void registerMessageCallback(in String application, in IIncomingMessageCallback messageCallback);
		
		void unregisterMessageCallback(in String application);
		
		void sendMessage(in String action, in String sessionid, in ParameterMapImpl map);
		
		/**
		* Call to get information if the client is connected to the EPIC network.
		*
		* @return True if the client is connected and authenticated at the server. False otherwise (no internet connection, not connected to the server, not authenticated at the server)
		*/  
		boolean isConnectedToEpicNetwork();
	
		
	
	}