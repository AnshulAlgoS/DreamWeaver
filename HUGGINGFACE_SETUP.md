# Hugging Face Model Setup Guide

This guide explains how to download and use AI models from Hugging Face with the DreamWeaver app
using the RunAnywhere SDK.

## 📋 Overview

DreamWeaver uses the **RunAnywhere SDK** to run AI models locally on your Android device. This
means:

- ✅ Complete privacy - your stories never leave your device
- ✅ Offline capability - works without internet after model download
- ✅ No API costs - zero cost per generation
- ✅ Fast responses - low-latency on-device inference

## 🔧 Prerequisites

1. **Python 3.7+** installed on your computer
2. **Hugging Face account** (free) - sign up at https://huggingface.co
3. **Android device or emulator** with 3GB+ free storage
4. **USB cable** to connect your Android device (or ADB wireless)

## 📦 Step 1: Install Hugging Face CLI

Open your terminal and install the Hugging Face Hub library:

```bash
pip install huggingface_hub
```

Verify installation:

```bash
huggingface-cli --version
```

## 🔐 Step 2: Login to Hugging Face

1. Create a Hugging Face account at https://huggingface.co/join if you don't have one

2. Generate an access token:
    - Go to https://huggingface.co/settings/tokens
    - Click "New token"
    - Give it a name (e.g., "DreamWeaver")
    - Select "Read" permission
    - Click "Generate token"
    - Copy the token

3. Login using the CLI:

```bash
huggingface-cli login
```

Paste your token when prompted.

## 📥 Step 3: Download AI Model

### Recommended Models for DreamWeaver

We recommend GGUF format models that are quantized for mobile devices:

#### 1. **Qwen2.5-3B-Instruct** (Recommended - Best Balance)

- **Size**: ~2GB (Q4_K_M quantization)
- **Quality**: Excellent for storytelling
- **Speed**: Fast inference on mobile

```bash
huggingface-cli download Qwen/Qwen2.5-3B-Instruct-GGUF \
  qwen2.5-3b-instruct-q4_k_m.gguf \
  --local-dir ./dreamweaver_models
```

#### 2. **Qwen2.5-1.5B-Instruct** (Lighter - For older devices)

- **Size**: ~1GB (Q4_K_M quantization)
- **Quality**: Good for basic storytelling
- **Speed**: Very fast

```bash
huggingface-cli download Qwen/Qwen2.5-1.5B-Instruct-GGUF \
  qwen2.5-1.5b-instruct-q4_k_m.gguf \
  --local-dir ./dreamweaver_models
```

#### 3. **Qwen2.5-7B-Instruct** (High Quality - For powerful devices)

- **Size**: ~4.5GB (Q4_K_M quantization)
- **Quality**: Excellent storytelling with better coherence
- **Speed**: Slower but higher quality

```bash
huggingface-cli download Qwen/Qwen2.5-7B-Instruct-GGUF \
  qwen2.5-7b-instruct-q4_k_m.gguf \
  --local-dir ./dreamweaver_models
```

#### 4. **SmolLM2-360M-Instruct** (Smallest - Ultra-light)

- **Size**: ~200MB (Q4_K_M quantization)
- **Quality**: Basic, good for testing
- **Speed**: Extremely fast

```bash
huggingface-cli download HuggingFaceTB/SmolLM2-360M-Instruct-GGUF \
  smollm2-360m-instruct-q4_k_m.gguf \
  --local-dir ./dreamweaver_models
```

### Download Alternative: Using Python Script

If you prefer using Python directly:

```python
from huggingface_hub import hf_hub_download

# Download model
model_path = hf_hub_download(
    repo_id="Qwen/Qwen2.5-3B-Instruct-GGUF",
    filename="qwen2.5-3b-instruct-q4_k_m.gguf",
    local_dir="./dreamweaver_models"
)

print(f"Model downloaded to: {model_path}")
```

## 📱 Step 4: Transfer Model to Android Device

### Method 1: Using ADB (Recommended)

1. **Enable USB Debugging** on your Android device:
    - Go to Settings → About Phone
    - Tap "Build Number" 7 times to enable Developer Options
    - Go to Settings → Developer Options
    - Enable "USB Debugging"

2. **Connect your device** via USB cable

3. **Verify ADB connection**:

```bash
adb devices
```

You should see your device listed.

4. **Create directory** on device:

```bash
adb shell mkdir -p /sdcard/Android/data/com.dreamweaver.ai/files/models
```

5. **Push the model** to device:

```bash
adb push ./dreamweaver_models/qwen2.5-3b-instruct-q4_k_m.gguf \
  /sdcard/Android/data/com.dreamweaver.ai/files/models/
```

6. **Verify transfer**:

```bash
adb shell ls -lh /sdcard/Android/data/com.dreamweaver.ai/files/models/
```

### Method 2: Manual Transfer

1. Connect your Android device to your computer
2. Open the device in file explorer
3. Navigate to `Android/data/com.dreamweaver.ai/files/`
4. Create a `models` folder if it doesn't exist
5. Copy the `.gguf` file into the `models` folder

### Method 3: Download Directly on Device

You can also use a terminal app on Android (like Termux) to download directly:

```bash
# Install Termux from F-Droid
# Then run:
pkg install python
pip install huggingface_hub
huggingface-cli download Qwen/Qwen2.5-3B-Instruct-GGUF \
  qwen2.5-3b-instruct-q4_k_m.gguf \
  --local-dir /sdcard/Android/data/com.dreamweaver.ai/files/models
```

## ⚙️ Step 5: Configure the App

### Update Model Configuration

Edit `app/src/main/java/com/dreamweaver/ai/ai/RunAnywhereHelper.kt`:

```kotlin
companion object {
    private const val TAG = "RunAnywhereHelper"
    
    // Change these to match your downloaded model
    private const val MODEL_REPO = "Qwen/Qwen2.5-3B-Instruct-GGUF"
    private const val MODEL_FILE = "qwen2.5-3b-instruct-q4_k_m.gguf"
}
```

### For Different Models:

**Qwen2.5-1.5B:**

```kotlin
private const val MODEL_REPO = "Qwen/Qwen2.5-1.5B-Instruct-GGUF"
private const val MODEL_FILE = "qwen2.5-1.5b-instruct-q4_k_m.gguf"
```

**Qwen2.5-7B:**

```kotlin
private const val MODEL_REPO = "Qwen/Qwen2.5-7B-Instruct-GGUF"
private const val MODEL_FILE = "qwen2.5-7b-instruct-q4_k_m.gguf"
```

**SmolLM2-360M:**

```kotlin
private const val MODEL_REPO = "HuggingFaceTB/SmolLM2-360M-Instruct-GGUF"
private const val MODEL_FILE = "smollm2-360m-instruct-q4_k_m.gguf"
```

## 🚀 Step 6: Build and Run

1. **Rebuild the app** in Android Studio:
   ```bash
   ./gradlew clean assembleDebug
   ```

2. **Install** on your device:
   ```bash
   ./gradlew installDebug
   ```

3. **Launch** the app and start creating stories!

## 🔍 Troubleshooting

### Model Not Loading

**Problem**: App uses fallback instead of local model

**Solutions**:

- Verify model file is in correct location: `/sdcard/Android/data/com.dreamweaver.ai/files/models/`
- Check file name matches exactly in code
- Ensure file wasn't corrupted during transfer (check file size)
- Check Android logs: `adb logcat | grep RunAnywhereHelper`

### Out of Memory Error

**Problem**: App crashes when loading model

**Solutions**:

- Use a smaller model (1.5B or 360M variant)
- Close other apps to free RAM
- Try a more aggressively quantized model (Q3 or Q2 instead of Q4)

### Slow Inference

**Problem**: Story generation takes too long

**Solutions**:

- Use a smaller model (1.5B or 360M)
- Reduce `maxTokens` in generation config
- Use Q4 or Q3 quantization (not Q6 or Q8)

### Download Fails

**Problem**: Hugging Face download times out or fails

**Solutions**:

```bash
# Use resume functionality
huggingface-cli download Qwen/Qwen2.5-3B-Instruct-GGUF \
  qwen2.5-3b-instruct-q4_k_m.gguf \
  --local-dir ./dreamweaver_models \
  --resume-download
```

### ADB Not Working

**Problem**: Can't connect to device via ADB

**Solutions**:

- Enable USB Debugging in Developer Options
- Try different USB cable (some cables are charge-only)
- Install device-specific USB drivers
- Try wireless ADB:
  ```bash
  adb tcpip 5555
  adb connect YOUR_DEVICE_IP:5555
  ```

## 📊 Model Comparison

| Model | Size | RAM Usage | Speed | Quality | Best For |
|-------|------|-----------|-------|---------|----------|
| SmolLM2-360M | ~200MB | ~500MB | ⚡⚡⚡⚡⚡ | ⭐⭐ | Testing, old devices |
| Qwen2.5-1.5B | ~1GB | ~1.5GB | ⚡⚡⚡⚡ | ⭐⭐⭐ | Balanced, daily use |
| Qwen2.5-3B | ~2GB | ~3GB | ⚡⚡⚡ | ⭐⭐⭐⭐ | **Recommended** |
| Qwen2.5-7B | ~4.5GB | ~6GB | ⚡⚡ | ⭐⭐⭐⭐⭐ | High-end devices |

## 🎯 Optimization Tips

### 1. Model Selection

- Start with **Qwen2.5-3B** for best balance
- Use **1.5B** if you have < 4GB RAM
- Try **7B** if you have 8GB+ RAM and want best quality

### 2. Quantization Levels

- **Q4_K_M**: Best balance (recommended)
- **Q3_K_M**: Faster, slightly lower quality
- **Q5_K_M**: Better quality, slower
- **Q2_K**: Very fast, noticeable quality loss

### 3. Context Length

Reduce context if model is slow:

```kotlin
// In initializeLocalAI()
contextLength = 1024  // Instead of 2048
```

### 4. Generation Parameters

Balance quality vs speed:

```kotlin
maxTokens = 200,        // Shorter responses = faster
temperature = 0.7f,     // Lower = more focused
topP = 0.9f            // Adjust sampling
```

## 🔗 Useful Links

- [Hugging Face Model Hub](https://huggingface.co/models?library=gguf)
- [RunAnywhere SDK Documentation](https://github.com/RunanywhereAI/runanywhere-sdks)
- [RunAnywhere Sample App](https://github.com/RunanywhereAI/Hackss)
- [GGUF Format Info](https://huggingface.co/docs/hub/gguf)
- [Quantization Guide](https://huggingface.co/docs/transformers/quantization)

## 📝 Model Licensing

When using models from Hugging Face, always check their license:

- Most Qwen models: Apache 2.0 (commercial use OK)
- SmolLM2: Apache 2.0 (commercial use OK)
- Check specific model card for terms

## 🤝 Community Models

Browse more GGUF models for storytelling:

- [Search GGUF models on Hugging Face](https://huggingface.co/models?library=gguf&sort=trending)
- Popular choices: Qwen, SmolLM, Phi, Gemma, Llama

## 💡 Tips for Best Results

1. **First Run**: Model loading takes 10-30 seconds first time
2. **Warm Up**: First few generations may be slower
3. **Context**: Keeping context short (6 messages) helps performance
4. **Storage**: Keep 2x model size free for temp files
5. **Battery**: Model inference is CPU-intensive, keep device charged

---

**Need Help?**

- Check the [main README](README.md) for general setup
- Review [RunAnywhere SDK docs](https://github.com/RunanywhereAI/runanywhere-sdks)
- Open an issue on GitHub

Happy storytelling! 🎨✨
