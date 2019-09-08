package com.moquality.android;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.Screenshot;

import java.io.IOException;

public class MoQuality {

    public static String TAG = "MQ";

    public int log(String message) {
        Log.i(TAG, message);
        return 0;
    }

    public void takeScreenshot(String name) throws IOException {
        log("Saving screenshot "+name);
        ScreenCapture capture = Screenshot.capture();
        capture.setName(name);
        capture.setFormat(Bitmap.CompressFormat.PNG);
        try {
            capture.process();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}

