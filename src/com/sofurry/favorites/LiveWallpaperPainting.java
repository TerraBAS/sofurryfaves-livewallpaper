package com.sofurry.favorites;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.sofurry.favorites.util.FileStorage;
import com.sofurry.favorites.util.MediaScannerNotifier;
import com.sofurry.favorites.util.SubmissionStorage;

public class LiveWallpaperPainting extends Thread {

	public static final int AJAXTYPE_APIERROR = 5;

	public static final int SCALINGMODE_FIT = 0;
	public static final int SCALINGMODE_SCROLLING = 1;
	public static final int SCALINGMODE_SCROLLING_FILLSCREEN = 2;
	
	/** Reference to the View and the context */
	private SurfaceHolder surfaceHolder;
	private Context context;

	/** State */
	private boolean wait;
	private boolean run;
	private int width;
	private int height;
	private static int totalFavoritePages;
	private int rotateInterval = 1800;
	private int scalingMode = SCALINGMODE_FIT;
	private static int contentLevel = 0;
	private static int imageSizeSetting = 0;
	private static int contentSource = 1;
	private static String search = "";
	private static String username = "";
	private static String password = "";
	private long lastTouchTime;
	private long lastLastTouchTime;
	private long doubleTouchThreshold = 300;
	private Bitmap currentImageBig = null;
	private Bitmap currentImage = null;
	private Bitmap oldImage = null;
	private float xoffset, yoffset;
	private float xoffsetold, yoffsetold;
	private static String errorMessage = null;
	private boolean skipTransition = false;
	private final static String sfapp = "SoFurry LiveWallpaper";
	private LinkedBlockingQueue<WallpaperEntry> wallpaperQueue = new LinkedBlockingQueue<WallpaperEntry>();
	private WallpaperEntry currentWallpaperEntry;
	private static ContentLoaderThread contentLoaderThread = null;
	private Bitmap hourglass;
	private static LiveWallpaperPainting instance = null;

	private int numScreensX = 1;
	private int numScreensY = 1;
	private float scrollingOffsetX = 0.0f;
	private float scrollingOffsetY = 0.0f;
	
	/** Time tracking */
	private long previousTime;

	public LiveWallpaperPainting(SurfaceHolder surfaceHolder, Context context, int imageSizeSetting, int scalingMode, int rotateInterval, int contentlevel,
			int contentsource, String search, String username, String password) {
		// keep a reference of the context and the surface
		// the context is needed is you want to inflate
		// some resources from your livewallpaper .apk
		FileStorage.setContext(context);
		hourglass = BitmapFactory.decodeResource(context.getResources(), R.drawable.hourglass);
		this.surfaceHolder = surfaceHolder;
		this.context = context;
		// don't animate until surface is created and displayed
		this.wait = true;
		this.totalFavoritePages = 1;
		this.contentLevel = contentlevel;
		this.scalingMode = scalingMode;
		this.contentSource = contentsource;
		this.search = search;
		this.username = username;
		this.password = password;
		this.rotateInterval = rotateInterval;
		LiveWallpaperPainting.imageSizeSetting = imageSizeSetting;
		this.lastTouchTime = 0;
		this.lastLastTouchTime = 0;
		Log.i(sfapp, "LiveWallpaperPainting constructor called");
		
		if (contentLoaderThread == null || !contentLoaderThread.isAlive()) {
			contentLoaderThread = new ContentLoaderThread(wallpaperQueue);
			contentLoaderThread.start();
		}
		instance = this;
	}

	/**
	 * Pauses the livewallpaper animation
	 */
	public void pausePainting() {
		Log.d(sfapp, "pausePainting");
		this.wait = true;
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * Resume the livewallpaper animation
	 */
	public void resumePainting() {
		Log.d(sfapp, "resumePainting");
		this.wait = false;
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * Stop the livewallpaper animation
	 */
	public void stopPainting() {
		Log.d(sfapp, "stopPainting");
		this.run = false;
		if (contentLoaderThread != null)
			contentLoaderThread.stopThread();
		
		synchronized (this) {
			this.notify();
		}
	}

	@Override
	public void run() {
		this.run = true;
		Canvas c = null;
		while (run) {
			try {
				// while (width == 0 || height == 0) {
				// try {
				// Thread.sleep(100);
				// } catch (InterruptedException e) {
				// Log.i(sfapp, "Sleep interrupted", e);
				// }
				// }

				Log.d(sfapp, "locking canvas 1");
				c = this.surfaceHolder.lockCanvas(null);
				if (c != null) {
					Log.i(sfapp, "Width:" + width + " Height:" + height + " CWidth:" + c.getWidth() + " CHeight:"
						+ c.getHeight());
					if (width == 0)
						width = c.getWidth();
					if (height == 0)
						height = c.getWidth();
					synchronized (this.surfaceHolder) {
						
						if (currentImage == null) {
							currentImage = Bitmap.createBitmap(c.getWidth(), c.getHeight(), Bitmap.Config.RGB_565);
							Canvas ci = new Canvas();
							ci.setBitmap(currentImage);
							
							Paint paint = new Paint();
							paint.setStyle(Paint.Style.STROKE);
							paint.setColor(Color.YELLOW);
							paint.setAntiAlias(true);
							paint.setTextSize(20);
							ci.drawText("Install the SoFurry", 50, c.getHeight() / 2, paint);
							ci.drawText("widget to control the", 50, c.getHeight() / 2 + 25, paint);
							ci.drawText("live wallpaper", 50, c.getHeight() / 2 + 50, paint);
						}
						
						repaintImage(c);
						Log.d(sfapp, "Rendering loading text...");
						Paint paint = new Paint();
						paint.setAntiAlias(true);
						if (!skipTransition) {
							c.drawBitmap(hourglass, 20, 70, paint);
						}
						this.surfaceHolder.unlockCanvasAndPost(c);
						errorMessage = null;
						// 	Check if there's a new image
						if (this.run)
							updateImage();

						if (oldImage != null && this.run && !skipTransition && scalingMode == SCALINGMODE_FIT) {
							transitionImage();
						}
						skipTransition = false;
						
						Log.d(sfapp, "locking canvas 2");
						c = this.surfaceHolder.lockCanvas(null);
						Log.d(sfapp, "Drawing...");
						if (this.run)
							repaintImage(c);
						
						if (errorMessage != null) {
							paint = new Paint();
							paint.setColor(Color.RED);
							paint.setAntiAlias(true);
							paint.setTextSize(35);
							c.drawText(errorMessage, 22, 212, paint);
							paint.setColor(Color.YELLOW);
							c.drawText(errorMessage, 20, 210, paint);
						}
						oldImage = null;
					}
				}
			} finally {
				try {
					if (c != null) {
						this.surfaceHolder.unlockCanvasAndPost(c);
					}
				} catch (Exception e2) {
					Log.e(sfapp, "Exception on unlockCanvasAndPost", e2);
				}
			}
			Log.d("run", "before final synchronized");
			// pause if no need to animate
			synchronized (this) {
				if (wait && this.run) {
					Log.d(sfapp, "waiting...");
					try {
						wait(1000 * rotateInterval);
					} catch (Exception e) {
						e.printStackTrace();
					}
					Log.d(sfapp, "waking up after wait");
				}
			}
		}
		Log.d(sfapp, "EXITING RUN");
	}

	private void transitionImage() {
		Canvas c = null;
		int maxAnimationSteps = 100;
		Log.d(sfapp, "Doing transition");
		for (int pos = 0; pos < maxAnimationSteps; pos++) {
			if (!this.run)
				return;
			// Do a smooth transition
			int transitionX = (int) ((Math.sin(Math.PI * ((float) pos / (float) maxAnimationSteps) - (Math.PI / 2.0)) + 1) / 2 * (float) width);
			c = this.surfaceHolder.lockCanvas(null);
			if (c != null) {
				c.drawColor(Color.BLACK);
				c.drawBitmap(oldImage, xoffsetold - transitionX, yoffsetold, null);
				c.drawBitmap(currentImage, xoffset + width - transitionX, yoffset, null);
				this.surfaceHolder.unlockCanvasAndPost(c);
			}
		}

	}

	/**
	 * Invoke when the surface dimension change
	 * 
	 * @param width
	 * @param height
	 */
	public void setSurfaceSize(int width, int height) {
		Log.d(sfapp, "SetSurfaceSize: " + width + "/" + height);
		synchronized (this.surfaceHolder) {
			this.width = width;
			this.height = height;
			skipTransition = true;
			xoffset = 0;
			xoffsetold = 0;
			yoffset = 0;
			yoffsetold = 0;
		}
		synchronized (this) {
			if (currentImageBig != null)
				currentImage = rescaleImage(currentImageBig);
			notify();
		}
	}

	public void forceRepaint() {
		Log.d(sfapp, "ForceRepaint");
		synchronized (this.surfaceHolder) {
			skipTransition = true;
		}
		synchronized (this) {
			notify();
		}
	}

	/**
	 * Invoke while the screen is touched
	 * 
	 * @param event
	 */
	public void doTouchEvent(MotionEvent event) {
		this.wait = false;
		synchronized (this) {
			if (contentLoaderThread == null || !contentLoaderThread.isAlive()){
				contentLoaderThread = new ContentLoaderThread(wallpaperQueue);
				contentLoaderThread.start();
			}
		}
	}

	public static void launchSaveImage() {
		synchronized (instance) {
			if (instance.currentImageBig != null) {
				String file = SubmissionStorage.saveImageToGallery(instance.currentWallpaperEntry.getName()+".jpg", instance.currentImageBig);
		        // Tell the media scanner about the new file so that it is
		        // immediately available to the user.
				MediaScannerNotifier notifier = new MediaScannerNotifier(instance.context, file, null);
			}
		}
	}
	
	
	public static void launchBrowser() {
		synchronized (instance) {
			if (instance.currentWallpaperEntry != null && instance.currentWallpaperEntry.getPageUrl() != null) {
				Intent defineIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(instance.currentWallpaperEntry
						.getPageUrl()));
				PendingIntent pendingIntent = PendingIntent.getActivity(instance.context, 0, defineIntent, 0);
				try {
					pendingIntent.send();
				} catch (CanceledException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void launchNextImage() {
		Log.e(sfapp, "Resetting previousTime!");
		synchronized (instance) {
			instance.previousTime = 0;
			instance.lastTouchTime = 0;
			instance.lastLastTouchTime = 0;
			instance.notify();
		}
	}
	
	public static boolean isLiveWallpaperRunning() {
		if (instance == null)
			return false;
		
		return true;
	}
	
	
	/**
	 * load the new image
	 */
	private void updateImage() {
		long currentTime = System.currentTimeMillis();
		long elapsed = currentTime - previousTime;
		Log.d(sfapp, "updateImage: currentTime=" + currentTime + " previousTime=" + previousTime);
		if (elapsed > 1000 * rotateInterval) {
			wait = false;
			Log.d(sfapp, "fetching image URL");
			previousTime = currentTime;
			Bitmap image = null;
			WallpaperEntry entry = getNextQueuedWallpaper();
			if (entry != null) {
				currentWallpaperEntry = entry;
				image = SubmissionStorage.loadSubmissionImage(entry.getId());
				SubmissionStorage.deleteSubmissionImage(entry.getId());
			} else {
				entry = getNewWallpaper();
				if (entry != null && entry.getImageUrl() != null) {
					currentWallpaperEntry = entry;
					Log.d(sfapp, "Image: " + entry.getImageUrl());
					Log.d(sfapp, "creating drawable image");
					image = fetchBitmap(entry);
				}
			}
			if (image != null) {
				currentImageBig = image;
				currentImage = rescaleImage(currentImageBig);
			}
			previousTime = currentTime;
		}
		wait = true;
	}

	private WallpaperEntry getNextQueuedWallpaper() {
		WallpaperEntry entry = null;
		if (!wallpaperQueue.isEmpty()) {
			Log.d(sfapp, "Wallpaper queue has "+wallpaperQueue.size()+" entries");
			entry = wallpaperQueue.poll();
			Log.d(sfapp, "Taking entry from queue: "+entry.getId());
		}
		return entry;
	}
	
	
	private Bitmap rescaleImage(Bitmap image) {
		if (scalingMode != SCALINGMODE_FIT)
			return rescaleImageToVirtualScreen(image);
		else
			return rescaleImageToScreen(image);
	}
	
	private Bitmap rescaleImageToScreen(Bitmap image) {
		int imageWidth = currentImageBig.getWidth();
		int imageHeight = currentImageBig.getHeight();

		double imageAspect = (double) imageWidth / imageHeight;
		double canvasAspect = (double) width / height;
		double scaleFactor;

		if (imageAspect < canvasAspect) {
			scaleFactor = (double) height / imageHeight;
		} else {
			scaleFactor = (double) height / imageWidth;
		}

		float scaleWidth = ((float) scaleFactor) * imageWidth;
		float scaleHeight = ((float) scaleFactor) * imageHeight;
		Log.d(sfapp, "canvas size: " + width + "/" + height);
		Log.d(sfapp, "canvas aspect: " + canvasAspect);
		Log.d(sfapp, "image dimensions: " + imageWidth + "/" + imageHeight);
		Log.d(sfapp, "image aspect: " + imageAspect);
		Log.d(sfapp, "scaleFactor: " + scaleFactor);
		Log.d(sfapp, "Scaled dimensions: " + scaleWidth + "/" + scaleHeight);

		xoffsetold = xoffset;
		yoffsetold = yoffset;
		oldImage = currentImage;

		xoffset = (float) ((width - scaleWidth) / 2.0);
		yoffset = (float) ((height - scaleHeight) / 2.0);

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap
		return Bitmap.createScaledBitmap(image, (int) scaleWidth, (int) scaleHeight, true);
	}

	private Bitmap rescaleImageToVirtualScreen(Bitmap image) {
		int imageWidth = currentImageBig.getWidth();
		int imageHeight = currentImageBig.getHeight();

		int virtualWidth;
		int virtualHeight;

		if (numScreensX > 1)
			virtualWidth = Math.abs((int)(width * (numScreensX-(2.0/3.0))));
		else
			virtualWidth = Math.abs(width * numScreensX);
			
		if (numScreensY > 1)
			virtualHeight = Math.abs((int)(height * (numScreensY-(2.0/3.0))));
		else
			virtualHeight = Math.abs(height * numScreensY);

		double imageAspect = (double) imageWidth / imageHeight;
		double canvasAspect = (double) virtualWidth / virtualHeight;
		double scaleFactor;

		if (imageAspect < canvasAspect) {
			if (scalingMode == SCALINGMODE_SCROLLING)
				scaleFactor = (double) virtualHeight / imageHeight;
			else 
				scaleFactor = (double) virtualWidth / imageWidth;
		} else {
			if (scalingMode == SCALINGMODE_SCROLLING)
				scaleFactor = (double) virtualWidth / imageWidth;
			else
				scaleFactor = (double) virtualHeight / imageHeight;
		}

		float scaleWidth = ((float) scaleFactor) * imageWidth;
		float scaleHeight = ((float) scaleFactor) * imageHeight;
		Log.d(sfapp, "num Screens: " + numScreensX + "/" + numScreensY);
		Log.d(sfapp, "virtual canvas size: " + virtualWidth + "/" + virtualHeight);
		Log.d(sfapp, "virtual canvas aspect: " + canvasAspect);
		Log.d(sfapp, "image dimensions: " + imageWidth + "/" + imageHeight);
		Log.d(sfapp, "image aspect: " + imageAspect);
		Log.d(sfapp, "scaleFactor: " + scaleFactor);
		Log.d(sfapp, "Scaled dimensions: " + scaleWidth + "/" + scaleHeight);

		xoffsetold = xoffset;
		yoffsetold = yoffset;
		oldImage = currentImage;

		virtualWidth = Math.abs(width * numScreensX);
		virtualHeight = Math.abs(height * numScreensY);

		xoffset = (float) ((virtualWidth - scaleWidth) / 2.0);
		yoffset = (float) ((virtualHeight - scaleHeight) / 2.0);

		Log.d(sfapp, "Offset: " + xoffset + "/" + yoffset);

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();
		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap
		return Bitmap.createScaledBitmap(image, (int) scaleWidth, (int) scaleHeight, true);
	}

	
	private void repaintImage(Canvas canvas) {
		if (currentImage != null) {
			Log.d(sfapp, "drawing on canvas");
			canvas.drawColor(Color.BLACK);
			
			if (scalingMode != SCALINGMODE_FIT) {
				int virtualScreenWidth = Math.abs((numScreensX-1) * width);
				int virtualScreenHeight= Math.abs((numScreensY-1) * height);
				float shiftedScrollingOffsetX = scrollingOffsetX/3.0f*2.0f + ((1.0f-(1.0f/3.0f*2.0f)) / 2.0f);
				float shiftedScrollingOffsetY = scrollingOffsetY/3.0f*2.0f + ((1.0f-(1.0f/3.0f*2.0f)) / 2.0f);
			
				if (numScreensX <= 1)
					shiftedScrollingOffsetX = 0.0f;
				if (numScreensY <= 1)
					shiftedScrollingOffsetY = 0.0f;
			
				int xpos = -(int)((shiftedScrollingOffsetX) * virtualScreenWidth) + (int)xoffset;
				int ypos = -(int)((shiftedScrollingOffsetY) * virtualScreenHeight) + (int)yoffset;
				Log.d(sfapp, "DRAWING: virtualScreenWidth: "+virtualScreenWidth+" scrollingOffsetX:"+scrollingOffsetX+" xoffset:"+xoffset+" x="+xpos);
				Log.d(sfapp, "DRAWING: virtualScreenHeight: "+virtualScreenHeight+" scrollingOffsetY:"+scrollingOffsetY+" yoffset:"+yoffset+" y="+ypos);
				canvas.drawBitmap(currentImage, xpos, ypos, null);
			} else {
				canvas.drawBitmap(currentImage, xoffset, yoffset, null);
			}
			Log.d(sfapp, "drawing done");
		}
	}

	protected static Bitmap fetchBitmap(WallpaperEntry entry) {
		try {
			Log.d(sfapp, "Fetching image...");
			URL myImageURL = new URL(entry.getImageUrl());
			HttpURLConnection connection = (HttpURLConnection) myImageURL.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream is = connection.getInputStream();
			Log.d(sfapp, is.available() + " bytes available to be read from server");

			Log.d(sfapp, "creating drawable...");
			Bitmap bitmap = BitmapFactory.decodeStream(is);

			return bitmap;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected static WallpaperEntry getNewWallpaper() {
		Random random = new Random();
		boolean useAuthentication = false;
		String viewSource = "" + contentSource;

		if (username != null && username.trim().length() > 0) {
			useAuthentication = true;
			Authentication.updateAuthenticationInformation(username, password);
		} else if (viewSource.equals("5") && (search == null || search.trim().length() <= 0)) {
			viewSource = "8"; // Show featured works if no search term entered
		} else if (!viewSource.equals("0") && !viewSource.equals("5")) {
			viewSource = "8"; // Show featured works if not authenticated
		}
		String requestUrl = "http://chat.sofurry.com/ajaxfetch.php";
		Map<String, String> requestParameters = new HashMap<String, String>();
		int page = random.nextInt(totalFavoritePages);
		requestParameters.put("f", "browse");
		requestParameters.put("contentType", "1");
		requestParameters.put("page", "" + page);
		requestParameters.put("sort", "0");
		if (search != null && search.trim().length() > 0)
			requestParameters.put("search", search);
		requestParameters.put("tab", "favourites");
		requestParameters.put("contentLevel", "" + contentLevel);
		requestParameters.put("viewSource", viewSource);

		try {
			Log.d(sfapp, "Sending request");
			// add authentication parameters to the request
			HttpResponse response;
			if (useAuthentication) {
				Map<String, String> authRequestParameters = Authentication.addAuthParametersToQuery(requestParameters);
				response = HttpRequest.doPost(requestUrl, authRequestParameters);
			} else {
				response = HttpRequest.doPost(requestUrl, requestParameters);
			}
			String httpResult = EntityUtils.toString(response.getEntity());
			if (useAuthentication && Authentication.parseResponse(httpResult) == false) {
				Log.d(sfapp, "RE-SENDING REQUEST with new auth credentials");
				// Retry request with new otp sequence if it failed for the
				// first time
				Map<String, String> authRequestParameters = Authentication.addAuthParametersToQuery(requestParameters);
				response = HttpRequest.doPost(requestUrl, authRequestParameters);
				httpResult = EntityUtils.toString(response.getEntity());
			}
			String errorMessage = parseErrorMessage(httpResult);
			if (errorMessage == null) {
				Log.d(sfapp, httpResult);
				JSONObject jsonParser = new JSONObject(httpResult);
				JSONArray pagecontents = new JSONArray(jsonParser.getString("pagecontents"));
				int pages = pagecontents.getJSONObject(0).getInt("totalpages");
				if (pages > 0) {
					totalFavoritePages = pages;
				}
				JSONArray items = pagecontents.getJSONObject(0).getJSONArray("items");
				int numResults = items.length();
				if (numResults <= 0)
					return null;
				int i = random.nextInt(numResults);
				JSONObject jsonItem = items.getJSONObject(i);
				String thumb = jsonItem.getString("thumb");
				if (!thumb.startsWith("http://www.sofurry.com")) {
					thumb = "http://www.sofurry.com" + thumb;
				}
				WallpaperEntry entry = new WallpaperEntry();
				entry.setId(Integer.parseInt(jsonItem.getString("pid")));
				entry.setName(jsonItem.getString("name"));
				entry.setPageUrl("http://www.sofurry.com/page/" + jsonItem.getString("pid"));
				if (LiveWallpaperPainting.imageSizeSetting == 0)
					entry.setImageUrl("http://www.sofurry.com/std/preview?page="+entry.getId());
				else
					entry.setImageUrl("http://www.sofurry.com/std/content?page="+entry.getId());
				
				return entry;
			}

		} catch (JSONException e) {
			Log.e(sfapp, "Exception: ", e);
			errorMessage = "User/Password wrong!";
		} catch (Exception e) {
			Log.e(sfapp, "Exception: ", e);
			errorMessage = "Error loading image!";
		}

		return null;
	}

	protected static String parseErrorMessage(String httpResult) {
		try {
			// check for json error message and parse it
			Log.d(sfapp, "response: " + httpResult);
			JSONObject jsonParser;
			jsonParser = new JSONObject(httpResult);
			int messageType = jsonParser.getInt("messageType");
			if (messageType == AJAXTYPE_APIERROR) {
				String error = jsonParser.getString("error");
				Log.e(sfapp, "Error: " + error);
				return error;
			}
		} catch (JSONException e) {
			Log.e(sfapp, e.toString());
		}

		return null;

	}

	public int getContentLevel() {
		return contentLevel;
	}

	public void setContentLevel(int contentLevel) {
		this.contentLevel = contentLevel;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getRotateInterval() {
		return rotateInterval;
	}

	public void setRotateInterval(int rotateInterval) {
		this.rotateInterval = rotateInterval;
	}

	public int getContentSource() {
		return contentSource;
	}

	public void setContentSource(int contentSource) {
		this.contentSource = contentSource;
	}

	public String getSearch() {
		return search;
	}

	public void setSearch(String search) {
		this.search = search;
	}

	public int getTotalFavoritePages() {
		return totalFavoritePages;
	}

	public void setTotalFavoritePages(int totalFavoritePages) {
		this.totalFavoritePages = totalFavoritePages;
	}

	public void clearPreloadedWallpapers() {
		wallpaperQueue.clear();
		FileStorage.clearFileCache(null);
		
	}

	public void setScalingMode(int mode) {
		this.scalingMode = mode;
	}
	
	public int getScalingMode() {
		return this.scalingMode;
	}

	public void setNumScreensX(int numScreensX) {
		this.numScreensX = numScreensX;
	}

	public void setNumScreensY(int numScreensY) {
		this.numScreensY = numScreensY;
	}

	public void setScrollingOffsetX(float scrollingOffsetX) {
		this.scrollingOffsetX = scrollingOffsetX;
	}

	public void setScrollingOffsetY(float scrollingOffsetY) {
		this.scrollingOffsetY = scrollingOffsetY;
	}

	public void setSkipTransition(boolean skipTransition) {
		this.skipTransition = skipTransition;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	
	
}
