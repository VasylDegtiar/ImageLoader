package com.example.vasyl.imageloader.flickr;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {
    private static final String SEARCH_QUERY = "searchQuery";
    private static final String LAST_RESULT_ID = "lastResultId";

    public static String getStoredQuery(Context context) {
        //use preference for load
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(SEARCH_QUERY, null);
    }

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(SEARCH_QUERY, query)
                .apply();
    }
    //use preference for save
    public static void setLastResultId(Context context, String lastResultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(LAST_RESULT_ID, lastResultId)
                .apply();
    }
}
