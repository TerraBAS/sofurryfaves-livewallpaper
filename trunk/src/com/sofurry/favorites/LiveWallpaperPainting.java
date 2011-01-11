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
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.sofurry.favorites.util.FileStorage;
import com.sofurry.favorites.util.MediaScannerNotifier;
import com.sofurry.favorites.util.SubmissionStorage;

public class LiveWallpaperPainting extends Thread {

	public static final int AJAXTYPE_APIERROR = 5;

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
	private static int contentLevel = 0;
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

	/** Time tracking */
	private long previousTime;

	public LiveWallpaperPainting(SurfaceHolder surfaceHolder, Context context, int rotateInterval, int contentlevel,
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
		this.contentSource = contentsource;
		this.search = search;
		this.username = username;
		this.password = password;
		this.rotateInterval = rotateInterval;
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
						ci.drawRect(new Rect(0, 0, width, 200), paint);
						ci.drawText("Tap 3 times to", 50, 110, paint);
						ci.drawText("open in Browser", 50, 135, paint);
						ci.drawText("Tap 3 times to", 50, c.getHeight() / 2, paint);
						ci.drawText("load new image", 50, c.getHeight() / 2 + 25, paint);
					}

					repaintImage(c);
					Log.d(sfapp, "Rendering loading text...");
					Paint paint = new Paint();
					paint.setAntiAlias(true);
					c.drawBitmap(hourglass, 20, 70, paint);

					this.surfaceHolder.unlockCanvasAndPost(c);
					errorMessage = null;
					// Check if there's a new image
					if (this.run)
						updateImage();

					if (oldImage != null && this.run && !skipTransition) {
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
			xoffset = 0;
			xoffsetold = 0;
			yoffset = 0;
			yoffsetold = 0;
			skipTransition = true;
		}
		synchronized (this) {
			if (currentImageBig != null)
				currentImage = rescaleImage(currentImageBig);
			notify();
		}
	}

	/**
	 * Invoke while the screen is touched
	 * 
	 * @param event
	 */
	public void doTouchEvent(MotionEvent event) {
		// TODO: fetch new image
		this.wait = false;
		synchronized (this) {
			if (contentLoaderThread == null || !contentLoaderThread.isAlive()){
				contentLoaderThread = new ContentLoaderThread(wallpaperQueue);
				contentLoaderThread.start();
			}
			
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastTouchTime > 60) {
				Log.d(sfapp, "Currenttime: " + currentTime + " lastTouch: " + lastTouchTime + " run: " + run
						+ " wait: " + wait);
				if (currentTime - lastTouchTime < doubleTouchThreshold
						&& currentTime - lastLastTouchTime < doubleTouchThreshold) {
					if (event.getY() < 200 && currentWallpaperEntry != null
							&& currentWallpaperEntry.getPageUrl() != null) {
						launchBrowser();
					} else {
//						Log.e(sfapp, "Resetting previousTime!");
//						previousTime = 0;
//						lastTouchTime = 0;
//						lastLastTouchTime = 0;
//						notify();
						
						Intent mainMenuIntent = new Intent(context, MainMenu.class);
						PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainMenuIntent, 0);
						try {
							pendingIntent.send();
						} catch (CanceledException e) {
							e.printStackTrace();
						}
						
						
					}
				}
				lastLastTouchTime = lastTouchTime;
			}
			lastTouchTime = System.currentTimeMillis();
		}
	}

	public static void launchSaveImage() {
		synchronized (instance) {
			if (instance.currentImageBig != null) {
				String file = SubmissionStorage.saveImageToGallery(instance.currentWallpaperEntry.getName()+".jpg", instance.currentImageBig);
		        // Tell the media scanner about the new file so that it is
		        // immediately available to the user.
				MediaScannerNotifier notifier = new MediaScannerNotifier(instance.context, file, null);
		        //MediaScannerConnection.scanFile(instance.context, new String[]{file}, null);
			}
		}
	}
	
	
//    public static void saveMediaEntry(String imagePath,String title,String description,long dateTaken,int orientation,Location loc) {
//    	synchronized (instance) {
//    		ContentValues v = new ContentValues();
//    		v.put(Images.Media.TITLE, title);
//    		v.put(Images.Media.DISPLAY_NAME, title);
//    		v.put(Images.Media.DESCRIPTION, description);
//    		v.put(Images.Media.DATE_ADDED, dateTaken);
//    		v.put(Images.Media.DATE_TAKEN, dateTaken);
//    		v.put(Images.Media.DATE_MODIFIED, dateTaken) ;
//    		v.put(Images.Media.MIME_TYPE, "image/jpeg");
//    		v.put(Images.Media.ORIENTATION, orientation);
//    		File f = new File(imagePath) ;
//    		File parent = f.getParentFile() ;
//    		String path = parent.toString().toLowerCase() ;
//    		String name = parent.getName().toLowerCase() ;
//    		v.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
//    		v.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
//    		v.put(Images.Media.SIZE,f.length()) ;
//    		f = null ;
//    		v.put("_data",imagePath) ;
//    		ContentResolver c = instance.context.getContentResolver() ;
//    		c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v);
//    	}
//    }


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
		int imageWidth = currentImageBig.getWidth();
		int imageHeight = currentImageBig.getHeight();

		double imageAspect = (double) imageWidth / imageHeight;
		double canvasAspect = (double) width / height;
		double scaleFactor;

		if (imageAspect < canvasAspect) {
			scaleFactor = (double) height / imageHeight;
		} else {
			scaleFactor = (double) width / imageWidth;
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

	private void repaintImage(Canvas canvas) {
		if (currentImage != null) {
			Log.d(sfapp, "drawing on canvas");
			canvas.drawColor(Color.BLACK);
			canvas.drawBitmap(currentImage, xoffset, yoffset, null);
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
				entry.setImageUrl(thumb.replace("/thumbnails/", "/preview/"));
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

}
