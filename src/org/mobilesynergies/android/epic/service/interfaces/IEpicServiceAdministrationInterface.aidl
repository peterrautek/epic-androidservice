package org.mobilesynergies.android.epic.service.interfaces;

import org.mobilesynergies.android.epic.service.interfaces.IPresenceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.IServiceStatusChangeCallback;
import org.mobilesynergies.android.epic.service.interfaces.NetworkNodeImpl;
import org.mobilesynergies.android.epic.service.interfaces.ParameterMapImpl;
import org.mobilesynergies.android.epic.service.interfaces.EpicCommandInfoImpl;

	interface IEpicServiceAdministrationInterface{
	
	
		int getVersion();
		
		void registerServiceStatusChangeCallback(in IServiceStatusChangeCallback callback);
		
		/**
		* Registering a callback function to receive notifications about presence updates
		*
		*/ 
		void registerPresenceStatusChangeCallback(in IPresenceStatusChangeCallback callback);
		
		/**
		* Get a list of epic nodes in the network of the registered user.
		*
		*/
		NetworkNodeImpl[] getNetworkNodes();
		
		/**
		 * Execute a remote command.
		 * @param epicNode The node that executes the command 
		 * @param command The name of the command
		 * @param sessionId The sessionId of this command. If null a new command is created.
		 * @param inParameters The parameters for the command of null if no parameters are known or needed
		 * @param outParameters The parameters that are needed for completion of the next stage of the command. The caller must provide an empty ParameterMap here to receive the necessary parameters. 
		 * @return The sessionId of this command. This id is used to later refer to this command.
		 */          		
		String executeRemoteCommand(in String epicNode, in String command, in String sessionId, in ParameterMapImpl inParameters, out ParameterMapImpl outParameters);
		
		/**
		* Call to get information if the client is connected to the EPIC network.
		*
		* @return True if the client is connected and authenticated at the server. False otherwise (no internet connection, not connected to the server, not authenticated at the server)
		*/  
		boolean isConnectedToEpicNetwork();
		
		
		/**
		 * Get a list of remote commands for one epic node
		 * 
		 */
		 EpicCommandInfoImpl[] getRemoteCommands(in String epicNode);
		
		
				    
	}