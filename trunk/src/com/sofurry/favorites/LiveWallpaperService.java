package com.sofurry.favorites;

import android.content.SharedPreferences;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

/**
 * 
 * A sample class that defines a LiveWallpaper an it's associated Engine.
 * The Engine delegates all the Wallpaper painting stuff to a specialized Thread.
 * 
 * Sample from <a href="http://blog.androgames.net">Androgames tutorials blog</a>
 * Under GPL v3 : http://www.gnu.org/licenses/gpl-3.0.html
 * 
 * @author antoine vianey
 *
 */
public class LiveWallpaperService extends WallpaperService {

	public static final String PREFERENCES = "com.sofurry.favorites.livewallpaper";
	public static final String PREFERENCE_ROTATETIME = "preference_rotatetime";
	public static final String PREFERENCE_CONTENTLEVEL = "preference_contentlevel";
	public static final String PREFERENCE_CONTENTSOURCE = "preference_contentsource";
	public static final String PREFERENCE_SEARCH = "preference_search";
	public static final String PREFERENCE_USERNAME = "preference_username";
	public static final String PREFERENCE_PASSWORD = "preference_password";

	@Override
	public Engine onCreateEngine() {
		Log.d("LW Service", "onCreateEngine");
		return new SampleEngine();
	}

	@Override
	public void onCreate() {
		Log.d("LW Service", "onCreate");
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.d("LW Service", "onDestroy");
		super.onDestroy();
	}

	public class SampleEngine extends Engine implements SharedPreferences.OnSharedPreferenceChangeListener {

		private LiveWallpaperPainting painting;
		private SharedPreferences prefs;
		
		SampleEngine() {
			SurfaceHolder holder = getSurfaceHolder();
			prefs = LiveWallpaperService.this.getSharedPreferences(PREFERENCES, 0);
			prefs.registerOnSharedPreferenceChangeListener(this);
			painting = new LiveWallpaperPainting(holder, getApplicationContext(), 
					Integer.parseInt(prefs.getString(PREFERENCE_ROTATETIME, "1800")),
					Integer.parseInt(prefs.getString(PREFERENCE_CONTENTLEVEL, "0")),
					Integer.parseInt(prefs.getString(PREFERENCE_CONTENTSOURCE, "1")),
					prefs.getString(PREFERENCE_SEARCH, ""),
					prefs.getString(PREFERENCE_USERNAME, ""),
					prefs.getString(PREFERENCE_PASSWORD, ""));
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			painting.setRotateInterval(Integer.parseInt(prefs.getString(PREFERENCE_ROTATETIME, "1800")));
			painting.setContentLevel(Integer.parseInt(prefs.getString(PREFERENCE_CONTENTLEVEL, "0")));
			painting.setContentSource(Integer.parseInt(prefs.getString(PREFERENCE_CONTENTSOURCE, "1")));
			painting.setSearch(prefs.getString(PREFERENCE_SEARCH, ""));
			painting.setUsername(prefs.getString(PREFERENCE_USERNAME, ""));
			painting.setPassword(prefs.getString(PREFERENCE_PASSWORD, ""));
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			Log.d("SoFurryLW", "onCreate");
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy() {
			Log.d("SoFurryLW", "onDestroy");
			super.onDestroy();
			// remove listeners and callbacks here
			painting.stopPainting();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			Log.d("LW Service", "onVisibilityChanged "+visible);
			if (visible) {
				painting.resumePainting();
			} else {
				// remove listeners and callbacks here
				painting.pausePainting();
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			Log.d("LW Service", "onSurfaceChanged");
			super.onSurfaceChanged(holder, format, width, height);
			painting.setSurfaceSize(width, height);
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			Log.d("LW Service", "onSurfaceCreated");
			super.onSurfaceCreated(holder);
			painting.start();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			Log.d("LW Service", "onSurfaceDestroy");
			super.onSurfaceDestroyed(holder);
			boolean retry = true;
			painting.stopPainting();
			while (retry) {
				try {
					painting.join();
					retry = false;
				} catch (InterruptedException e) {}
			}
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset, 
				float xStep, float yStep, int xPixels, int yPixels) {
			Log.d("LW Service", "onOffsetsChanged");
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			super.onTouchEvent(event);
			painting.doTouchEvent(event);
		}
		
	}
	
}