package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.android.epic.service.interfaces.ParameterMapImpl;

	interface IIncomingMessageCallback {
	
		/**
		* Handle the message sent from a different application node
		*
		* @return Return true if the message shall be considered as read. 
		*/
		boolean handleMessage(in ParameterMapImpl message);
				    
	}