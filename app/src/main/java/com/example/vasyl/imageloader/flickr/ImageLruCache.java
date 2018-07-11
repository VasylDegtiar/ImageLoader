package com.example.vasyl.imageloader.flickr;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class ImageLruCache {

    private static ImageLruCache sImageLruCache;
    private LruCache<String,Bitmap> mBitmapLruCache;

    public static ImageLruCache get(Context context){
        if (sImageLruCache == null){
            sImageLruCache = new ImageLruCache(context);
        }
        return sImageLruCache;
    }

    private ImageLruCache(Context context) {
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mBitmapLruCache = new LruCache<>(cacheSize);
        mBitmapLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    // Add photo into LruCache if it isn't already there
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (key == null || bitmap == null) {
            return;
        }
        if (getBitmapFromMemCache(key) == null) {
            mBitmapLruCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        if (key == null) {
            return null;
        }
        return mBitmapLruCache.get(key);
    }

}
