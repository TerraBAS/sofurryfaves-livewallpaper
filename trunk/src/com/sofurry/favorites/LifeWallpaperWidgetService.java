package com.sofurry.favorites;

import java.util.Date;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.widget.RemoteViews;

public class LiveWallpaperWidgetService extends Service {
	public static final String UPDATE = "update";
	public static final String PLUS = "plus";
	public static final String MINUS = "minus";
	public static final long  MODIFY= 86400000;

	@Override
	public void onStart(Intent intent, int startId) {
		String command = intent.getAction();
		int appWidgetId = intent.getExtras().getInt(
				AppWidgetManager.EXTRA_APPWIDGET_ID);
		RemoteViews remoteView = new RemoteViews(getApplicationContext()
				.getPackageName(), R.layout.countdownwidget);
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(getApplicationContext());
		SharedPreferences prefs = getApplicationContext().getSharedPreferences(
				"prefs", 0);
		
		
		//plus button pressed
		if(command.equals(PLUS)){
			SharedPreferences.Editor edit=prefs.edit();
			edit.putLong("goal" + appWidgetId,prefs.getLong("goal" + appWidgetId, 0)+MODIFY);
			edit.commit();
		//minus button pressed
		}else if(command.equals(MINUS)){
			SharedPreferences.Editor edit=prefs.edit();
			edit.putLong("goal" + appWidgetId,prefs.getLong("goal" + appWidgetId, 0)-MODIFY);
			edit.commit();
		}
		
		
		long goal = prefs.getLong("goal" + appWidgetId, 0);
		//compute the time left
		long left = goal - new Date().getTime();
		int days = (int) Math.floor(left / (long) (60 * 60 * 24 * 1000));
		left = left - days * (long) (60 * 60 * 24 * 1000);
		int hours = (int) Math.floor(left / (60 * 60 * 1000));
		left = left - hours * (long) (60 * 60 * 1000);
		int mins = (int) Math.floor(left / (60 * 1000));
		left = left - mins * (long) (60 * 1000);
		int secs = (int) Math.floor(left / (1000));
		//put the text into the textView
		remoteView.setTextViewText(R.id.TextView01, days + " days\n" + hours
				+ " hours " + mins + " mins " + secs + " secs left");
		//set buttons
		remoteView.setOnClickPendingIntent(R.id.plusbutton,CountdownWidget.makeControlPendingIntent(getApplicationContext(),PLUS,appWidgetId));
		remoteView.setOnClickPendingIntent(R.id.minusbutton,CountdownWidget.makeControlPendingIntent(getApplicationContext(),MINUS,appWidgetId));
		// apply changes to widget
		appWidgetManager.updateAppWidget(appWidgetId, remoteView);
		super.onStart(intent, startId);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

}
