# Production release signing

HisabMate release builds must use a persistent Android signing keystore. The release workflow intentionally fails closed when signing secrets are missing; it never publishes an APK signed with a temporary CI key.

## Required GitHub Actions secrets

Configure these under **Repository → Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `UPLOAD_KEYSTORE_BASE64` | Base64-encoded PKCS#12/JKS keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Password for the `upload` key |

The Android Gradle configuration currently expects the key alias `upload`. The CI workflow validates this alias before building.

### Encode a keystore on Windows PowerShell

Run locally in the directory containing your keystore. Do not commit the keystore or paste its contents into issues, pull requests, chat, or source files.

```powershell
$bytes = [System.IO.File]::ReadAllBytes(".\my-upload-key.jks")
[Convert]::ToBase64String($bytes) | Set-Clipboard
```

Paste the clipboard value into the `UPLOAD_KEYSTORE_BASE64` secret. Add the two passwords as separate Actions secrets. Keep at least two secure offline backups of the original keystore and passwords.

### Encode on Linux/macOS

```bash
base64 -w 0 my-upload-key.jks
```

## Release procedure

1. Confirm the original signing key is the correct key for the distribution channel and existing installed users.
2. Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Run the complete unit/Robolectric test and lint suite.
4. Commit and merge the verified change to `main`.
5. Create and push a matching version tag, for example `v1.0.7`.
6. The release workflow validates that the tag matches Gradle `versionName`, checks the keystore and alias, runs tests and release lint, builds APK and AAB, verifies signatures, generates SHA-256 checksums, and creates a GitHub Release.
7. Never replace an already published release tag. Publish a new version instead.

A release cannot be published until all signing secrets are configured. The workflow does not silently fall back to an ephemeral signing key.

## Important v1.0.6 compatibility notice

The published v1.0.6 artifacts were signed using a temporary CI-generated key that was not retained. A newly created keystore will not match that certificate. For sideloaded installations signed with the v1.0.6 temporary key, a future APK signed by a different key cannot be installed as an in-place update.

If the original production signing key used by earlier installs exists, recover and validate it before selecting the long-term signing identity. If it cannot be recovered, users may need to export an in-app JSON backup, uninstall the old app, install the new APK, and restore the backup. Uninstalling removes local app data.

For Google Play distribution, distinguish the Play app-signing key from the upload key. The Play Console manages app signing when Play App Signing is enabled; an upload-key reset does not change the app-signing certificate delivered to users.

## CI artifact policy

The `android-ci.yml` workflow may use a temporary key solely to prove that release variants compile and their APK/AAB signatures are structurally valid. It does not upload or publish those release artifacts. Only the dedicated release workflow can publish downloadable release assets, and it requires the persistent keystore secrets above.
