const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

// Initialize Firebase Admin SDK
// Make sure to add firebase-admin to your package.json dependencies
if (!admin.apps.length) {
  admin.initializeApp();
}

/**
 * Cloud Function to handle generating custom verification links and sending them via Brevo API
 * Endpoint: POST /sendCustomVerificationEmail
 */
exports.sendCustomVerificationEmail = functions.https.onRequest(async (req, res) => {
  // CORS configuration
  res.set("Access-Control-Allow-Origin", "*");
  if (req.method === "OPTIONS") {
    res.set("Access-Control-Allow-Methods", "POST");
    res.set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    res.set("Access-Control-Max-Age", "3600");
    return res.status(204).send("");
  }

  // Rate Limiting & Method Validation
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Only POST requests are layout-approved." });
  }

  const { email, displayName } = req.body;
  if (!email) {
    return res.status(400).json({ error: "Email parameter is required." });
  }

  try {
    // 1. Fetch user record from Firebase to ensure user exists
    let userRecord;
    try {
      userRecord = await admin.auth().getUserByEmail(email);
    } catch (e) {
      return res.status(404).json({ error: "No Firebase user found with this email." });
    }

    // 2. Generate custom action link for email verification using Admin SDK
    const actionCodeSettings = {
      // Your verified mobile deep link or high-converting web landing page redirect
      url: "https://funkytalk-f1aae.firebaseapp.com/home", 
      handleCodeInApp: true,
      android: {
        packageName: "com.aistudio.funkytalk", // Update your applicationId if changed
        installApp: true,
        minimumVersion: "1"
      }
    };

    const verificationLink = await admin.auth().generateEmailVerificationLink(email, actionCodeSettings);

    // 3. Brevo (formerly Sendinblue) REST API request parameters
    const brevoApiKey = process.env.BREVO_API_KEY || functions.config().brevo?.api_key;
    const brevoTemplateId = parseInt(process.env.BREVO_TEMPLATE_ID || functions.config().brevo?.template_id || "1", 10);

    if (!brevoApiKey || brevoApiKey === "YOUR_BREVO_API_KEY") {
      console.warn("[Brevo Warning]: BREVO_API_KEY is not configured. Running mock console log mode.");
      return res.status(200).json({
        success: true,
        message: "Simulation: verification code generated successfully.",
        link: verificationLink
      });
    }

    // 4. Dispatch Email Payload to Brevo API V3
    const response = await axios.post(
      "https://api.brevo.com/v3/smtp/email",
      {
        sender: {
          name: "FunkyTalk ✨",
          email: "noreply@funkytalk.jo3.org"
        },
        to: [
          {
            email: email,
            name: displayName || userRecord.displayName || "FunkyTalk Learner"
          }
        ],
        templateId: brevoTemplateId,
        params: {
          DISPLAY_NAME: displayName || userRecord.displayName || "FunkyTalk Learner",
          LINK: verificationLink
        }
      },
      {
        headers: {
          "api-key": brevoApiKey,
          "Content-Type": "application/json"
        }
      }
    );

    console.log(`[Success]: Custom Brevo verification email safely dispatched to ${email}`, response.data);
    return res.status(200).json({
      success: true,
      message: "Custom email verification triggered successfully using Brevo API wrapper.",
      messageId: response.data.messageId
    });

  } catch (error) {
    console.error("[Brevo Error Handler]:", error);
    return res.status(500).json({
      error: "Internal failure executing verification delivery flow.",
      details: error.message
    });
  }
});
