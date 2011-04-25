package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.android.epic.service.interfaces.NetworkNodeImpl;

	interface IPresenceStatusChangeCallback {
	
		/**
		* Called by the service if the presence status of a node changes
		*/
		void onPresenceStatusChanged(in NetworkNodeImpl node);
			    
	}