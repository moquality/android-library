package com.moquality.android;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.test.runner.screenshot.ScreenCapture;
import androidx.test.runner.screenshot.Screenshot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

public class MoQuality {

    public static String TAG = "MQ";
    public static MoQuality instance;

    RoboConfig config;

    List<Object> pageObjects;
    Map<String, Object> objectMap;

    private MoQuality(){
        pageObjects = new ArrayList<>();
        objectMap = new HashMap<>();
    }

    public static MoQuality get() {
        if(instance == null) {
            instance = new MoQuality();
        }
        return instance;
    }

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

    public void startRoboTest(Map<String, String> config) {
        log("Starting Robo Test");
        // TODO: Implement Android Robo

        RoboConfig roboConfig = new RoboConfig(config);
        startPageObjectRobo(roboConfig);
    }

    public void startPageObjectRobo(RoboConfig config) {
        this.config = config;

        Object currentPage = getCurrentPage();
        int count = 1000;
        while(count>0) {

            if (currentPage != null) {

                Map<String, Map<String, String>> pageMethods = this.config.getPom().get(currentPage.getClass().getCanonicalName());
                List<String> mSignature = query(currentPage, pageMethods);
                Log.d(TAG, "query selected " + mSignature);
                String methodName = mSignature.remove(0);
                List<Class> mParamTypes = new ArrayList<>();
                try {
                    for (String mParam : mSignature) {
                        mParamTypes.add(getClass(mParam));
                    }
                    Method m = currentPage.getClass().getMethod(methodName, mParamTypes.toArray(new Class[0]));
                    m.invoke(currentPage, generateParams(mSignature));
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

            } else {
                Log.e(TAG, "Current Page is NULL. Cannot proceed");
                break;
            }
            count--;
        }

//        Log.d(TAG, "Page Object = "+currentPage.getClass().getCanonicalName());
//        Method[] methods = currentPage.getClass().getDeclaredMethods();
//        for(Method m : methods) {
//            Log.d(TAG, "- Method="+m.getName());
//            try {
//                Object[] args = getOrGenerateParams(m.getParameterTypes());
//                m.invoke(currentPage, args);
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private Class getClass(String klass) {
        switch (klass) {
            case "int": return int.class;
            case "java.lang.Integer": return  Integer.class;
            case "java.lang.String": return String.class;
        }
        return null;
    }

    private Object[] generateParams(List<String> classes) {
        List<Object> params = new ArrayList<>();
        Random random = new Random();
        String[] strings = { "+", "-", "*", "/" };
        for(String klass : classes) {
            switch (klass) {
                case "int":
                case "java.lang.Integer":
                    params.add(random.nextInt(10)); break;
                case "java.lang.String":
                    params.add(strings[random.nextInt(strings.length)]); break;
            }
        }
        return params.toArray();
    }

    private List<String> query(Object currentPage, Map<String, Map<String, String>> pageMethods) {
        // TODO: Change random to querying server.
        Set<String> methods = pageMethods.keySet();
        int i = new Random().nextInt(methods.size());
        String method = null;
        while(i>=0){
            method = methods.iterator().next();
            i--;
        }
        List<String> mSignature = new ArrayList<>();
        mSignature.add(method);
        String[] params = pageMethods.get(method).get("params").split(",");
        mSignature.addAll(Arrays.asList(params));

        return mSignature;
    }

    private Object[] getOrGenerateParams(Class<?>[] parameterTypes) {
        List<Object> objects = new ArrayList<>();

        for(Class pType : parameterTypes) {
            Log.d(TAG, "-- pType class="+ pType.getCanonicalName());
            switch (pType.getCanonicalName()) {
                case "java.lang.String": objects.add("0"); break;
                case "int": objects.add(1); break;
            }
        }

        return objects.toArray();

    }

    private Object getCurrentPage() {
        Map<String, String> mapping = this.config.getMapping();
        Set<String> matchers = mapping.keySet();
        for(String matcher : matchers) {
            switch(matcher) {
                case "*": String klass = mapping.get(matcher);
                    return this.objectMap.get(klass);
                default:
                    // TODO: apply matchers on page
            }
        }
        return null;
    }

    public MoQuality registerPageObjects(Object ...objects) {
        for(Object object : objects) {
            pageObjects.add(object);
            objectMap.put(object.getClass().getCanonicalName(), object);
            objectMap.put(object.getClass().getSimpleName(), object);
        }
        return this;
    }
}

