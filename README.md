# Zara Challenge

An Android app for browsing Rick and Morty characters, viewing character details, and managing a personal favourites list. The project separates presentation, business logic, and data access to keep the app easier to understand and test.

## Features

- Browse characters from the [Rick and Morty API](https://rickandmortyapi.com/), with search, status and gender filters, and pagination.
- View character details, including status, origin, location, episode count, and similar characters.
- Add and remove favourites, saved locally on the device.
- Fall back to cached character results when the network is unavailable.

## Architecture

The app follows a **Clean Architecture structure** within a single Gradle module:

- **Presentation:** Jetpack Compose screens render UI state exposed by ViewModels using `StateFlow`.
- **Domain:** Business logic, domain models, repository interfaces, and use cases are independent of UI and data-source details.
- **Data:** Repository implementations coordinate remote and local sources and map their models to domain models.

The presentation layer uses **MVVM**: ViewModels manage screen state and user actions, and Compose observes and renders that state. **Hilt** provides dependencies across the app. This separation keeps responsibilities clear and makes domain and presentation behavior easier to test.

## Tech stack

- Kotlin, Coroutines, and Flow
- Jetpack Compose, Material 3, and Navigation Compose
- Retrofit, Gson, and OkHttp for API access
- Room for character caching and persistent favourites
- Coil for image loading
- JUnit, Coroutines Test, MockK, Robolectric, and AndroidX testing libraries

## Build and test

Open the project in Android Studio, or run:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
