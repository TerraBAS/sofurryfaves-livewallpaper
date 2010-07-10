package com.sofurry.favorites.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class SubmissionStorage {

	
	public static Bitmap loadSubmissionImage(int id) {
		return loadIcon("image"+id);
	}

	public static void saveSubmissionImage(int id, Bitmap icon) {
		saveIcon("image"+id, icon);
	}
	
	public static void deleteSubmissionImage(int id) {
		FileStorage.deleteFile("image"+id);
	}
	
	private static Bitmap loadIcon(String filename) {
		FileInputStream is;
		Bitmap bitmap = null;
		try {
			is = FileStorage.getFileInputStream(filename);
			if (is != null && is.available() > 0) {
				bitmap = BitmapFactory.decodeStream(is);
			} else {
				Log.w("soFurryApp", "Can't load from external storage");
			}
		} catch (Exception e) {
			Log.e("soFurryApp", "error in loadIcon", e);
		}
		
		return bitmap;
	}
	
	private static void saveIcon(String filename, Bitmap icon) {
		FileOutputStream os;
		try {
			os = FileStorage.getFileOutputStream(filename );
			if (os != null) {
				icon.compress(CompressFormat.JPEG, 80, os);
			} else {
				Log.w("soFurryApp", "Can't save to external storage");
			}
		} catch (Exception e) {
			Log.e("soFurryApp", "error in saveIcon", e);
		}
	}

	public static String saveImageToGallery(String filename, Bitmap icon) {
		String fullfilepath = null;
	    try {
		    File path = new File(Environment.getExternalStorageDirectory()+"/FurryWallpapers");
	        path.mkdirs();
		    File file = new File(path, filename);
		    fullfilepath = file.getAbsolutePath();
		    Log.d("SF Wallpaper", "Saving image "+file.getAbsolutePath());
//		    file.createNewFile();

	        FileOutputStream os = new FileOutputStream(file);
			icon.compress(CompressFormat.JPEG, 80, os);
	        os.close();
	    } catch (Exception e) {
	        Log.w("ExternalStorage", "Error writing file", e);
	    }
	    return fullfilepath;
	}


}
