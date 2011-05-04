package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.IIncomingMessageCallback;

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
		*
		* void registerMessageCallback(in String application, in IIncomingMessageCallback messageCallback);
		* currently not implemented */
		
		/**
		* unregister a callback to receive messages
		*/	
		void unregisterMessageCallback(in String application);
		
		/**
		* send a message 
		*/	
		void sendMessage(in String action, in String sessionid, in Bundle data);
		
		/**
		* get the current state of the service
		*/
		int getState();
		
		
		
	
	}