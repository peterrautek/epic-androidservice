package org.mobilesynergies.android.epic.service.interfaces;

	/**
	* An interface that needs to be implemented to receive updates about state changes of the EpicService.
	*/
	interface IApplicationStatusChangeCallback {
	
		/**
		* Called by the service if the application status changes
		*/
		void onApplicationStatusChanged(in String status);
			    
	}