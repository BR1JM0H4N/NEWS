<div align="center">

<img src="https://raw.githubusercontent.com/BR1JM0H4N/NEWS/refs/heads/main/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png" width="120" height="120" alt="NEWS app icon" />

# 📰 NEWS

**A fast, fully native Android news reader — built with Kotlin & Jetpack Compose**

Headlines straight from Google News RSS, wrapped in a clean Material 3 UI, with
read-aloud support and zero WebViews.

[![Android CI Fast](https://github.com/BR1JM0H4N/NEWS/actions/workflows/Android.yml/badge.svg)](https://github.com/BR1JM0H4N/NEWS/actions/workflows/Android.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-24-brightgreen)](https://developer.android.com/tools/releases/platforms)
[![Target SDK](https://img.shields.io/badge/targetSdk-34-brightgreen)](https://developer.android.com/tools/releases/platforms)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-4.0.0-orange)](https://github.com/BR1JM0H4N/NEWS/releases)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

</div>

---

## ✨ Highlights

| | |
|---|---|
| 🧭 **Guided first launch** | A one-time onboarding screen lets you pick your country and category before your first feed loads — changeable anytime later in Settings |
| 🖼️ **100% native UI** | No WebViews anywhere — every screen is built with Jetpack Compose |
| 🎨 **Material You** | Light / Dark / System theming, plus optional dynamic color that follows your wallpaper on Android 12+ |
| 🌍 **Country + category feeds** | Choose your region and topic; mapped directly to Google News RSS parameters |
| 🔗 **Related coverage** | Expand any headline to see other outlets covering the same story — no extra network calls |
| 🔊 **Read aloud** | Have headlines read to you via the system Text-to-Speech engine, with adjustable speed, pitch, and voice |
| ⚡ **Live top story card** | The biggest story of the moment, front and center |
| ⚙️ **Everything in Settings** | Theme, country/category, related coverage, TTS tuning, and an About panel — all in one place |

---

## 📱 How it works

1. **First launch** → a short onboarding screen asks for your **country** and preferred **category**
2. Your choice is saved instantly and drives the Google News RSS feed URL (`hl`, `gl`, `ceid`, and topic parameters)
3. The **Home** screen shows a live top-story card followed by the rest of your feed
4. Tap the **read-aloud** action to have headlines narrated hands-free
5. Head into **Settings** anytime to change your country, category, theme, or TTS voice — and find the **About** panel at the bottom

---

## 🏗️ Project structure

```
app/src/main/java/com/mohan/news/
├── MainActivity.kt                # Compose navigation host (onboarding → home → settings → about)
├── NewsViewModel.kt               # App state, feed loading, settings, TTS orchestration
├── data/
│   ├── Models.kt                  # Article, RelatedSource, country/category catalog
│   └── SettingsRepository.kt      # DataStore-backed persisted settings + onboarding flag
├── network/
│   ├── GoogleNewsUrlBuilder.kt    # Builds Google News RSS URLs from country/category
│   ├── RssParser.kt               # Parses RSS XML + related-coverage <li> links
│   └── NewsRepository.kt          # OkHttp fetch + error handling
├── tts/
│   └── HeadlineTtsManager.kt      # Wraps Android TextToSpeech, queues headlines
└── ui/
    ├── theme/                     # Material 3 theme, dynamic color, typography
    ├── components/                # LiveFeedCard, NewsArticleCard, loading/error states
    └── screens/                   # OnboardingScreen, HomeScreen, SettingsScreen, AboutScreen
```

---

## 🚀 Getting started

### Requirements

- Android Studio Koala (2024.1) or newer
- JDK 17
- Android SDK Platform 34

### Build

```bash
git clone https://github.com/BR1JM0H4N/NEWS.git
cd NEWS
./gradlew assembleDebug
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

### Run tests / CI locally

```bash
./gradlew check
```

The same checks run automatically on every push and pull request via the **Android CI Fast** workflow (badge above).

---

## 🧠 Technical notes

<details>
<summary><b>How "related coverage" works</b></summary>

Google News RSS embeds related-coverage links directly inside each `<item>`'s
`<description>` field as an HTML `<ol><li>` list — each `<li>` links to another outlet
covering the same story cluster. `RssParser.kt` extracts these directly, so related
sources are fast and accurate to Google's own story clustering, with no extra network
calls or fuzzy title-matching required.

</details>

<details>
<summary><b>Feed URL format</b></summary>

- Top headlines: `https://news.google.com/rss?hl=en-US&gl=US&ceid=US:en`
- Category feed: `https://news.google.com/rss/headlines/section/topic/TECHNOLOGY?hl=en-US&gl=US&ceid=US:en`

Both country and category are fully driven by the onboarding screen and Settings → News Feed.

</details>

---

## 🤝 Contributing

Issues and pull requests are welcome! If you spot a bug or have a feature idea, open an
issue on the [issue tracker](https://github.com/BR1JM0H4N/NEWS/issues).

## 📄 License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

<div align="center">

Made with ❤️ by [@BR1JM0H4N](https://github.com/BR1JM0H4N)

</div>
