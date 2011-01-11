package com.sofurry.favorites;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.RemoteViews;

public class LiveWallpaperWidgetService extends Service {

	public static final String ACTION_WIDGET_OPENBROWSER = "openBrowser";
	public static final String ACTION_WIDGET_NEXTWALLPAPER = "nextWallpaper";
	public static final String ACTION_WIDGET_SAVEFILE = "saveFile";
	public static final String UPDATE = "update";

	IWallpaperRemote mService = null;
	RemoteServiceConnection mConnection = null;
	boolean isBound = false;

	@Override
	public void onStart(Intent intent, int startId) {
		String command = intent.getAction();
		int appWidgetId = intent.getExtras().getInt(
				AppWidgetManager.EXTRA_APPWIDGET_ID);
		RemoteViews remoteView = new RemoteViews(getApplicationContext()
				.getPackageName(), R.layout.widget);
		AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(getApplicationContext());

		if (!isBound || mService == null) {
            Log.i("LiveWallpaperWidgetService", "connecting...");
            mConnection = new RemoteServiceConnection();
            Log.i("LiveWallpaperWidgetService", "binding...");
			//getApplicationContext().bindService(new Intent(IWallpaperRemote.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
            Intent i = new Intent("com.sofurry.favorites.IWallpaperRemote");
            getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
            getApplicationContext().startService(i);
            Log.i("LiveWallpaperWidgetService", "binding done.");
		}
		
        if (command == null) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: No command");
        } else if (command.equals(ACTION_WIDGET_OPENBROWSER)) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: Open Browser");
        	if (mService != null) {
                Log.i("LiveWallpaperWidgetService", "past mService check");
				try {
					mService.remoteOpenBrowser();
				} catch (RemoteException e) {
	                Log.e("LiveWallpaperWidgetService", "error calling remote function", e);
				}
			}
		} else if (command.equals(ACTION_WIDGET_NEXTWALLPAPER)) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: Next Wallpaper");
			if (mService != null) {
				try {
					mService.remoteNextWallpaper();
				} catch (RemoteException e) {
	                Log.e("LiveWallpaperWidgetService", "error calling remote function", e);
				}
			}
		} else if (command.equals(ACTION_WIDGET_SAVEFILE)) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: Save File");
			if (mService != null) {
				try {
					mService.remoteSaveFile();
				} catch (RemoteException e) {
	                Log.e("LiveWallpaperWidgetService", "error calling remote function", e);
				}
			}
		}
		
		//set buttons
		remoteView.setOnClickPendingIntent(R.id.button_one,LiveWallpaperWidget.makeControlPendingIntent(getApplicationContext(),ACTION_WIDGET_OPENBROWSER,appWidgetId));
		remoteView.setOnClickPendingIntent(R.id.button_two,LiveWallpaperWidget.makeControlPendingIntent(getApplicationContext(),ACTION_WIDGET_NEXTWALLPAPER,appWidgetId));

		// apply changes to widget
		appWidgetManager.updateAppWidget(appWidgetId, remoteView);
		super.onStart(intent, startId);
	}

    class RemoteServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, 
			IBinder service ) {
            mService = IWallpaperRemote.Stub.asInterface(service);
            isBound = true;
            Log.i("WidgetServiceConnection", "binding to service succeeded");
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
            isBound = false;
            Log.i("WidgetServiceConnection", "binding to service lost!");
        }
    };

    
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
        Log.i("LiveWallpaperWidgetService", "onDestroy");
		if (mConnection != null)
			getApplicationContext().unbindService(mConnection);
	}
}
