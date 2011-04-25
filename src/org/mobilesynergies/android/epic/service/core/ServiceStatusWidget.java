package org.mobilesynergies.android.epic.service.core;

import org.mobilesynergies.android.epic.service.EpicService;
import org.mobilesynergies.android.epic.service.R;
import org.mobilesynergies.android.epic.service.administration.LogActivity;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class ServiceStatusWidget extends AppWidgetProvider{

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d("WordWidget.UpdateService", "onUpdate()");
		// To prevent any ANR timeouts, we perform the update in a service
		context.startService(new Intent(context, EpicService.class));
	}

	public void update(Context context, String state) {
		RemoteViews updateViews = buildUpdate(context, state);
		// Push update for this widget to the home screen
		ComponentName thisWidget = new ComponentName(context, ServiceStatusWidget.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(context);
		manager.updateAppWidget(thisWidget, updateViews);
	}

	public RemoteViews buildUpdate(Context context, String state) {
		// Build an update that holds the updated widget contents
		RemoteViews views = null;
		views = new RemoteViews(context.getPackageName(), R.layout.widget_word);

		String wordTitle = "State";
		views.setTextViewText(R.id.word_title, wordTitle);
		views.setTextViewText(R.id.word_type, " of service:");
		views.setTextViewText(R.id.definition, state);

		// When user clicks on widget, launch log activity
		Intent intent = new Intent();
		intent.setClassName("org.mobilesynergies.android.epic.service", LogActivity.class.getCanonicalName());
		PendingIntent pendingIntent = PendingIntent.getActivity(context,
				0 /* no requestCode */, intent, 0 /* no flags */);
		views.setOnClickPendingIntent(R.id.widget, pendingIntent);
		return views;
	}


}
