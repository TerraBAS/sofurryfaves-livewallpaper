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
import android.widget.Toast;

public class LiveWallpaperWidgetService extends Service {

	public static final String ACTION_WIDGET_OPENBROWSER = "openBrowser";
	public static final String ACTION_WIDGET_NEXTWALLPAPER = "nextWallpaper";
	public static final String ACTION_WIDGET_SAVEFILE = "saveFile";
	public static final String UPDATE = "update";

	IWallpaperRemoteService mService = null;
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
			getApplicationContext().bindService(new Intent(IWallpaperRemoteService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
            Log.i("LiveWallpaperWidgetService", "binding done.");
		}
		
        if (command == null) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: No command");
        } else if (command.equals(ACTION_WIDGET_OPENBROWSER)) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: Open Browser");
        	if (mService != null) {
                Log.i("LiveWallpaperWidgetService", "past mService check");
				try {
					if (mService.isLiveWallpaperRunning() == false) {
						showLWPWarning();
					} else {
						Toast toast = Toast.makeText(getApplicationContext(), "Opening current wallpaper on sofurry.com...", 1);
						toast.show();
						mService.remoteOpenBrowser();
					}
				} catch (RemoteException e) {
	                Log.e("LiveWallpaperWidgetService", "error calling remote function", e);
				}
			}
		} else if (command.equals(ACTION_WIDGET_NEXTWALLPAPER)) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: Next wallpaper");
			if (mService != null) {
				try {
					if (mService.isLiveWallpaperRunning() == false) {
						showLWPWarning();
					} else {
						Toast toast = Toast.makeText(getApplicationContext(), "Loading next wallpaper...", 1);
						toast.show();
						mService.remoteNextWallpaper();
					}
				} catch (RemoteException e) {
	                Log.e("LiveWallpaperWidgetService", "error calling remote function", e);
				}
			}
		} else if (command.equals(ACTION_WIDGET_SAVEFILE)) {
            Log.i("LiveWallpaperWidgetService", "COMMAND: Save File");
			if (mService != null) {
				try {
					if (mService.isLiveWallpaperRunning() == false) {
						showLWPWarning();
					} else {
						Toast toast = Toast.makeText(getApplicationContext(), "Current wallpaper saved in gallery.", 1);
						toast.show();
						mService.remoteSaveFile();
					}
				} catch (RemoteException e) {
	                Log.e("LiveWallpaperWidgetService", "error calling remote function", e);
				}
			}
		}
		
		//set buttons
		remoteView.setOnClickPendingIntent(R.id.button_one,LiveWallpaperWidget.makeControlPendingIntent(getApplicationContext(),ACTION_WIDGET_OPENBROWSER,appWidgetId));
		//TODO: Implement back function
		//remoteView.setOnClickPendingIntent(R.id.button_two,LiveWallpaperWidget.makeControlPendingIntent(getApplicationContext(),ACTION_WIDGET_PREVWALLPAPER,appWidgetId));
		remoteView.setOnClickPendingIntent(R.id.button_three,LiveWallpaperWidget.makeControlPendingIntent(getApplicationContext(),ACTION_WIDGET_NEXTWALLPAPER,appWidgetId));
		remoteView.setOnClickPendingIntent(R.id.button_four,LiveWallpaperWidget.makeControlPendingIntent(getApplicationContext(),ACTION_WIDGET_SAVEFILE,appWidgetId));

		// apply changes to widget
		appWidgetManager.updateAppWidget(appWidgetId, remoteView);
		super.onStart(intent, startId);
	}

	private void showLWPWarning() {
        Toast toast = Toast.makeText(getApplicationContext(), "Please activate the SoFurry Live Wallpaper!", 3);
        toast.show();
	}

	
    class RemoteServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className, 
			IBinder service ) {
            mService = IWallpaperRemoteService.Stub.asInterface(service);
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
