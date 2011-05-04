package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.IIncomingMessageCallback;

/**
* The administration interface of the EpicService
*/
interface IEpicServiceApplicationInterface {
	
		/**
		* check version of service implementation
		*/
		int getVersion();
		
		/**
		* register a callback that receives updates if the state of the service changes
		*/
		void registerServiceStatusChangeCallback(in IServiceStatusChangeCallback callback);

		/**
		* register a callback to receive messages
		* currently not implemented 
		
		* void registerMessageCallback(in String application, in IIncomingMessageCallback messageCallback);
		* 
		
		*
		* unregister a callback to receive messages
		* currently not implemented 
		*	
		void unregisterMessageCallback(in String application);
		*/
		
		/**
		* send a message 
		*/	
		void sendMessage(in String action, in String sessionid, in Bundle data);
		
		/**
		* get the current state of the service
		*/
		int getState();
		
		
		
	
	}