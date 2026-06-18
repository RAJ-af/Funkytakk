# FunkyTalk ✨ Custom Brevo Verification Gateway

This backend package routes Firebase Authentication registration events through your custom authenticated domain on **Brevo** (`noreply@funkytalk.jo3.org`), skipping Firebase's stock templates entirely. It generates secure, signed verification links dynamically with the Google/Firebase Admin SDK.

---

## 🛠️ Environment Variables Configuration

Deploy with these environment configs set inside your cloud hosting platform or Firebase Functions Runtime Config:

```bash
# Brevo API Credentials (V3 Send SMTP Key)
BREVO_API_KEY=xkeysib-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

# Your pre-designed Brevo Template containing %DISPLAY_NAME% and %LINK%
BREVO_TEMPLATE_ID=1
```

---

## 🚀 Deployment Instructions

### Method 1: Firebase Clould Functions

1. Navigate to this directory in your terminal:
   ```bash
   cd backend_brevo
   ```
2. Make sure you are logged into the Firebase CLI on your computer:
   ```bash
   firebase login
   ```
3. Set your custom environment config variables:
   ```bash
   firebase functions:config:set brevo.api_key="YOUR_BREVO_API_KEY" brevo.template_id="YOUR_TEMPLATE_ID"
   ```
4. Deploy the Cloud Function:
   ```bash
   firebase deploy --only functions
   ```
5. Get the deployed Trigger URL, and bind it to your application's `.env` configuration file or AI Studio secrets:
   ```env
   FUNKYTALK_BACKEND_URL=https://<region>-<project-id>.cloudfunctions.net
   ```

---

## 🛡️ Security & Anti-Fraud Best Practices

1. **Client-IP Rate Limiting**: Ensure you bind automated IP checks (e.g., using Cloud Armor, express-rate-limit if deployed as a stateful container, or Cloudflare Rules) to throttle repeat registration attempts from single connections.
2. **Action link Validation**: The generated link is signed securely using your Firebase project's private key. Do NOT output or expose the generated verification URL directly inside client-facing error logs.
3. **CORS Security**: Update `Access-Control-Allow-Origin` in `index.js` to whitelist only your production mobile deep linking host or secure web domains (e.g. `https://funkytalk-f1aae.firebaseapp.com` instead of wildcard `*`) prior to launching to production.
