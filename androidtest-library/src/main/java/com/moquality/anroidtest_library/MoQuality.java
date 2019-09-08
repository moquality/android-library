package com.moquality.anroidtest_library;

import android.util.Log;

public class MoQuality {

    public static String TAG = "MQ";

    public int log(String message) {
        Log.i(TAG, message);
        return 0;
    }
}
