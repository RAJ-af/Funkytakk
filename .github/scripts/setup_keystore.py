import os
import subprocess
import re
import base64

artifacts_dir = os.path.abspath("keystore-artifacts")
os.makedirs(artifacts_dir, exist_ok=True)
home_android = os.path.expanduser("~/.android")
os.makedirs(home_android, exist_ok=True)

# ----------------------------------------------------
# 1. SETUP DEBUG KEYSTORE (Used to sign Debug APK)
# ----------------------------------------------------
debug_dst = os.path.abspath(os.path.join(home_android, "debug.keystore"))
debug_saved = os.path.abspath(os.path.join(artifacts_dir, "debug.keystore"))
debug_b64_env = os.environ.get("DEBUG_KEYSTORE_BASE64", "").strip()

if debug_b64_env:
    print("Restoring debug.keystore from DEBUG_KEYSTORE_BASE64 secret...")
    data = base64.b64decode(debug_b64_env)
    with open(debug_dst, "wb") as f:
        f.write(data)
    with open(debug_saved, "wb") as f:
        f.write(data)
    with open("debug.keystore", "wb") as f:
        f.write(data)
elif os.path.exists("debug.keystore"):
    print("Found debug.keystore in workspace...")
    with open("debug.keystore", "rb") as f:
        data = f.read()
    with open(debug_dst, "wb") as f:
        f.write(data)
    with open(debug_saved, "wb") as f:
        f.write(data)
elif os.path.exists("debug.keystore.base64"):
    print("Decoding debug.keystore from debug.keystore.base64...")
    with open("debug.keystore.base64", "r") as f:
        raw_b64 = f.read().strip()
    data = base64.b64decode(raw_b64)
    with open(debug_dst, "wb") as f:
        f.write(data)
    with open(debug_saved, "wb") as f:
        f.write(data)
    with open("debug.keystore", "wb") as f:
        f.write(data)
else:
    print("Generating standard debug.keystore...")
    gen_debug_cmd = [
        "keytool", "-genkeypair", "-v",
        "-keystore", debug_dst,
        "-alias", "androiddebugkey",
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", "android",
        "-keypass", "android",
        "-dname", "CN=Android Debug, O=Android, C=US"
    ]
    subprocess.check_call(gen_debug_cmd)
    with open(debug_dst, "rb") as f:
        data = f.read()
    with open(debug_saved, "wb") as f:
        f.write(data)
    with open("debug.keystore", "wb") as f:
        f.write(data)

# Extract Debug Fingerprints
cmd = ["keytool", "-list", "-v", "-keystore", debug_dst, "-storepass", "android", "-alias", "androiddebugkey"]
debug_out = subprocess.check_output(cmd).decode("utf-8")
d_sha1 = (re.search(r"SHA1:\s+([0-9A-Fa-f:]+)", debug_out) or ["", "Unknown"])[1].strip()
d_sha256 = (re.search(r"SHA256:\s+([0-9A-Fa-f:]+)", debug_out) or ["", "Unknown"])[1].strip()

# ----------------------------------------------------
# 2. SETUP RELEASE KEYSTORE (For Production Builds)
# ----------------------------------------------------
rel_path = os.path.abspath(os.path.join(artifacts_dir, "funkytalk-release.keystore"))
custom_b64 = os.environ.get("KEYSTORE_BASE64", "").strip()
rel_pwd = os.environ.get("KEYSTORE_PASSWORD", "FunkyTalk2026!")
rel_alias = os.environ.get("KEY_ALIAS", "upload")

if custom_b64:
    print("Restoring release keystore from KEYSTORE_BASE64 secret...")
    data = base64.b64decode(custom_b64)
    with open(rel_path, "wb") as f:
        f.write(data)
else:
    print("Generating dedicated release keystore for FunkyTalk...")
    gen_rel_cmd = [
        "keytool", "-genkeypair", "-v",
        "-keystore", rel_path,
        "-alias", rel_alias,
        "-keyalg", "RSA",
        "-keysize", "2048",
        "-validity", "10000",
        "-storepass", rel_pwd,
        "-keypass", rel_pwd,
        "-dname", "CN=FunkyTalk, OU=Mobile, O=FunkyTalk, L=SanFrancisco, S=CA, C=US"
    ]
    subprocess.check_call(gen_rel_cmd)

# Extract Release Fingerprints
cmd = ["keytool", "-list", "-v", "-keystore", rel_path, "-storepass", rel_pwd, "-alias", rel_alias]
rel_out = subprocess.check_output(cmd).decode("utf-8")
r_sha1 = (re.search(r"SHA1:\s+([0-9A-Fa-f:]+)", rel_out) or ["", "Unknown"])[1].strip()
r_sha256 = (re.search(r"SHA256:\s+([0-9A-Fa-f:]+)", rel_out) or ["", "Unknown"])[1].strip()

# ----------------------------------------------------
# 3. SAVE FINGERPRINTS TEXT FILE & BASE64 BACKUPS
# ----------------------------------------------------
txt_content = f"""================================================================================
FUNKYTALK ANDROID APP - SHA FINGERPRINTS & KEYSTORE DETAILS
================================================================================
Package Name: com.aistudio.funkytalk.kxmpzq

[1] DEBUG KEYSTORE (Used by app-debug.apk):
--------------------------------------------------------------------------------
Keystore File:      debug.keystore
Store Password:     android
Key Alias:          androiddebugkey
Key Password:       android

SHA-1:
{d_sha1}

SHA-256:
{d_sha256}

[2] RELEASE KEYSTORE (Used for Production Builds):
--------------------------------------------------------------------------------
Keystore File:      funkytalk-release.keystore
Store Password:     {rel_pwd}
Key Alias:          {rel_alias}
Key Password:       {rel_pwd}

SHA-1:
{r_sha1}

SHA-256:
{r_sha256}

================================================================================
HOW TO FIX GOOGLE SIGN-IN (ERROR 10) IN FIREBASE:
================================================================================
1. Open Google Firebase Console: https://console.firebase.google.com/
2. Go to Project Settings (Gear icon) -> General.
3. Under "Your apps", find "com.aistudio.funkytalk.kxmpzq".
4. Click "Add fingerprint".
5. Paste the SHA-1 for the APK you are installing:
   - For Debug APK (app-debug.apk):
     {d_sha1}
   - For Release APK:
     {r_sha1}
6. Click Save! Google Sign-in will immediately work without Error 10.
================================================================================
"""
with open(os.path.join(artifacts_dir, "SHA_FINGERPRINTS.txt"), "w") as f:
    f.write(txt_content)

# Also create base64 string files for easy storing in GitHub Secrets
with open(debug_saved, "rb") as f:
    debug_b64 = base64.b64encode(f.read()).decode("utf-8")
with open(os.path.join(artifacts_dir, "debug.keystore.base64.txt"), "w") as f:
    f.write(debug_b64)

with open(rel_path, "rb") as f:
    release_b64 = base64.b64encode(f.read()).decode("utf-8")
with open(os.path.join(artifacts_dir, "release.keystore.base64.txt"), "w") as f:
    f.write(release_b64)

# ----------------------------------------------------
# 4. PRINT CONSOLE BANNER
# ----------------------------------------------------
print("\n" + "="*80)
print("🔑 FUNKYTALK SHA FINGERPRINTS & KEYSTORE READY")
print("="*80)
print(f"DEBUG SHA-1   : {d_sha1}")
print(f"DEBUG SHA-256 : {d_sha256}")
print(f"RELEASE SHA-1 : {r_sha1}")
print(f"RELEASE SHA-256: {r_sha256}")
print("="*80 + "\n")

# ----------------------------------------------------
# 5. EXPORT GITHUB OUTPUTS & STEP SUMMARY
# ----------------------------------------------------
github_output = os.environ.get("GITHUB_OUTPUT")
if github_output:
    with open(github_output, "a") as f:
        f.write(f"debug_sha1={d_sha1}\n")
        f.write(f"debug_sha256={d_sha256}\n")
        f.write(f"release_sha1={r_sha1}\n")
        f.write(f"release_sha256={r_sha256}\n")
        f.write(f"release_password={rel_pwd}\n")
        f.write(f"release_alias={rel_alias}\n")

github_summary = os.environ.get("GITHUB_STEP_SUMMARY")
if github_summary:
    summary_md = f"""## 🔑 FunkyTalk Keystore & Firebase SHA Fingerprints

> **Copy the SHA-1 below and paste it into Firebase Console to fix Google Sign-in (Error 10)!**

### 📱 Debug Keystore (Used by `app-debug.apk`)
| Property | Value |
| :--- | :--- |
| **Package Name** | `com.aistudio.funkytalk.kxmpzq` |
| **Keystore File** | `debug.keystore` (attached below) |
| **Alias** | `androiddebugkey` |
| **Password** | `android` |
| **SHA-1** | `{d_sha1}` |
| **SHA-256** | `{d_sha256}` |

#### 📋 Quick Copy for Firebase Console (Debug APK):
```text
{d_sha1}
```

---

### 🔐 Release Keystore (For Production Builds)
| Property | Value |
| :--- | :--- |
| **Keystore File** | `funkytalk-release.keystore` (attached below) |
| **Alias** | `{rel_alias}` |
| **Password** | `{rel_pwd}` |
| **SHA-1** | `{r_sha1}` |
| **SHA-256** | `{r_sha256}` |

#### 📋 Quick Copy for Firebase Console (Release APK):
```text
{r_sha1}
```

---

### 🚀 How to Add to Firebase Console:
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Open your project -> **Project Settings** ⚙️ -> **General** tab
3. Under **Your apps**, select `com.aistudio.funkytalk.kxmpzq`
4. Click **Add fingerprint** and paste the **SHA-1**
5. Click **Save**! Google Sign-in will work immediately.
"""
    with open(github_summary, "a") as f:
        f.write(summary_md)
