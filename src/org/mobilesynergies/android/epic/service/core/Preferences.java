package org.mobilesynergies.android.epic.service.core;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Storing server and user settings.
 *  
 * @author Peter
 */
public final class Preferences {
    
	/**
	 * The user settings are used for persistent storage of the user preferences. 
	 * @param context The context for the SharedPreferences 
	 * @return The SharedPreferences that contain the user prefereces
	 */
	public static SharedPreferences getUserSettings(Context context) {
		return context.getSharedPreferences("epic-user-preferences", 0);
	}
	
	
	public static String getConfiguredServerName(Context context) {
		SharedPreferences settings = getUserSettings(context);
		String servername = settings.getString("server", "www.mobilesynergies.org");
		return servername;
	}
	
	public static void setConfiguredServerName(Context context, String servername) {
		SharedPreferences settings = getUserSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("server", servername);
		editor.commit();
	}
	
	public static String getConfiguredServiceName(Context context) {
		SharedPreferences settings = getUserSettings(context);
		String service = settings.getString("service", "box");
		return service;
	}
	
	public static int getConfiguredPort(Context context) {
		SharedPreferences settings = getUserSettings(context);
		int port = settings.getInt("port", 5222);
		return port;
	}

	
	public static void setConfiguredServiceName(Context context, String service) {
		SharedPreferences settings = getUserSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("service", service);
		editor.commit();
	}
	

	public static String getConfiguredDeviceName(Context context) {
		SharedPreferences settings = getUserSettings(context);
		String device = settings.getString("device", "android-device");
		return device;
	}
	
	public static void setConfiguredDeviceName(Context context, String device) {
		SharedPreferences settings = getUserSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("device", device);
		editor.commit();
	}

	public static void setConfiguredPort(Context context, int port) {
		SharedPreferences settings = getUserSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("port", port);
		editor.commit();
	}
	
	
	
	
	/**
	 * The service settings are used for persistent settings of the service such as username, password, etc. 
	 * @param context The context for the SharedPreferences 
	 * @return The SharedPreferences that contain the service settings
	 */
	public static SharedPreferences getServiceSettings(Context context) {
        return context.getSharedPreferences("epic-service-preferences", 0);
    }
    
	public static String getUserName(Context context){
		SharedPreferences settings = getServiceSettings(context);
		String username = settings.getString("username", "");
		return username;
	}
	
	public static String getUserPassword(Context context){
		SharedPreferences settings = getServiceSettings(context);
		String password = settings.getString("password", "");
		return password;
	}
	
	public static void setUserName(Context context, String username){
		SharedPreferences settings = getServiceSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", username);
		editor.commit();
	}
	
	public static void setUserPassword(Context context, String password){
		SharedPreferences settings = getServiceSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("password", password);
		editor.commit();
	}
	

	
	
	public static void deleteUserName(Context context) {
		SharedPreferences settings = getServiceSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("username");
		editor.commit();
	}

	public static void deleteUserPassword(Context context) {
		SharedPreferences settings = getServiceSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.remove("password");
		editor.commit();
	}

	
	
	public static boolean isRegistered(Context context) {
		String username = Preferences.getUserName(context);
		if((username==null)||(username.length()<=0))
			return false;
		String password = Preferences.getUserPassword(context);
		if((password==null)||(password.length()<=0))
			return false;
		
		return true;
	}


	
	/**
	 * The log is used for persistent log messages that are used for debugging.
	 * They have to be deleted on a regular basis or loging has to be disabled for production code. 
	 * Otherwise reopening and attaching new logmessages will make the service much slower. 
	 * @param context The context for the SharedPreferences 
	 * @return The SharedPreferences that contain the log
	 */	
    public static SharedPreferences getLog(Context context) {
        return context.getSharedPreferences("epic-service-log", 0);
    }
    
    public static void log(Context context, String tag, String message){
    	//disabled for release
    	/*SharedPreferences log = getLog(context);
    	SharedPreferences.Editor editor = log.edit();
    	Date date = new Date(System.currentTimeMillis());
    	String logmessage = date.toGMTString() + "\n" +
    	"[" + tag + "]: "+ message;
    	int index = 0;
    	index = log.getInt("counter", 0);
		editor.putString("log"+index, logmessage);
		editor.putInt("counter", index+1);
		editor.commit();*/
    }

	public static String[] getLogItems(Context context) {
		SharedPreferences log = getLog(context);
		int numberoflogitems = log.getInt("counter", 0);
		String logitems[] = new String[numberoflogitems];
		for(int i=numberoflogitems-1; i>=0; i--) {
			logitems[i] = log.getString("log"+i, "empty message");
		}
		return logitems;
	}

	public static void deleteLog(Context context) {
		SharedPreferences log = getLog(context);
		SharedPreferences.Editor editor = log.edit();
		editor.clear();
		editor.commit();		
	}


	public static boolean isFirstRun(Context context) {
		SharedPreferences settings = getServiceSettings(context);
		boolean firstrun = settings.getBoolean("firstrun", true);
		return firstrun;
	}


	public static void setIsFirstRun(Context context, boolean firstrun) {
		SharedPreferences settings = getServiceSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean("firstrun", firstrun);
		editor.commit();
		
	}

	
/*
	public static void setDefaultPackagePermission(Context context, int permission){
		SharedPreferences settings = getServiceSettings(context);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("defaultpermission", permission);
		editor.commit();
	}
	

	public static int getDefaultPackagePermission(Context context) {
		SharedPreferences settings = getServiceSettings(context);
		int permission = settings.getInt("defaultpermission", ConfigurationDatabase.PERMISSION_ALLOW);
		return permission;
	}

*/
	

	




	



	
	
	
}
