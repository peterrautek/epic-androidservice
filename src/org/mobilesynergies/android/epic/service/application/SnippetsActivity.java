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
 * Extracts the information received from the cloud and tries to perform some useful action.
 * The message must contain a title, and url field and might contain a selection field.
 * 
 * Currently implemented actions: 
 * start web browser
 * start maps
 * start youtube
 * start dialer (if selection is a number)
 * copy text to clipboard (if text is selected)
 * 
 *  
 * 
 * @author Peter
 */

public class SnippetsActivity extends Activity {

	/** 
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent callingIntent = getIntent();
		if(callingIntent!=null){
			handleSnippet(callingIntent);
		}
		finish();
	}
	

	



	public void handleSnippet(Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			String url = (String) extras.get("url");
			if(url!=null){
				url = url.trim();
			}
			String title = (String) extras.get("title");
			if(title!=null){
				title = title.trim();
			}
			String selection = (String) extras.get("selection");
			if(selection!=null){
				selection = selection.trim();
			}


			if (title != null && url != null && url.startsWith("http")) {
				Intent launchIntent = getLaunchIntent(url, title, selection);

				if (launchIntent != null) {
					playNotificationSound();
					startActivity(launchIntent);
				} else {
					if (selection != null && selection.length() > 0) {  // have selection
						generateNotification(selection, "copied the selected string to the clipboard", launchIntent);
					} else {
						generateNotification(url, title, launchIntent);
					}
				}
			}
		}
	}
	
	

	private Intent getLaunchIntent(String url, String title, String sel) {
		Intent intent = null;
		String number = parseTelephoneNumber(sel);
		if (number != null) {
			intent = new Intent("android.intent.action.DIAL",
					Uri.parse("tel:" + number));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			ClipboardManager cm =
				(ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			cm.setText(number);
		} else if (sel != null && sel.length() > 0) {
			// No intent for selection - just copy to clipboard
			ClipboardManager cm =
				(ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			cm.setText(sel);
		} else {
			final String GMM_PACKAGE_NAME = "com.google.android.apps.maps";
			final String GMM_CLASS_NAME = "com.google.android.maps.MapsActivity";

			intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			if (isMapsURL(url)) {
				intent.setClassName(GMM_PACKAGE_NAME, GMM_CLASS_NAME);
			}

			// Fall back if we can't resolve intent (i.e. app missing)
			PackageManager pm = getPackageManager();
			if (null == intent.resolveActivity(pm)) {
				intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			}
		}
		return intent;
	}

	private void generateNotification(String msg, String title, Intent intent) {
		int icon = R.drawable.notification_icon;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, title, when);
		notification.setLatestEventInfo(this, title, msg,
				PendingIntent.getActivity(this, 0, intent, 0));
		notification.flags |= Notification.FLAG_AUTO_CANCEL;

		SharedPreferences settings = Preferences.getServiceSettings(this);
		int notificatonID = settings.getInt("notificationID", 0); // allow multiple notifications

		NotificationManager nm =
			(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		nm.notify(notificatonID, notification);
		playNotificationSound();

		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("notificationID", ++notificatonID % 32);
		editor.commit();
	}

	private void playNotificationSound( ) {
		Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		if (uri != null) {
			Ringtone rt = RingtoneManager.getRingtone(this, uri);
			rt.setStreamType(AudioManager.STREAM_NOTIFICATION);
			if (rt != null) rt.play();
		}
	}

	private String parseTelephoneNumber(String sel) {
		if (sel == null || sel.length() == 0) return null;

		// Hack: Remove trailing left-to-right mark (Google Maps adds this)
		if (sel.codePointAt(sel.length() - 1) == 8206) {
			sel = sel.substring(0, sel.length() - 1);
		}

		String number = null;
		if (sel.matches("([Tt]el[:]?)?\\s?[+]?(\\(?[0-9|\\s|\\-|\\.]\\)?)+")) {
			String elements[] = sel.split("([Tt]el[:]?)");
			number = elements.length > 1 ? elements[1] : elements[0];
			number = number.replace(" ", "");

			// Remove option (0) in international numbers, e.g. +44 (0)20 ...
			if (number.matches("\\+[0-9]{2,3}\\(0\\).*")) {
				int openBracket = number.indexOf('(');
				int closeBracket = number.indexOf(')');
				number = number.substring(0, openBracket) +
				number.substring(closeBracket + 1);
			}
		}
		return number;
	}

	private boolean isMapsURL(String url) {
		return url.matches("http://maps\\.google\\.[a-z]{2,3}(\\.[a-z]{2})?[/?].*") ||
		url.matches("http://www\\.google\\.[a-z]{2,3}(\\.[a-z]{2})?/maps.*");
	}




}