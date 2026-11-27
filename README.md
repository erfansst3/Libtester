# Adivery Pre-Roll

Android Java sample using Adivery VAST with AndroidX Media3 ExoPlayer and Google IMA.

The `adivery-preroll` branch builds a debug APK with GitHub Actions. Download `adivery-preroll-debug-apk` from the workflow run artifacts.

The sample uses a public test video URL. Replace `contentUri` in `MainActivity.java` to use your own video.

CI uses the Android SDK command-line tools directly.

Build uses compileSdk 36 and Media3 1.11.1.
