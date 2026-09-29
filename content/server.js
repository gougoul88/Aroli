const express = require('express');
const multer = require('multer');
const path = require('path');
const fs = require('fs');
const app = express();

// Content directory is the directory where this server is running
const contentDir = __dirname;
const audioDir = path.join(contentDir, 'audio');
const imagesDir = path.join(contentDir, 'images');
const manifestPath = path.join(contentDir, 'manifest.json');

// Ensure directories exist
[audioDir, imagesDir].forEach(dir => {
    if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
    }
});

// Middleware
app.use(express.json());
app.use(express.static(contentDir));

// File upload configuration
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        const uploadDir = file.fieldname === 'audio' ? audioDir : imagesDir;
        cb(null, uploadDir);
    },
    filename: (req, file, cb) => {
        // Use the requested filename from the form
        const ext = path.extname(file.originalname);
        const name = req.body.filename || file.originalname;
        cb(null, name);
    }
});

const upload = multer({ storage });

// Helper: Load manifest
function loadManifest() {
    try {
        if (fs.existsSync(manifestPath)) {
            return JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
        }
    } catch (err) {
        console.error('Error loading manifest:', err);
    }
    return { appVersion: '1.0', stories: [] };
}

// Helper: Save manifest
function saveManifest(manifest) {
    try {
        fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2));
    } catch (err) {
        console.error('Error saving manifest:', err);
        throw err;
    }
}

// GET /api/stories - Get all stories
app.get('/api/stories', (req, res) => {
    try {
        const manifest = loadManifest();
        res.json(manifest);
    } catch (err) {
        res.status(500).json({ error: 'Failed to load stories' });
    }
});

// POST /api/stories - Add new story
app.post('/api/stories', upload.fields([
    { name: 'audio', maxCount: 1 },
    { name: 'image', maxCount: 1 }
]), (req, res) => {
    try {
        const manifest = loadManifest();
        const storyData = JSON.parse(req.body.story);

        // Set file paths
        if (req.files.audio && req.files.audio[0]) {
            storyData.audioFile = req.files.audio[0].filename;
        }
        if (req.files.image && req.files.image[0]) {
            storyData.imageFile = req.files.image[0].filename;
        }

        // Check if story already exists
        const existingIndex = manifest.stories.findIndex(s => s.id === storyData.id);
        if (existingIndex >= 0) {
            manifest.stories[existingIndex] = storyData;
        } else {
            manifest.stories.push(storyData);
        }

        // Update version if provided
        if (req.body.appVersion) {
            manifest.appVersion = req.body.appVersion;
        }

        saveManifest(manifest);
        res.json({ success: true, story: storyData });
    } catch (err) {
        console.error('Error adding story:', err);
        res.status(500).json({ error: 'Failed to add story' });
    }
});

// PUT /api/stories/:id - Update story
app.put('/api/stories/:id', upload.fields([
    { name: 'audio', maxCount: 1 },
    { name: 'image', maxCount: 1 }
]), (req, res) => {
    try {
        const manifest = loadManifest();
        const storyId = req.params.id;
        const storyData = JSON.parse(req.body.story);
        const oldStory = manifest.stories.find(s => s.id === storyId);

        if (!oldStory) {
            return res.status(404).json({ error: 'Story not found' });
        }

        // Handle audio file
        if (req.files.audio && req.files.audio[0]) {
            // Delete old audio if different
            if (oldStory.audioFile && oldStory.audioFile !== req.files.audio[0].filename) {
                const oldAudioPath = path.join(audioDir, oldStory.audioFile);
                if (fs.existsSync(oldAudioPath)) {
                    fs.unlinkSync(oldAudioPath);
                }
            }
            storyData.audioFile = req.files.audio[0].filename;
        } else {
            // Keep existing audio file
            storyData.audioFile = oldStory.audioFile;
        }

        // Handle image file
        if (req.files.image && req.files.image[0]) {
            // Delete old image if different
            if (oldStory.imageFile && oldStory.imageFile !== req.files.image[0].filename) {
                const oldImagePath = path.join(imagesDir, oldStory.imageFile);
                if (fs.existsSync(oldImagePath)) {
                    fs.unlinkSync(oldImagePath);
                }
            }
            storyData.imageFile = req.files.image[0].filename;
        } else {
            // Keep existing image file
            storyData.imageFile = oldStory.imageFile;
        }

        // Update story
        const storyIndex = manifest.stories.findIndex(s => s.id === storyId);
        manifest.stories[storyIndex] = storyData;

        // Update version if provided
        if (req.body.appVersion) {
            manifest.appVersion = req.body.appVersion;
        }

        saveManifest(manifest);
        res.json({ success: true, story: storyData });
    } catch (err) {
        console.error('Error updating story:', err);
        res.status(500).json({ error: 'Failed to update story' });
    }
});

// DELETE /api/stories/:id - Delete story
app.delete('/api/stories/:id', (req, res) => {
    try {
        const manifest = loadManifest();
        const storyId = req.params.id;
        const story = manifest.stories.find(s => s.id === storyId);

        if (!story) {
            return res.status(404).json({ error: 'Story not found' });
        }

        // Delete audio file
        if (story.audioFile) {
            const audioPath = path.join(audioDir, story.audioFile);
            if (fs.existsSync(audioPath)) {
                fs.unlinkSync(audioPath);
                console.log(`🗑️ Deleted audio: ${story.audioFile}`);
            }
        }

        // Delete image file
        if (story.imageFile) {
            const imagePath = path.join(imagesDir, story.imageFile);
            if (fs.existsSync(imagePath)) {
                fs.unlinkSync(imagePath);
                console.log(`🗑️ Deleted image: ${story.imageFile}`);
            }
        }

        // Remove from manifest
        manifest.stories = manifest.stories.filter(s => s.id !== storyId);
        saveManifest(manifest);

        res.json({ success: true });
    } catch (err) {
        console.error('Error deleting story:', err);
        res.status(500).json({ error: 'Failed to delete story' });
    }
});

// GET /api/file-info/:filename - Get file info
app.get('/api/file-info/:type/:filename', (req, res) => {
    try {
        const dir = req.params.type === 'audio' ? audioDir : imagesDir;
        const filePath = path.join(dir, req.params.filename);
        
        if (fs.existsSync(filePath)) {
            const stats = fs.statSync(filePath);
            res.json({
                exists: true,
                size: stats.size,
                name: req.params.filename
            });
        } else {
            res.json({ exists: false });
        }
    } catch (err) {
        res.status(500).json({ error: 'Failed to get file info' });
    }
});

const PORT = 3000;
app.listen(PORT, () => {
    console.log(`🚀 Aroli Story Manager running at http://localhost:${PORT}`);
    console.log(`📁 Content directory: ${contentDir}`);
    console.log(`🎵 Audio directory: ${audioDir}`);
    console.log(`🖼️  Images directory: ${imagesDir}`);
    console.log(`📋 Manifest file: ${manifestPath}`);
});
