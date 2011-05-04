package org.mobilesynergies.android.epic.service.administration;

import java.util.HashMap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;


/**
 * The user can set a permission for each installed package. The package permission is stored in this data base.  
 * @author Peter
 *
 */
public class ConfigurationDatabase {

	private static final String CLASS_TAG = ConfigurationDatabase.class.getSimpleName();

	private static final String DATABASE_NAME = "permissions.db";
	private static final int DATABASE_VERSION = 1;

	private static final String TABLE_PERMISSIONS = "permissions";

	private static final String FIELD_PACKAGEID = "PACKAGE_ID";
	private static final String FIELD_PERMISSION = "PERMISSION";

	

	public static final int PERMISSION_UNKNOWN = 0;
	public static final int PERMISSION_ALLOW = 1;
	public static final int PERMISSION_DISALLOW = 2;
	public static final int PERMISSION_ASK = 3;
	
	private static HashMap<String, String> sProjectionMap;
	private DatabaseHelper mDatabaseHelper = null;

    public void close(){
    	if(mDatabaseHelper!=null){
    		mDatabaseHelper.close();
    	}
    }

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {

			db.execSQL("CREATE TABLE " + TABLE_PERMISSIONS + " ("
					+ FIELD_PACKAGEID + " TEXT NOT NULL, "
					+ FIELD_PERMISSION + " INT NOT NULL "
					+ ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(CLASS_TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS "+TABLE_PERMISSIONS);
			onCreate(db);
		}


	}


	public ConfigurationDatabase(Context context){
		mDatabaseHelper = new DatabaseHelper(context);
	}

/**
 * updates or creates a entry for package "packagename" with permission value permission 
 * @param packagename the package to be updated or created
 * @param permission the new permission
 * @return true
 */
	public boolean updateEntry(String packagename, int permission){
		if(packagename==null){
    		throw new IllegalArgumentException("Table " + TABLE_PERMISSIONS);
    	}
    	
		String[] projection = {FIELD_PACKAGEID, FIELD_PERMISSION};
		String selection = FIELD_PACKAGEID + " = ? ";
		String[] selectionArgs = {packagename}; 
		boolean bContains = contains(TABLE_PERMISSIONS, projection, selection, selectionArgs, null);

		ContentValues cv = new ContentValues();
		cv.put(FIELD_PERMISSION, permission);
		
		SQLiteDatabase db = null;
		try{
			db = mDatabaseHelper.getWritableDatabase();
			if(bContains){
				db.update(TABLE_PERMISSIONS, cv, selection, selectionArgs);
			} else {	
				cv.put(FIELD_PACKAGEID, packagename);
				db.insert(TABLE_PERMISSIONS, new String(), cv);
			}

		} finally {
			if(db!=null){
				db.close();
			}
		}
		return true;
	}
	

	public int getPermissionValue(String packagename) {
		if(packagename==null){
    		throw new IllegalArgumentException("Table " + TABLE_PERMISSIONS);
    	}
		
		SQLiteQueryBuilder qeryBuilder = new SQLiteQueryBuilder();

		qeryBuilder.setTables(TABLE_PERMISSIONS);
		qeryBuilder.setProjectionMap(sProjectionMap);
		
		// Get the database and run the query
		SQLiteDatabase db = null;
		Cursor c = null;
		
		String[] projection = {FIELD_PACKAGEID, FIELD_PERMISSION};
		String selection = FIELD_PACKAGEID + " = ? ";
		String[] selectionArgs = {packagename};
		
		int iPermission = PERMISSION_UNKNOWN;
		try{
			db = mDatabaseHelper.getReadableDatabase();
			c = qeryBuilder.query(db, projection, selection, selectionArgs, null, null, null);
			
			if(c==null){
				//Log.d(CLASS_TAG, "c is null!!!");
			} else if(c.getCount()==0){
				
			} else if (c.getCount()==1){
				c.moveToFirst();
				iPermission = c.getInt(1);
			} else {
				//error
				Log.e(CLASS_TAG, "inconsistent permission database!");
			}
		} finally {
			if(c!=null){
				c.close();
			}
			if(db!=null){
				db.close();
			}
		}
		return iPermission;
	}



	public boolean contains(String strTable, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder qeryBuilder = new SQLiteQueryBuilder();

		if(strTable.equalsIgnoreCase(TABLE_PERMISSIONS)){
			qeryBuilder.setTables(TABLE_PERMISSIONS);
			qeryBuilder.setProjectionMap(sProjectionMap);
		} else {
			throw new IllegalArgumentException("Table " + strTable);
		}

		// Get the database and run the query
		SQLiteDatabase db = null;
		Cursor c = null;
		boolean bContains = false;
		try{
			db = mDatabaseHelper.getReadableDatabase();
			c = qeryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
			if(c.getCount()>0){
				bContains=true;
			}
		} finally {
			if(c!=null){
				c.close();
			}
			if(db!=null){
				db.close();
			}
		}
		return bContains;
	}

	public static String getUniqueId(String packagename, String classname) {
		return packagename+"/"+classname;
	}


	static {

		sProjectionMap = new HashMap<String, String>();        
		sProjectionMap.put(FIELD_PACKAGEID, FIELD_PACKAGEID);
		sProjectionMap.put(FIELD_PERMISSION, FIELD_PERMISSION);

	}




}
