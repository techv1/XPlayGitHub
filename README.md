# XPlay 🎬

A **premium Android video streaming app** built to Netflix/Disney+ quality standards.

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin (latest stable) + Coroutines + Flow |
| UI | Jetpack Compose (Material 3, heavily customized) |
| Player | Media3 ExoPlayer |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Networking | Retrofit + OkHttp |
| Images | Coil |
| Persistence | Room + DataStore |
| Pagination | Paging 3 |

## Project Structure

```
app/
├── data/
│   ├── remote/          # Retrofit services, DTOs
│   ├── local/           # Room entities, DAOs, DataStore
│   └── repository/      # Repository implementations
├── domain/
│   ├── model/           # Video, Category, Recommendation, PlaybackState
│   └── usecase/         # GetHomeFeed, GetRecommendationsFor, ToggleFavorite...
├── presentation/
│   ├── home/
│   ├── search/
│   ├── details/
│   ├── player/
│   ├── library/
│   └── components/
├── ui/theme/            # Color.kt, Type.kt, Shape.kt, Motion.kt
└── di/
```

## Build Order (Milestones)

1. ✅ **Foundation** — Hilt, theme system, navigation graph
2. 🔲 Data layer — Room, Retrofit, repositories
3. 🔲 Home screen
4. 🔲 Details screen + shared-element transition
5. 🔲 Player screen — custom controls, gestures, PiP/Cast
6. 🔲 Search screen
7. 🔲 Library/Profile screen
8. 🔲 Recommendation engine refinement
9. 🔲 Performance pass
10. 🔲 Polish pass

## Getting Started

```bash
git clone https://github.com/techv1/XPlayGitHub.git
cd XPlayGitHub
# Open in Android Studio Iguana or newer
# Sync Gradle, run on device or emulator (API 26+)
```

## License
MIT
