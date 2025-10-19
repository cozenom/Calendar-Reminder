# Room Database Schemas

This directory contains exported database schemas from Room.

## Purpose

Room automatically generates JSON schema files here when you build the project. These files:

- Document the database structure at each version
- Help verify migrations are correct
- Should be committed to version control to track schema history

## Files

Each JSON file represents the database schema at a specific version:

- `com.example.calendarapp.data.database.AppDatabase/5.json` - Version 5 schema
- `com.example.calendarapp.data.database.AppDatabase/6.json` - Version 6 schema
- etc.

## Building

Schema files are generated automatically when running:

```bash
./gradlew build
```

The schema export path is configured in `app/build.gradle.kts`:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

## Note

These files will be generated once you build the project with Java 17 or higher.
