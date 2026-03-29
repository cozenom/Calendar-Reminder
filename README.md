# Calendar Reminder App

A simple Android app to help you set and track recurring reminders for anything — medications, plant watering, vitamins, habits, and more.

## What It Does

- **Set Reminders**: Schedule daily reminders at specific times with customizable frequencies
- **Track Completion**: Mark reminders as done or missed, view your history on an interactive calendar
- **Visual Calendar**: See your reminder history at a glance with color-coded dots:
  - 🟢 Green: Reminders completed
  - 🔴 Red: Missed reminders
- **Missed Reminder Alerts**: Get notified when you've missed a reminder

## Privacy & Data

**Your data stays on your device.** This app stores all reminder information locally using Room database - no cloud sync, no external servers, no data collection. Your information is private and under your control.

## Built With

- Kotlin & Jetpack Compose
- Room Database
- Material Design 3 with dark mode support
- WorkManager for notifications

## Architecture

This app uses the **MVVM (Model-View-ViewModel)** pattern to keep code organized and maintainable:

- **Model** (`data/` folder): Handles all data operations
  - `model/`: Database entities
  - `dao/`: Database queries (Data Access Objects)
  - `repository/`: Business logic layer
  - `database/`: Room database configuration
  - `notification/`: Background tasks and notifications

- **View** (`MainActivity.kt`): All UI components
  - Composable functions that display data
  - User interactions (buttons, forms, calendar)

- **ViewModel** (`viewmodel/`): Coordinates between Model and View
  - Holds UI state
  - Manages data flow from database to screen
  - Survives screen rotations

**Benefits**: Separating these layers makes the code easier to test, debug, and maintain. Changes to the UI don't affect data logic, and vice versa.

## Requirements

- Android 8.0 (API 26) or higher
- Notification permissions for reminders
