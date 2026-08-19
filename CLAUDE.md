# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

reSync is a single-module Android app (Kotlin + Jetpack Compose) that downloads fanfiction from
fanfiction.net and archiveofourown.org, converts it into an EPUB, and hands it to another app
(typically the reMarkable app) via an Android share intent. Direct reMarkable Cloud integration
(pairing, account storage, direct upload) was removed as unmaintained and is planned to be
reimplemented from scratch in a future PR — see "reMarkable Cloud" below for the anchor points
left in place for that.

## Commands

```bash
./gradlew test                  # all unit tests (what CI runs)
./gradlew testDebugUnitTest     # debug-variant unit tests only
./gradlew testDebugUnitTest --tests "eu.monniot.resync.downloader.ArchiveOfOurOwnDriverTest"
./gradlew testDebugUnitTest --tests "*.ArchiveOfOurOwnDriverTest.parse_oneShotStory_firstChapter"
./gradlew assembleDebug         # build APK
./gradlew installDebug          # build + install on a connected device/emulator
./gradlew connectedAndroidTest  # instrumented tests (needs a device; Room DB tests live here)
```

There is no linter configured beyond the Kotlin compiler.

Java 17 source/target, Gradle daemon toolchain is JDK 21 (`gradle/gradle-daemon-jvm.properties`).
`assembleRelease` will fail on any machine but the owner's — `app/build.gradle` hardcodes an
absolute path to a debug signing keystore. Use debug builds.

## Architecture

Two entry-point activities, both pure Compose (`setContent(null) { ReSyncTheme { … } }`):

- `LauncherActivity` → `ui/launcher/LauncherScreen` — bottom-nav shell over Search, Consolidate
  (experimental), and Settings. Consolidate and Settings currently have nothing to do without a
  cloud integration and show placeholder content (see "reMarkable Cloud" below).
- `DeepLinkActivity` — the primary flow. Registered in the manifest for ff.net and AO3 story URLs,
  so the app appears in the browser's "open with" sheet. `DeepLinkActivity.parsePath` turns the URI
  into `(StoryId, ChapterId, DriverType)` and hands it to `ui/downloader/DownloadScreen`.

### Scraping: the Driver + WebView mechanism

`downloader/Driver` is an abstract class with two implementations (`FanFictionNetDriver`,
`ArchiveOfOurOwnDriver`). Two things about it drive most of the design:

1. **Pages are fetched through a real `WebView`, not OkHttp.** FF.Net sits behind Cloudflare, so the
   app loads the URL in a WebView and injects
   `javascript:window.grabber.extractSource(document.querySelector('html').innerHTML)` on
   `onPageFinished`, receiving the HTML back through a `@JavascriptInterface`. Consequently the
   driver must live in the same composable tree as the `AndroidView { WebView }` (see
   `DownloadScreen`), the download cannot run in the background (hence `KeepScreenOn`), and
   `driver.ready()` must be awaited before any `readChapter` call.
2. **Parsing is separated from fetching.** `parseWebPage(source, storyId, chapterId): Chapter` is a
   pure jsoup function, which is why unit tests can feed it saved HTML fixtures from
   `app/src/test/resources/{ao3,ffnet}/`. Add fixtures there when sites change their markup — that
   is the main regression suite.

Fetched HTML is cached on disk under `filesDir` per story/chapter and re-parsed on subsequent runs.
Note both drivers currently point `tmpChaptersFolder` at `filesDir/ffnet`.

Site quirks encoded in the drivers: AO3 rate-limits (~40 chapters / 5 min) and returns a "Retry
later" body → `Driver.Companion.RateLimited`; FF.Net shows a Cloudflare interstitial ("DDoS
protection by") → `Driver.Companion.WaitAndTryAgain`, which reloads the extractor after 5s.
`Chapter.chapterIndex` maps chapter number → `ChapterId`; AO3 reads it from the chapter `<select>`,
FF.Net synthesizes it since chapter ids are just numbers there.

### Download flow

`DownloadScreen` renders a `DownloadState` sealed hierarchy; `downloadLogic` is a single suspend
function that walks the flow and pushes states via `setState`. User interaction inside that linear
flow is done by suspending on a `CompletableDeferred` that a Compose callback completes. Flow:
fetch initial chapter → (if multi-chapter) let the user pick All/One/Range → fetch the rest with
`readWithRateLimit` retry wrapper → `makeEpub` → hand off via the Android Share sheet.

### EPUB generation

`Epub.kt` builds the book with epublib and contains accumulated workarounds for what the reMarkable
reader tolerates: stripping `&nbsp;`, self-closing `<br>`, rewriting `<hr …>` to `<hr/>`, dropping
the xhtml namespace. A generated cover page carries title/author/chapter range.

`FileName.make`/`FileName.parse` are the round-trip convention for uploaded files:
`"<Story>.epub"`, `"<Story> - Ch 3.epub"`, `"<Story> - Ch 3-7.epub"`. The Consolidate screen relies
on `parse` to group documents back into stories, so changing the format breaks it.

### reMarkable Cloud — removed, anchor points for a future reimplementation

The `rmcloud/` package (pairing, Retrofit APIs for sync 1.0/1.5, `EncryptedSharedPreferences`
account storage, direct upload) and the unwired `ui/SetupRemarkableStateMachine` pairing UI were
deleted wholesale — the protocol was undocumented, unmaintained, and reMarkable has likely changed
their cloud API since it was written. Uploading now always goes through the Android Share sheet:
`downloadLogic` (`ui/downloader/DownloadScreen.kt`) writes the epub to `filesDir/epub/` and sends
an `ACTION_SEND` intent through the `eu.monniot.resync.fileprovider` `FileProvider`.

Anchor points left in place for a from-scratch reimplementation:
- `ui/launcher/SettingsScreen.kt` — currently a placeholder; the natural home for future
  account/pairing UI. `SettingsGroup`/`SettingsMenuLine`/`SettingsTileTexts` etc. are the reusable
  building blocks the old account list used and are still there.
- `ui/launcher/ConsolidateScreen.kt`'s `ConsolidateViewModel.refreshDocuments()` — currently a
  no-op; the re-entry point for pulling a remote document list and upserting it into the local DB.
- `database/Document.kt` — the `fromApi` mapper from the old Retrofit DTO was removed, but the
  Room entity/DAO/database are untouched and ready to be repopulated.

### Local database

Room (`database/Document.kt`) mirrors the reMarkable document list so the Consolidate screen can
group per-chapter epubs of the same story. Single entity, `fallbackToDestructiveMigration`, DAO
exposes `Flow`s consumed by `ConsolidateViewModel`. Nothing currently populates it (see above), so
Consolidate only reflects whatever was written to the DB before the cloud integration was removed.

## Testing notes

- Unit tests run on the JVM with `includeAndroidResources = true`. Robolectric is used only where an
  Android class is genuinely needed (e.g. `android.net.Uri` in `DeepLinkActivityTest`).
- `app/src/test/java/android/util/Log.java` is a deliberate stub shadowing the framework `Log` so
  tests can run without mocks. Its own doc says not to extend this trick to other Android classes —
  use Robolectric instead.
- Code that touches `Context` directly (e.g. `context.filesDir`/`FileProvider` inside
  `downloadLogic`) is untestable as unit tests; existing TODOs suggest factoring it out as a
  parameter when touching that area.
