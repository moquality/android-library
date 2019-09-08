# android-library



### Run single test

```
./gradlew cAT -Pandroid.testInstrumentationRunnerArguments.class=com.moquality.android.AppTest
```

Create screenshots folder
```
adb shell
cd /sdcard/Pictures
mkdir screenshots
```

Screenshots are in the Pictures/screenshots folder. Command to pull.
```
adb pull /sdcard/Pictures/screenshots .
```