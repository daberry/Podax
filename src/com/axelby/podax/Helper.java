package com.axelby.podax;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.util.LruCache;

import com.android.volley.Cache;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.Volley;

import java.nio.ByteBuffer;


public class Helper {

	private static RequestQueue _requestQueue = null;
	private static ImageLoader _imageLoader = null;
	private static LruCache<String, Bitmap> _imageCache = new LruCache<String, Bitmap>(10);
	private static DiskBasedCache _diskCache = null;

	public static boolean ensureWifi(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = cm.getActiveNetworkInfo();
		if (netInfo == null || !netInfo.isConnectedOrConnecting())
			return false;
		// always OK if we're on wifi
		if (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
			return true;
		// check for wifi only pref
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("wifiPref", true))
			return false;
		// check for 3g data turned off
		return netInfo.isConnected();
	}

	public static String getTimeString(int milliseconds) {
		int seconds = milliseconds / 1000;
		final int SECONDSPERHOUR = 60 * 60;
		final int SECONDSPERMINUTE = 60;
		int hours = seconds / SECONDSPERHOUR;
		int minutes = seconds % SECONDSPERHOUR / SECONDSPERMINUTE;
		seconds = seconds % SECONDSPERMINUTE;

		StringBuilder builder = new StringBuilder();
		if (hours > 0) {
			builder.append(hours);
			builder.append(":");
			if (minutes < 10)
				builder.append("0");
		}
		builder.append(minutes);
		builder.append(":");
		if (seconds < 10)
			builder.append("0");
		builder.append(seconds);
		return builder.toString();
	}

	public static void registerMediaButtons(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.registerMediaButtonEventReceiver(new ComponentName(context, MediaButtonIntentReceiver.class));
	}

	public static void unregisterMediaButtons(Context context) {
		AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioManager.unregisterMediaButtonEventReceiver(new ComponentName(context, MediaButtonIntentReceiver.class));
	}

	public static RequestQueue getRequestQueue(Context context) {
		if (_requestQueue == null)
			_requestQueue = Volley.newRequestQueue(context);
		return _requestQueue;
	}

	public static ImageLoader getImageLoader(Context context) {
		if (_diskCache == null)
			_diskCache = new DiskBasedCache(context.getExternalCacheDir());
		if (_imageLoader == null) {
			_imageLoader = new ImageLoader(getRequestQueue(context), new ImageLoader.ImageCache() {
				@Override
				public Bitmap getBitmap(String key) {
					if (_imageCache.get(key) != null)
						return _imageCache.get(key);
					if (_diskCache.getFileForKey(key).exists())
						return BitmapFactory.decodeFile(_diskCache.getFileForKey(key).getAbsolutePath());
					return null;
				}

				@Override
				public void putBitmap(String key, Bitmap bitmap) {
					_imageCache.put(key, bitmap);

					Cache.Entry entry = new Cache.Entry();
					// only put a max 512x512 image in the disk cache
					if (bitmap.getWidth() > 512 && bitmap.getHeight() > 512)
						bitmap = Bitmap.createScaledBitmap(bitmap, 512, 512, false);
					ByteBuffer buffer = ByteBuffer.allocate(bitmap.getRowBytes() * bitmap.getHeight());
					bitmap.copyPixelsToBuffer(buffer);
					entry.data = buffer.array();
					_diskCache.put(key, entry);
				}
			});
		}
		return _imageLoader;
	}

	public static Bitmap getCachedImage(Context context, String url) {
		if (_diskCache == null)
			_diskCache = new DiskBasedCache(context.getExternalCacheDir());
		String key = "#W0#H0" + url;
		if (_imageCache.get(key) != null)
			return _imageCache.get(key);
		if (_diskCache.getFileForKey(key).exists())
			return BitmapFactory.decodeFile(_diskCache.getFileForKey(url).getAbsolutePath());
		return null;
	}

	public static Bitmap getCachedImage(Context context, String url, int width, int height) {
		if (_diskCache == null)
			_diskCache = new DiskBasedCache(context.getExternalCacheDir());

		String key = "#W" + width + "#H" + height + url;
		if (_imageCache.get(key) != null)
			return _imageCache.get(key);

		Bitmap fullSize = getCachedImage(context, url);
		if (fullSize == null)
			return null;

		Bitmap scaled = Bitmap.createScaledBitmap(fullSize, width, height, false);
		_imageCache.put(key, scaled);
		return scaled;
	}

	public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK)
				>= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}
}
