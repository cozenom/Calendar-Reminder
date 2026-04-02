# Calendar Reminder App

Lightweight Android app for scheduling periodic reminders — medications, plant watering, pet feeding, vitamins, habits, and more. Works completely offline with zero data collection.

## Features

- **Custom Schedules**: Set reminders for specific times and days of the week with optional end dates
- **Persistent Notifications**: Reliable notifications that don't auto-dismiss, with one-tap "Take" button
- **Visual Calendar**: Color-coded history showing completed (green) and missed (red) reminders
- **Multiple Daily Reminders**: Schedule multiple times per day for the same item
- **Always Works**: Survives device reboots, requires no internet connection

## Privacy

**100% offline. Zero data collection.** All data stored locally on your device using SQLite database. No cloud sync, no analytics, no external servers, no permissions beyond notifications. Your information never leaves your phone.

## Requirements

- Android 8.0+ (API 26)
- ~5 MB storage
- Notification permissions (requested on first run)

## Technical Stack

- Kotlin + Jetpack Compose with Material3
- Room database (SQLite) for local storage
- AlarmManager + WorkManager for reliable notifications
- MVVM architecture
