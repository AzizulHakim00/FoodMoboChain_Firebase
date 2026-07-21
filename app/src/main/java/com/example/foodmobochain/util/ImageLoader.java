package com.example.foodmobochain.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.util.LruCache;

public final class ImageLoader {
    private static final int CACHE_KB = Math.max(4 * 1024,
            (int) (Runtime.getRuntime().maxMemory() / 1024L / 10L));
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(CACHE_KB) {
        @Override
        protected int sizeOf(String key, Bitmap bitmap) {
            return Math.max(1, bitmap.getByteCount() / 1024);
        }
    };
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);

    private ImageLoader() { }

    public static void load(ImageView imageView, String url, @DrawableRes int fallback) {
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(fallback);
        imageView.setTag(url);
        if (url == null || !url.startsWith("https://")) return;

        Bitmap cached = CACHE.get(url);
        if (cached != null && !cached.isRecycled()) {
            imageView.setImageBitmap(cached);
            return;
        }

        EXECUTOR.execute(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(9000);
                connection.setInstanceFollowRedirects(true);
                connection.setRequestProperty("User-Agent", "FoodMoboChain-Android/1.2");
                connection.connect();
                if (connection.getResponseCode() < 200 || connection.getResponseCode() >= 300) return;
                try (InputStream input = connection.getInputStream()) {
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    if (bitmap == null) return;
                    CACHE.put(url, bitmap);
                    imageView.post(() -> {
                        Object current = imageView.getTag();
                        if (url.equals(current) && !bitmap.isRecycled()) imageView.setImageBitmap(bitmap);
                    });
                }
            } catch (Exception ignored) {
                // The fallback remains visible when a public image is unavailable.
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }
}
