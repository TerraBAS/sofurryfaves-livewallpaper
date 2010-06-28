package com.sofurry.favorites;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import android.graphics.Bitmap;
import android.util.Log;

import com.sofurry.favorites.util.FileStorage;
import com.sofurry.favorites.util.SubmissionStorage;

public class ContentLoaderThread extends Thread {
	boolean runIt = true;
	LinkedBlockingQueue<WallpaperEntry> resultList;

	// Set saveUserAvatar to true to save the returned thumbnail as the
	// submission's user avatar
	public ContentLoaderThread(LinkedBlockingQueue<WallpaperEntry> resultList) {
		this.resultList = resultList;
	}

	public void stopThread() {
		runIt = false;
	}

	public void run() {
		while (runIt) {

			while (resultList.size() < 5) {
				// TODO: Load submission data and images
				Bitmap image = null;
				WallpaperEntry entry = LiveWallpaperPainting.getNewWallpaper();
				if (entry.getImageUrl() != null) {
					Log.d("SF Preloader", "Pre-loaded Image: " + entry.getImageUrl());
					image = LiveWallpaperPainting.fetchBitmap(entry);
					SubmissionStorage.saveSubmissionImage(entry.getId(), image);
					resultList.add(entry);
					ArrayList<String> fileList = new ArrayList<String>();
					for (WallpaperEntry e : resultList) {
						fileList.add("image"+e.getId());
					}
					FileStorage.clearFileCache(fileList);
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
