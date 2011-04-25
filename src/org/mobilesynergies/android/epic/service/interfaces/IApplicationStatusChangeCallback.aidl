package org.mobilesynergies.android.epic.service.interfaces;

	interface IApplicationStatusChangeCallback {
	
		/**
		* Called by the service if the application status changes
		*/
		void onApplicationStatusChanged(in String status);
			    
	}