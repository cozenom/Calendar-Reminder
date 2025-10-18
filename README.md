# Medication Reminder App

A simple and reliable Android app to help you manage your medication schedules with timely reminders
and visual tracking.

## Features

### 📋 Medication Management

- Add multiple medications with custom names
- Set flexible schedules (1x, 2x, 3x+ daily)
- Configure specific reminder times for each dose
- Select which days of the week to take each medication
- Set start dates and optional end dates
- Edit or delete medications anytime

### 🔔 Smart Notifications

- Persistent notifications that stay until you dismiss them
- Exact-time alarms for reliable medication reminders
- Quick "Take" button directly in notifications
- Sound and vibration alerts
- Automatic rescheduling after device reboot
- No duplicate notifications

### 📅 Calendar View

- Visual monthly calendar of all medications
- Color-coded status indicators:
    - 🟢 Green = Taken
    - 🔴 Red = Missed/Pending
- Tap any date to see scheduled medications
- Track your complete medication history

### ✅ Intake Tracking

- Mark medications as taken from the calendar or notifications
- View today's medication status on the Medications tab
- Real-time updates across all screens
- Complete intake history

## Requirements

- Android 8.0 (API 26) or higher
- Notification permissions (Android 13+)
- Exact alarm permissions (Android 12+)

## Tech Stack

- **Kotlin** with Jetpack Compose
- **Room Database** for local storage
- **WorkManager** and **AlarmManager** for scheduling
- **Material 3** design
- **MVVM Architecture**

## Building

```bash
# Build the app
./gradlew build

# Install on device
./gradlew installDebug

# Run tests
./gradlew test
```

## License

This project is built for personal use.
