package com.moquality.android;

import android.graphics.Bitmap;
import android.os.Looper;
import android.util.Log;

import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.Screenshot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MoQuality implements SocketIOHandlerThread.Callback {

    private static String TAG = "MQ";

    private SocketIOHandlerThread mHandlerThread;

    private ArrayList<Class<?>> appTests = new ArrayList<Class<?>>();

    public int log(String message) {
        Log.i(TAG, message);
        return 0;
    }

    public void register(Class test, String deviceId) {
        this.appTests.add(test);

        // launch Socket IO chat thread
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        mHandlerThread = new SocketIOHandlerThread(this, deviceId);
        mHandlerThread.start();
        mHandlerThread.prepareHandler();
    }

    public void unregister(Class test){
        for (Class<?> testClass:appTests) {
            if (testClass.getSimpleName().equalsIgnoreCase(test.getSimpleName())){
                appTests.remove(testClass);
            }
        }
    }

    public void shutdown() {
        if(mHandlerThread != null){
            mHandlerThread.quit();
            mHandlerThread.interrupt();
        }
    }

    public void runSocketIOTest() {
        mHandlerThread.queueTask(TestConstants.SOCKET_IO_START);
        try {
            long threadStartTime = System.currentTimeMillis();
            long executionTimeInMillis = 0;
            while (mHandlerThread.isThreadAlive()) {
                executionTimeInMillis = System.currentTimeMillis() - threadStartTime;
            }

            Log.i("SOCKET_IO THREAD", "Execution time = " + executionTimeInMillis/1000 + " seconds");
            //Thread.sleep(5000);
        } catch (Exception e) {
            Log.d("SOCKET IO", "Test interrupted");
        }
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

    @Override
    public void onSocketTaskCompleted(int taskId) {
        switch (taskId){
            case TestConstants.SOCKET_IO_MSG_RECEIVED:
                Log.i(TAG, "Message received in UI");
                break;
            case TestConstants.SOCKET_IO_DISCONNECTED:
                break;
        }
    }

    @Override
    public void onSocketEventReceived(String eventName, String method, List<Class> classArgs, List<String> stringArgs) {
        switch (eventName){
            case TestConstants.SOCKET_EVENT_CALL:
                Log.i(TAG, "CALL command received for this device.");
                callAppTestMethod(method, classArgs, stringArgs);
                break;
            case TestConstants.SOCKET_EVENT_STATUS:
                Log.i(TAG, "STATUS command received for this device.");
                break;
            case TestConstants.SOCKET_EVENT_RETURN:
                Log.i(TAG, "RETURN command received for this device.");
        }
    }

    private void setMode(String mode) {
        switch (mode) {
            case "reflect":
                mHandlerThread.queueTask(TestConstants.REFLECT_MODE);

                break;
            case "quit":
                this.mHandlerThread.quit();

                break;
            default:
                Log.i(TAG, "Unhandled mode " + mode);
                //mHandlerThread.queueTask(TestConstants.DEFAULT_MODE);
        }
    }

    private void callAppTestMethod(String method, List<Class> classArgs, List<String> stringArgs){
        Object obj;
        if (appTests!=null) {

            if (stringArgs!=null && stringArgs.size()>0) {
                Log.i(TAG, "******* METHOD = " + method + " ARGS = " + stringArgs.get(0));
            } else {
                Log.i(TAG, "******* METHOD = " + method);
            }
            if (method.equalsIgnoreCase("setMode")){
                if (stringArgs!=null) {
                    setMode(stringArgs.get(0));
                }
            } else {
                for (Class<?> testClass:appTests) {
                    try {
                        obj= testClass;
                        Method m = testClass.getMethod(method, classArgs.toArray(new Class[0]));

                        try {
                            Log.i(TAG, testClass.getSimpleName() + " - method called = " + m.toString());
                            try {
                                String data = "";
                                if (stringArgs != null && stringArgs.size() > 0) {
                                    data = m.invoke(obj, stringArgs.toArray(new String[0])).toString();
                                } else {
                                    data = m.invoke(obj, "").toString();
                                }
                                Log.i(TAG, "return " + data);
                            } catch (NullPointerException e) {
                                Log.i(TAG, "Error returning data from method invoke()");
                            }
                        } catch (IllegalAccessException e) {
                            Log.i(TAG, method + " invoke error - Illegal Access Exception");
                        } catch (InvocationTargetException e) {
                            Log.i(TAG, method + " invoke error - Invocation Target Exception");
                        }
                    } catch (NoSuchMethodException e) {
                        Log.i(TAG, "Method (" + method + ") not found in app test.");
                    }
                }
            }
        }
    }
}

