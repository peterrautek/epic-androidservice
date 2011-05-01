package org.mobilesynergies.android.epic.service.application;

import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.core.Preferences;

import android.app.Activity;
import android.app.KeyguardManager;
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
import android.os.PowerManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.Window;
import android.widget.TextView;

/**
 * Rings the phone. 
 * 
 * @author Peter
 */

public class RingMyPhoneActivity extends Activity {

	private static final String CLASS_TAG = "RingMyPhoneActivity";
	private Ringtone mRingtone = null;

	PowerManager.WakeLock mWakeLock = null;
	KeyguardManager mKeyguardManager = null; 
	KeyguardManager.KeyguardLock mKeyguardLock = null;


	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.ringit);
		mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);		

		try {
			//this seems to fix an android bug concerning the keyguard and wake lock!?
			//not completely sure though
			Thread.sleep(750);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		PowerManager pm = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, CLASS_TAG);
		/*	if(!mWakeLock.isHeld()){
			mWakeLock.acquire();
		}*/


	}

	@Override
	protected void onResume() {
		if(!mWakeLock.isHeld()){
			mWakeLock.acquire();
		}
		disableKeyguard();

		playRingtone();
		super.onResume();
	}


	@Override
	protected void onDestroy() {
		if(mWakeLock.isHeld()){
			mWakeLock.release();
		}
		enableKeyguard();
		super.onDestroy();
	}

	private void playRingtone( ) {
		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
		if (uri != null) {
			mRingtone = RingtoneManager.getRingtone(this, uri);

			if (mRingtone != null) {
				Log.e(CLASS_TAG, "Playing ringtone!");
				mRingtone.setStreamType(AudioManager.STREAM_RING);
				mRingtone.play();

			} else {
				Log.e(CLASS_TAG, "Could not play ringtone!");
			}

		} else {
			Log.e(CLASS_TAG, "Could not play audio file! File unknown!");
		}
	}

	@Override
	protected void onPause() {
		if(mWakeLock.isHeld()){
			mWakeLock.release();
		}

		if(mRingtone!=null){
			if(mRingtone.isPlaying()){
				mRingtone.stop();
				mRingtone=null;
			}
		}

		super.onPause();
	}



	private synchronized void enableKeyguard() {
		if (mKeyguardLock != null) {
			mKeyguardLock.reenableKeyguard();
			mKeyguardLock = null;
		}
	}
	private synchronized void disableKeyguard() {
		if (mKeyguardLock == null) {
			mKeyguardLock = mKeyguardManager.newKeyguardLock(CLASS_TAG);
			mKeyguardLock.disableKeyguard();
		}

	}



}