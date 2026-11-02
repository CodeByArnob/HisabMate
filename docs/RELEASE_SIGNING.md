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

The initial v1.0.6 assets were first published using a temporary CI-generated key. On 2 November 2026, the APK and AAB assets on the existing v1.0.6 GitHub Release were rebuilt, verified and replaced using the persistent `upload` keystore documented below. The current downloadable APK is signed with SHA-256 certificate fingerprint `D9:3D:A9:CF:4E:28:CD:90:4A:C7:95:4C:41:2B:C1:78:38:55:4A:B2:8D:7B:7C:F5:78:D0:C4:91:31:8D:43:A9`.

Any copy of v1.0.6 or earlier APKs downloaded before the asset replacement may still be installed with a temporary certificate. Android will not update such an installation in place with the newly signed APK. Export a JSON backup from the installed app, uninstall the old build, install the current GitHub Release APK, then restore the backup. Uninstalling removes local app data. After installing the current re-signed APK, future GitHub APK updates must continue using this same persistent keystore.

For Google Play distribution, distinguish the Play app-signing key from the upload key. The Play Console manages app signing when Play App Signing is enabled; an upload-key reset does not change the app-signing certificate delivered to users.

## CI artifact policy

The `android-ci.yml` workflow may use a temporary key solely to prove that release variants compile and their APK/AAB signatures are structurally valid. It does not upload or publish those release artifacts. Only the dedicated release workflow can publish downloadable release assets, and it requires the persistent keystore secrets above.


## Generated upload-key metadata

The newly generated local keystore uses:

- File name: `upload-keystore.jks` (kept outside the repository)
- Alias: `upload`
- Subject: `CN=Arnobrazz, O=CodeByArnob, C=BD`
- Key: RSA 2048, SHA256withRSA
- Validity: 10,000 days (2 November 2026 through 20 March 2054)
- SHA-256 certificate fingerprint: `D9:3D:A9:CF:4E:28:CD:90:4A:C7:95:4C:41:2B:C1:78:38:55:4A:B2:8D:7B:7C:F5:78:D0:C4:91:31:8D:43:A9`

The corresponding **public certificate only** is tracked at [`.github/signing/upload-certificate.pem`](../.github/signing/upload-certificate.pem). The private `.jks` keystore and both passwords must remain outside Git. Configure the locally generated keystore as the Actions secrets listed above.

This persistent signing identity is now used by the current v1.0.6 GitHub Release APK/AAB assets. It replaces the temporary certificate used by the first v1.0.6 upload. Keep the private keystore and both passwords securely backed up; losing them would break future in-place update continuity.
