# ScreenLock App


A Andriod Studio app that enhances user safety and promotes healthy screen usage by monitoring the distance between the user and the smartphone screen. The application aims to prevent eye strain and discomfort by providing real-time warnings when the user is too close to the screen and automatically locking the device if the unsafe distance is maintained. Through the integration of machine learning and computer vision, the project seeks to create a practical and user-friendly solution that encourages healthier digital habits.
## Features

- User-friendly interface.
- Implements modern Android components like Material Design and ConstraintLayout.
- Includes a sample face detection feature (if applicable).
- Customizable and scalable architecture.

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer.
- Gradle 7.0 or newer.
- Android SDK 21 or newer.
- Java 11 or newer.

### Installation

Clone the repository:

```bash
git clone https://github.com/Santhosh1l/Screenlock-application.git
cd my-application
```

Open the project in Android Studio:

1. Open Android Studio.
2. Click on `File` -> `Open`.
3. Navigate to the project directory and select it.

Sync the project with Gradle:

1. Click on `File` -> `Sync Project with Gradle Files`.
2. Wait for the sync to complete.

## Project Structure

```plaintext
MyApplication/
│
├── app/
│   ├── build.gradle         // Module-level Gradle file
│   ├── src/
│   │   ├── androidTest/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           └── myapplication/
│   │   │   │               └── MainActivityInstrumentedTest.java   // Instrumentation tests
│   │   │   └── res/
│   │   │       └── values/
│   │   │           └── strings.xml       // Strings used in tests (if needed)
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── example/
│   │   │   │           └── myapplication/
│   │   │   │               ├── MainActivity.java       // Main activity
│   │   │   │               ├── LockScreenReceiver.java // Broadcast receiver
│   │   │   │               └── OtherClasses.java      // Other classes
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   │   └── activity_main.xml  // Layout files
│   │   │   │   └── values/
│   │   │   │       └── dimens.xml           // Dimensions and other resources
│   │   │   │       └── strings.xml           // Strings for the app
│   │   │   ├── AndroidManifest.xml          // Manifest file
│   │   └── assets/                            // Asset files (if any)
│   │
│   ├── test/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── example/
│   │   │           └── myapplication/
│   │   │               └── MainActivityUnitTest.java  // Unit tests
│   │   └── res/
│   │       └── values/
│   │           └── strings.xml
│   └── build.gradle
│
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── .gitignore
├── gradle.properties
├── settings.gradle
└── build.gradle

```

## Build and Run

To build and run the project:

1. Open Android Studio.
2. Click on `Run` -> `Run 'app'`.

This will compile the code and install the app on an Android device or emulator.

## Dependencies

This project uses the following dependencies:

- [Kotlin Standard Library](https://kotlinlang.org/api/latest/jvm/stdlib/)
- [AndroidX Core](https://developer.android.com/jetpack/androidx/releases/core)
- [AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat)
- [Material Components](https://material.io/develop/android)
- [ConstraintLayout](https://developer.android.com/reference/androidx/constraintlayout/widget/ConstraintLayout)

Dependencies are managed via Gradle and can be found in the `build.gradle.kts` file of the `:app` module.
Here's the updated README section with your usage instructions:


## Usage

To use the app, follow these steps:

1. **Open the App:** Launch the application on your device.
2. **Accept Camera Access:** The app requires access to your camera to monitor the distance between your face and the screen.
3. **Proximity Alert & Lock:** If you are too close to the screen, the app will display a warning. If you do not move back, the device will automatically lock, and the camera will turn off.
4. **Focus:** This app is designed to help reduce eye strain and prevent children from using mobile phones too close to their eyes.

