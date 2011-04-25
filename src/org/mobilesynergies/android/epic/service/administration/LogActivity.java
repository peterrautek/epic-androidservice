package org.mobilesynergies.android.epic.service.administration;

import org.mobilesynergies.android.epic.service.core.Preferences;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

/**
 * Shows the log
 * 
 * @author Peter
 */

public class LogActivity extends ListActivity {

	public static final String INTENTACTION = "org.mobilesynergies.android.epic.service.log";

	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String logitems[] = Preferences.getLogItems(this);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, logitems);
		setListAdapter(adapter);
	}

	Handler handlerUpdate = new Handler(){
		public void handleMessage(android.os.Message msg) {
			String logitems[] = Preferences.getLogItems(LogActivity.this);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(LogActivity.this, android.R.layout.simple_list_item_1, logitems);
			setListAdapter(adapter);
		};
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "Delete Log").setIcon(android.R.drawable.ic_menu_delete);
		menu.add(1, 1, 1, "Refresh List").setIcon(android.R.drawable.ic_menu_edit);
		return true;
	}


	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {
		case 0:
			Preferences.deleteLog(this);
			return true;
		case 1:
			handlerUpdate.sendEmptyMessage(0);
		}
	
		return false;
	}


	



}