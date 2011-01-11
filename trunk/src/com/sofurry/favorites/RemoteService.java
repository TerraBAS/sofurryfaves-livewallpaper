package com.sofurry.favorites;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class RemoteService extends Service {


    public IBinder onBind(Intent intent) {
    	return mBinder;
    }
    
    /**
     * The IRemoteInterface is defined through IDL
     */
    private final IWallpaperRemote.Stub mBinder = new IWallpaperRemote.Stub() {
    	public void remoteOpenBrowser() throws RemoteException {
    		LiveWallpaperPainting.launchBrowser();
    	}
    	public void remoteNextWallpaper() throws RemoteException {
    		LiveWallpaperPainting.launchNextImage();
    	}
    	public void remoteSaveFile() throws RemoteException {
    		LiveWallpaperPainting.launchSaveImage();
    	}
    };

	
}
