package com.sofurry.favorites;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
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
	private String imageUrl;
	private int totalFavoritePages;
	private int rotateInterval = 1800;
	private int contentLevel = 0;
	private String username = "";
	private String password = "";
	private long lastTouchTime;
	private long lastLastTouchTime;
	private long doubleTouchThreshold = 300;
	private Bitmap currentImage = null;
	private Bitmap oldImage = null;
	private float xoffset, yoffset;
	private float xoffsetold, yoffsetold;
	private String pageUrl = null;

	/** Time tracking */
	private long previousTime;

	public LiveWallpaperPainting(SurfaceHolder surfaceHolder, Context context, int rotateInterval, int contentlevel,
			String username, String password) {
		// keep a reference of the context and the surface
		// the context is needed is you want to inflate
		// some resources from your livewallpaper .apk
		this.surfaceHolder = surfaceHolder;
		this.context = context;
		// don't animate until surface is created and displayed
		this.wait = true;
		this.totalFavoritePages = 1;
		this.contentLevel = contentlevel;
		this.username = username;
		this.password = password;
		this.rotateInterval = rotateInterval;
		this.lastTouchTime = 0;
		this.lastLastTouchTime = 0;
	}

	/**
	 * Pauses the livewallpaper animation
	 */
	public void pausePainting() {
		this.wait = true;
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * Resume the livewallpaper animation
	 */
	public void resumePainting() {
		this.wait = false;
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * Stop the livewallpaper animation
	 */
	public void stopPainting() {
		this.run = false;
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
				c = this.surfaceHolder.lockCanvas(null);
				synchronized (this.surfaceHolder) {
					
					if (currentImage == null) {
						currentImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
						Canvas ci = new Canvas();
						ci.setBitmap(currentImage);

						Paint paint = new Paint();
						paint.setStyle(Paint.Style.STROKE);
						paint.setColor(Color.YELLOW);
						paint.setAntiAlias(true);
						paint.setTextSize(20);
						ci.drawRect(new Rect(0,0,width,200), paint);
						ci.drawText("Tap 3 times to", 50, 110, paint);
						ci.drawText("open in Browser", 50, 135, paint);
						ci.drawText("Tap 3 times to", 50, height / 2, paint);
						ci.drawText("load new image", 50, height / 2 + 25, paint);
					}
					
					
					repaintImage(c);
					Log.d("run", "Rendering loading text...");
					Paint paint = new Paint();
					paint.setColor(Color.BLACK);
					paint.setStyle(Paint.Style.FILL);
					paint.setAntiAlias(true);
					paint.setTextSize(25);
					c.drawText("Loading next image...", 22, 72, paint);
					paint.setColor(Color.YELLOW);
					c.drawText("Loading next image...", 20, 70, paint);
				}
				this.surfaceHolder.unlockCanvasAndPost(c);

				// Check if there's a new image
				updateImage();

				if (oldImage != null) {
					transitionImage();
				}

				c = this.surfaceHolder.lockCanvas(null);
				synchronized (this.surfaceHolder) {
					Log.d("run", "Drawing...");
					repaintImage(c);
					oldImage = null;
				}
			} finally {
				if (c != null) {
					this.surfaceHolder.unlockCanvasAndPost(c);
				}
			}
			// pause if no need to animate
			synchronized (this) {
				if (wait) {
					try {
						wait();
					} catch (Exception e) {
					}
				}
			}
		}
	}

	private void transitionImage() {
		Canvas c = null;
		int maxAnimationSteps = 100;
		Log.d("transition", "Doing transition");
		for (int pos = 0; pos < maxAnimationSteps; pos++) {
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
		this.width = width;
		this.height = height;
		synchronized (this) {
			previousTime = 0;
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
			long currentTime = System.currentTimeMillis();
			if (currentTime - lastTouchTime > 60) {
				Log.d("doubletouch", "Currenttime: " + currentTime + " lastTouch: " + lastTouchTime);
				if (currentTime - lastTouchTime < doubleTouchThreshold
						&& currentTime - lastLastTouchTime < doubleTouchThreshold) {
					if (event.getY() < 200 && pageUrl != null) {
						Intent defineIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl));
						PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, defineIntent, 0);
						try {
							pendingIntent.send();
						} catch (CanceledException e) {
							e.printStackTrace();
						}
					} else {
						previousTime = 0;
						notify();
					}
				}
				lastLastTouchTime = lastTouchTime;
			}
			lastTouchTime = System.currentTimeMillis();
		}
	}

	/**
	 * load the new image
	 */
	private void updateImage() {
		long currentTime = System.currentTimeMillis();
		long elapsed = currentTime - previousTime;
		if (elapsed > 1000 * rotateInterval) {
			wait = false;
			Log.d("doDraw", "fetching image URL");
			imageUrl = getNewImageUrl();
			if (imageUrl != null) {
				Log.d("doDraw", "Image: " + imageUrl);
				Log.d("doDraw", "creating drawable image");
				Bitmap image = fetchBitmap(imageUrl);

				if (image != null) {
					int imageWidth = image.getWidth();
					int imageHeight = image.getHeight();

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
					Log.d("doDraw", "canvas size: " + width + "/" + height);
					Log.d("doDraw", "canvas aspect: " + canvasAspect);
					Log.d("doDraw", "image dimensions: " + imageWidth + "/" + imageHeight);
					Log.d("doDraw", "image aspect: " + imageAspect);
					Log.d("doDraw", "scaleFactor: " + scaleFactor);
					Log.d("doDraw", "Scaled dimensions: " + scaleWidth + "/" + scaleHeight);

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
					currentImage = Bitmap.createScaledBitmap(image, (int) scaleWidth, (int) scaleHeight, true);
				}
			}
			previousTime = currentTime;
		}
		wait = true;
	}

	private void repaintImage(Canvas canvas) {
		if (currentImage != null) {
			Log.d("doDraw", "drawing on canvas");
			canvas.drawColor(Color.BLACK);
			canvas.drawBitmap(currentImage, xoffset, yoffset, null);
		}
	}

	private Bitmap fetchBitmap(String url) {
		try {
			Log.d("image", "Fetching image...");
			URL myImageURL = new URL(url);
			HttpURLConnection connection = (HttpURLConnection) myImageURL.openConnection();
			connection.setDoInput(true);
			connection.connect();
			InputStream is = connection.getInputStream();
			Log.d("image", is.available() + " bytes available to be read from server");
			Log.d("image", "creating drawable...");
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

	private String getNewImageUrl() {
		Random random = new Random();
		boolean useAuthentication = false;
		String viewSource = "8"; // Show featured works if not authenticated

		if (username != null && username != "") {
			useAuthentication = true;
			Authentication.updateAuthenticationInformation(username, password);
			viewSource = "1";
		}
		String requestUrl = "http://sofurry.com/ajaxfetch.php";
		Map<String, String> requestParameters = new HashMap<String, String>();
		int page = random.nextInt(totalFavoritePages);
		requestParameters.put("f", "browse");
		requestParameters.put("contentType", "1");
		requestParameters.put("page", "" + page);
		requestParameters.put("sort", "0");
		requestParameters.put("tab", "favourites");
		requestParameters.put("contentLevel", "" + contentLevel);
		requestParameters.put("viewSource", viewSource);

		try {
			Log.d("getNewImageUrl", "Sending request");
			// add authentication parameters to the request
			HttpResponse response;
			if (useAuthentication) {
				Map<String, String> authRequestParameters = Authentication.addAuthParametersToQuery(requestParameters);
				response = HttpRequest.doPost(requestUrl, authRequestParameters);
			} else {
				response = HttpRequest.doPost(requestUrl, requestParameters);
			}
			String httpResult = EntityUtils.toString(response.getEntity());
			ArrayList<String> resultList = new ArrayList<String>();
			if (useAuthentication && Authentication.parseResponse(httpResult) == false) {
				Log.d("getNewImageUrl", "RE-SENDING REQUEST with new auth credentials");
				// Retry request with new otp sequence if it failed for the
				// first time
				Map<String, String> authRequestParameters = Authentication.addAuthParametersToQuery(requestParameters);
				response = HttpRequest.doPost(requestUrl, authRequestParameters);
				httpResult = EntityUtils.toString(response.getEntity());
			}
			String errorMessage = parseErrorMessage(httpResult);
			if (errorMessage == null) {
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
				String thumb = "http://sofurry.com" + jsonItem.getString("thumb");
				pageUrl = "http://sofurry.com/page/" + jsonItem.getString("pid");
				String preview = thumb.replace("/thumbnails/", "/preview/");
				return preview;
			}

		} catch (ClientProtocolException e) {
			Log.e("getNewImageUrl", "Exception: ", e);
		} catch (IOException e) {
			Log.e("getNewImageUrl", "Exception: ", e);
		} catch (JSONException e) {
			Log.e("getNewImageUrl", "Exception: ", e);
		}

		return null;
	}

	protected String parseErrorMessage(String httpResult) {
		try {
			// check for json error message and parse it
			Log.d("Chat.parseErrorMessage", "response: " + httpResult);
			JSONObject jsonParser;
			jsonParser = new JSONObject(httpResult);
			int messageType = jsonParser.getInt("messageType");
			if (messageType == AJAXTYPE_APIERROR) {
				String error = jsonParser.getString("error");
				Log.e("ChatList.parseErrorMessage", "Error: " + error);
				return error;
			}
		} catch (JSONException e) {
			Log.e("Chat.parseResponse", e.toString());
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

}
