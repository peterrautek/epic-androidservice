package org.mobilesynergies.android.epic.service.interfaces;

/**
* Interface that must be implemented to receive remote command calls
*/
	interface IRemoteCommandCallback {
	
		/**
		* Called when a remote node requests execution
		*  
		*/
		void executeCommand(in Bundle parameters);
				    
	}