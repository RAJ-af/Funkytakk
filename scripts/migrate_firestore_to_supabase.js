/**
 * One-time Migration Script: Firestore 'rooms' -> Supabase 'voice_rooms'
 * 
 * Usage:
 *   node scripts/migrate_firestore_to_supabase.js
 * 
 * Maps camelCase Firestore fields to snake_case Supabase columns.
 * Matches Firestore hostId to Supabase user UUID (via Firestore users collection and Supabase profiles).
 * Skips and flags any room whose host cannot be matched to a Supabase user.
 */

const https = require('https');

const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'funkytalk-f1aae';
const FIREBASE_API_KEY = process.env.FIREBASE_API_KEY || 'AIzaSyAZi997wGgWE7WJ-N5NO9kOWL2agpijq-8';
const SUPABASE_URL = process.env.SUPABASE_URL || 'https://dulpqochkxtamqztauea.supabase.co';
const SUPABASE_KEY = process.env.SUPABASE_SERVICE_ROLE_KEY || process.env.SUPABASE_ANON_KEY || 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR1bHBxb2Noa3h0YW1xenRhdWVhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI4MDA3OTUsImV4cCI6MjA4ODM3Njc5NX0.yrpyrNkNnsXf1fXvVpxPlFu3IeQn6Oz0TK6BoCrOd-o';

function httpsRequest(url, options = {}, body = null) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const reqOptions = {
      hostname: parsed.hostname,
      port: parsed.port || 443,
      path: parsed.pathname + parsed.search,
      method: options.method || 'GET',
      headers: options.headers || {}
    };

    const req = https.request(reqOptions, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsedData = data ? JSON.parse(data) : null;
          resolve({ status: res.statusCode, headers: res.headers, data: parsedData, raw: data });
        } catch (e) {
          resolve({ status: res.statusCode, headers: res.headers, data: null, raw: data });
        }
      });
    });

    req.on('error', reject);
    if (body) {
      req.write(typeof body === 'string' ? body : JSON.stringify(body));
    }
    req.end();
  });
}

function parseFirestoreField(field) {
  if (!field) return null;
  if ('stringValue' in field) return field.stringValue;
  if ('integerValue' in field) return parseInt(field.integerValue, 10);
  if ('doubleValue' in field) return parseFloat(field.doubleValue);
  if ('booleanValue' in field) return field.booleanValue;
  if ('nullValue' in field) return null;
  if ('timestampValue' in field) return field.timestampValue;
  if ('arrayValue' in field) {
    const values = field.arrayValue.values || [];
    return values.map(parseFirestoreField);
  }
  if ('mapValue' in field) {
    const fields = field.mapValue.fields || {};
    const obj = {};
    for (const k of Object.keys(fields)) {
      obj[k] = parseFirestoreField(fields[k]);
    }
    return obj;
  }
  return null;
}

function parseFirestoreDoc(doc) {
  const fields = doc.fields || {};
  const obj = {};
  for (const k of Object.keys(fields)) {
    obj[k] = parseFirestoreField(fields[k]);
  }
  // doc.name is e.g. projects/.../databases/(default)/documents/rooms?pageSize=100/FT5272
  const parts = doc.name.split('/');
  obj._id = parts[parts.length - 1];
  return obj;
}

async function getFirebaseAuthToken() {
  const signUpRes = await httpsRequest(
    `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${FIREBASE_API_KEY}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    },
    {
      email: `migrator_${Date.now()}@funkytalk.com`,
      password: 'MigrationPassword123!',
      returnSecureToken: true
    }
  );

  if (signUpRes.status === 200 && signUpRes.data?.idToken) {
    return signUpRes.data.idToken;
  }
  throw new Error(`Failed to obtain Firebase Auth token: ${JSON.stringify(signUpRes.data)}`);
}

async function run() {
  console.log('================================================================');
  console.log('STARTING ONE-TIME FIRESTORE -> SUPABASE VOICE ROOMS MIGRATION');
  console.log('================================================================');

  // 1. Get Firebase Auth token for Firestore access
  console.log('\n[1/4] Authenticating with Firebase...');
  const fbToken = await getFirebaseAuthToken();
  console.log('  Firebase token obtained.');

  // 2. Fetch Supabase users and profiles
  console.log('\n[2/4] Fetching Supabase users & profiles...');
  const profilesRes = await httpsRequest(
    `${SUPABASE_URL}/rest/v1/profiles?select=*`,
    {
      method: 'GET',
      headers: {
        'apikey': SUPABASE_KEY,
        'Authorization': `Bearer ${SUPABASE_KEY}`
      }
    }
  );
  const supabaseProfiles = profilesRes.data || [];
  console.log(`  Found ${supabaseProfiles.length} Supabase profiles.`);

  // 3. Fetch Firestore users collection for UID -> Email mapping
  console.log('\n[3/4] Fetching Firestore users for identity resolution...');
  const fbUsersRes = await httpsRequest(
    `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/users`,
    {
      headers: { 'Authorization': `Bearer ${fbToken}` }
    }
  );
  const fbUsersDocs = fbUsersRes.data?.documents || [];
  const fbUserMap = new Map(); // uid -> email
  for (const doc of fbUsersDocs) {
    const parsed = parseFirestoreDoc(doc);
    if (parsed.uid && parsed.email) {
      fbUserMap.set(parsed.uid, parsed.email.toLowerCase().trim());
    }
  }
  console.log(`  Cached ${fbUserMap.size} Firestore user identities.`);

  // 4. Fetch Firestore rooms
  console.log('\n[4/4] Fetching Firestore rooms collection...');
  let fbRoomsDocs = [];
  let nextRoomsPageToken = null;
  do {
    const pageUrl = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/rooms` + 
      (nextRoomsPageToken ? `?pageToken=${encodeURIComponent(nextRoomsPageToken)}` : '');
    const fbRoomsRes = await httpsRequest(pageUrl, {
      headers: { 'Authorization': `Bearer ${fbToken}` }
    });
    const docs = fbRoomsRes.data?.documents || [];
    fbRoomsDocs.push(...docs);
    nextRoomsPageToken = fbRoomsRes.data?.nextPageToken || null;
  } while (nextRoomsPageToken);
  console.log(`  Found ${fbRoomsDocs.length} rooms in Firestore.`);

  const migratedRooms = [];
  const unmatchableRooms = [];

  for (const doc of fbRoomsDocs) {
    const r = parseFirestoreDoc(doc);
    const roomId = r.roomId || r._id;
    const rawHostId = r.hostId || '';

    // Identity Resolution: Look up matching Supabase user UUID
    let matchedSupabaseUserId = null;

    // Check if rawHostId matches auth_user_id directly
    const directMatch = supabaseProfiles.find(p => p.auth_user_id === rawHostId);
    if (directMatch) {
      matchedSupabaseUserId = directMatch.auth_user_id;
    }

    // Otherwise check if Firestore users has an email for this hostId
    if (!matchedSupabaseUserId && fbUserMap.has(rawHostId)) {
      const email = fbUserMap.get(rawHostId);
      const profileMatch = supabaseProfiles.find(p => p.email && p.email.toLowerCase().trim() === email);
      if (profileMatch) {
        matchedSupabaseUserId = profileMatch.auth_user_id;
      }
    }

    // Check if rawHostId itself is an email
    if (!matchedSupabaseUserId && rawHostId.includes('@')) {
      const profileMatch = supabaseProfiles.find(p => p.email && p.email.toLowerCase().trim() === rawHostId.toLowerCase().trim());
      if (profileMatch) {
        matchedSupabaseUserId = profileMatch.auth_user_id;
      }
    }

    if (!matchedSupabaseUserId) {
      unmatchableRooms.push({
        roomId,
        title: r.title || 'Untitled Room',
        rawHostId,
        reason: `Host identifier '${rawHostId}' could not be matched to any registered Supabase user.`
      });
      continue;
    }

    // Map fields 1:1 camelCase -> snake_case
    const createdTimestamp = r.createdAt ? new Date(r.createdAt).toISOString() : new Date().toISOString();
    const lastActivityTimestamp = r.lastActivityAt ? new Date(r.lastActivityAt).toISOString() : createdTimestamp;

    const payload = {
      title: r.title || 'Untitled Room',
      language: r.language || 'English',
      host_id: matchedSupabaseUserId,
      participants: r.totalParticipants || (r.speakingCount || 1) + (r.listeningCount || 0),
      created_at: createdTimestamp,
      country_code: r.languageCode || r.countryCode || 'us',
      tag: r.levelTag || r.tag || 'General',
      room_type: r.roomType || 'voice',
      description: r.description || '',
      banner_image_url: r.bannerImageUrl || '',
      card_background_type: r.cardBackgroundType || 'image',
      host_is_verified: !!r.hostIsVerified,
      host_is_vip: !!r.hostIsVip,
      is_live: r.isLive !== false,
      language_code: r.languageCode || 'us',
      language_flag: r.languageFlag || '',
      level_tag: r.levelTag || '',
      listening_count: r.listeningCount || 0,
      speaking_count: r.speakingCount || 1,
      preview_avatars: Array.isArray(r.previewAvatars) ? r.previewAvatars : [],
      status: r.status || 'active',
      status_tag: r.statusTag || 'LIVE',
      total_visits_24h: r.totalVisits24h || 1,
      last_1h_joins: r.last1HourJoins || 0,
      last_2h_growth_rate: r.last2HourGrowthRate || 0.0,
      last_activity_at: lastActivityTimestamp
    };

    // Insert into Supabase voice_rooms
    const insertRes = await httpsRequest(
      `${SUPABASE_URL}/rest/v1/voice_rooms`,
      {
        method: 'POST',
        headers: {
          'apikey': SUPABASE_KEY,
          'Authorization': `Bearer ${SUPABASE_KEY}`,
          'Content-Type': 'application/json',
          'Prefer': 'return=representation'
        }
      },
      payload
    );

    if (insertRes.status >= 200 && insertRes.status < 300) {
      migratedRooms.push({ roomId, title: payload.title, host_id: matchedSupabaseUserId });
    } else {
      console.error(`  Failed to insert room ${roomId}: ${insertRes.status} ${insertRes.raw}`);
    }
  }

  console.log('\n================================================================');
  console.log('MIGRATION SUMMARY');
  console.log('================================================================');
  console.log(`Successfully migrated into Supabase voice_rooms: ${migratedRooms.length}`);
  for (const m of migratedRooms) {
    console.log(`  - [MIGRATED] Room "${m.title}" (Original ID: ${m.roomId}) -> Supabase host: ${m.host_id}`);
  }

  console.log(`\nFlagged & Skipped (Host could not be matched): ${unmatchableRooms.length}`);
  for (const u of unmatchableRooms) {
    console.log(`  - [FLAGGED / UNMATCHED] Room "${u.title}" (ID: ${u.roomId})`);
    console.log(`    Original hostId: "${u.rawHostId}"`);
    console.log(`    Reason: ${u.reason}`);
  }
  console.log('================================================================\n');
}

run().catch(err => {
  console.error('Migration failed with error:', err);
  process.exit(1);
});
