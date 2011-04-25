package org.mobilesynergies.android.epic.service.remoteui;

public interface SubmitActionListener {
	
	/**
	 * Implement this method to get informed that a 
	 * submit action was triggered. The variable was 
	 * updated and by the user.
	 * @param variable The name of the variable that was updated
	 */
	public void onSubmitAction(String variable);
	
	/**
	 * Implement this method to get informed that a 
	 * submit action was triggered and everything needs to be submitted.
	 * 
	 */
	public void onSubmitAction();
	
	
}
