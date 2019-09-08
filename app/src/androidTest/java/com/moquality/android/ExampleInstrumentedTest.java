package com.moquality.android;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
//import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.moquality.android", appContext.getPackageName());
    }

    @Test
    public void testMQLog() {
        MoQuality mq = new MoQuality();
        int ret = mq.log("Hello MoQuality");
        assertEquals(ret, 0);
    }
}
