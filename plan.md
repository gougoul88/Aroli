# Plan: AROLI Story Box Android App

## Context / Decisions gathered
- Greenfield project, workspace currently only has empty AROLI.txt.
- Old phone: Android 10-12 (API 29-32).
- Local audio: single fixed folder, flat file list, MP3/OGG/M4A/WAV only (WMA dropped - no native Android codec support).
- Web audio: laguildedesecrivains.fr is a TEXT collaborative-fiction site (no audio, no API) - NOT used directly.
  Instead: audio + a JSON manifest are stored directly in this project's own GitHub repo
  (https://github.com/gougoul88/Aroli), under a dedicated `content/` folder (mp3s + images + manifest.json).
  App fetches the manifest and files straight from GitHub (raw.githubusercontent.com), downloads+caches MP3
  on first play (offline after that). No separate web host/VPS/FTP upload needed - adding a story is just
  committing new files to the repo's `content/` folder.
- Kiosk/auto-start: user wants NO adb/root/Device Owner (must be shareable with non-programmers as a plain
  sideloaded APK). Resolution: use Home-launcher replacement (intent-filter CATEGORY_HOME) as the auto-start
  mechanism (simpler & more reliable than BOOT_COMPLETED receiver, which is restricted on Android 10+ anyway),
  plus optional self-service Screen Pinning (startLockTask(), no Device Owner needed) for extra lock-down.
  Known limitation: without Device Owner, notification shade pulldown and back+overview-hold unpin gesture
  are still theoretically possible for a curious kid - documented as accepted trade-off.
- Prev/Next at list ends: CONFIRMED wrap-around (looping), not clamp.
- Profiles (son/daughter): decided best low-complexity solution - keep ONE shared content catalog (single
  local folder / single web manifest, no duplicated data layer), but add TWO lightweight child profiles that
  each remember their own resume position (last played index/id) in that shared list. Switching profile is a
  small always-visible icon/avatar (not hidden like Parent Settings, since kids pick their own profile) -
  default labels e.g. "Enfant 1 / Enfant 2", custom name/picture editable later as a nice-to-have.

## Recommended architecture
- Native Android app, Kotlin, Jetpack Compose UI, min SDK 29 / target SDK current stable.
- Single Activity (MainActivity), full screen, declared as HOME/DEFAULT/LAUNCHER, launchMode=singleTask,
  excludeFromRecents=true, onBackPressed() no-op, optional startLockTask() screen pinning on launch.
- UI: Compose Row with 3 weighted zones - left third = big "previous" tap target/arrow icon, middle third =
  Coil AsyncImage (cover art) or big Text(filename) fallback, right third = big "next" tap target/arrow icon.
  Tap middle = play/pause toggle. Small always-visible profile-switcher icon/avatar (2 profiles, e.g.
  "Enfant 1 / Enfant 2") in a corner, distinct from the hidden Parent Settings gesture.
- Playback: Media3 ExoPlayer instance in a ViewModel; AudioManager focus handling; BroadcastReceiver for
  ACTION_AUDIO_BECOMING_NOISY (pause on headphone unplug).
- Navigation: ONE shared ordered playlist (List<StoryItem>) for both profiles (no duplicated content/data
  layer), but PER-PROFILE persisted current index/id via Jetpack DataStore (Preferences), plus a persisted
  "active profile" flag, so each child's playback position survives reboot independently. Prev/Next wrap
  around at list boundaries (confirmed, kid-friendly endless-loop feel).
- Data layer - two repository implementations behind a common interface (StoryRepository: list(), resolvePlayableUri(item)):
  1. LocalFolderRepository: Storage Access Framework - one-time ACTION_OPEN_DOCUMENT_TREE folder picker
     (parent selects folder once in a settings screen), persist URI permission
     (takePersistableUriPermission), list files via DocumentFile each app start. Metadata/cover art via
     built-in android.media.MediaMetadataRetriever (getEmbeddedPicture()); if no embedded art, look for a
     same-name .jpg/.png sibling; else fall back to displaying the filename (per requirement).
  2. GitHubContentRepository: fetch JSON manifest (OkHttp) from a fixed GitHub raw URL pointing at this repo's
     `content/manifest.json` (e.g. `https://raw.githubusercontent.com/gougoul88/Aroli/main/content/manifest.json`),
     e.g. `{ "stories": [ {"id","title","audioFile","imageFile"} ] }` with paths relative to `content/`.
     Audio/image raw URLs are built from the same repo/branch (`content/audio/<audioFile>`,
     `content/images/<imageFile>`). Cache manifest to disk for offline listing.
     On play: check app's external-files cache dir for already-downloaded file by id; if missing and online,
     download via OkHttp then play; if offline and not cached, show a friendly "unavailable offline" state.
- Settings/mode switch: hidden gesture (e.g. 5x tap on a screen corner) opens a "Parent Settings" screen -
  toggle Local/Web mode, run the folder picker, clear cache. GitHub repo/branch is a hardcoded app constant
  (not user-editable), since content lives in this project's own repo. Not exposed in the main 3-zone kid UI.
- Permissions: INTERNET, ACCESS_NETWORK_STATE (web mode); SAF folder permission is granted via picker (no
  runtime storage permission needed for the read-only folder-tree approach - avoids MANAGE_EXTERNAL_STORAGE
  Settings-diving for non-technical setup).

## Steps (phased, each independently verifiable)

**Phase 0 - Project scaffolding** (no deps)
1. Create new Android Studio/Gradle Kotlin project (Compose template), package e.g. `com.aroli.storybox`,
   min SDK 29, target SDK current. Add Media3 ExoPlayer, OkHttp, Coil, DataStore-preferences, kotlinx-serialization
   (or Gson) dependencies.

**Phase 1 - Core playback + UI shell** (*depends on Phase 0*)
2. Build the 3-zone Compose layout (dumb UI first, static placeholder list) with left/right big tap zones,
   middle image-or-text display, and the small profile-switcher icon (2 fixed profiles).
3. Build PlayerViewModel wrapping ExoPlayer: play/pause, prev/next navigation (wrap-around) over an
   in-memory `List<StoryItem>`, audio focus + noisy-becoming receiver.

**Phase 2 - Local file mode** (*depends on Phase 1, parallel with Phase 3*)
4. Implement SAF folder-picker flow + persisted URI permission (first-run or via Parent Settings).
5. Implement LocalFolderRepository: list DocumentFile children, filter by supported extensions
   (mp3/ogg/m4a/wav), sort by filename.
6. Implement metadata/cover-art extraction via MediaMetadataRetriever + same-name-image fallback + filename
   fallback text.

**Phase 3 - Web mode** (*depends on Phase 1, parallel with Phase 2*)
7. Create the `content/` folder in this repo (`content/manifest.json`, `content/audio/`, `content/images/`) and
   define & document the JSON manifest schema (stories: id/title/audioFile/imageFile, paths relative to `content/`).
8. Implement GitHubContentRepository: fetch+parse+cache manifest from the raw.githubusercontent.com URL,
   build raw audio/image URLs from repo+branch+path, connectivity check (ACCESS_NETWORK_STATE).
9. Implement on-demand download-and-cache-then-play logic with offline fallback state.

**Phase 4 - Mode switching + Parent Settings** (*depends on Phase 2 & 3*)
10. DataStore-backed app settings: current mode (local/web), manifest URL, persisted folder URI, active
    profile flag, and per-profile last-played index/id (2 independent bookmarks over the shared list).
11. Hidden-gesture "Parent Settings" screen: mode toggle, folder picker trigger, manifest URL editor, clear
    cache button.

**Phase 5 - Kiosk / auto-start** (*depends on Phase 1, can start in parallel with Phase 2/3/4*)
12. AndroidManifest: HOME/DEFAULT/LAUNCHER intent-filter on MainActivity, singleTask, excludeFromRecents.
13. Override back button as no-op; add optional startLockTask() screen pinning on launch with graceful
    handling if pinning isn't available/allowed.
14. Restore active profile + its last-played index/story on cold start (post-reboot) from DataStore.

**Phase 6 - Polish** (*depends on all above*)
15. Kid-friendly styling (large fonts/icons, high-contrast colors, simple animations for prev/next).
16. Error/empty states (no files found, manifest unreachable first run, unsupported file skipped).

## Relevant files (to be created - greenfield project)
- `app/src/main/AndroidManifest.xml` - HOME launcher intent-filter, permissions (INTERNET, ACCESS_NETWORK_STATE).
- `app/src/main/java/.../MainActivity.kt` - singleTask, back no-op, lock task trigger.
- `app/src/main/java/.../ui/StoryBoxScreen.kt` - 3-zone Compose layout.
- `app/src/main/java/.../player/PlayerViewModel.kt` - ExoPlayer wrapper, navigation logic.
- `app/src/main/java/.../data/StoryRepository.kt` (interface) + `LocalFolderRepository.kt` + `GitHubContentRepository.kt`.
- `app/src/main/java/.../data/AppSettings.kt` - DataStore preferences wrapper.
- `app/src/main/java/.../ui/ParentSettingsScreen.kt` - hidden gesture entry, mode/folder config.
- `content/manifest.json` - JSON manifest listing stories (id/title/audioFile/imageFile), committed to this repo.
- `content/audio/*.mp3` - story audio files, committed to this repo.
- `content/images/*.jpg` (or .png) - optional cover images, committed to this repo.

## Verification
1. Install APK on the Android 10-12 test phone, select it as Home ("Always"), reboot -> confirms Story Box
   UI auto-appears without manual launch.
2. Local mode: point folder picker at a test folder containing mp3s with/without embedded ID3 cover art and
   with/without sibling image files - confirm middle zone shows art, else sibling image, else filename text.
3. Web mode: commit a small test manifest.json + 2-3 mp3/jpg files under `content/` in the GitHub repo, confirm
   the app's list loads from raw.githubusercontent.com, first play downloads and caches, then toggle airplane
   mode and confirm cached items still replay.
4. Prev/Next wrap-around test: confirm navigating past the last item loops to the first, and vice versa.
5. Reboot test confirms each profile's last-played story/position is restored independently (switch profile,
   play different items on each, reboot, confirm both bookmarks kept).
6. Confirm back button and (if enabled) screen pinning prevent easily leaving the app.

## Further Considerations
1. Screen Pinning vs Device Owner: current plan uses no-adb Screen Pinning (weaker, self-service, one-time
   system dialog) per user's explicit "no adb/root, easy install" requirement. If a determined child or the
   user later wants bulletproof lockdown, Device Owner (adb-provisioned) could be revisited - deliberately
   out of scope now.
2. Profile customization (real names/photos for "Enfant 1/2" instead of default labels) - deferred as a
   future enhancement, not required for v1.

## Scope boundaries (explicitly excluded from this plan)
- No Play Store publishing (sideloaded APK only).
- No WMA support.
- No Device Owner / full MDM-grade kiosk lockdown.
- No subfolder navigation for local files (flat single folder only).
- No scraping/TTS integration with laguildedesecrivains.fr (site is text-only, not used as an audio source).
- No user-configurable web host/manifest URL - GitHub repo/branch is fixed at build time; adding stories means
  committing files to `content/` in this repo.
- No parental PIN lock on the hidden settings gesture in v1 (flagged as future enhancement).
