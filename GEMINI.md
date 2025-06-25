# Project Overview

This is an Android application built with Kotlin and Jetpack Compose, following a modularized, feature-based architecture. The app appears to be a team collaboration tool, similar in concept to Discord or Slack, with features for projects, channels, direct messaging, and scheduling.

## Core Technologies

*   **UI:** Jetpack Compose
*   **Architecture:** MVVM (Model-View-ViewModel) with UseCases
*   **Dependency Injection:** Hilt
*   **Asynchronous Programming:** Kotlin Coroutines & Flow
*   **Backend:** Firebase (Firestore, Authentication, Storage)
*   **Navigation:** Jetpack Navigation for Compose
*   **Image Loading:** Coil

## Project Structure

The project is divided into several module types:

*   `app`: The main application module, responsible for tying all the other modules together, including navigation graphs and dependency injection setup.
*   `core`: Contains shared functionality used across multiple feature modules.
    *   `core_common`: Common utilities, constants, and helper classes.
    *   `core_navigation`: Navigation logic, routes, and the `NavigationManger`.
    *   `core_ui`: Reusable Jetpack Compose components, theme, and UI-related utilities.
    *   `core_fcm`: Firebase Cloud Messaging setup and handling.
*   `data`: Handles data sources, repositories, and data mapping (DTOs). It interacts directly with Firebase.
*   `domain`: Contains the core business logic of the application. This includes UseCases, domain models (entities), and repository interfaces.
*   `feature`: Each feature of the application is encapsulated in its own module. This promotes separation of concerns and independent development. Examples include:
    *   `feature_home`: The main screen, likely displaying a list of projects and direct messages.
    *   `feature_chat`: The chat screen for channels and direct messages.
    *   `feature_calendar`: A calendar view for schedules.
    *   `feature_login`, `feature_signup`: User authentication screens.
    *   Many other features for creating/editing projects, channels, schedules, etc.

## Key Features

*   **Project Management:** Users can create, join, and manage projects.
*   **Channels:** Projects can have channels for communication, similar to Slack or Discord.
*   **Direct Messaging:** Users can have one-on-one conversations.
*   **Scheduling:** A calendar feature allows for the creation and viewing of schedules.
*   **User Authentication:** Users can sign up, log in, and manage their accounts.
*   **Real-time Updates:** The use of Firebase suggests that many features are updated in real-time.

## How to Run

1.  **Set up Firebase:** A `google-services.json` file is required in the `app` module. This file should be obtained from a Firebase project with Firestore, Authentication, and Storage enabled.
2.  **Build and Run:** The project can be built and run on an Android device or emulator using Android Studio.

## High-Level Flow

1.  The app starts with `MainActivity`, which sets up the main navigation graph.
2.  The `feature_splash` module likely checks the user's authentication status.
3.  If not authenticated, the user is directed to the `feature_login` or `feature_signup` screens.
4.  Once authenticated, the user is taken to the `feature_main` module, which likely hosts the `feature_home` screen as the main content area.
5.  The `feature_home` screen acts as the central hub, allowing navigation to projects, DMs, and other features.
6.  The `NavigationManger` in `core_navigation` handles all navigation between feature modules.
7.  ViewModels in each feature module use UseCases from the `domain` module to interact with data.
8.  UseCases get data from Repositories, which are implemented in the `data` module and communicate with Firebase.
