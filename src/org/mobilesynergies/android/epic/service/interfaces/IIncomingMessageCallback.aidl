package org.mobilesynergies.android.epic.service.interfaces;

/**
* Interface for callbacks that receive incoming messages
*/
	interface IIncomingMessageCallback {
	
		/**
		* Handle the message sent from a different application node
		*
		* @return Return true if the message shall be considered as read. 
		*/
		boolean handleMessage(in Bundle data);
				    
	}