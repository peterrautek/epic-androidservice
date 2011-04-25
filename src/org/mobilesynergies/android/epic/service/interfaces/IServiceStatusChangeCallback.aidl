package org.mobilesynergies.android.epic.service.interfaces;

	interface IServiceStatusChangeCallback{
	
		/**
		* Called by the service if the status changes
		*/
		void onServiceStatusChanged(in int status);
			    
	}