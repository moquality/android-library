# android-library

## ADDING MOQUALITY LIBRARY TO YOUR PROJECT'S TEST CLASSES
-------------------------------------------------------

- in your main build.gradle (Project) file, add:

```
allprojects {
    repositories {
	...

        // for MoQuality library
        maven {url "https://jitpack.io" }

	...
    ]
]    
```

- under the "dependencies {" section of your build.gradle (Module) file, add:
```
    // MoQuality dependencies
    implementation 'com.github.moquality:android-library:master-SNAPSHOT'
    testImplementation 'com.github.moquality:android-library:master-SNAPSHOT'
    androidTestImplementation 'com.github.moquality:android-library:master-SNAPSHOT'
```

- in your test class,  declare a private instance of the MoQuality library:

```
private MoQuality moQBot = new MoQuality();
```

Add an initialize method at the top of the same class,

```
    @Before
    public void initialize() {
        String deviceId;

        Bundle extras = InstrumentationRegistry.getArguments();

        deviceId = extras.getString("deviceId");

        Log.i(TAG, "Arguments = " + extras.toString());
        Log.i(TAG, "deviceId" + deviceId);

        moQBot.register(this, deviceId);
    }
```

Now add the following MoQuality specific calls,

```
    @Test
    public void runTest() {
        moQBot.runSocketIOTest();
    }

    @After
    public void shutdown() {
        moQBot.shutdown();
    }
```

Next write your public test methods as normal. If you wish to capture a screen shot via the library during the test, insert the following code:

```
        try {
            moQBot.takeScreenshot("<optional name>");
        } catch (IOException e) {
            e.printStackTrace();
        }
```

Finally create a JSON test script, that calls your test methods. For example,
```
[ {
  "type": "espresso",  <--- indicates the type of command, "espresso", "uiautomator", "devices",  or "signal"
  "msg":
    {
    "deviceId": "device1", <---- allows you to target a specific device for the test script
    "cmd": "openNavDrawer", <---- reference to the exact test method name
    "args": [] <--- optional cmd arguments that get passed back to the test class via the library 
    }
  },
   {
     "type": "signal", <--- signal allows you to run internal commands on the device outside of the test class
     "msg":
       {
         "deviceId": "device1",
         "cmd": "sleep", <--- puts the specified device in a wait state
         "args": [
           3000   <--- number of milliseconds to wait
         ]
       }
   }
]
```

-----------------------------------------------------------------------------------

## Run demo tests with the library and Google I/O scheduler sample project.

Grab the "iosched-modified" folder from the sample project on Github at 
https://github.com/moquality/multidevice
and open the folder in Android Studio. Start up an instance of an emulated device (should list as "emulator-5554" when the adb devices command is run.
Compile the the project and under the Gradle tab window >install options, click the "installDebug", "installStaging", and "installStagingAndroidTest" options. 
Once installed, open the I/O scheduler app to check the it is ready to run and then background the app.

In one terminal window type the following adb command:
```
> adb -s emulator-5554  shell am instrument -w -e class com.google.samples.apps.iosched.tests.pages.HomePage -e deviceId device1 com.google.samples.apps.iosched.test/androidx.test.runner.AndroidJUnitRunner
```
to spool up the I/O scheduler app in test mode referencing the HomePage test class

Now in a second terminal window, cd into the "server" folder of the sample project and type:
```
> yarn runner
```

The Google I/O app on the emulator should now run through the sample tests outlined in the HomePage class while also outputting testing status in the secon terminal window as it proceeds through each step.