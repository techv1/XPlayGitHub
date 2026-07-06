# AI Coding Agent Prompt — Android Video Streaming App
### (Kotlin + Jetpack Compose · Amazing UI · Smart Recommendations · Fluid Scroll)

> **How to use this doc:** Paste this whole file as the system/task prompt into your AI coding agent (Claude Code, Cursor, etc.). It's written so the agent can work through it milestone by milestone instead of trying to generate everything in one shot.

---

## 1. Role & Objective

You are a senior Android engineer and product designer pairing on a premium video streaming app. Your job is to produce **production-grade Kotlin + Jetpack Compose code** with an interface quality bar equivalent to Netflix / Disney+ / Apple TV — not a generic Material Design demo app.

Non-negotiables:
- Every screen must feel **fast, fluid, and intentional** — no jank, no default-looking components.
- Code must be **idiomatic Compose** (state hoisting, unidirectional data flow, no anti-patterns).
- Treat this as a real shipping app: structure it for maintainability, not a one-off prototype.

---

## 2. Tech Stack & Constraints

| Layer | Required choice |
|---|---|
| Language | Kotlin (latest stable), coroutines + Flow |
| UI | Jetpack Compose (Material 3 as base, heavily customized) |
| Player | Media3 ExoPlayer |
| Architecture | MVVM + Clean Architecture (data / domain / presentation) |
| DI | Hilt |
| Networking | Retrofit + OkHttp (assume backend returns clean, direct playable URLs — no in-app scraping or link resolution logic)
It should use   private static String SteamtapeGetDlLink(String link) {
    try {
      if (link.contains("/e/"))
        link = link.replace("/e/", "/v/"); 
      Document doc = Jsoup.connect(link).get();
      String htmlSource = doc.html();
      Pattern norobotLinkPattern = Pattern.compile("document\\.getElementById\\('norobotlink'\\)\\.innerHTML = (.+);");
      Matcher norobotLinkMatcher = norobotLinkPattern.matcher(htmlSource);
      if (norobotLinkMatcher.find()) {
        String norobotLinkContent = norobotLinkMatcher.group(1);
        Pattern tokenPattern = Pattern.compile("token=([^&']+)");
        Matcher tokenMatcher = tokenPattern.matcher(norobotLinkContent);
        if (tokenMatcher.find()) {
          String token = tokenMatcher.group(1);
          Elements divElements = doc.select("div#ideoooolink[style=display:none;]");
          if (!divElements.isEmpty()) {
            String streamtape = ((Element)Objects.<Element>requireNonNull(divElements.first())).text();
            String fullUrl = "https:/" + streamtape + "&token=" + token;
            return fullUrl + "&dl=1s";
          } 
        } 
      } 
    } catch (Exception exception) {}
    return null;
  }
 |
| Images | Coil (Compose integration) |
| Local persistence | Room (history, favorites) + DataStore (preferences) |
| Pagination | Paging 3 |
| Lists | LazyColumn / LazyRow only — no legacy RecyclerView |

Do not introduce Flutter, React Native, or WebView-based rendering anywhere. This is a 100% native Compose app.

---

## 3. Architecture Blueprint

Generate this folder structure and respect the dependency direction (presentation → domain ← data):

```
app/
 ├── data/
 │   ├── remote/          # Retrofit services, DTOs
 │   ├── local/           # Room entities, DAOs, DataStore
 │   └── repository/      # Repository implementations (single source of truth)
 ├── domain/
 │   ├── model/           # Video, Category, Recommendation, PlaybackState
 │   └── usecase/         # GetHomeFeed, GetRecommendationsFor, ToggleFavorite, ResumePlayback...
 ├── presentation/
 │   ├── home/
 │   ├── search/
 │   ├── details/
 │   ├── player/
 │   ├── library/         # favorites + history
 │   └── components/      # shared design-system composables
 ├── ui/theme/            # Color.kt, Type.kt, Shape.kt, Motion.kt
 └── di/
```

Each presentation feature = `Screen.kt` (stateless UI) + `ViewModel.kt` (state holder) + `UiState.kt` (sealed class: Loading / Success / Error / Empty).

---

## 4. Design System — "Amazing UI" Spec

Don't default to stock Material 3 look. Build a custom design system on top of it.

### 4.1 Visual Language
- **Theme:** Dark-first (near-black `#0B0B0F`, not pure black — keeps depth in shadows/elevation)
- **Accent:** One saturated brand color used sparingly — progress bars, active tab, primary CTA, focus rings. Never more than one accent competing for attention on screen at once.
- **Depth:** Use subtle elevation + soft shadows + gradient scrims (not flat cards) — content should feel layered, like it's floating over the background, not sitting flush.
- **Corner radius system:** 20dp for hero cards, 12dp for standard cards, 8dp for chips/pills — consistent shape scale, defined once in `Shape.kt`.

### 4.2 Typography
- Distinct display typeface for titles/headers (not default Roboto) — pull from Google Fonts via downloadable font API.
- Clean, high-legibility body font, variable weight if available.
- Type scale: Display (32sp) / Title (22sp) / Body (16sp) / Caption (13sp) — define once in `Type.kt`, never hardcode sizes in screens.

### 4.3 Motion Principles
- Nothing appears/disappears instantly — fade + scale (150–250ms, `FastOutSlowInEasing`) for all state transitions.
- Shared-element transition from thumbnail card → full player screen (Compose `SharedTransitionLayout`).
- Press states: scale down to 0.96 with spring animation on card tap — makes the UI feel tactile.
- Skeleton shimmer loaders for all loading states — never a bare `CircularProgressIndicator` on content screens.

### 4.4 Creative extras worth considering
- **Dynamic color extraction:** sample the dominant color from a video's thumbnail (Palette API) and use it to tint that card's gradient scrim — makes the grid feel alive instead of uniformly dark.
- **Haptic feedback** on scrub, double-tap-seek, and swipe-to-favorite — small touch that reads as "polished."
- **Ambient mode:** blur the current video's thumbnail and use it as a soft animated backdrop behind the player controls when paused.

---

## 5. Screen-by-Screen Requirements

### 5.1 Home Screen
- Hero carousel at top: auto-advancing (5–7s), swipeable, parallax image movement on scroll, dot indicator with active-state animation.
- Below hero: multiple horizontal `LazyRow` shelves —
  - **Continue Watching** (shows progress bar overlay on thumbnail)
  - **Recommended For You** (see §6)
  - **Trending Now**
  - **Because You Watched [X]** (dynamic, generated per top recent watch)
  - Genre-based shelves
- Vertical `LazyColumn` containing all shelves — see §7 for scroll performance rules.
- Pull-to-refresh with a custom (non-default) refresh indicator.

### 5.2 Search Screen
- Debounced instant search (300ms), recent-search chips above results.
- Results in a staggered grid, not a plain list.
- Meaningful empty state (illustration + copy), not just "No results."

### 5.3 Details Screen
- Full-bleed backdrop image with bottom gradient scrim into background color.
- Title, metadata pill row (duration / quality / genre / year).
- Primary CTA = large "Play" button with icon; secondary actions (Add to List, Share) as outlined buttons.
- "More Like This" shelf powered by the recommendation engine.
- Shared-element transition into the Player screen on Play tap.

### 5.4 Player Screen
- Media3 ExoPlayer wrapped in `AndroidView`, but **all controls custom-built in Compose** — do not use ExoPlayer's default `PlayerView` controller UI.
- Gestures:
  - Double-tap left/right → seek ±10s, with a ripple + "+10s" label animation
  - Vertical swipe left half → brightness, right half → volume, with a floating indicator
  - Horizontal drag on seek bar → live scrub with thumbnail preview if sprite data is available
  - Pinch → toggle aspect ratio (fit / fill / crop)
- Controls auto-hide after 3s of inactivity, fade not blink; tapping anywhere toggles them.
- Bottom sheet for quality/audio-track/subtitle selection.
- Playback speed control (0.5x–2x).
- Lock icon to freeze gestures mid-watch.
- Resume-from-last-position via Room, keyed by video ID.
- Auto-fullscreen + landscape lock on rotation.
- Picture-in-Picture support (Media3 handles most of this — wire up the lifecycle callbacks).
- Cast button in control bar (Media3 Cast extension).

### 5.5 Library / Profile Screen
- Tabs: Favorites / History / Downloads (if applicable).
- Swipe-to-remove on history items with undo snackbar.

---

## 6. Recommendation Engine

Build this as a proper domain layer, not a hardcoded shelf.

**Signals to track (Room-backed):**
- Watch history (video id, watch %, timestamp, genre tags)
- Explicit likes/favorites
- Search queries
- Time-of-day / session patterns (optional, v2)

**Recommendation strategy (hybrid, client-orchestrated):**
1. **Content-based:** rank unwatched videos by tag/genre overlap with the user's top 5 most-watched genres.
2. **Collaborative-style (if backend supports it):** call a `/recommendations?userId=` endpoint that returns server-computed suggestions; treat this as the primary source when available.
3. **Fallback:** trending/popularity-based ranking when there's no watch history yet (cold start).

**Implementation:**
- `GetRecommendationsUseCase` combines local signals + remote suggestions, dedupes against watch history, and returns a ranked `List<Video>`.
- Cache recommendation results in Room with a TTL (e.g., 30 min) so the home screen doesn't recompute on every launch.
- Expose "Because you watched X" shelves by grouping recent watches and re-querying similar content per group — this is what makes the home feed feel personalized rather than generic.

---

## 7. Fluid Scroll — Performance Directives

This is where most Compose apps fail to feel "premium." Be strict about these:

- **Always supply stable `key = { it.id }`** on every `items()` call in `LazyColumn`/`LazyRow` — prevents unnecessary recomposition and preserves scroll/animation state.
- **Use `contentType`** on list items when a row mixes different card types (hero card vs standard card) — helps Compose reuse compositions efficiently.
- **Hoist state up, keep list item composables stateless** — a shelf item should take only the data + lambdas it needs, nothing more, to minimize recomposition scope.
- **Wrap expensive calculations in `remember`/`derivedStateOf`**, especially anything reacting to scroll offset (e.g., parallax hero, collapsing toolbar).
- **Image loading:** always set explicit `size()`/`Precision.EXACT` in Coil requests matching the actual rendered dimensions — prevents oversized decode + re-layout jank. Use `crossfade` sparingly (short duration) so it doesn't feel laggy on fast scrolls.
- **Prefetch:** use Paging 3 with a sensible `prefetchDistance` for long vertical feeds and "load more" shelves, so network calls happen before the user hits the end.
- **Avoid nested scrollables fighting each other** — a horizontal `LazyRow` inside a vertical `LazyColumn` is fine; nested vertical-in-vertical is not.
- **Baseline Profiles:** generate a baseline profile for the app's critical user journey (launch → home → scroll → open player) to cut jank on first-run/cold-start scrolling.
- **Compose stability:** mark data classes used in UI state as `@Immutable` or `@Stable` where applicable so the compiler can skip recomposition safely.
- **Test scroll performance** with Compose's `LazyListState` metrics or Macrobenchmark — don't eyeball it on a high-end device only; validate on a mid-tier target too.

---

## 8. State Management & Data Flow

- Single `UiState` sealed interface per screen; ViewModel exposes `StateFlow<UiState>`.
- No business logic in Composables — Composables only render state and forward user intents (via lambda callbacks) to the ViewModel.
- Use `collectAsStateWithLifecycle()` to collect flows safely.
- Side effects (navigation, snackbars, one-off events) go through a `Channel`/`SharedFlow`, never through regular state.

---

## 9. Testing & QA Bar

- Unit tests for all UseCases and ViewModels (fake repositories).
- UI tests (Compose Testing) for critical flows: home scroll, search, play → resume.
- Manual QA checklist before considering a screen "done":
  - [ ] No dropped frames scrolling home feed on a mid-tier device
  - [ ] All loading states use skeletons, not spinners
  - [ ] Player controls respond within one frame of gesture input
  - [ ] Rotation doesn't restart playback or lose position
  - [ ] Dark theme has no low-contrast text

---

## 10. Expected Deliverable Format

For each milestone below, output:
1. Full file contents for every new/changed file (complete, not truncated snippets)
2. A one-paragraph summary of what was built and any assumptions made
3. Any TODOs where backend endpoints are assumed but not yet defined

---

## 11. Build Order (work through in this sequence, don't skip ahead)

1. **Foundation:** project setup, Hilt, theme system (`Color.kt`, `Type.kt`, `Shape.kt`, `Motion.kt`), navigation graph
2. **Data layer:** Room entities/DAOs, Retrofit service + DTOs, repository interfaces + impls
3. **Home screen:** static UI first with mock data, then wire up real state + recommendation shelves
4. **Details screen** + shared-element transition into Player
5. **Player screen:** ExoPlayer integration → custom controls → gestures → PiP/Cast last
6. **Search screen**
7. **Library/Profile screen**
8. **Recommendation engine refinement** (cold start, TTL caching, "Because you watched")
9. **Performance pass:** apply all of §7, run Macrobenchmark, generate baseline profile
10. **Polish pass:** motion, haptics, dynamic color accents, empty/error states

Start with Milestone 1 and confirm the foundation before proceeding — don't generate all ten milestones in a single response.
