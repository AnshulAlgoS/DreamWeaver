# 🔥 DreamWeaver Setup Instructions

## Quick Start Guide

Follow these steps to get DreamWeaver running on your machine.

## Prerequisites

✅ Android Studio Hedgehog (2023.1.1) or newer  
✅ JDK 8 or higher  
✅ Android SDK with API 24+  
✅ Google account for Firebase

## Step 1: Firebase Project Setup

### 1.1 Create Firebase Project

1. Open your browser and go to: https://console.firebase.google.com/
2. Click **"Add project"** or **"Create a project"**
3. Enter project name: `DreamWeaver` (or your preferred name)
4. Click **Continue**
5. (Optional) Enable Google Analytics
6. Click **Create project**
7. Wait for project creation to complete
8. Click **Continue** to go to project dashboard

### 1.2 Add Android App to Firebase

1. In Firebase Console dashboard, click the **Android icon** (or "Add app")
2. Fill in the following details:
    - **Android package name**: `com.dreamweaver.ai` ⚠️ (Must match exactly!)
    - **App nickname** (optional): `DreamWeaver`
    - **Debug signing certificate** (optional): Leave blank for now
3. Click **Register app**
4. **Download google-services.json**
    - Click the **"Download google-services.json"** button
    - Save the file to your computer
5. Click **Next** (you can skip the SDK setup steps as they're already in the code)
6. Click **Continue to console**

### 1.3 Enable Firestore Database

1. In Firebase Console, click **"Firestore Database"** in the left sidebar
    - It's under "Build" section
2. Click **"Create database"**
3. Choose your security rules:
    - For **development/testing**: Select **"Start in test mode"**
    - For **production**: Select **"Start in production mode"**
    - You can change these later
4. Click **Next**
5. Select your **Firestore location** (closest to your users)
    - Note: This cannot be changed later
6. Click **Enable**
7. Wait for Firestore to be provisioned

## Step 2: Configure the Android App

### 2.1 Add google-services.json

1. Locate the `google-services.json` file you downloaded
2. Copy it to the `app/` directory of the DreamWeaver project
3. Replace the existing placeholder file
4. File structure should look like:
   ```
   DreamWeaver/
   ├── app/
   │   ├── google-services.json  ← Your file here
   │   ├── build.gradle.kts
   │   └── src/
   ├── build.gradle.kts
   └── settings.gradle.kts
   ```

### 2.2 Verify Package Name

Ensure the package name in your `google-services.json` matches the app:

**In google-services.json:**

```json
{
  "client": [
    {
      "client_info": {
        "android_client_info": {
          "package_name": "com.dreamweaver.ai"
        }
      }
    }
  ]
}
```

**In app/build.gradle.kts:**

```kotlin
android {
    namespace = "com.dreamweaver.ai"
    defaultConfig {
        applicationId = "com.dreamweaver.ai"
    }
}
```

## Step 3: Build & Run

### 3.1 Open Project in Android Studio

1. Launch Android Studio
2. Click **"Open"** or **File → Open**
3. Navigate to the DreamWeaver project directory
4. Click **OK**
5. Wait for Gradle sync to complete
    - This may take a few minutes on first run
    - Android Studio will download dependencies

### 3.2 Sync Gradle

If sync doesn't start automatically:

1. Click **File → Sync Project with Gradle Files**
2. Wait for sync to complete
3. Check for any errors in the "Build" panel

### 3.3 Build the Project

**Option A: Via Android Studio**

1. Click **Build → Make Project** (or press Ctrl+F9 / Cmd+F9)
2. Wait for build to complete

**Option B: Via Terminal**

```bash
# On macOS/Linux:
./gradlew build

# On Windows:
gradlew.bat build
```

### 3.4 Run the App

**Prepare Device:**

**Option A - Physical Device:**

1. Enable Developer Options on your Android device:
    - Go to Settings → About phone
    - Tap "Build number" 7 times
2. Enable USB Debugging:
    - Go to Settings → Developer options
    - Enable "USB debugging"
3. Connect device via USB
4. Accept "Allow USB debugging" prompt on device

**Option B - Emulator:**

1. In Android Studio, click **Tools → Device Manager**
2. Click **"Create Device"**
3. Select a device (e.g., Pixel 6)
4. Select system image (API 24+, recommend API 34)
5. Click **Finish**
6. Click **Play button** to start emulator

**Run the App:**

1. Select your device from the dropdown at the top of Android Studio
2. Click the **Run button** (green play icon) or press Shift+F10
3. Wait for app to install and launch

## Step 4: Test the App

### 4.1 Grant Permissions

When you first tap the microphone icon:

1. Android will request microphone permission
2. Tap **"Allow"** or **"While using the app"**

### 4.2 Verify Firebase Connection

Check Android Studio Logcat:

1. Open **Logcat** panel (View → Tool Windows → Logcat)
2. Search for: `DreamWeaver`
3. You should see: `"Firestore connection successful!"`

### 4.3 Test Features

✅ **Onboarding**: See welcome screen  
✅ **Start Story**: Tap "Start Story" button  
✅ **Text Input**: Type a message and send  
✅ **Voice Input**: Tap mic, speak, see transcription  
✅ **AI Response**: Receive AI continuation  
✅ **TTS**: Hear AI response spoken  
✅ **Firebase**: Messages saved to Firestore

## Step 5: Verify Firebase Data

1. Go to Firebase Console: https://console.firebase.google.com/
2. Select your DreamWeaver project
3. Click **"Firestore Database"** in sidebar
4. You should see two collections:
    - **`sessions`**: Story sessions
    - **`stories`**: Individual messages
    - **`app_status`**: App initialization test
5. Click on any document to view data

## Troubleshooting

### ❌ "google-services.json not found"

**Solution:** Make sure the file is in `app/` directory, not in project root

### ❌ "Firestore connection failed"

**Solutions:**

- Check internet connectivity
- Verify Firestore is enabled in Firebase Console
- Confirm google-services.json package name matches app

### ❌ "Speech recognition not available"

**Solutions:**

- Install Google app from Play Store
- Enable Google Voice Typing in device settings
- Check microphone permission

### ❌ "TTS not speaking"

**Solutions:**

- Go to Settings → Accessibility → Text-to-Speech
- Install Google Text-to-Speech engine
- Set preferred engine to Google
- Check device volume

### ❌ Build errors

**Solutions:**

```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Invalidate caches in Android Studio
File → Invalidate Caches → Invalidate and Restart
```

## Security Note 🔒

The provided Firebase configuration uses **test mode** for easy development. For production:

1. Go to Firebase Console → Firestore Database → Rules
2. Replace rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

3. Implement Firebase Authentication in the app

## Need Help?

- Check the main [README.md](README.md) for detailed documentation
- Review Firebase docs: https://firebase.google.com/docs/android/setup
- Check Android docs: https://developer.android.com/

---

🎉 **Congratulations!** You're ready to start weaving stories with DreamWeaver!
