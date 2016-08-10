package com.datasnap.android.utils;

import android.content.Context;
import android.text.TextUtils;

import java.util.Collection;

public final class Utils {
  private Utils() {
    throw new AssertionError("No instances");
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

  /**
   * Returns true if the collection is null, or empty.
   */
  public static boolean isCollectionNullOrEmpty(Collection collection) {
    // Rather than using text.trim().length() == 0, use getTrimmedLength to avoid allocating an
    // extra string object
    return collection == null || collection.size() == 0;
  }
}
