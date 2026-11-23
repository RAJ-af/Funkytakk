# C0 — Chat audit (READ-ONLY, change NOTHING)

WORK IN ONE ANSWER: list the files you need (max 5 lines), then IMMEDIATELY write `docs/CHAT_AUDIT.md` (full file) and finish with a list of what you could not verify. Do not change any code in this prompt.

Attach `docs/CHAT_UPGRADE_PLAN.md`. We will NOT rewrite chat: we fix what is broken and add features on top. First I need the truth about the CURRENT chat.

Write `docs/CHAT_AUDIT.md` with these sections:
1. **File map:** every chat-related file (UI, ViewModel, repository, crypto, storage, workers, services) with one line each.
2. **Crypto API:** exact names/signatures of the functions that encrypt, decrypt, load keys, unlock the vault (1:1 and group). Which of them must stay unchanged.
3. **Send flow and receive flow** step by step (which thread/dispatcher, which table/RPC, which Realtime subscription, where the mailbox row is deleted).
4. **Media flow (image, voice):** how files are encrypted, uploaded (Edge Function `upload-to-appwrite`?), what goes into `storage_path`, how the receiver downloads, decrypts, caches and plays. List every point where it can fail silently (download not finished, decrypt exception, wrong key, missing permission, codec, expired file, Appwrite permission) and say which ones the code handles.
5. **Why voice/image can fail on the other phone:** from the code, list the most likely causes in order. If there is logging, say what to look for in logcat.
6. **Local storage:** what is stored where (Room? files? DataStore?), and exactly what Chat Backup reads/writes, so later changes stay compatible.
7. **Speed:** where the time goes between tap and bubble (encrypt, network, Realtime), with numbers if you can measure in a debug build (no content in logs).
8. **Keep / replace list:** what works and must stay untouched (crypto, block, report, image send, backup), what is broken, what is missing.
Rules: do not log or print message text, keys or tokens.
