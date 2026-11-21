import base64
import binascii
import os
import re
import subprocess
import sys
import tempfile


def fail(msg: str) -> None:
    print(f"::error::{msg}", file=sys.stderr)
    sys.exit(1)


def extract_sha_fingerprints(keystore_path: str, alias: str, password_env_var: str | None, plain_password: str | None) -> tuple[str, str]:
    cmd = ["keytool", "-list", "-v", "-keystore", keystore_path, "-alias", alias]
    run_env = os.environ.copy()
    if password_env_var:
        cmd.extend(["-storepass:env", password_env_var])
    elif plain_password is not None:
        run_env["KEYTOOL_TMP_PASS"] = plain_password
        cmd.extend(["-storepass:env", "KEYTOOL_TMP_PASS"])

    try:
        output = subprocess.check_output(cmd, env=run_env, stderr=subprocess.STDOUT).decode("utf-8", errors="replace")
    except subprocess.CalledProcessError as exc:
        fail(f"keytool failed while inspecting {os.path.basename(keystore_path)} (exit {exc.returncode}). Check alias and password secrets.")

    sha1_match = re.search(r"SHA1:\s*([0-9A-Fa-f:]+)", output)
    sha256_match = re.search(r"SHA256:\s*([0-9A-Fa-f:]+)", output)
    if not sha1_match or not sha256_match:
        fail(f"Could not extract SHA-1 and SHA-256 from {os.path.basename(keystore_path)}.")

    return sha1_match.group(1).strip().upper(), sha256_match.group(1).strip().upper()


def decode_secret_b64(raw_value: str, secret_name: str) -> bytes:
    cleaned = "".join(raw_value.split())
    try:
        data = base64.b64decode(cleaned, validate=True)
    except (binascii.Error, ValueError):
        fail(f"Secret {secret_name} does not contain valid base64 data.")
    if not data:
        fail(f"Secret {secret_name} decoded to empty bytes.")
    return data


def main() -> None:
    required_secrets = [
        "DEBUG_KEYSTORE_BASE64",
        "KEYSTORE_BASE64",
        "KEYSTORE_PASSWORD",
        "KEY_ALIAS",
        "KEY_PASSWORD",
    ]
    missing = [name for name in required_secrets if not os.environ.get(name, "").strip()]
    if missing:
        fail(f"Missing required GitHub secret(s): {', '.join(missing)}. Never leave keystore secrets empty.")

    debug_b64 = os.environ["DEBUG_KEYSTORE_BASE64"]
    release_b64 = os.environ["KEYSTORE_BASE64"]
    keystore_password = os.environ["KEYSTORE_PASSWORD"].strip()
    key_alias = os.environ["KEY_ALIAS"].strip()
    key_password = os.environ["KEY_PASSWORD"].strip()

    # Mask secret values immediately in GitHub Actions logs
    print(f"::add-mask::{keystore_password}")
    print(f"::add-mask::{key_password}")
    print(f"::add-mask::{key_alias}")

    # 1. Restore Debug Keystore -> ~/.android/debug.keystore
    home_android = os.path.expanduser("~/.android")
    os.makedirs(home_android, exist_ok=True)
    debug_dst = os.path.abspath(os.path.join(home_android, "debug.keystore"))
    debug_bytes = decode_secret_b64(debug_b64, "DEBUG_KEYSTORE_BASE64")
    with open(debug_dst, "wb") as f:
        f.write(debug_bytes)
    os.chmod(debug_dst, 0o600)

    # 2. Restore Release Keystore -> $RUNNER_TEMP/release.keystore (outside repo)
    runner_temp = os.environ.get("RUNNER_TEMP", "").strip() or tempfile.gettempdir()
    os.makedirs(runner_temp, exist_ok=True)
    release_dst = os.path.abspath(os.path.join(runner_temp, "release.keystore"))
    release_bytes = decode_secret_b64(release_b64, "KEYSTORE_BASE64")
    with open(release_dst, "wb") as f:
        f.write(release_bytes)
    os.chmod(release_dst, 0o600)

    # 3. Extract Fingerprints (never generate any keystore)
    d_sha1, d_sha256 = extract_sha_fingerprints(
        keystore_path=debug_dst,
        alias="androiddebugkey",
        password_env_var=None,
        plain_password="android",
    )
    r_sha1, r_sha256 = extract_sha_fingerprints(
        keystore_path=release_dst,
        alias=key_alias,
        password_env_var="KEYSTORE_PASSWORD",
        plain_password=None,
    )

    if d_sha256 == r_sha256:
        fail("Release keystore fingerprint matches debug keystore fingerprint. Check KEYSTORE_BASE64.")

    # 4. Write fingerprints-only SHA_FINGERPRINTS.txt outside repo
    fingerprints_file = os.path.abspath(os.path.join(runner_temp, "SHA_FINGERPRINTS.txt"))
    txt_content = f"""FUNKYTALK ANDROID APP - SHA FINGERPRINTS
Package Name: com.funkytalk.app

[DEBUG]
SHA-1:   {d_sha1}
SHA-256: {d_sha256}

[RELEASE]
SHA-1:   {r_sha1}
SHA-256: {r_sha256}
"""
    with open(fingerprints_file, "w", encoding="utf-8") as f:
        f.write(txt_content)

    # 5. Print fingerprints only to build log
    print("=" * 72)
    print("KEYSTORE FINGERPRINTS RESTORED FROM SECRETS")
    print(f"DEBUG   SHA-1  : {d_sha1}")
    print(f"DEBUG   SHA-256: {d_sha256}")
    print(f"RELEASE SHA-1  : {r_sha1}")
    print(f"RELEASE SHA-256: {r_sha256}")
    print("=" * 72)

    # 6. Export Gradle signing env vars via GITHUB_ENV
    github_env = os.environ.get("GITHUB_ENV")
    if github_env:
        with open(github_env, "a", encoding="utf-8") as f:
            f.write(f"RELEASE_STORE_FILE={release_dst}\n")
            f.write(f"RELEASE_STORE_PASSWORD={keystore_password}\n")
            f.write(f"RELEASE_KEY_ALIAS={key_alias}\n")
            f.write(f"RELEASE_KEY_PASSWORD={key_password}\n")

    # 7. Export fingerprints and file path via GITHUB_OUTPUT
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as f:
            f.write(f"debug_sha1={d_sha1}\n")
            f.write(f"debug_sha256={d_sha256}\n")
            f.write(f"release_sha1={r_sha1}\n")
            f.write(f"release_sha256={r_sha256}\n")
            f.write(f"sha_fingerprints_file={fingerprints_file}\n")

    # 8. Write fingerprints only to Job Summary
    github_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if github_summary:
        summary_md = f"""## Signing Certificate Fingerprints

| Build | SHA-1 | SHA-256 |
| :--- | :--- | :--- |
| **Debug** | `{d_sha1}` | `{d_sha256}` |
| **Release** | `{r_sha1}` | `{r_sha256}` |
"""
        with open(github_summary, "a", encoding="utf-8") as f:
            f.write(summary_md)


if __name__ == "__main__":
    main()
