WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY implement, show every changed file in full, and finish with a list of what you could not verify.

# PROMPT 1 — Create the release keystore ONCE (manual workflow)

Problem: the current build script generates a NEW release keystore on every build, so the release SHA changes every time. We need ONE permanent release keystore. The repo is private. GitHub secrets that already exist: `DEBUG_KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`. The secrets `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEY_PASSWORD` do NOT exist yet.

## Task
Create ONLY a new file `.github/workflows/generate-release-keystore.yml`. Do not change any other workflow or script in this prompt.

- Trigger: `workflow_dispatch` only (manual). Runner: ubuntu-latest. JDK 21 (setup-java@v4, temurin).
- Safety check 1: pass `secrets.KEYSTORE_BASE64` into an env var. If it is NOT empty, stop with the message `A release keystore secret already exists. Delete the KEYSTORE_BASE64 secret first if you really want a new key (a new key breaks Google Sign-In / Drive backup until the new SHA is added).` This prevents overwriting a real key.
- Safety check 2: `secrets.KEYSTORE_PASSWORD` must be non-empty and at least 6 characters, else fail with a clear message (never print the value).
- Generate a PKCS12 keystore with keytool: alias `funkytalk`, RSA 2048, validity 10000 days, store password = key password = `KEYSTORE_PASSWORD`, DName `CN=FunkyTalk, OU=Mobile, O=FunkyTalk, L=New Delhi, ST=Delhi, C=IN`.
- Create exactly 3 files in a folder `out/`:
  1. `release.keystore`
  2. `release.keystore.base64.txt` (`base64 -w0`)
  3. `SHA_FINGERPRINTS.txt` with RELEASE SHA-1 and RELEASE SHA-256 (from `keytool -list -v`), the alias, and these exact instructions: `Add secrets: KEYSTORE_BASE64 = content of release.keystore.base64.txt, KEY_ALIAS = funkytalk, KEY_PASSWORD = same value as KEYSTORE_PASSWORD. Then add the SHA-1 and SHA-256 above to Firebase (Project settings > your Android app) and to the Android OAuth client in Google Cloud.`
- Upload `out/` with `actions/upload-artifact@v4`, name `release-keystore-files`, `retention-days: 1`, `if-no-files-found: error`.
- Write the two SHA values (only these) to the job summary (`$GITHUB_STEP_SUMMARY`).
- NEVER print the password, the base64 or the keystore in logs (use `set +x`, no `echo` of secrets, no `cat` of the base64). NEVER create a GitHub Release. NEVER commit anything to the repo.

## Finish with
A short checklist for the owner (in English): run the workflow > download the artifact > add the 3 secrets > add the SHAs to Firebase and Google Cloud > DELETE the artifact and the files from the phone/Downloads after saving a backup in two safe places.
List what you could not verify (real run on GitHub).
