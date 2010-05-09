package com.sofurry.favorites;

import android.content.SharedPreferences;
import android.service.wallpaper.WallpaperService;
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
	public static final String PREFERENCE_USERNAME = "preference_username";
	public static final String PREFERENCE_PASSWORD = "preference_password";

	@Override
	public Engine onCreateEngine() {
		return new SampleEngine();
	}

	@Override
	public void onCreate() {
		super.onCreate();
	}

	@Override
	public void onDestroy() {
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
					prefs.getString(PREFERENCE_USERNAME, ""),
					prefs.getString(PREFERENCE_PASSWORD, ""));
		}

		public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
			painting.setRotateInterval(Integer.parseInt(prefs.getString(PREFERENCE_ROTATETIME, "1800")));
			painting.setContentLevel(Integer.parseInt(prefs.getString(PREFERENCE_CONTENTLEVEL, "0")));
			painting.setUsername(prefs.getString(PREFERENCE_USERNAME, ""));
			painting.setPassword(prefs.getString(PREFERENCE_PASSWORD, ""));
		}

		@Override
		public void onCreate(SurfaceHolder surfaceHolder) {
			super.onCreate(surfaceHolder);
			setTouchEventsEnabled(true);
		}

		@Override
		public void onDestroy() {
			super.onDestroy();
			// remove listeners and callbacks here
			painting.stopPainting();
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			if (visible) {
				painting.resumePainting();
			} else {
				// remove listeners and callbacks here
				painting.pausePainting();
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			painting.setSurfaceSize(width, height);
		}

		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
			painting.start();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
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
		}

		@Override
		public void onTouchEvent(MotionEvent event) {
			super.onTouchEvent(event);
			painting.doTouchEvent(event);
		}
		
	}
	
}