# Changelog

All notable changes to Aroli will be documented in this file.

## [1.4] - TBD

### ✨ Coming Soon

- Under development...

---

## [1.3] - 2026-10-09

### ✨ New Features

- **AI Story Filtering in Manage Stories**: Parents can now control whether AI-generated stories are shown
  - Checkbox "Show AI-Generated Stories" in Manage Stories screen (same level as Age and Language)
  - Checked by default (shows all stories)
  - Unchecked (hides AI-generated stories)
  - Filter indicator in counter showing "AI: Yes/No"

### 🔧 Technical Changes

- Added AI-generated story filtering in ManageStoriesScreen
- Stories marked with "ai: true" in manifest.json can be filtered
- Story Manager web tool displays 🤖 AI badge for easy identification
- Improved filter feedback with AI status in counter

---

## [1.2] - 2026-10-09

### ✨ New Features

- **Selective Story Management (Web Mode)**: Parents can now choose exactly which stories to show/download instead of all published stories appearing automatically
  - Manage Stories screen accessible via Parent Settings
  - On-demand sync - stories refresh when opening Manage Stories
  - New stories default to unchecked for better control

- **Age-Based Story Filtering**: Child age input (0-18) in Manage Stories screen
  - Stories automatically filtered by age range (ageMin/ageMax)
  - Easy number input field in parent settings

- **Language Selection in Parent Menu**: Moved language preference to Manage Stories screen
  - Horizontal layout for Age and Language selectors
  - Support for French (Français), English, and German (Deutsch)

- **Folder Organization**: Stories can now be organized by folders in GitHub manifest
  - Virtual folder tree displayed in story browser
  - Navigate folders with visual hierarchy
  - Backward compatible - stories without folder work as before

- **Improved Volume Control**: Quadratic scaling for better human perception
  - Default volume set to 100%
  - Volume limiter range: 10-100%
  - More natural volume response curve

- **Enhanced Installation**: Interactive setup scripts
  - Windows: `install.bat`
  - Mac/Linux: `install.sh`
  - Prompts for Kiosk Mode configuration
  - Automatically launches app after installation

### 🔧 Technical Changes

- **AI Story Management Moved to Story Manager**: 
  - Removed "Allow AI Generated Stories" toggle from ParentSettings
  - AI status is now marked and managed in the Story Manager web tool
  - All stories marked in the manifest are shown (no filtering)
  - Story Manager displays AI badge (🤖 AI) next to AI-generated stories
  
- Removed time-based sync period configuration
- Implemented on-demand manifest refresh
- Added `FolderNavigableRepository` interface for folder navigation
- Improved story filtering logic with age range support (removed AI filter)
- Refactored `ManageStoriesScreen` for better UX
- Enhanced GitHub workflow to include install scripts in releases

### 📦 Manifest Schema Update

The `manifest.json` now includes optional fields:
```json
{
  "appVersion": "1.2",
  "stories": [
    {
      "id": "story-id",
      "title": "Story Title",
      "audioFile": "audio.mp3",
      "imageFile": "image.png",
      "language": "fr",
      "ai": true,
      "publishedDate": "2026-10-09",
      "ageMin": 3,
      "ageMax": 8,
      "folder": "Aventures/Forêt"
    }
  ]
}
```

### 🐛 Bug Fixes

- Fixed story list visibility in Manage Stories screen
- Improved layout spacing for better usability
- Better handling of empty filtered results

---

## [1.1] - 2026-10-02

### ✨ New Features

- Initial web mode support
- Multi-language story support
- AI story filtering
- Age recommendations

---

## [1.0] - 2026-09-25

### ✨ New Features

- Initial release
- Device Owner Kiosk Mode
- Parental Controls with PIN protection
- Story playback with ExoPlayer
- Local folder story support
- Dark theme UI
