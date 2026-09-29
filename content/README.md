# 📖 Aroli Story Manager

A simple, bilingual (English/French) web-based tool to manage stories for the Aroli Android app. Add, edit, delete, and organize stories with automatic file management.

## 🚀 Quick Start

### Option 1: Using the Batch File (Easiest)
1. Double-click **`start.bat`** in this folder
2. A server window will open and your browser will launch automatically
3. The Story Manager will open at `http://localhost:3000`

### Option 2: Manual Start
1. Open PowerShell or Command Prompt
2. Navigate to this folder:
   ```powershell
   cd C:\Projects\repo\Aroli\content
   ```
3. Start the server:
   ```powershell
   node server.js
   ```
4. Open your browser and go to: **`http://localhost:3000/story-manager-server.html`**

## ✨ Features

### Story Management
- ✅ **Add Stories** - Create new stories with auto-generated IDs
- ✅ **Edit Stories** - Modify story metadata and replace files
- ✅ **Delete Stories** - Remove stories and their associated files
- ✅ **View Current Files** - See which audio/image files are assigned

### File Management
- 📁 Automatically organizes audio files in `audio/` folder
- 🖼️ Automatically organizes image files in `images/` folder
- 🗑️ Deletes files when you remove a story
- 🔄 Replaces files when you edit a story

### Bilingual Interface
- 🇬🇧 English
- 🇫🇷 Français

Toggle between languages using the buttons in the top-right corner.

### Data Management
- 📋 All stories stored in `manifest.json`
- 💾 Automatic manifest updates with every action
- 📥 Download manifest.json anytime for backup

## 📂 Folder Structure

```
content/
├── start.bat                    (Click to start everything!)
├── README.md                    (This file)
├── server.js                    (Backend server - handles file operations)
├── story-manager-server.html    (Web interface)
├── manifest.json                (Your stories data)
├── audio/                       (Story audio files)
├── images/                      (Story cover images)
├── package.json                 (Dependencies)
├── node_modules/                (Dependencies - installed automatically)
└── package-lock.json            (Dependency version lock)
```

## 🎯 How to Use

### Adding a Story

1. **Title** - Enter the story name
2. **Language** - Select language (French/English/Spanish/German)
3. **Published Date** - Pick a date (defaults to today)
4. **Age Range** - Set minimum and maximum age (optional)
5. **Audio File** - Upload MP3, WAV, or M4A file
   - Story ID auto-generates from filename + timestamp
   - Example: Select `la-cigogne.mp3` → ID becomes `la-cigogne-1695292800`
6. **Cover Image** - Upload PNG, JPG, GIF, or WebP
7. **AI Generated** - Check if story was AI-generated
8. Click **"Add Story"** ✅

### Editing a Story

1. Click **"✏️ Edit"** button next to a story
2. Form pre-fills with story details
3. Optional: Replace audio or image file
   - Current filename shown below upload area
   - Leave empty to keep existing file
4. Modify any fields
5. Click **"💾 Update Story"** ✅

### Deleting a Story

1. Click **"🗑️ Delete"** button next to a story
2. Confirm deletion
3. Story, audio file, and image file all removed ✅

### Exporting Stories

1. Click **"📥 Download manifest.json"**
2. Your browser downloads the latest manifest with all stories

## 🔧 Server Details

**Backend Server:**
- Runs on `http://localhost:3000`
- Uses Express.js for handling requests
- Multer for file uploads
- All files managed in this folder

**API Endpoints:**
- `GET /api/stories` - Get all stories
- `POST /api/stories` - Add new story
- `PUT /api/stories/:id` - Update story
- `DELETE /api/stories/:id` - Delete story

## 📋 manifest.json Structure

Each story has this format:

```json
{
  "id": "story-name-1695292800",
  "title": "Story Title",
  "audioFile": "story-name-1695292800.mp3",
  "imageFile": "story-name-1695292800.jpg",
  "language": "fr",
  "ai": true,
  "publishedDate": "2026-09-29",
  "ageMin": 3,
  "ageMax": 8
}
```

## ⚙️ Requirements

- **Node.js** v18 or later (Download from https://nodejs.org)
- **npm** (comes with Node.js)
- Modern web browser (Chrome, Firefox, Safari, Edge)

## 🆘 Troubleshooting

### Server won't start
- ✅ Check Node.js is installed: `node --version`
- ✅ Verify you're in the content folder
- ✅ Try reinstalling dependencies: `npm install express multer`

### Port 3000 already in use
- ✅ Another app is using port 3000
- ✅ Close other Node.js servers or change the port in `server.js`

### Files not uploading
- ✅ Check file format (MP3/WAV for audio, PNG/JPG for images)
- ✅ Ensure file size is reasonable (<100MB)
- ✅ Check browser console for errors (F12 → Console)

### Browser won't open automatically
- ✅ Open manually: `http://localhost:3000/story-manager-server.html`
- ✅ Make sure server is running (check console window)

## 📝 Notes

- Story IDs are automatically generated from filenames + Unix timestamp
- All file operations are automatic - no manual file management needed
- manifest.json is auto-updated with every add/edit/delete
- Files stored in `audio/` and `images/` folders for organization

## 📞 Support

For issues or questions, check:
1. Browser console (F12 → Console tab)
2. Server console window for error messages
3. Ensure manifest.json is readable and valid JSON

---

**Made with ❤️ for Aroli**
