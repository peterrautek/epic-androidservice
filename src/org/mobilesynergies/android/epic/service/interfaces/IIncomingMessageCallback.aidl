package org.mobilesynergies.android.epic.service.interfaces;

	interface IIncomingMessageCallback {
	
		/**
		* Handle the message sent from a different application node
		*
		* @return Return true if the message shall be considered as read. 
		*/
		boolean handleMessage(in Bundle data);
				    
	}