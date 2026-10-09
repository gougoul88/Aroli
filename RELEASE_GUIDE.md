# 📦 Aroli Release Guide

This guide explains how to create and share Aroli releases with users.

## 🚀 Quick Release Process (5 minutes)

### 1. Update Version

Edit `app/build.gradle.kts`:
```kotlin
versionCode = 3          // Always increment
versionName = "1.2"      // Use semantic versioning (major.minor.patch)
```

### 2. Commit Changes

```bash
git add app/build.gradle.kts CHANGELOG.md
git commit -m "Release v1.2: Add selective story management and age filtering"
git push origin main
```

### 3. Create Release Tag

```bash
git tag v1.2
git push origin v1.2
```

✅ **Done!** GitHub Actions will automatically:
- Build the APK
- Create a GitHub Release page
- Attach the APK file
- Generate release notes

### 4. Share with Users

Users can now download from:
```
https://github.com/yourusername/Aroli/releases
```

And install using the provided scripts:
- Windows: `install.bat`
- Mac/Linux: `./install.sh`

---

## 📋 Version Numbering Convention

Use **Semantic Versioning**: `MAJOR.MINOR.PATCH`

- **1.0.0** → Initial release
- **1.1.0** → New features (backward compatible)
- **1.1.1** → Bug fix
- **2.0.0** → Major changes (breaking changes)

Examples:
- `v1.0` - Initial release
- `v1.1` - Add French stories
- `v1.2` - Add AI story filter
- `v2.0` - Redesign UI

---

## 🔧 Manual Build (If CI/CD Fails)

If you need to manually build locally:

```bash
# Build release APK
./gradlew assembleRelease

# APK location:
app/build/outputs/apk/release/app-release.apk
```

Then manually upload to GitHub Releases.

---

## 🐛 Troubleshooting

### Build fails on GitHub Actions

Check the workflow:
1. Go to **Actions** tab on GitHub
2. Click the failed workflow
3. See the error details
4. Common issues:
   - Gradle cache issues: Try re-running
   - Java version mismatch: Check `.github/workflows/build-release.yml`

### APK Not Building Locally

```bash
# Clean and rebuild
./gradlew clean assembleRelease

# Check Java version
java -version

# Ensure JDK 17+ is installed
```

---

## 📚 Useful Commands

```bash
# List all version tags
git tag

# Delete a tag (if you made a mistake)
git tag -d v1.1
git push origin :refs/tags/v1.1

# List releases
git log --oneline --decorate

# View GitHub Actions logs
# -> Go to Actions tab on GitHub
```

---

## ✅ Pre-Release Checklist

Before creating a release:

- [ ] Update `versionCode` and `versionName`
- [ ] Test on actual Android device
- [ ] Update README with new features (optional)
- [ ] Commit all changes
- [ ] Create git tag
- [ ] Push tag to trigger CI/CD
- [ ] Verify GitHub Release was created
- [ ] Download and test the APK

---

## 📞 Distribution Tips for Users

When sharing with others:

1. **Link to Releases Page**: 
   ```
   https://github.com/yourusername/Aroli/releases
   ```

2. **Instructions to Share**:
   - Download latest `app-release.apk`
   - Download `install.bat` or `install.sh`
   - Place both files in same folder
   - Run install script
   - Done!

3. **Alternative (No Script)**:
   - Install [ADB Tools](https://developer.android.com/tools/releases/platform-tools)
   - Enable USB Debugging on Android
   - Run: `adb install app-release.apk`

---

## 🎯 Next Steps

1. ✅ Commit and push your code
2. ✅ Create a version tag (`git tag v1.0`)
3. ✅ GitHub Actions builds automatically
4. ✅ Share the release link with users
5. ✅ Users download and run `install.bat`/`install.sh`

That's it! 🎉
