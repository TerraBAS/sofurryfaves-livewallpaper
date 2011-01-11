package com.sofurry.favorites;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.RemoteViews;
import android.widget.Toast;

public class LiveWallpaperWidget extends AppWidgetProvider {

	public static String ACTION_WIDGET_OPENBROWSER = "OpenBrowser";
	public static String ACTION_WIDGET_NEXTWALLPAPER = "NextWallpaper";
	public static String ACTION_WIDGET_SAVEFILE = "SaveFile";

	IWallpaperRemote mService = null;
	boolean isBound = false;

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        context.bindService(new Intent(IWallpaperRemote.class.getName()),
                mConnection, Context.BIND_AUTO_CREATE);

		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
//		Intent configIntent = new Intent(context, ClickOneActivity.class);
//		configIntent.setAction(ACTION_WIDGET_CONFIGURE);
		
		Intent activeBrowser = new Intent(context, LiveWallpaperWidget.class);
		activeBrowser.setAction(ACTION_WIDGET_OPENBROWSER);
		PendingIntent openBrowserIntent = PendingIntent.getBroadcast(context, 0, activeBrowser, 0);

		Intent activeNext = new Intent(context, LiveWallpaperWidget.class);
		activeNext.setAction(ACTION_WIDGET_OPENBROWSER);
		PendingIntent nextWallpaperIntent = PendingIntent.getActivity(context, 0, activeNext, 0);
		
		remoteViews.setOnClickPendingIntent(viewId, pendingIntent)ClickPendingIntent(viewId, pendingIntent)PendingIntent(R.id.button_one, openBrowserIntent);
		remoteViews.setOnClickPendingIntent(R.id.button_two, nextWallpaperIntent);

		appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
	}
	
	@Override
	public void onReceive(Context context, Intent intent) {
		
		// v1.5 fix that doesn't call onDelete Action
		final String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else {
			// check, if our Action was called
			if (intent.getAction().equals(ACTION_WIDGET_OPENBROWSER)) {
				String msg = "Viewing wallpaper on sofurry.com";
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
				if (mService != null) {
					try {
						mService.remoteOpenBrowser();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (intent.getAction().equals(ACTION_WIDGET_NEXTWALLPAPER)) {
				String msg = "Next wallpaper";
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
				if (mService != null) {
					try {
						mService.remoteNextWallpaper();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} else if (intent.getAction().equals(ACTION_WIDGET_SAVEFILE)) {
				String msg = "Wallpaper saved to gallery";
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
				
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
				NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification noty = new Notification(R.drawable.icon, msg, System.currentTimeMillis());
				noty.setLatestEventInfo(context, "Notice", msg, contentIntent);
				notificationManager.notify(1, noty);

				if (mService != null) {
					try {
						mService.remoteSaveFile();
					} catch (RemoteException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} else {
				
			}
			
			super.onReceive(context, intent);
		}
	}
	
	@Override
	public void onDeleted(Context context, int[] widgetIDs) {
		if (mConnection != null)
			context.unbindService(mConnection);
	}
	
	
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  We are communicating with our
            // service through an IDL interface, so get a client-side
            // representation of that from the raw service object.
            mService = IWallpaperRemote.Stub.asInterface(service);

        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
        }
    };

	
}
