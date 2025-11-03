# ⚡ DreamWeaver - Quick Start Guide

Get DreamWeaver running in **5 minutes**!

## Prerequisites

- ✅ Android Studio installed
- ✅ Google account

## 🚀 3-Step Setup

### Step 1: Firebase (2 minutes)

1. Go to: https://console.firebase.google.com/
2. Click **"Add project"** → Name it "DreamWeaver"
3. Click Android icon → Package: `com.dreamweaver.ai`
4. **Download `google-services.json`**
5. Go to **"Firestore Database"** → **"Create database"** → **"Test mode"**

### Step 2: Configure (30 seconds)

1. Copy `google-services.json` to `app/` folder (replace placeholder)
2. Open project in Android Studio
3. Wait for Gradle sync

### Step 3: Run (30 seconds)

1. Click **Run ▶️** (or Shift+F10)
2. Grant microphone permission when prompted
3. Start telling stories!

## 🎉 That's it!

### First Use:

1. Tap **"Start Story"**
2. Type or speak: *"Once upon a time, in a magical forest..."*
3. Watch AI continue your story!

### Verify Firebase:

- Check Logcat for: `"Firestore connection successful!"`
- Go to Firebase Console → Firestore → See your data

## 📖 Need More Details?

- Full setup guide: `SETUP_INSTRUCTIONS.md`
- Complete docs: `README.md`
- Project info: `PROJECT_SUMMARY.md`

## 🐛 Quick Fixes

**Can't find google-services.json?**
→ Must be in `app/` directory (not project root)

**Firebase not working?**
→ Package name must be exactly: `com.dreamweaver.ai`

**Voice not working?**
→ Grant microphone permission in app

**TTS not speaking?**
→ Install Google Text-to-Speech from Play Store

## 🎯 Test Checklist

- [ ] App launches
- [ ] Type message → AI responds
- [ ] Tap mic → Speak → AI responds
- [ ] Messages appear in chat
- [ ] Hear AI voice response

---

**Ready to weave amazing stories!** ✨📖
