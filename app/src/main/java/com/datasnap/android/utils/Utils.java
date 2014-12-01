package com.datasnap.android.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import java.util.Collection;

public final class Utils {
    private Utils() {
        throw new AssertionError("No instances");
    }

    /**
     * Returns true if the application has the given permission.
     */
    public static boolean hasPermission(Context context, String permission) {
        return context.checkCallingOrSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Returns true if the application has the given feature.
     */
    public static boolean hasFeature(Context context, String feature) {
        return context.getPackageManager().hasSystemFeature(feature);
    }

    /**
     * Returns the system service for the given string.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getSystemService(Context context, String serviceConstant) {
        return (T) context.getSystemService(serviceConstant);
    }

    /**
     * Returns true if the string is null, or empty (when trimmed).
     */
    public static boolean isNullOrEmpty(String text) {
        // Rather than using text.trim().length() == 0, use getTrimmedLength to avoid allocating an
        // extra string object
        return TextUtils.isEmpty(text) || TextUtils.getTrimmedLength(text) == 0;
    }

    //** Get the string resource for the given key. Returns null if not found.

    /**
     * Get the identifier for the resource with a given type and key.
     */
    private static int getIdentifier(Context context, String type, String key) {
        return context.getResources().getIdentifier(key, type, context.getPackageName());
    }


    static final String WRITE_KEY_RESOURCE_IDENTIFIER = "analytics_write_key";
    static final String QUEUE_SIZE_RESOURCE_IDENTIFIER = "analytics_queue_size";
    static final String FLUSH_INTERVAL_IDENTIFIER = "analytics_flush_interval";
    static final String DEBUGGING_RESOURCE_IDENTIFIER = "analytics_debugging";

    public static String getResourceString(Context context, String key) {
        int id = getIdentifier(context, "string", key);
        if (id != 0) {
            return context.getResources().getString(id);
        } else {
            return null;
        }
    }


    /**
     * Returns true if the collection is null, or empty.
     */
    public static boolean isCollectionNullOrEmpty(Collection collection) {
        // Rather than using text.trim().length() == 0, use getTrimmedLength to avoid allocating an
        // extra string object
        return collection == null || collection.size() == 0;
    }
}
