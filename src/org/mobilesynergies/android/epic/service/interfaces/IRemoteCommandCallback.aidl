package org.mobilesynergies.android.epic.service.interfaces;

	interface IRemoteCommandCallback {
	
		/**
		* Called when a remote node requests execution
		*  
		*/
		void executeCommand(in Bundle parameters);
				    
	}