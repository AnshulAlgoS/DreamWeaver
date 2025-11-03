# 🚀 Running DreamWeaver in Pixel 4 Emulator

## Current Status

The DreamWeaver project is **complete and ready**, but to run it in an emulator, we need to complete
a few setup steps.

---

## ⚠️ Important Prerequisites

### 1. Firebase Configuration

**Status:** ⚠️ **Mock configuration in place** (for testing only)

The current `google-services.json` contains **dummy values** for demonstration purposes. Firebase
features will **NOT work** until you replace it with your actual Firebase configuration.

**To get real Firebase:**

1. Go to https://console.firebase.google.com/
2. Create project: "DreamWeaver"
3. Add Android app: `com.dreamweaver.ai`
4. Download `google-services.json`
5. Replace `app/google-services.json`
6. Enable Firestore Database

---

## 📱 Option 1: Run via Android Studio (RECOMMENDED)

This is the **easiest and most reliable** way to run the app.

### Steps:

1. **Open Project in Android Studio**
   ```
   - Launch Android Studio
   - File → Open
   - Navigate to: /Users/anshulsaxena/AndroidStudioProjects/DreamWeaver
   - Click OK
   ```

2. **Wait for Gradle Sync**
   ```
   - Android Studio will automatically sync Gradle
   - This downloads dependencies (may take 2-5 minutes first time)
   - Wait for "Gradle sync finished" message
   ```

3. **Create/Select Pixel 4 Emulator**
   ```
   - Click device dropdown at top (or Tools → Device Manager)
   - If no devices exist:
     • Click "Create Device"
     • Select "Pixel 4" from Phone category
     • Click Next
     • Select system image (recommend API 34 - Android 14)
     • Click Next → Finish
   - Select your Pixel 4 device from dropdown
   ```

4. **Run the App**
   ```
   - Click the green Run button (▶️) at top
   - OR press Shift+F10
   - Wait for emulator to start (1-2 minutes first time)
   - App will install and launch automatically
   ```

5. **Test the App**
   ```
   - You'll see the onboarding screen
   - Tap "Start Story"
   - Try typing a message
   - Note: Voice input won't work without real Firebase
   - Note: TTS may not work in emulator without Google services
   ```

---

## 📱 Option 2: Run via Command Line

### Prerequisites Check:

```bash
# Check Java
java -version
# Should show Java 8 or higher

# Check Android SDK
ls ~/Library/Android/sdk
# Should show directories like 'platforms', 'build-tools', etc.
```

### Steps:

1. **Download Gradle Wrapper JAR**

   The gradle wrapper JAR is missing. You need to either:

   **Option A:** Let Android Studio generate it (recommended)
    - Open project in Android Studio
    - It will auto-generate the wrapper

   **Option B:** Download manually
   ```bash
   cd /Users/anshulsaxena/AndroidStudioProjects/DreamWeaver
   mkdir -p gradle/wrapper
   curl -L https://github.com/gradle/gradle/raw/master/gradle/wrapper/gradle-wrapper.jar \
     -o gradle/wrapper/gradle-wrapper.jar
   ```

2. **Build the Project**
   ```bash
   cd /Users/anshulsaxena/AndroidStudioProjects/DreamWeaver
   ./gradlew assembleDebug
   ```

3. **Create Pixel 4 AVD (if not exists)**
   ```bash
   # Set environment variables
   export ANDROID_HOME=~/Library/Android/sdk
   export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin
   
   # Create AVD
   avdmanager create avd -n Pixel_4_API_34 -k "system-images;android-34;google_apis;x86_64" -d "pixel_4"
   ```

4. **Start Emulator**
   ```bash
   # In one terminal window
   $ANDROID_HOME/emulator/emulator -avd Pixel_4_API_34
   ```

5. **Install App**
   ```bash
   # In another terminal (wait for emulator to fully boot)
   ./gradlew installDebug
   
   # Or use adb directly
   $ANDROID_HOME/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

6. **Launch App**
   ```bash
   $ANDROID_HOME/platform-tools/adb shell am start -n com.dreamweaver.ai/.MainActivity
   ```

---

## 🎯 Quick Commands Summary

```bash
# Set up environment (add to ~/.zshrc for permanent)
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

# Navigate to project
cd /Users/anshulsaxena/AndroidStudioProjects/DreamWeaver

# Build (first time will download dependencies)
./gradlew assembleDebug

# List available emulators
$ANDROID_HOME/emulator/emulator -list-avds

# Start emulator (background)
$ANDROID_HOME/emulator/emulator -avd Pixel_4_API_34 &

# Wait for emulator to boot, then install
./gradlew installDebug

# Launch app
adb shell am start -n com.dreamweaver.ai/.MainActivity

# View logs
adb logcat | grep DreamWeaver
```

---

## 🐛 Troubleshooting

### Issue: "gradlew: command not found"

**Solution:**

```bash
chmod +x gradlew
./gradlew --version
```

### Issue: "gradle-wrapper.jar not found"

**Solution:** Open in Android Studio first, or download manually (see Option 2, Step 1)

### Issue: "No emulators available"

**Solution:**

```bash
# Check if system images are installed
sdkmanager --list | grep "system-images"

# Install if missing
sdkmanager "system-images;android-34;google_apis;x86_64"

# Create AVD
avdmanager create avd -n Pixel_4_API_34 -k "system-images;android-34;google_apis;x86_64" -d "pixel_4"
```

### Issue: "Build failed"

**Solution:**

```bash
# Clean and rebuild
./gradlew clean
./gradlew assembleDebug

# Check Java version
java -version  # Should be 8+

# Update gradle wrapper
./gradlew wrapper --gradle-version 8.2
```

### Issue: "Firebase connection failed in app"

**Expected** - You're using mock Firebase config. Replace with real one for full functionality.

### Issue: "Emulator won't start"

**Solution:**

```bash
# Check HAXM/Intel virtualization
# On Mac with Apple Silicon: Use ARM system images
sdkmanager "system-images;android-34;google_apis;arm64-v8a"

# Recreate AVD with ARM image
avdmanager create avd -n Pixel_4_API_34_ARM -k "system-images;android-34;google_apis;arm64-v8a" -d "pixel_4"
```

---

## ⚡ Recommended Approach

### **Best: Use Android Studio** ✅

1. Open project in Android Studio
2. Wait for Gradle sync
3. Click Run ▶️
4. Done!

**Why?**

- Handles all dependencies automatically
- Creates gradle wrapper
- Manages emulators easily
- Shows build errors clearly
- Provides debugger and logcat

---

## 📊 What Works Without Real Firebase

### ✅ Will Work:

- App launches
- Onboarding screen
- UI navigation
- Text input
- AI story generation (pattern-based)
- Chat bubbles display
- Animations

### ⚠️ Won't Work:

- Firebase data persistence
- Retrieving old sessions
- Voice input (needs Google services + real Firebase)
- Some TTS features in emulator

---

## 🎯 Testing Checklist

Once app is running:

- [ ] App launches to onboarding screen
- [ ] "Start Story" button works
- [ ] Chat screen appears
- [ ] Can type in text field
- [ ] Send button appears when typing
- [ ] Message appears after sending
- [ ] AI generates response
- [ ] AI response appears in purple bubble
- [ ] User message in blue bubble
- [ ] New Story button works

---

## 🔥 Next Steps

### To Get Full Functionality:

1. **Replace Firebase Config**
    - Create real Firebase project
    - Download actual google-services.json
    - Replace the mock file

2. **Test on Real Device**
    - Physical device has better Google services support
    - Voice input works better
    - TTS is more reliable

3. **Deploy to Play Store** (Future)
    - Add Firebase Authentication
    - Implement production security rules
    - Generate signed APK
    - Create Play Store listing

---

## 💡 Pro Tips

1. **First Time Setup**: Always use Android Studio for first build
2. **Emulator Performance**: Use x86_64 images on Intel Mac, arm64-v8a on Apple Silicon
3. **Faster Builds**: Keep emulator running between builds
4. **Debugging**: Use Android Studio's Logcat to see "DreamWeaver" logs
5. **Firebase**: App is functional even without real Firebase for UI testing

---

## 📞 Need Help?

If you encounter issues:

1. **Check Android Studio Messages Panel** - Shows detailed errors
2. **View Gradle Console** - Shows build progress
3. **Check Logcat** - Shows runtime logs
4. **Try Clean Build**: Build → Clean Project → Rebuild Project

---

## ✅ Summary

**To run the app NOW:**

```bash
# Easiest way:
1. Open Android Studio
2. Open project folder
3. Wait for sync
4. Click Run ▶️
```

**That's it!** The app will build, emulator will start, and app will launch automatically.

---

🔥 **DreamWeaver is ready to run!**

Just open it in Android Studio and click the Run button!
