package com.sofurry.favorites.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

public class FileStorage {

	private static Context context;
	private static boolean mExternalStorageAvailable = false;
	private static boolean mExternalStorageWriteable = false;

	public static FileOutputStream getFileOutputStream(String filename) throws IOException {
		checkExternalMedia();
		File f = null;
		if (!mExternalStorageAvailable || !mExternalStorageWriteable) {
			f = new File(context.getCacheDir() + "/" + filename);
		} else {	
			File d = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.sofurry.favorites/");
			d.mkdirs();
			f = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.sofurry.favorites/"+filename);
		}
		Log.i("FileStorage", "writing file " + f.getAbsolutePath()+" - "+filename);
		if (f.createNewFile() && f.canWrite()) {
			return new FileOutputStream(f);
		}
		return null;
	}

	public static FileInputStream getFileInputStream(String filename) throws FileNotFoundException {
		checkExternalMedia();
		File f = null;
		if (!mExternalStorageAvailable || !mExternalStorageWriteable) {
			f = new File(context.getCacheDir() + "/" + filename);
		} else {
			f = new File(Environment.getExternalStorageDirectory()+"/Android/data/com.sofurry.favorites/"+filename);
		}
		if (f.canRead()) {
			Log.i("FileStorage", "reading file " + f.getAbsolutePath()+" - "+filename);
			return new FileInputStream(f);
		} else {
			Log.i("FileStorage", "Can't read file " + filename);
		}
		return null;
	}

	public static void deleteFile(String filename) {
		File f = new File(Environment.getDownloadCacheDirectory() + "/" + filename);
		if (f.canRead()) {
			f.delete();
		}
	}
	
	public static void clearFileCache() {
		File dir = Environment.getDownloadCacheDirectory();
		if (dir != null && dir.isDirectory()) {
			try {
				File[] children = dir.listFiles();
				if (children.length > 0) {
					for (int i = 0; i < children.length; i++) {
						File[] temp = children[i].listFiles();
						for (int x = 0; x < temp.length; x++) {
							temp[x].delete();
						}
					}
				}
			} catch (Exception e) {
				Log.e("FileStorage", "failed to clean cache", e);
			}
		}

	}
	
	private static void checkExternalMedia() {
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}
	}

	public static void setContext(Context c) {
		context = c;
	}

}
