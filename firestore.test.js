const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_creator_1";
const BOB_UID = "bob_viewer_2";
const ADMIN_UID = "super_admin_0";
const ADMIN_EMAIL = "parmeshwarparmeshwar38324@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

function validUserPayload(uid, overrides = {}) {
  const now = pastDate(5000);
  return {
    userId: uid,
    displayName: "Aarav Sharma",
    handle: "@aarav_creates",
    email: "aarav@example.com",
    phone: "",
    bio: "Tech & Culture creator from Mumbai",
    photoUrl: "",
    countryCode: "IN",
    languageCode: "hi",
    role: "CREATOR",
    accountStatus: "ACTIVE",
    restrictedMode: false,
    minorMode: false,
    twoFactorEnabled: true,
    watchHistoryPaused: false,
    uploadRestricted: false,
    commentRestricted: false,
    monetizationEnabled: true,
    kycVerified: false,
    upiId: "aarav@upi",
    bankAccountMasked: "XXXX4321",
    estimatedEarningsInr: 0,
    confirmedEarningsInr: 0,
    availableBalanceInr: 0,
    pendingBalanceInr: 0,
    totalPaidOutInr: 0,
    fontScaleMultiplier: 1.0,
    highContrastMode: false,
    darkThemeMode: "SYSTEM",
    interests: ["Technology", "Music"],
    notifSubscribers: true,
    notifComments: true,
    notifUploads: true,
    notifEarnings: true,
    notifPayouts: true,
    notifSecurity: true,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validVideoPayload(creatorId, overrides = {}) {
  const now = pastDate(5000);
  return {
    creatorId,
    creatorName: "Aarav Sharma",
    creatorHandle: "@aarav_creates",
    title: "India Space Tech 2026",
    description: "Exploring orbital missions in 4K.",
    videoUrl: "https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4",
    thumbnailKey: "img_thumb_tech",
    thumbnailUrl: "",
    format: "LONG",
    visibility: "PUBLIC",
    category: "Technology",
    languageCode: "en",
    countryCode: "IN",
    hashtags: ["#BharatTech", "#Space"],
    durationSec: 596,
    maxResolution: "4K",
    musicTitle: "Original Score",
    musicLicenseId: "LIC-BT-001",
    chaptersSummary: "00:00 Intro | 02:15 Launch",
    subtitlesText: "Welcome to BharatTube Tech.",
    viewCount: 0,
    likeCount: 0,
    dislikeCount: 0,
    commentCount: 0,
    copyrightHash: "sha256_abc123",
    copyrightStatus: "CLEAR",
    monetized: true,
    adRevenueInr: 0,
    scheduledPublishAtMs: 0,
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validPlatformConfig(overrides = {}) {
  return {
    creatorSharePercent: 50,
    platformSharePercent: 50,
    indiaMinPayoutInr: 1000,
    adsEnabled: true,
    preRollAdsEnabled: true,
    midRollAdsEnabled: true,
    shortsAdsEnabled: true,
    sponsoredHomeAdsEnabled: true,
    adFrequency: 3,
    monetizationEnabled: true,
    autoPayoutEnabled: true,
    registrationEnabled: true,
    uploadsEnabled: true,
    longVideosEnabled: true,
    shortsEnabled: true,
    liveStreamingEnabled: true,
    commentsEnabled: true,
    likesEnabled: true,
    subscriptionsEnabled: true,
    followingEnabled: true,
    chatEnabled: true,
    searchEnabled: true,
    notificationsEnabled: true,
    downloadsEnabled: true,
    communityPostsEnabled: true,
    maintenanceMode: false,
    announcementText: "Welcome to BharatTube Global!",
    disabledCountries: [],
    disabledLanguages: [],
    adDisabledCountries: [],
    monetizationDisabledCountries: [],
    updatedBy: ADMIN_UID,
    updatedAt: pastDate(1000),
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("videos").where("visibility", "==", "PUBLIC").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("platform_config").doc("global").get());
  await assertFails(
    unauthDb.collection("videos").doc("vid_1").set(validVideoPayload(ALICE_UID))
  );
});

test("User profile PII isolation and anti-privilege-escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Alice cannot self-assign ADMIN role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, { role: "ADMIN" }))
  );

  // Alice creates valid CREATOR profile
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Bob cannot read or list Alice's private user PII
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Alice cannot escalate her own role or lift uploadRestricted
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: pastDate(100),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      uploadRestricted: true,
      updatedAt: pastDate(100),
    })
  );

  // Alice can update her own bio & language
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      bio: "Updated creator bio",
      languageCode: "mr",
      updatedAt: pastDate(100),
    })
  );
});

test("Videos: filtered PUBLIC query succeeds, unfiltered fails, private video isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("videos").doc("vid_pub_1").set(validVideoPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb
      .collection("videos")
      .doc("vid_priv_1")
      .set(validVideoPayload(ALICE_UID, { visibility: "PRIVATE" }))
  );

  // Filtered PUBLIC query succeeds for Bob
  await assertSucceeds(
    bobDb.collection("videos").where("visibility", "==", "PUBLIC").get()
  );

  // Unfiltered query rejected
  await assertFails(bobDb.collection("videos").get());

  // Bob cannot read Alice's PRIVATE video
  await assertFails(bobDb.collection("videos").doc("vid_priv_1").get());

  // Bob can increment viewCount/likeCount via engagement branch, but cannot edit title
  await assertSucceeds(
    bobDb.collection("videos").doc("vid_pub_1").update({
      viewCount: 1,
      likeCount: 1,
      updatedAt: pastDate(100),
    })
  );
  await assertFails(
    bobDb.collection("videos").doc("vid_pub_1").update({
      title: "Hijacked Title",
      updatedAt: pastDate(100),
    })
  );

  // Reject future timestamp (clock tampering)
  await assertFails(
    aliceDb.collection("videos").doc("vid_future").set(
      validVideoPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) })
    )
  );
});

test("Super Admin controls: email_verified required, 50:50 sum=100 enforced, audit logs immutable", async () => {
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Unverified email spoof rejected
  await assertFails(
    spoofedAdminDb.collection("platform_config").doc("global").set(validPlatformConfig())
  );

  // Regular user cannot update platform_config
  await assertFails(
    bobDb.collection("platform_config").doc("global").set(validPlatformConfig())
  );

  // Invalid revenue split (60 + 50 != 100) rejected even for Admin
  await assertFails(
    verifiedAdminDb
      .collection("platform_config")
      .doc("global")
      .set(validPlatformConfig({ creatorSharePercent: 60, platformSharePercent: 50 }))
  );

  // Valid 50:50 config succeeds for Admin
  await assertSucceeds(
    verifiedAdminDb.collection("platform_config").doc("global").set(validPlatformConfig())
  );

  // Signed-in user can read global config
  await assertSucceeds(bobDb.collection("platform_config").doc("global").get());

  // Admin audit log creation succeeds and update/delete are strictly blocked
  await assertSucceeds(
    verifiedAdminDb.collection("admin_audit_logs").doc("log_1").set({
      adminUid: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      action: "UPDATE_REVENUE_SPLIT",
      targetType: "platform_config",
      targetId: "global",
      details: "Set 50% Creator / 50% BharatTube",
      createdAt: pastDate(500),
    })
  );
  await assertFails(
    verifiedAdminDb.collection("admin_audit_logs").doc("log_1").update({ details: "Tampered" })
  );
  await assertFails(
    verifiedAdminDb.collection("admin_audit_logs").doc("log_1").delete()
  );
});

test("Revenue ledger & Payouts: strict creator isolation and valid 50:50 math", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("revenue_ledger").doc("rev_1").set({
      creatorId: ALICE_UID,
      videoId: "vid_pub_1",
      campaignId: "camp_1",
      format: "LONG",
      countryCode: "IN",
      grossRevenueInr: 1180,
      networkFeeInr: 180,
      netRevenueInr: 1000,
      creatorSharePercent: 50,
      platformSharePercent: 50,
      creatorEarnedInr: 500,
      platformEarnedInr: 500,
      status: "CONFIRMED",
      createdAt: pastDate(500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("revenue_ledger").where("creatorId", "==", ALICE_UID).get()
  );
  await assertFails(
    bobDb.collection("revenue_ledger").where("creatorId", "==", ALICE_UID).get()
  );

  await assertSucceeds(
    aliceDb.collection("payouts").doc("pay_1").set({
      creatorId: ALICE_UID,
      countryCode: "IN",
      currencyCode: "INR",
      amountInr: 1000,
      taxDeductedInr: 100,
      netPayoutInr: 900,
      method: "UPI",
      destinationMasked: "aa***@upi",
      transactionId: "TXN_BT_10001",
      idempotencyKey: "IDEM_ALICE_10001",
      status: "PROCESSING",
      failureReason: "",
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertFails(bobDb.collection("payouts").doc("pay_1").get());
});

test("User subcollections and Chats memberUids isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_1").set({
      title: "Watch Later",
      description: "Saved videos",
      privacy: "PRIVATE",
      videoIds: ["vid_pub_1"],
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("playlists").get()
  );

  // Chat thread between Alice & Bob
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_1").set({
      title: "Collab Chat",
      groupChat: false,
      memberUids: [ALICE_UID, BOB_UID],
      lastMessage: "Hey!",
      blockedByUids: [],
      createdAt: pastDate(500),
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    bobDb.collection("chats").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(bobDb.collection("chats").get());
});

test("Multi-channel creation, unique handle claims, and owner customization", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(1000);

  // Alice creates two channels under one account
  const chPayload = (name, handle) => ({
    ownerId: ALICE_UID,
    name,
    handle,
    description: "Official channel",
    category: "Technology",
    avatarUrl: "",
    avatarKey: "img_hero_bharattube",
    bannerUrl: "",
    bannerKey: "img_channel_banner",
    socialLinks: ["https://bharattube.example/@tech"],
    appearanceTheme: "SAFFRON",
    countryCode: "IN",
    languageCode: "en",
    channelStatus: "ACTIVE",
    subscriberCount: 1,
    followerCount: 1,
    totalViews: 0,
    verified: false,
    ownershipVerified: true,
    ownershipProofLabel: "Google OAuth Verified",
    createdAt: now,
    updatedAt: now,
  });

  await assertSucceeds(aliceDb.collection("channels").doc("ch_alice_1").set(chPayload("Alice Tech", "@alicetech")));
  await assertSucceeds(aliceDb.collection("channels").doc("ch_alice_2").set(chPayload("Alice Music", "@alicemusic")));

  // Handle reservation prevents duplicate claim by Bob
  await assertSucceeds(
    aliceDb.collection("channel_handles").doc("alicetech").set({
      ownerId: ALICE_UID,
      channelId: "ch_alice_1",
      handle: "@alicetech",
      createdAt: now,
    })
  );
  await assertFails(
    bobDb.collection("channel_handles").doc("alicetech").set({
      ownerId: BOB_UID,
      channelId: "ch_bob_1",
      handle: "@alicetech",
      createdAt: now,
    })
  );

  // Bob cannot customize Alice's channel
  await assertFails(
    bobDb.collection("channels").doc("ch_alice_1").update({
      name: "Hijacked",
      updatedAt: pastDate(100),
    })
  );
});

