package org.mobilesynergies.android.epic.service.administration;

import java.util.Collections;
import java.util.List;

import org.mobilesynergies.android.epic.service.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;


public class ServiceConfigurationActivity extends Activity {

	private static final String CLASS_TAG = ServiceConfigurationActivity.class.getSimpleName();

	public static final String INTENTACTION = "org.mobilesynergies.action.ConfigurePermissions";

	private static Bitmap mBitmapAllow = null;
	private static Bitmap mBitmapDisallow = null;
	private static Bitmap mBitmapAsk = null;

	private static int mNumberOfApplications = 0;
	private PackageManager mPackageManager = null;

	public static int PERMISSION_DEFAULT = ConfigurationDatabase.PERMISSION_ALLOW;

	private static class EfficientAdapter extends BaseAdapter {

		public static final String PERMISSION_STRING_ASK = "ask me";
		public static final String PERMISSION_STRING_ALLOW = "allow";
		public static final String PERMISSION_STRING_DISALLOW = "disallow";

		private LayoutInflater mInflater;

		public EfficientAdapter(Context context) {
			// Cache the LayoutInflate to avoid asking for a new one each time.
			mInflater = LayoutInflater.from(context);
		}



		/**
		 * The number of items in the list is determined by the number of speeches
		 * in our array.
		 *
		 * @see android.widget.ListAdapter#getCount()
		 */
		public int getCount() {
			return mNumberOfApplications;
		}

		/**
		 * Since the data comes from an array, just returning the index is
		 * sufficent to get at the data. If we were using a more complex data
		 * structure, we would return whatever object represents one row in the
		 * list.
		 *
		 * @see android.widget.ListAdapter#getItem(int)
		 */
		public Object getItem(int position) {
			return position;
		}

		/**
		 * Use the array index as a unique id.
		 *
		 * @see android.widget.ListAdapter#getItemId(int)
		 */
		public long getItemId(int position) {
			return position;
		}

		/**
		 * Make a view to hold each row.
		 *
		 * @see android.widget.ListAdapter#getView(int, android.view.View,
		 *      android.view.ViewGroup)
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			// A ViewHolder keeps references to children views to avoid unneccessary calls
			// to findViewById() on each row.
			ViewHolder holder;

			// When convertView is not null, we can reuse it directly, there is no need
			// to reinflate it. We only inflate a new View when the convertView supplied
			// by ListView is null.
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.list_item_icon_text, null);

				// Creates a ViewHolder and store references to the two children views
				// we want to bind data to.
				holder = new ViewHolder();
				holder.text = (TextView) convertView.findViewById(R.id.text);
				holder.icon = (ImageView) convertView.findViewById(R.id.icon);
				holder.permission = (ImageView) convertView.findViewById(R.id.permission);
				holder.permissionText = (TextView) convertView.findViewById(R.id.TextViewPermission);

				convertView.setTag(holder);
			} else {
				// Get the ViewHolder back to get fast access to the TextView
				// and the ImageView.
				holder = (ViewHolder) convertView.getTag();
			}

			// Bind the data efficiently with the holder.
			holder.text.setText(LABELS[position]);
			holder.icon.setImageBitmap(ICONS[position]);

			if(PERMISSIONS[position]==ConfigurationDatabase.PERMISSION_UNKNOWN){
				PERMISSIONS[position] = PERMISSION_DEFAULT;
			}

			if(PERMISSIONS[position]==ConfigurationDatabase.PERMISSION_ALLOW){
				holder.permission.setImageBitmap(mBitmapAllow);
				holder.permissionText.setText(PERMISSION_STRING_ALLOW);
			} else if(PERMISSIONS[position]==ConfigurationDatabase.PERMISSION_DISALLOW){
				holder.permission.setImageBitmap(mBitmapDisallow);
				holder.permissionText.setText(PERMISSION_STRING_DISALLOW);
			} else if(PERMISSIONS[position]==ConfigurationDatabase.PERMISSION_ASK){
				holder.permission.setImageBitmap(mBitmapAsk);
				holder.permissionText.setText(PERMISSION_STRING_ASK);
			}

			holder.icon.setImageBitmap(ICONS[position]);

			return convertView;
		}

		static class ViewHolder {
			TextView text;
			ImageView icon;
			ImageView permission;
			TextView permissionText;
		}
	}


	private EfficientAdapter mAdapter = null;
	private ConfigurationDatabase mPermissionDatabase = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
				
		mPackageManager = getPackageManager();
		mPermissionDatabase = new ConfigurationDatabase(this);

		mBitmapAsk = BitmapFactory.decodeResource(getResources(), R.drawable.cloud_ask);
		mBitmapDisallow = BitmapFactory.decodeResource(getResources(), R.drawable.cloud_no);
		mBitmapAllow = BitmapFactory.decodeResource(getResources(), R.drawable.cloud_yes);

		mAdapter = new EfficientAdapter(this);
		ListView lv = new ListView(this);
		lv.setAdapter(mAdapter);
		lv.setOnItemClickListener(mClickListener);
		
		setContentView(lv);
		
		mProgressDialog = new ProgressDialog(this);	
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setMessage("Populating the list of applications");
		mProgressDialog.setTitle("Please wait");
		mProgressDialog.show();
		
		final SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(this);
		boolean first_run = app_preferences.getBoolean("first_run_config", true);

		if (first_run) {
			new AlertDialog.Builder(this)
			.setTitle("Help")
			.setMessage("Using Cuckoo allows you to remotely start applications on your device. \nTo control the behaviour of Cuckoo when an application is remotely launched you can set a specific permission for each application. \nBy clicking on an application in the list you change its permission.")
			.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					SharedPreferences.Editor editor = app_preferences.edit();
					editor.putBoolean("first_run_config", false);
					editor.commit();
					mThreadFillApplicationList = new ThreadFillApplicationList();
					mThreadFillApplicationList.start();
				}
			})
			.show();
		} else {
			mThreadFillApplicationList = new ThreadFillApplicationList();
			mThreadFillApplicationList.start();			
		}

		
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	
	@Override
	protected void onDestroy() {
		if(mPermissionDatabase!=null){
			mPermissionDatabase.close();
		}
		super.onDestroy();
	}
	
	ProgressDialog mProgressDialog = null;
	
	private ThreadFillApplicationList mThreadFillApplicationList = null;

	private class ThreadFillApplicationList extends Thread{
		public void run(){
			List<ApplicationInfo> list = mPackageManager.getInstalledApplications(0);
			Collections.sort(list, new ApplicationInfo.DisplayNameComparator(mPackageManager)); 

			int iListSize = list.size();
			LABELS = new String[iListSize];
			ICONS = new Bitmap[iListSize];
			PERMISSIONS = new int[iListSize];
			PACKAGENAMES = new String[iListSize];
			CLASSNAMES = new String[iListSize];
			int iInitialized = 0;
			
			mProgressDialog.dismiss();
			for (int index=0; index<iListSize; index++) { 
				ApplicationInfo content = list.get(index);
				Intent intent = null;
				intent = mPackageManager.getLaunchIntentForPackage(content.packageName);
				if(intent!=null){

					LABELS[iInitialized]= (String) mPackageManager.getApplicationLabel(content);
					BitmapDrawable bm = (BitmapDrawable) mPackageManager.getApplicationIcon(content);
					ICONS[iInitialized]= bm.getBitmap();

					PACKAGENAMES[iInitialized] = content.packageName;
					CLASSNAMES[iInitialized] = content.className;
					String strUniqueId = ConfigurationDatabase.getUniqueId(content.packageName, content.className);
					int iPermission = mPermissionDatabase.getPermissionValue(strUniqueId);
					PERMISSIONS[iInitialized] = iPermission;

					iInitialized++;
					//send the number of applications that are initialized
					mHandleListChanges.sendEmptyMessage(iInitialized);

				}

			} 
		}
	};

	OnItemClickListener mClickListener = new OnItemClickListener(){ 
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			int iPermission = PERMISSIONS[position];
			iPermission++;
			if(iPermission>3){
				iPermission=1;
			}

			PERMISSIONS[position] = iPermission;
			String strUniqueId = ConfigurationDatabase.getUniqueId(PACKAGENAMES[position], CLASSNAMES[position]);
			mPermissionDatabase.updateEntry(strUniqueId, iPermission);
			view.invalidate();
			mAdapter.notifyDataSetChanged();
		}
	};


	

	Handler mHandleListChanges = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			mNumberOfApplications = msg.what;
			mAdapter.notifyDataSetChanged();
			super.handleMessage(msg);
		}
	};
	
	
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, 0, 0, "Help").setIcon(android.R.drawable.ic_menu_help);
		
		return true;
	}


	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch (item.getItemId()) {
		case 0:
			new AlertDialog.Builder(this)
			.setTitle("Help")
			.setMessage("Using Cuckoo allows you to remotely start applications on your device. \nTo control the behaviour of Cuckoo when an application is remotely launched you can set a specific permission for each application. \nBy clicking on an application in the list you change its permission.")
			.setNeutralButton("Close", new DialogInterface.OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					
				}})
			.show();
		
			return true;
		}
		return false;
	}
	
	
	



	private static String[] LABELS = {};
	private static Bitmap[] ICONS = {};
	private static int[] PERMISSIONS = {};
	private static String[] PACKAGENAMES = {};
	private static String[] CLASSNAMES = {};

	
}
