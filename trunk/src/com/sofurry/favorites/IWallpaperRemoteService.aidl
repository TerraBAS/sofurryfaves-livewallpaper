package com.sofurry.favorites;

interface IWallpaperRemoteService {
	void remoteOpenBrowser();
	void remoteNextWallpaper();
	void remoteSaveFile();
	boolean isLiveWallpaperRunning();
}