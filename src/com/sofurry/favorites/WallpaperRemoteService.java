package com.sofurry.favorites;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

public class WallpaperRemoteService extends Service {


    public IBinder onBind(Intent intent) {
    	Log.i("WallpaperRemoteService", "onBind() called");
    	return new WallpaperRemoteServiceImpl();
    }
    
    /**
     * The IRemoteInterface is defined through IDL
     */
    public class WallpaperRemoteServiceImpl extends IWallpaperRemoteService.Stub {
    	public void remoteOpenBrowser() throws RemoteException {
    		LiveWallpaperPainting.launchBrowser();
    	}
    	public void remoteNextWallpaper() throws RemoteException {
    		LiveWallpaperPainting.launchNextImage();
    	}
    	public void remoteSaveFile() throws RemoteException {
    		LiveWallpaperPainting.launchSaveImage();
    	}
		public boolean isLiveWallpaperRunning() throws RemoteException {
			return LiveWallpaperPainting.isLiveWallpaperRunning();
		}
    	
    };

	
}
