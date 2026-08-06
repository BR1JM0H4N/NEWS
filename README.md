# NEWS — Native Android App

A fully native rewrite of the NEWS app (Kotlin + Jetpack Compose), replacing the previous
WebView-based implementation. Headlines are fetched directly from Google News RSS feeds
and rendered with a native Material 3 UI.

## What changed from the old version

1. **Fully native UI** — no WebView anywhere. All screens are Jetpack Compose.
2. **Material 3 "carded" design** with:
   - Light / Dark / System theme toggle
   - Optional dynamic color (Material You) that follows your wallpaper's palette on Android 12+
3. **Country + category selection** in Settings, mapped directly to Google News RSS URL
   parameters (`hl`, `gl`, `ceid`, and `/headlines/section/topic/<TOPIC>`).
4. **Animated initial loading screen** with a spinning newspaper icon while the first feed loads.
5. **Live Feed hero card** at the top of the home screen showing the current top story.
6. **Main headline cards** that expand to show "other sources covering this story," pulled
   from Google News' own related-coverage links embedded in each RSS item. Can be turned
   off in Settings.
7. **Read headlines aloud** using the system Text-to-Speech engine, with adjustable
   speed, pitch, and voice — accessible via the floating action button on the home screen.
8. **About screen** with a link to https://github.com/BR1JM0H4N

## Project structure

```
app/src/main/java/com/mohan/news/
├── MainActivity.kt              # Compose navigation host
├── NewsViewModel.kt             # App state, feed loading, settings, TTS orchestration
├── data/
│   ├── Models.kt                 # Article, RelatedSource, country/category catalog
│   └── SettingsRepository.kt     # DataStore-backed persisted settings
├── network/
│   ├── GoogleNewsUrlBuilder.kt   # Builds Google News RSS URLs from country/category
│   ├── RssParser.kt              # Parses RSS XML + related-coverage <li> links
│   └── NewsRepository.kt         # OkHttp fetch + error handling
├── tts/
│   └── HeadlineTtsManager.kt     # Wraps Android TextToSpeech, queues headlines
└── ui/
    ├── theme/                    # Material 3 theme, dynamic color, typography
    ├── components/                # LiveFeedCard, NewsArticleCard, loading/error states
    └── screens/                   # HomeScreen, SettingsScreen, AboutScreen
```

## Building

This project uses Gradle with the Kotlin DSL. To build:

```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

Requirements:
- Android Studio Koala (2024.1) or newer recommended
- JDK 17
- Android SDK Platform 34

> Note: this sandbox environment doesn't have network access to Google's Maven repo or
> the Gradle distribution server, so the build could not be compiled/verified end-to-end
> here. The code was carefully hand-reviewed for correctness (imports, API surface for
> the pinned Compose BOM 2024.09.02 / Material3 1.3.x, brace balance, etc.), but please
> run a build in Android Studio and let me know if anything needs fixing — I can iterate
> quickly from any error output.

## Notes on the "differing takes" feature

Google News RSS embeds related-coverage links directly inside each `<item>`'s
`<description>` field as an HTML `<ol><li>` list — each `<li>` has a link and source name
for another outlet covering the same story cluster. `RssParser.kt` extracts these without
needing a second network call or fuzzy title-matching, so it's fast and accurate to
Google's own story clustering.

## Notes on RSS feed URLs

- Top headlines: `https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en`
- Category feed: `https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en`

Both country and category are fully driven by Settings → News Feed.
