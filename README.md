# হাওয়া ট্রেডার্স ম্যানেজার (Hawa Traders Manager)

Offline product price lookup for an agri shop. Kotlin + Jetpack Compose (Material 3), Room, DataStore, Coil.
Package `com.hawatraders.manager`, minSdk 24, targetSdk 35.

## Open & run
1. Android Studio (Ladybug 2024.2 or newer, JDK 17+): File > Open > select this folder, let Gradle sync.
2. Pick a device/emulator (API 24+) and press Run.

## Signed release APK
1. Build > Generate Signed App Bundle / APK > APK, create or choose a keystore, choose `release`.
   Or: copy `keystore.properties.example` to `keystore.properties`, fill it in, run `./gradlew assembleRelease`.
2. Output: `app/build/outputs/apk/release/`.

## Font
Noto Sans Bengali could not be bundled automatically. See the comment in `ui/theme/Type.kt` (3 TTFs + 1 line).

## Release via GitHub Actions
Workflows live in `.github/workflows/`:
- `ci.yml`: on every push/PR runs lint and builds a debug APK (downloadable from the run's Artifacts).
- `release.yml`: builds a **signed** release APK, verifies it with apksigner, attaches APK + SHA256 to a GitHub Release.

One-time setup:
1. Create a keystore (keep a private backup; losing it means you can never update the app):
   `keytool -genkeypair -v -keystore hawa-release.jks -alias hawa -keyalg RSA -keysize 2048 -validity 10000`
2. Encode it: `base64 -w0 hawa-release.jks` (macOS: `base64 -i hawa-release.jks`).
3. GitHub repo > Settings > Secrets and variables > Actions > New repository secret:
   `KEYSTORE_BASE64` (output above), `KEYSTORE_PASSWORD`, `KEY_ALIAS` (e.g. hawa), `KEY_PASSWORD`.
4. Publish: `git tag v1.0.0 && git push origin v1.0.0`, or Actions > Release APK > Run workflow.
   versionName comes from the tag, versionCode from the run number.

Tip: run `gradle wrapper --gradle-version 8.9` once locally and commit `gradlew` + `gradle/wrapper/gradle-wrapper.jar`.
