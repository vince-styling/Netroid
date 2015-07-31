package com.vincestyling.netroid.sample;

import android.util.Log;

public class AppLog {
    private static final String TAG = "netroid_sample";

    public static void d(String msg) {
        try {
            Log.d(TAG, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void d(String msg, Throwable tr) {
        try {
            Log.d(TAG, msg, tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String msg) {
        try {
            Log.i(TAG, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void i(String msg, Throwable tr) {
        try {
            Log.i(TAG, msg, tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void e(String msg) {
        try {
            Log.e(TAG, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void e(String format, Object... args) {
        try {
            Log.e(TAG, String.format(format, args));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void e(String msg, Throwable tr) {
        try {
            Log.e(TAG, msg, tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void e(Throwable tr) {
        try {
            Log.e(TAG, tr.getMessage(), tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void w(String msg) {
        try {
            Log.w(TAG, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void w(String msg, Throwable tr) {
        try {
            Log.w(TAG, msg, tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void v(String msg) {
        try {
            Log.v(TAG, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void v(String msg, Throwable tr) {
        try {
            Log.v(TAG, msg, tr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
