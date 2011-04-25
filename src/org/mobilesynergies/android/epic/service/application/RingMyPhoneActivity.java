package org.mobilesynergies.android.epic.service.application;

import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.core.Preferences;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.widget.TextView;

/**
 * Rings the phone. 
 * 
 * @author Peter
 */

public class RingMyPhoneActivity extends Activity {

	private Ringtone mRingtone = null;
	
	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		playRingtone();
		
		
	}
	

	private void playRingtone( ) {
		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		if (uri != null) {
			mRingtone = RingtoneManager.getRingtone(this, uri);
			mRingtone.setStreamType(AudioManager.STREAM_RING);
			if (mRingtone != null) mRingtone.play();
			
		}
	}
	
	@Override
	protected void onPause() {
		if(mRingtone!=null){
			if(mRingtone.isPlaying()){
				mRingtone.stop();
				mRingtone=null;
			}
		}
		
		super.onPause();
	}


}