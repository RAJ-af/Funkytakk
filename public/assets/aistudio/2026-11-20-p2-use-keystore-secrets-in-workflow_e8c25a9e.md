WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

# PROMPT 2 — Build workflow must use the keystore SECRETS (run only AFTER the 3 secrets exist)

Secrets that now exist: `DEBUG_KEYSTORE_BASE64`, `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`. Repo is private. Goal: debug SHA and release SHA never change between builds.

## Task — edit `.github/workflows/build-apk.yml` and `.github/scripts/setup_keystore.py`
1. **Never generate a keystore.** Delete the code that generates a release keystore, the `keystore-artifacts/` folder logic and everything that writes `debug.keystore.base64.txt` / `release.keystore.base64.txt`.
2. **Restore from secrets by name:**
   - Debug: `DEBUG_KEYSTORE_BASE64` > `~/.android/debug.keystore` (keep today's behaviour; it already works and the debug SHA must stay `CA:4A:70:53:...`).
   - Release: `KEYSTORE_BASE64` > `$RUNNER_TEMP/release.keystore` (outside the repo folder).
3. **Fail fast:** if any of the 5 secrets is empty, stop with a message that names the missing SECRET NAME (never a value).
4. **Signing config:** export `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD` (= KEYSTORE_PASSWORD), `RELEASE_KEY_ALIAS` (= KEY_ALIAS), `RELEASE_KEY_PASSWORD` (= KEY_PASSWORD) through `$GITHUB_ENV`. Check `app/build.gradle.kts`: `signingConfigs.release` must read exactly these env vars, and the release build type must use it. No hardcoded passwords, NO fallback to the debug signing config. Show the Gradle diff.
5. **Verify after build:** for every `*-release.apk` run `apksigner verify --print-certs` (build-tools) and compare the SHA-256 with the release keystore's SHA-256; FAIL the job if they differ or if the APK is signed by the debug key. Print DEBUG and RELEASE SHA-1/SHA-256 in the log and the job summary (fingerprints only).
6. **Artifacts and Release:**
   - Do NOT upload any `.keystore`, `.jks`, or `*base64.txt` file anywhere (artifact or Release).
   - GitHub Release `release-build-N` contains ONLY the release APKs + `SHA_FINGERPRINTS.txt`. No debug APK, no `mapping.txt` (Crashlytics mapping is injected by Gradle already).
   - The workflow artifact may keep debug + release APKs + `SHA_FINGERPRINTS.txt`.
7. Add to `.gitignore`: `*.keystore`, `*.jks`, `*base64.txt`, `keystore-artifacts/`.
8. Never `echo` a secret; use `set +x`.

## Finish with
What changed, and what you could not verify (a real run, apksigner on the real APKs). The first successful build must print the SAME debug SHA-1 as before and a release SHA that stays identical on the next build.
