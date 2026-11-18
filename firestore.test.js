const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const CHARLIE_UID = "charlie_uid_003";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
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

test("Unauthenticated users are rejected from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("publicProfiles").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("friendCodes").doc("VD-1234").get());
  await assertFails(unauthDb.collection("gameRooms").doc("ROOM01").get());
});

test("UserSettings, QuoteHistory, Habits, and VisualizerProfiles enforce owner isolation and schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate(10000);

  // UserSettings create & partial update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      darkMode: true,
      morningQuoteEnabled: true,
      morningHour: 8,
      morningMinute: 0,
      randomQuotesEnabled: true,
      activeThemeAnime: "FRIEREN",
      selectedVisualizerProfileId: "preset_frieren",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      darkMode: false,
      activeThemeAnime: "SAKURA",
      updatedAt: pastDate(1000),
    })
  );
  // Bob cannot read or write Alice's settings
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      darkMode: false,
      updatedAt: pastDate(1000),
    })
  );

  // QuoteHistoryEntry create, list, update favorite, reject extra id field
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quotes").doc("q_1").set({
      userId: ALICE_UID,
      quoteText: "Cada pequeno paso hoy ilumina tu manana.",
      animeKey: "FRIEREN",
      characterName: "Frieren",
      moodTag: "Serenidad",
      imageUrl: "https://cdn.myanimelist.net/images/anime/1015/138006.jpg",
      imageSource: "Jikan / MyAnimeList",
      favorite: false,
      sourceType: "MORNING",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quotes").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("quotes").doc("q_1").update({
      favorite: true,
      updatedAt: pastDate(1000),
    })
  );
  // Reject shadow field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("quotes").doc("q_bad").set({
      id: "q_bad",
      userId: ALICE_UID,
      quoteText: "Bad",
      animeKey: "FRIEREN",
      characterName: "Frieren",
      moodTag: "Calma",
      imageUrl: "",
      favorite: false,
      sourceType: "MANUAL",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("quotes").get()
  );

  // HabitItem create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("h_1").set({
      userId: ALICE_UID,
      title: "Meditar 10 minutos",
      animeTag: "APOTHECARY",
      completedDates: ["2026-11-17"],
      currentStreak: 1,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("habits").doc("h_1").update({
      completedDates: ["2026-11-16", "2026-11-17"],
      currentStreak: 2,
      updatedAt: pastDate(1000),
    })
  );

  // VisualizerProfile create & update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("visualizerProfiles").doc("vp_1").set({
      userId: ALICE_UID,
      name: "Flujo de Frieren",
      baseAnimeKey: "FRIEREN",
      particleStyle: "PETALS",
      waveformStyle: "ORBITAL",
      ambientSound: "NIGHT_RAIN",
      sensitivity: 1.2,
      primaryHex: "#7DD3FC",
      secondaryHex: "#F9A8D4",
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("PublicProfiles, FriendCodes, and FriendRequests enforce lookup safety and workflow integrity", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      friendCode: "VD-ALICE1",
      selectedAvatarId: "frieren",
      activeThemeAnime: "FRIEREN",
      activityLevel: 3,
      totalFocusMinutes: 45,
      climberBestMeters: 180,
      dailyChallengeScore: 120,
      dailyChallengeDate: "2026-11-17",
      quotesCount: 6,
      favoritesCount: 3,
      habitsCompletedTotal: 12,
      bestHabitStreak: 5,
      dailyChallengesCompleted: 2,
      duoClimberWins: 1,
      duoMusicWins: 1,
      friendsCount: 2,
      maxEmotionalLevel: 3,
      profilePhotoBase64: "AAAA",
      medals: ["first_quote"],
      unlockedAvatars: ["frieren", "maomao"],
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Owner can update gallery photo Base64 and counters; oversized photo (>45000 chars) is rejected
  await assertSucceeds(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).update({
      profilePhotoBase64: "B".repeat(1024),
      quotesCount: 9,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    aliceDb.collection("publicProfiles").doc(ALICE_UID).update({
      profilePhotoBase64: "X".repeat(46000),
      updatedAt: pastDate(500),
    })
  );
  // Bob cannot overwrite Alice's gallery photo or achievement counters
  await assertFails(
    bobDb.collection("publicProfiles").doc(ALICE_UID).update({
      profilePhotoBase64: "HACK",
      updatedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    bobDb.collection("publicProfiles").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      friendCode: "VD-BOB22",
      selectedAvatarId: "okarun",
      activeThemeAnime: "DANDADAN",
      activityLevel: 2,
      totalFocusMinutes: 25,
      climberBestMeters: 90,
      dailyChallengeScore: 80,
      dailyChallengeDate: "2026-11-17",
      medals: ["first_quote"],
      unlockedAvatars: ["okarun"],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // FriendCode lookup: get allowed, list forbidden
  await assertSucceeds(
    bobDb.collection("friendCodes").doc("VD-BOB22").set({
      userId: BOB_UID,
      displayName: "Bob",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("friendCodes").doc("VD-BOB22").get());
  await assertFails(aliceDb.collection("friendCodes").get());

  // Unfiltered publicProfiles list rejected; scoped allowed
  await assertFails(aliceDb.collection("publicProfiles").get());
  await assertSucceeds(
    aliceDb.collection("publicProfiles").where("userId", "==", ALICE_UID).get()
  );

  // FriendRequest workflow
  await assertSucceeds(
    aliceDb.collection("friendRequests").doc("req_1").set({
      fromUid: ALICE_UID,
      toUid: BOB_UID,
      fromDisplayName: "Alice",
      fromFriendCode: "VD-ALICE1",
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  // Charlie cannot read or accept Bob's request
  await assertFails(charlieDb.collection("friendRequests").doc("req_1").get());
  await assertFails(
    charlieDb.collection("friendRequests").doc("req_1").update({
      status: "accepted",
      updatedAt: pastDate(1000),
    })
  );
  // Bob queries incoming requests and accepts
  await assertSucceeds(
    bobDb.collection("friendRequests").where("toUid", "==", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("friendRequests").doc("req_1").update({
      status: "accepted",
      updatedAt: pastDate(1000),
    })
  );
  // Cannot mutate after terminal accepted state
  await assertFails(
    bobDb.collection("friendRequests").doc("req_1").update({
      status: "declined",
      updatedAt: pastDate(500),
    })
  );
});

test("GameRooms support real-time multiplayer join, progress, and terminal state lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(10000);

  await assertSucceeds(
    aliceDb.collection("gameRooms").doc("ROOM99").set({
      roomCode: "ROOM99",
      gameType: "CLIMBER",
      status: "waiting",
      hostUid: ALICE_UID,
      hostName: "Alice",
      hostAvatarId: "frieren",
      hostProgress: 0,
      hostReady: true,
      guestUid: "",
      guestName: "",
      guestAvatarId: "",
      guestProgress: 0,
      guestReady: false,
      targetGoal: 500,
      sharedRewardGranted: false,
      invitedUid: BOB_UID,
      participantUids: [ALICE_UID],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob joins waiting room
  await assertSucceeds(
    bobDb.collection("gameRooms").doc("ROOM99").update({
      guestUid: BOB_UID,
      guestName: "Bob",
      guestAvatarId: "sakura",
      guestReady: true,
      participantUids: [ALICE_UID, BOB_UID],
      status: "active",
      updatedAt: pastDate(2000),
    })
  );

  // Scoped queries succeed for participants, fail for outsider
  await assertSucceeds(
    aliceDb.collection("gameRooms").where("participantUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(charlieDb.collection("gameRooms").doc("ROOM99").get());
  await assertFails(charlieDb.collection("gameRooms").get());

  // Host & guest progress updates
  await assertSucceeds(
    aliceDb.collection("gameRooms").doc("ROOM99").update({
      hostProgress: 260,
      updatedAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    bobDb.collection("gameRooms").doc("ROOM99").update({
      guestProgress: 250,
      status: "finished",
      sharedRewardGranted: true,
      updatedAt: pastDate(1000),
    })
  );

  // Terminal state lock: finished room cannot be modified further
  await assertFails(
    aliceDb.collection("gameRooms").doc("ROOM99").update({
      hostProgress: 999,
      updatedAt: pastDate(500),
    })
  );
});

test("FriendChats support live messages, Good Vibe counter wrap-around, Emotional Level up, and participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = pastDate(10000);
  const chatId = [ALICE_UID, BOB_UID].sort().join("__");
  const participants = [ALICE_UID, BOB_UID].sort();

  // Unauthenticated or outsider cannot read/create
  await assertFails(charlieDb.collection("friendChats").doc(chatId).get());

  // Alice initializes friendChat
  await assertSucceeds(
    aliceDb.collection("friendChats").doc(chatId).set({
      participantUids: participants,
      goodVibePoints: 0,
      emotionalLevel: 1,
      totalMessages: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice sends first message and boosts Good Vibe counter to 80
  await assertSucceeds(
    aliceDb.collection("friendChats").doc(chatId).collection("messages").doc("m_1").set({
      senderUid: ALICE_UID,
      senderName: "Alice",
      text: "Vamos con todo hoy",
      vibeBoost: 25,
      participantUids: participants,
      createdAt: pastDate(4000),
      updatedAt: pastDate(4000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("friendChats").doc(chatId).update({
      goodVibePoints: 80,
      emotionalLevel: 1,
      totalMessages: 1,
      updatedAt: pastDate(4000),
    })
  );

  // Bob responds, fills counter past 100 -> wraps to 5 and levels up Amistad Emocional to 2
  await assertSucceeds(
    bobDb.collection("friendChats").doc(chatId).collection("messages").doc("m_2").set({
      senderUid: BOB_UID,
      senderName: "Bob",
      text: "Buena vibra musical!",
      vibeBoost: 25,
      participantUids: participants,
      createdAt: pastDate(2000),
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("friendChats").doc(chatId).update({
      goodVibePoints: 5,
      emotionalLevel: 2,
      totalMessages: 2,
      updatedAt: pastDate(2000),
    })
  );

  // Scoped message list query succeeds for participants, fails for outsider Charlie
  await assertSucceeds(
    aliceDb
      .collection("friendChats")
      .doc(chatId)
      .collection("messages")
      .where("participantUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("friendChats")
      .doc(chatId)
      .collection("messages")
      .get()
  );
  await assertFails(
    charlieDb
      .collection("friendChats")
      .doc(chatId)
      .collection("messages")
      .where("participantUids", "array-contains", ALICE_UID)
      .get()
  );
  await assertFails(
    charlieDb
      .collection("friendChats")
      .doc(chatId)
      .collection("messages")
      .doc("m_1")
      .get()
  );
  await assertFails(
    charlieDb.collection("friendChats").doc(chatId).collection("messages").doc("m_3").set({
      senderUid: CHARLIE_UID,
      senderName: "Charlie",
      text: "Intruder",
      vibeBoost: 25,
      participantUids: participants,
      createdAt: pastDate(1000),
      updatedAt: pastDate(1000),
    })
  );
});
