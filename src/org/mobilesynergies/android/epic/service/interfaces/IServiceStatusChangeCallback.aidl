package org.mobilesynergies.android.epic.service.interfaces;

/**
* Interface that must be implemented to receive updates about state changes of the EpicService
*/
	interface IServiceStatusChangeCallback{
	
		/**
		* Called by the service if the status changes
		*/
		void onServiceStatusChanged(in int status);
			    
	}