package com.geoattend.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "geoattend_session";
    private static final String KEY_SESSION_ID = "current_session_id";
    private static final String KEY_LAST_CHECKOUT_DATE = "last_checkout_date";
    
    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void startSession(String sessionId) {
        prefs.edit().putString(KEY_SESSION_ID, sessionId).apply();
    }

    public String getSessionId() {
        return prefs.getString(KEY_SESSION_ID, null);
    }

    public void clearSession() {
        prefs.edit().remove(KEY_SESSION_ID).apply();
    }

    /**
     * Prevents same-day re-entry after Check-Out.
     */
    public void markCheckOutComplete(String dateString) {
        prefs.edit().putString(KEY_LAST_CHECKOUT_DATE, dateString).apply();
    }

    public boolean isAlreadyCheckedOutToday(String currentDateString) {
        String lastCheckOut = prefs.getString(KEY_LAST_CHECKOUT_DATE, "");
        return lastCheckOut.equals(currentDateString);
    }
}
