# 🎨 DreamWeaver - AI-Powered Interactive Storytelling App

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"/>
  <img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"/>
  <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"/>
  <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase"/>
</p>

**DreamWeaver** is an AI-powered storytelling companion app for Android that helps you create
amazing stories through interactive conversation. Using advanced AI models, it continues your story
ideas with creative and contextual suggestions, making storytelling collaborative and fun!

## ✨ Features

### 🤖 **AI-Powered Story Generation**

- Real-time story continuation using NVIDIA NIM API (Llama 3.1)
- Context-aware responses that understand your story's flow
- Intelligent fallback system for offline functionality
- Maintains conversation history for consistent storytelling

### 💬 **Interactive Two-Way Chat**

- Beautiful chat interface with distinct user/AI message bubbles
- Blue bubbles for your input, purple for AI responses
- Smooth animations and auto-scroll
- Real-time "AI thinking" indicator

### 🔊 **Voice Narration**

- Toggle voice narration on/off with a single tap
- AI speaks all story responses using Text-to-Speech
- Visual indicator (speaker icon) shows narration status
- Purple icon = Voice ON, Gray icon = Voice OFF

### 🎤 **Voice Input**

- Speak your story instead of typing
- Real-time speech-to-text conversion
- Hands-free storytelling experience
- Visual feedback when listening

### 🧠 **Smart Conversation Understanding**

- Recognizes conversational pauses ("wait", "hold on", "umm")
- Doesn't create story content from interruptions
- Provides friendly acknowledgments
- Natural conversation flow

### 🎨 **Beautiful UI/UX**

- Dark theme optimized for reading
- Smooth animations and transitions
- Material Design 3 components
- Responsive and intuitive interface

### 💾 **Cloud Storage (Firebase)**

- Automatic story session saving
- Resume previous conversations
- Message history persistence
- Cross-device sync capability

## 📱 Screenshots

*Coming soon*

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 24 or higher
- Kotlin 1.9.0+
- Firebase account (for cloud storage)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/DreamWeaver.git
   cd DreamWeaver
   ```

2. **Open in Android Studio**
    - Launch Android Studio
    - Select "Open an Existing Project"
    - Navigate to the cloned directory

3. **Configure Firebase**
    - Create a new Firebase project at [Firebase Console](https://console.firebase.google.com)
    - Add an Android app to your Firebase project
    - Download `google-services.json` and place it in `app/` directory
    - Enable Firestore Database in Firebase Console

4. **API Configuration**
    - The app uses NVIDIA NIM API for AI generation
    - API key is included in the code (for demo purposes)
    - For production, store API keys securely using BuildConfig or env variables

5. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or click the "Run" button in Android Studio

## 🎮 How to Use

### Starting a Story

1. **Launch the app** - You'll see the onboarding screen
2. **Tap "Start Story"** - Begins a new storytelling session
3. **Type or speak** - Enter your story beginning
4. **Watch the magic** - AI continues your story creatively!

### Voice Controls

- **Toggle Narration**: Tap the speaker icon in the top bar
- **Voice Input**: Tap the microphone button to speak
- **Stop Speaking**: Tap the microphone again to stop

### Conversation Tips

- Use natural language: "Once upon a time..."
- Ask questions: "What should happen next?"
- Give directions: "Make it more mysterious"
- Use pauses: "wait" or "hold on" if you need time to think

### Example Prompts

```
"Once upon a time in a magical forest..."
"The spaceship landed on a distant planet..."
"Create a scenario where Arjun would have died and Abhimanyu lived on"
"She opened the mysterious letter and discovered..."
```

## 🏗️ Architecture

### Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Android ViewModels
- **Networking**: OkHttp + Gson
- **Backend**: Firebase Firestore
- **AI Model**: NVIDIA NIM (Llama 3.1-8b-instruct)
- **Speech**: Android SpeechRecognizer + TextToSpeech

### Project Structure

```
app/
├── src/main/java/com/dreamweaver/ai/
│   ├── ai/                    # AI and speech helpers
│   │   ├── RunAnywhereHelper.kt
│   │   └── SpeechRecognizerHelper.kt
│   ├── data/                  # Data models
│   │   └── StoryMessage.kt
│   ├── firebase/              # Firebase integration
│   │   └── FirestoreHelper.kt
│   ├── ui/                    # UI components
│   │   ├── ChatScreen.kt
│   │   ├── OnboardingScreen.kt
│   │   └── theme/
│   ├── viewmodel/             # ViewModels
│   │   └── StoryViewModel.kt
│   ├── MainActivity.kt
│   └── DreamWeaverApplication.kt
```

### Key Components

#### **StoryViewModel**

- Manages app state and business logic
- Coordinates between AI, Firebase, and UI
- Handles message flow and context memory
- Controls voice narration state

#### **RunAnywhereHelper**

- AI story generation using NVIDIA NIM API
- Text-to-Speech integration
- Fallback story generation
- API request/response handling

#### **SpeechRecognizerHelper**

- Voice input recognition
- Speech-to-text conversion
- Real-time transcription

#### **FirestoreHelper**

- Cloud storage operations
- Session management
- Message persistence

## 🔧 Configuration

### AI Model Configuration

The app is configured to use NVIDIA's Llama 3.1-8b-instruct model. You can change the model in
`RunAnywhereHelper.kt`:

```kotlin
val requestBody = ApiRequest(
    model = "meta/llama-3.1-8b-instruct",  // Change model here
    messages = messages,
    temperature = 0.8,  // Creativity level (0.0-1.0)
    topP = 0.9,
    maxTokens = 300,    // Response length
    stream = false
)
```

### Firebase Configuration

1. Enable Firestore in Firebase Console
2. Set up security rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /sessions/{sessionId} {
      allow read, write: if true;
    }
    match /stories/{storyId} {
      allow read, write: if true;
    }
  }
}
```

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open
an issue first to discuss what you would like to change.

### Development Guidelines

1. Follow Kotlin coding conventions
2. Use Jetpack Compose for UI
3. Maintain MVVM architecture
4. Add comments for complex logic
5. Test on multiple devices/Android versions

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **NVIDIA NIM** for AI model API
- **Firebase** for backend infrastructure
- **Google** for Android development tools
- **Material Design** for UI guidelines

## 📧 Contact

For questions or feedback, please open an issue on GitHub.

---

<p align="center">Made with ❤️ and AI</p>
<p align="center">⭐ Star this repo if you find it helpful!</p>
