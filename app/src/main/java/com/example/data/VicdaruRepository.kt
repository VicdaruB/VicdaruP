package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AnimeSeries
import com.example.model.CuratedQuotesCatalog
import com.example.model.FriendChat
import com.example.model.FriendChatMessage
import com.example.model.FriendLink
import com.example.model.FriendRequest
import com.example.model.GameRoom
import com.example.model.HabitItem
import com.example.model.MedalsAndAvatarsCatalog
import com.example.model.PublicProfile
import com.example.model.QuoteHistoryEntry
import com.example.model.UserSettings
import com.example.model.VisualizerProfile
import com.example.network.AnimeImageFetcher
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class VicdaruRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Debes iniciar sesión con Google antes de acceder a Vicdaru.")
    }

    private fun todayIso(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun buildFriendCode(uid: String): String {
        val clean = uid.replace(Regex("[^A-Za-z0-9]"), "").uppercase(Locale.US)
        val suffix = (clean + "VICDARU").take(6)
        return "VD-$suffix"
    }

    private fun <T> Flow<T>.handleFirestoreErrors(op: OperationType, path: String, uid: String): Flow<T> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val exceptionToLog = firestoreError ?: (error as? Exception)
            if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(exceptionToLog, op, path)
            }
            throw error
        }

    // --- REALTIME OBSERVATION FLOWS ---

    fun observeUserSettings(uid: String): Flow<UserSettings?> = flow {
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap -> snap.toObject(UserSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observePublicProfile(uid: String): Flow<PublicProfile?> = flow {
        val path = "publicProfiles/$uid"
        emitAll(
            db.collection("publicProfiles").document(uid)
                .snapshots()
                .map { snap -> snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeQuotes(uid: String): Flow<List<QuoteHistoryEntry>> = flow {
        val path = "users/$uid/quotes"
        emitAll(
            db.collection("users").document(uid).collection("quotes")
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuoteHistoryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: Long.MAX_VALUE }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeHabits(uid: String): Flow<List<HabitItem>> = flow {
        val path = "users/$uid/habits"
        emitAll(
            db.collection("users").document(uid).collection("habits")
                .snapshots()
                .map { snap ->
                    snap.toObjects(HabitItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeVisualizerProfiles(uid: String): Flow<List<VisualizerProfile>> = flow {
        val path = "users/$uid/visualizerProfiles"
        emitAll(
            db.collection("users").document(uid).collection("visualizerProfiles")
                .snapshots()
                .map { snap ->
                    snap.toObjects(VisualizerProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.name }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeFriends(uid: String): Flow<List<FriendLink>> = flow {
        val path = "users/$uid/friends"
        emitAll(
            db.collection("users").document(uid).collection("friends")
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendLink::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.displayName }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeIncomingFriendRequests(uid: String): Flow<List<FriendRequest>> = flow {
        val path = "friendRequests"
        emitAll(
            db.collection("friendRequests")
                .whereEqualTo("toUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "pending" }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeOutgoingAcceptedRequests(uid: String): Flow<List<FriendRequest>> = flow {
        val path = "friendRequests"
        emitAll(
            db.collection("friendRequests")
                .whereEqualTo("fromUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "accepted" }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeInvitedRooms(uid: String): Flow<List<GameRoom>> = flow {
        val path = "gameRooms"
        emitAll(
            db.collection("gameRooms")
                .whereEqualTo("invitedUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.status == "waiting" }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeGameRoom(roomId: String, uid: String): Flow<GameRoom?> = flow {
        val path = "gameRooms/$roomId"
        emitAll(
            db.collection("gameRooms").document(roomId)
                .snapshots()
                .map { snap -> snap.toObject(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .handleFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun friendChatId(uid1: String, uid2: String): String =
        listOf(uid1, uid2).sorted().joinToString("__")

    fun observeFriendChat(uid: String, friendUid: String): Flow<FriendChat?> = flow {
        val chatId = friendChatId(uid, friendUid)
        val path = "friendChats/$chatId"
        emitAll(
            db.collection("friendChats").document(chatId)
                .snapshots()
                .map { snap ->
                    snap.toObject(FriendChat::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .handleFirestoreErrors(OperationType.GET, path, uid)
        )
    }

    fun observeFriendChatMessages(uid: String, friendUid: String): Flow<List<FriendChatMessage>> = flow {
        val chatId = friendChatId(uid, friendUid)
        val path = "friendChats/$chatId/messages"
        emitAll(
            db.collection("friendChats").document(chatId).collection("messages")
                .whereArrayContains("participantUids", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(FriendChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt?.toDate()?.time ?: Long.MAX_VALUE }
                }
                .handleFirestoreErrors(OperationType.LIST, path, uid)
        )
    }

    // --- BOOTSTRAP & SETTINGS ---

    suspend fun ensureUserBootstrapped(displayName: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val safeName = displayName.trim().ifBlank { "Viajero Estelar" }.take(60)
        val friendCode = buildFriendCode(uid)

        return try {
            val userRef = db.collection("users").document(uid)
            val userSnap = userRef.get().await()
            if (!userSnap.exists()) {
                userRef.set(
                    mapOf(
                        "userId" to uid,
                        "darkMode" to true,
                        "morningQuoteEnabled" to true,
                        "morningHour" to 8L,
                        "morningMinute" to 0L,
                        "randomQuotesEnabled" to true,
                        "activeThemeAnime" to AnimeSeries.SAKURA.key,
                        "selectedVisualizerProfileId" to "preset_sakura",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val pubRef = db.collection("publicProfiles").document(uid)
            val pubSnap = pubRef.get().await()
            if (!pubSnap.exists()) {
                pubRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "friendCode" to friendCode,
                        "selectedAvatarId" to "sakura",
                        "profilePhotoBase64" to "",
                        "activeThemeAnime" to AnimeSeries.SAKURA.key,
                        "activityLevel" to 1L,
                        "totalFocusMinutes" to 0L,
                        "climberBestMeters" to 0L,
                        "dailyChallengeScore" to 0L,
                        "dailyChallengeDate" to todayIso(),
                        "quotesCount" to 0L,
                        "favoritesCount" to 0L,
                        "habitsCompletedTotal" to 0L,
                        "bestHabitStreak" to 0L,
                        "dailyChallengesCompleted" to 0L,
                        "duoClimberWins" to 0L,
                        "duoMusicWins" to 0L,
                        "friendsCount" to 0L,
                        "maxEmotionalLevel" to 1L,
                        "medals" to listOf("first_quote"),
                        "unlockedAvatars" to listOf("sakura"),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val codeRef = db.collection("friendCodes").document(friendCode)
            if (!codeRef.get().await().exists()) {
                codeRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            // Starter quote if history is empty
            val quotesSnap = db.collection("users").document(uid).collection("quotes").limit(1).get().await()
            if (quotesSnap.isEmpty) {
                val starter = CuratedQuotesCatalog.morningQuote()
                val fetched = AnimeImageFetcher.fetchFreshImage(
                    anime = starter.anime,
                    characterHint = starter.characterName
                )
                saveQuoteEntry(
                    quoteText = starter.text,
                    anime = starter.anime,
                    characterName = starter.characterName,
                    moodTag = starter.moodTag,
                    imageUrl = fetched.imageUrl,
                    imageSource = fetched.sourceLabel,
                    sourceType = "MORNING"
                )
            }

            // Starter habits if none exist
            val habitsSnap = db.collection("users").document(uid).collection("habits").limit(1).get().await()
            if (habitsSnap.isEmpty) {
                createHabit("Respiración consciente (5 min)", AnimeSeries.FRIEREN.key)
                createHabit("Beber agua y estirar el cuerpo", AnimeSeries.APOTHECARY.key)
                createHabit("Bloque de enfoque sin distracciones", AnimeSeries.LITTLE_WITCH.key)
            }

            // Starter character visualizer presets (or upgrade legacy presets so each anime has its unique visualizer)
            val legacyStyles = setOf("ORBITAL", "BARS", "RIBBON", "RIPPLE", "")
            val vpCollection = db.collection("users").document(uid).collection("visualizerProfiles")
            val vpSnap = vpCollection.get().await()
            if (vpSnap.isEmpty) {
                AnimeSeries.entries.forEach { series ->
                    val pid = "preset_${series.key.lowercase(Locale.US)}"
                    vpCollection.document(pid).set(
                        mapOf(
                            "userId" to uid,
                            "name" to series.presetName,
                            "baseAnimeKey" to series.key,
                            "particleStyle" to series.defaultParticleStyle,
                            "waveformStyle" to series.defaultWaveformStyle,
                            "ambientSound" to series.defaultAmbientSound,
                            "sensitivity" to 1.15,
                            "primaryHex" to series.primaryHex,
                            "secondaryHex" to series.secondaryHex,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                val existingById = vpSnap.documents.associateBy { it.id }
                AnimeSeries.entries.forEach { series ->
                    val pid = "preset_${series.key.lowercase(Locale.US)}"
                    val doc = existingById[pid]
                    if (doc == null) {
                        vpCollection.document(pid).set(
                            mapOf(
                                "userId" to uid,
                                "name" to series.presetName,
                                "baseAnimeKey" to series.key,
                                "particleStyle" to series.defaultParticleStyle,
                                "waveformStyle" to series.defaultWaveformStyle,
                                "ambientSound" to series.defaultAmbientSound,
                                "sensitivity" to 1.15,
                                "primaryHex" to series.primaryHex,
                                "secondaryHex" to series.secondaryHex,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    } else {
                        val currentWave = doc.getString("waveformStyle").orEmpty()
                        if (currentWave in legacyStyles || currentWave != series.defaultWaveformStyle) {
                            vpCollection.document(pid).update(
                                mapOf(
                                    "name" to series.presetName,
                                    "waveformStyle" to series.defaultWaveformStyle,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                    }
                }
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateUserSettings(
        darkMode: Boolean? = null,
        morningQuoteEnabled: Boolean? = null,
        morningHour: Int? = null,
        morningMinute: Int? = null,
        randomQuotesEnabled: Boolean? = null,
        activeThemeAnime: String? = null,
        selectedVisualizerProfileId: String? = null,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val path = "users/$uid"
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (darkMode != null) updates["darkMode"] = darkMode
            if (morningQuoteEnabled != null) updates["morningQuoteEnabled"] = morningQuoteEnabled
            if (morningHour != null) updates["morningHour"] = morningHour.coerceIn(0, 23).toLong()
            if (morningMinute != null) updates["morningMinute"] = morningMinute.coerceIn(0, 59).toLong()
            if (randomQuotesEnabled != null) updates["randomQuotesEnabled"] = randomQuotesEnabled
            if (activeThemeAnime != null) updates["activeThemeAnime"] = activeThemeAnime.take(40)
            if (selectedVisualizerProfileId != null) {
                updates["selectedVisualizerProfileId"] = selectedVisualizerProfileId.take(128)
            }
            db.collection("users").document(uid).update(updates).await()
            if (activeThemeAnime != null) {
                db.collection("publicProfiles").document(uid).update(
                    mapOf(
                        "activeThemeAnime" to activeThemeAnime.take(40),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- QUOTES ---

    suspend fun saveQuoteEntry(
        quoteText: String,
        anime: AnimeSeries,
        characterName: String,
        moodTag: String,
        imageUrl: String,
        imageSource: String = "Multi-fuente Anime",
        sourceType: String,
    ): Result<QuoteHistoryEntry> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val quoteId = "q_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val path = "users/$uid/quotes/$quoteId"
        return try {
            val safeText = quoteText.trim().take(500)
            val safeChar = characterName.trim().take(80)
            val safeMood = moodTag.trim().take(40)
            val safeImg = imageUrl.trim().take(600)
            val safeImgSource = imageSource.trim().ifBlank { "Multi-fuente Anime" }.take(80)
            val safeSource = if (sourceType in listOf("MORNING", "RANDOM", "MANUAL")) sourceType else "MANUAL"
            db.collection("users").document(uid).collection("quotes").document(quoteId).set(
                mapOf(
                    "userId" to uid,
                    "quoteText" to safeText,
                    "animeKey" to anime.key,
                    "characterName" to safeChar,
                    "moodTag" to safeMood,
                    "imageUrl" to safeImg,
                    "imageSource" to safeImgSource,
                    "favorite" to false,
                    "sourceType" to safeSource,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            // Update theme anime to match the latest delivered quote & increment quote stats
            updateUserSettings(activeThemeAnime = anime.key)
            val pubSnap = db.collection("publicProfiles").document(uid).get().await()
            val prevQuotes = pubSnap.getLong("quotesCount") ?: 0L
            val nextQuotes = (prevQuotes + 1L).coerceAtMost(1000000L)
            val medalsToAdd = buildList {
                add("first_quote")
                if (nextQuotes >= 4L) add("quotes_collector")
            }
            val currentMedals = (pubSnap.get("medals") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val mergedMedals = (currentMedals + medalsToAdd).distinct().take(60)
            db.collection("publicProfiles").document(uid).update(
                mapOf(
                    "quotesCount" to nextQuotes,
                    "medals" to mergedMedals,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(
                QuoteHistoryEntry(
                    id = quoteId,
                    userId = uid,
                    quoteText = safeText,
                    animeKey = anime.key,
                    characterName = safeChar,
                    moodTag = safeMood,
                    imageUrl = safeImg,
                    imageSource = safeImgSource,
                    favorite = false,
                    sourceType = safeSource
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleQuoteFavorite(quoteId: String, favorite: Boolean, totalFavoritesAfter: Int): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val path = "users/$uid/quotes/$quoteId"
        return try {
            require(quoteId.isNotBlank())
            db.collection("users").document(uid).collection("quotes").document(quoteId).update(
                mapOf(
                    "favorite" to favorite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val safeFavs = totalFavoritesAfter.coerceIn(0, 1000000).toLong()
            db.collection("publicProfiles").document(uid).update(
                mapOf(
                    "favoritesCount" to safeFavs,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (favorite && safeFavs >= 3L) {
                unlockMedals(uid, listOf("favorite_keeper"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- HABITS ---

    suspend fun createHabit(title: String, animeTag: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.isEmpty()) return Result.failure(IllegalArgumentException("El título no puede estar vacío"))
        val habitId = "h_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}"
        val path = "users/$uid/habits/$habitId"
        return try {
            db.collection("users").document(uid).collection("habits").document(habitId).set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "animeTag" to animeTag.take(40),
                    "completedDates" to emptyList<String>(),
                    "currentStreak" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleHabitDate(habit: HabitItem, isoDate: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val path = "users/$uid/habits/${habit.id}"
        return try {
            val updatedDates = habit.completedDates.toMutableList()
            if (updatedDates.contains(isoDate)) {
                updatedDates.remove(isoDate)
            } else {
                updatedDates.add(isoDate)
            }
            val bounded = updatedDates.distinct().sorted().takeLast(400)
            val streak = calculateStreak(bounded)
            db.collection("users").document(uid).collection("habits").document(habit.id).update(
                mapOf(
                    "completedDates" to bounded,
                    "currentStreak" to streak.toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val allHabitsSnap = db.collection("users").document(uid).collection("habits").get().await()
            val allHabits = allHabitsSnap.toObjects(HabitItem::class.java)
            val totalHabitChecks = allHabits.sumOf { it.completedDates.size }.coerceIn(0, 1000000).toLong()
            val maxStreak = (allHabits.maxOfOrNull { it.currentStreak } ?: 0L).coerceIn(0L, 100000L)
            val pubRef = db.collection("publicProfiles").document(uid)
            val pubSnap = pubRef.get().await()
            val prevBestStreak = pubSnap.getLong("bestHabitStreak") ?: 0L
            pubRef.update(
                mapOf(
                    "habitsCompletedTotal" to totalHabitChecks,
                    "bestHabitStreak" to maxOf(prevBestStreak, maxStreak),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val earned = buildList {
                if (totalHabitChecks >= 5L) add("habit_spark")
                if (maxOf(prevBestStreak, maxStreak) >= 3L) add("habit_streak")
            }
            if (earned.isNotEmpty()) {
                unlockMedals(uid, earned)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteHabit(habitId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val path = "users/$uid/habits/$habitId"
        return try {
            db.collection("users").document(uid).collection("habits").document(habitId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    private fun calculateStreak(sortedDates: List<String>): Int {
        if (sortedDates.isEmpty()) return 0
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dateSet = sortedDates.toSet()
        var streak = 0
        val cal = java.util.Calendar.getInstance()
        if (!dateSet.contains(sdf.format(cal.time))) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        while (dateSet.contains(sdf.format(cal.time))) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }

    // --- VISUALIZER PROFILES ---

    suspend fun saveVisualizerProfile(profile: VisualizerProfile): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val profileId = profile.id.ifBlank {
            "custom_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        }
        val path = "users/$uid/visualizerProfiles/$profileId"
        return try {
            val cleanName = profile.name.trim().ifBlank { "Perfil Personal" }.take(50)
            val cleanSens = profile.sensitivity.coerceIn(0.2, 2.5)
            val docRef = db.collection("users").document(uid).collection("visualizerProfiles").document(profileId)
            if (docRef.get().await().exists()) {
                docRef.update(
                    mapOf(
                        "name" to cleanName,
                        "baseAnimeKey" to profile.baseAnimeKey.take(40),
                        "particleStyle" to profile.particleStyle.take(30),
                        "waveformStyle" to profile.waveformStyle.take(30),
                        "ambientSound" to profile.ambientSound.take(30),
                        "sensitivity" to cleanSens,
                        "primaryHex" to profile.primaryHex.take(16),
                        "secondaryHex" to profile.secondaryHex.take(16),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to cleanName,
                        "baseAnimeKey" to profile.baseAnimeKey.take(40),
                        "particleStyle" to profile.particleStyle.take(30),
                        "waveformStyle" to profile.waveformStyle.take(30),
                        "ambientSound" to profile.ambientSound.take(30),
                        "sensitivity" to cleanSens,
                        "primaryHex" to profile.primaryHex.take(16),
                        "secondaryHex" to profile.secondaryHex.take(16),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(profileId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- PROFILE, MEDALS, AVATARS, FOCUS & CLIMBER PROGRESS ---

    suspend fun selectAvatar(avatarId: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        return try {
            db.collection("publicProfiles").document(uid).update(
                mapOf(
                    "selectedAvatarId" to avatarId.take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "publicProfiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateProfilePhoto(photoBase64: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (photoBase64.length > 44000) {
            return Result.failure(IllegalArgumentException("La imagen seleccionada es demasiado grande."))
        }
        return try {
            db.collection("publicProfiles").document(uid).update(
                mapOf(
                    "profilePhotoBase64" to photoBase64,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "publicProfiles/$uid")
            Result.failure(e)
        }
    }

    suspend fun recordFocusMinutes(minutesAdded: Int): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (minutesAdded <= 0) return Result.success(Unit)
        val path = "publicProfiles/$uid"
        return try {
            val snap = db.collection("publicProfiles").document(uid).get().await()
            val profile = snap.toObject(PublicProfile::class.java) ?: return Result.success(Unit)
            val newMinutes = (profile.totalFocusMinutes + minutesAdded).coerceAtMost(1000000L)
            val newLevel = (1L + newMinutes / 20L + profile.climberBestMeters / 150L).coerceIn(1L, 999L)
            val today = todayIso()
            val dailyBase = if (profile.dailyChallengeDate == today) profile.dailyChallengeScore else 0L
            val newDaily = (dailyBase + minutesAdded * 10L).coerceAtMost(1000000L)

            val earnedMedals = profile.medals.toMutableList()
            if (newMinutes >= 15L && !earnedMedals.contains("deep_flow_15")) earnedMedals.add("deep_flow_15")
            if (newMinutes >= 60L && !earnedMedals.contains("deep_flow_60")) earnedMedals.add("deep_flow_60")

            db.collection("publicProfiles").document(uid).update(
                mapOf(
                    "totalFocusMinutes" to newMinutes,
                    "activityLevel" to newLevel,
                    "dailyChallengeScore" to newDaily,
                    "dailyChallengeDate" to today,
                    "medals" to earnedMedals.distinct().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordClimberRun(meters: Int, completedDailyChallenge: Boolean): Result<List<String>> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val path = "publicProfiles/$uid"
        return try {
            val snap = db.collection("publicProfiles").document(uid).get().await()
            val profile = snap.toObject(PublicProfile::class.java) ?: return Result.success(emptyList())
            val safeMeters = meters.coerceIn(0, 1000000).toLong()
            val bestMeters = maxOf(profile.climberBestMeters, safeMeters)
            val newLevel = (1L + profile.totalFocusMinutes / 20L + bestMeters / 150L).coerceIn(1L, 999L)
            val today = todayIso()
            val dailyBase = if (profile.dailyChallengeDate == today) profile.dailyChallengeScore else 0L
            val bonus = if (completedDailyChallenge) 100L else 0L
            val newDaily = (dailyBase + safeMeters / 2L + bonus).coerceAtMost(1000000L)
            val newDailyCompleted = if (completedDailyChallenge) {
                (profile.dailyChallengesCompleted + 1L).coerceAtMost(100000L)
            } else {
                profile.dailyChallengesCompleted
            }

            val unlockedAvatars = profile.unlockedAvatars.toMutableList()
            val newlyUnlocked = mutableListOf<String>()
            MedalsAndAvatarsCatalog.avatars.forEach { av ->
                if (bestMeters >= av.unlockMeters && !unlockedAvatars.contains(av.id)) {
                    unlockedAvatars.add(av.id)
                    newlyUnlocked.add(av.name)
                }
            }

            val earnedMedals = profile.medals.toMutableList()
            if (bestMeters >= 120L && !earnedMedals.contains("climber_altitude")) earnedMedals.add("climber_altitude")
            if (bestMeters >= 250L && !earnedMedals.contains("climber_250")) earnedMedals.add("climber_250")
            if (bestMeters >= 600L && !earnedMedals.contains("climber_600")) earnedMedals.add("climber_600")
            if (completedDailyChallenge && !earnedMedals.contains("daily_victor")) earnedMedals.add("daily_victor")

            db.collection("publicProfiles").document(uid).update(
                mapOf(
                    "climberBestMeters" to bestMeters,
                    "activityLevel" to newLevel,
                    "dailyChallengeScore" to newDaily,
                    "dailyChallengeDate" to today,
                    "dailyChallengesCompleted" to newDailyCompleted,
                    "unlockedAvatars" to unlockedAvatars.distinct().take(20),
                    "medals" to earnedMedals.distinct().take(60),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(newlyUnlocked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun unlockMedals(uid: String, newMedalIds: List<String>): Result<Unit> {
        val path = "publicProfiles/$uid"
        return try {
            val snap = db.collection("publicProfiles").document(uid).get().await()
            val profile = snap.toObject(PublicProfile::class.java) ?: return Result.success(Unit)
            val merged = (profile.medals + newMedalIds).distinct().take(60)
            if (merged.size != profile.medals.size) {
                db.collection("publicProfiles").document(uid).update(
                    mapOf(
                        "medals" to merged,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- FRIENDS & SOCIAL ---

    suspend fun sendFriendRequestByCode(
        myDisplayName: String,
        myFriendCode: String,
        targetCodeRaw: String,
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val code = targetCodeRaw.trim().uppercase(Locale.US)
        if (!code.matches(Regex("^[A-Za-z0-9_\\-]{3,20}$"))) {
            return Result.failure(IllegalArgumentException("Código de amigo inválido (usa letras, números o guion)."))
        }
        return try {
            val lookupSnap = db.collection("friendCodes").document(code).get().await()
            if (!lookupSnap.exists()) {
                return Result.failure(IllegalStateException("No se encontró ningún usuario con el código $code."))
            }
            val targetUid = lookupSnap.getString("userId")
                ?: return Result.failure(IllegalStateException("Código sin usuario asociado."))
            val targetName = lookupSnap.getString("displayName") ?: "Amigo"
            if (targetUid == uid) {
                return Result.failure(IllegalArgumentException("No puedes enviarte una solicitud a ti mismo."))
            }

            val reqId = "${uid}_$targetUid"
            db.collection("friendRequests").document(reqId).set(
                mapOf(
                    "fromUid" to uid,
                    "toUid" to targetUid,
                    "fromDisplayName" to myDisplayName.take(60),
                    "fromFriendCode" to myFriendCode.take(20),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(targetName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "friendRequests")
            Result.failure(e)
        }
    }

    suspend fun respondToFriendRequest(request: FriendRequest, accept: Boolean): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val path = "friendRequests/${request.id}"
        return try {
            db.collection("friendRequests").document(request.id).update(
                mapOf(
                    "status" to if (accept) "accepted" else "declined",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (accept) {
                db.collection("users").document(uid).collection("friends").document(request.fromUid).set(
                    mapOf(
                        "userId" to uid,
                        "friendUid" to request.fromUid,
                        "friendCode" to request.fromFriendCode.take(20),
                        "displayName" to request.fromDisplayName.take(60),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                val friendsCount = db.collection("users").document(uid).collection("friends").get().await().size().toLong()
                db.collection("publicProfiles").document(uid).update(
                    mapOf(
                        "friendsCount" to friendsCount.coerceIn(0L, 10000L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                unlockMedals(uid, listOf("social_bond"))
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncAcceptedOutgoingFriend(request: FriendRequest): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        return try {
            val friendRef = db.collection("users").document(uid).collection("friends").document(request.toUid)
            if (!friendRef.get().await().exists()) {
                val targetProfile = db.collection("publicProfiles").document(request.toUid).get().await()
                    .toObject(PublicProfile::class.java)
                if (targetProfile != null) {
                    friendRef.set(
                        mapOf(
                            "userId" to uid,
                            "friendUid" to targetProfile.userId,
                            "friendCode" to targetProfile.friendCode.take(20),
                            "displayName" to targetProfile.displayName.take(60),
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    val friendsCount = db.collection("users").document(uid).collection("friends").get().await().size().toLong()
                    db.collection("publicProfiles").document(uid).update(
                        mapOf(
                            "friendsCount" to friendsCount.coerceIn(0L, 10000L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    unlockMedals(uid, listOf("social_bond"))
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$uid/friends/${request.toUid}")
            Result.failure(e)
        }
    }

    fun observeFriendPublicProfiles(friends: List<FriendLink>): Flow<List<PublicProfile>> {
        if (friends.isEmpty()) return flowOf(emptyList())
        val uid = auth.currentUser?.uid.orEmpty()
        val flows = friends.map { link ->
            db.collection("publicProfiles").document(link.friendUid)
                .snapshots()
                .map { snap -> snap.toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    if (uid.isNotBlank()) handleFirestoreError(error as? Exception ?: Exception(error), OperationType.GET, "publicProfiles/${link.friendUid}")
                    emit(null)
                }
        }
        return combine(flows) { profiles -> profiles.filterNotNull() }
    }

    suspend fun fetchFriendPublicProfiles(friends: List<FriendLink>): Result<List<PublicProfile>> {
        return try {
            val profiles = friends.mapNotNull { link ->
                db.collection("publicProfiles").document(link.friendUid).get().await()
                    .toObject(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            Result.success(profiles)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "publicProfiles")
            Result.failure(e)
        }
    }

    // --- REALTIME MULTIPLAYER ROOMS ---

    suspend fun createMultiplayerRoom(
        myProfile: PublicProfile,
        gameType: String,
        targetGoal: Long,
        invitedUid: String = "",
    ): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..6).map { chars.random() }.joinToString("")
        val path = "gameRooms/$code"
        return try {
            db.collection("gameRooms").document(code).set(
                mapOf(
                    "roomCode" to code,
                    "gameType" to gameType,
                    "status" to "waiting",
                    "hostUid" to uid,
                    "hostName" to myProfile.displayName.ifBlank { "Anfitrión" }.take(60),
                    "hostAvatarId" to myProfile.selectedAvatarId.ifBlank { "sakura" }.take(40),
                    "hostProgress" to 0L,
                    "hostReady" to true,
                    "guestUid" to "",
                    "guestName" to "",
                    "guestAvatarId" to "",
                    "guestProgress" to 0L,
                    "guestReady" to false,
                    "targetGoal" to targetGoal.coerceIn(10L, 100000L),
                    "sharedRewardGranted" to false,
                    "invitedUid" to invitedUid.take(128),
                    "participantUids" to listOf(uid),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun joinMultiplayerRoom(myProfile: PublicProfile, roomCodeRaw: String): Result<GameRoom> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val code = roomCodeRaw.trim().uppercase(Locale.US)
        if (!code.matches(Regex("^[A-Z0-9]{4,12}$"))) {
            return Result.failure(IllegalArgumentException("Código de sala inválido."))
        }
        val path = "gameRooms/$code"
        return try {
            val docRef = db.collection("gameRooms").document(code)
            val snap = docRef.get().await()
            val room = snap.toObject(GameRoom::class.java)
                ?: return Result.failure(IllegalStateException("Sala $code no encontrada."))
            if (room.hostUid == uid) {
                return Result.success(room)
            }
            if (room.status != "waiting" || room.guestUid.isNotBlank()) {
                return Result.failure(IllegalStateException("La sala $code ya está en curso o completa."))
            }
            docRef.update(
                mapOf(
                    "guestUid" to uid,
                    "guestName" to myProfile.displayName.ifBlank { "Invitado" }.take(60),
                    "guestAvatarId" to myProfile.selectedAvatarId.ifBlank { "frieren" }.take(40),
                    "guestReady" to true,
                    "participantUids" to listOf(room.hostUid, uid),
                    "status" to "active",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            val updated = docRef.get().await().toObject(GameRoom::class.java) ?: room
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMultiplayerProgress(
        room: GameRoom,
        progress: Long,
        finishRoom: Boolean = false,
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (room.status == "finished") return Result.success(Unit)
        val path = "gameRooms/${room.id}"
        return try {
            val isHost = room.hostUid == uid
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (isHost) {
                updates["hostProgress"] = progress.coerceIn(0L, 100000L)
            } else {
                updates["guestProgress"] = progress.coerceIn(0L, 100000L)
            }
            if (finishRoom) {
                updates["status"] = "finished"
                updates["sharedRewardGranted"] = true
            }
            db.collection("gameRooms").document(room.id).update(updates).await()
            if (finishRoom) {
                grantMultiplayerDuoReward(room.gameType)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun grantMultiplayerDuoReward(gameType: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        val medalId = if (gameType == "CLIMBER") "duo_summit" else "duo_harmony"
        return try {
            unlockMedals(uid, listOf(medalId))
            val snap = db.collection("publicProfiles").document(uid).get().await()
            val profile = snap.toObject(PublicProfile::class.java)
            if (profile != null) {
                val today = todayIso()
                val dailyBase = if (profile.dailyChallengeDate == today) profile.dailyChallengeScore else 0L
                val bonus = 150L
                val updates = mutableMapOf<String, Any>(
                    "dailyChallengeScore" to (dailyBase + bonus).coerceAtMost(1000000L),
                    "dailyChallengeDate" to today,
                    "activityLevel" to (profile.activityLevel + 1L).coerceAtMost(999L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (gameType == "CLIMBER") {
                    updates["duoClimberWins"] = (profile.duoClimberWins + 1L).coerceAtMost(100000L)
                } else {
                    updates["duoMusicWins"] = (profile.duoMusicWins + 1L).coerceAtMost(100000L)
                }
                db.collection("publicProfiles").document(uid).update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "publicProfiles/$uid")
            Result.failure(e)
        }
    }

    // --- LIVE FRIEND CHAT & GOOD VIBE COUNTER ---

    suspend fun sendFriendChatMessage(
        friendUid: String,
        senderName: String,
        text: String,
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Sin sesión"))
        if (friendUid.isBlank() || friendUid == uid) {
            return Result.failure(IllegalArgumentException("Selecciona un amigo válido para chatear."))
        }
        val cleanText = text.trim().take(300)
        if (cleanText.isEmpty()) {
            return Result.failure(IllegalArgumentException("Escribe un mensaje primero."))
        }

        val chatId = friendChatId(uid, friendUid)
        val participants = listOf(uid, friendUid).sorted()
        val chatRef = db.collection("friendChats").document(chatId)
        val path = "friendChats/$chatId"

        return try {
            val initialSnap = chatRef.get().await()
            if (!initialSnap.exists()) {
                chatRef.set(
                    mapOf(
                        "participantUids" to participants,
                        "goodVibePoints" to 0L,
                        "emotionalLevel" to 1L,
                        "totalMessages" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val vibeBoost = 25L
            val safeSenderName = senderName.trim().ifBlank { "Amigo" }.take(60)
            val msgId = "m_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
            val msgRef = chatRef.collection("messages").document(msgId)

            val (didLevelUp, reachedLevel) = db.runTransaction { transaction ->
                val snap = transaction.get(chatRef)
                val currentPoints = (snap.getLong("goodVibePoints") ?: 0L).coerceIn(0L, 99L)
                val currentLevel = (snap.getLong("emotionalLevel") ?: 1L).coerceIn(1L, 999L)
                val currentMessages = (snap.getLong("totalMessages") ?: 0L).coerceAtLeast(0L)

                val totalPoints = currentPoints + vibeBoost
                val leveled = totalPoints >= 100L && currentLevel < 999L
                val nextPoints = if (totalPoints >= 100L) totalPoints % 100L else totalPoints
                val nextLevel = if (leveled) currentLevel + 1L else currentLevel

                transaction.set(
                    msgRef,
                    mapOf(
                        "senderUid" to uid,
                        "senderName" to safeSenderName,
                        "text" to cleanText,
                        "vibeBoost" to vibeBoost,
                        "participantUids" to participants,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    chatRef,
                    mapOf(
                        "goodVibePoints" to nextPoints,
                        "emotionalLevel" to nextLevel,
                        "totalMessages" to currentMessages + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                leveled to nextLevel
            }.await()

            if (didLevelUp) {
                val pubSnap = db.collection("publicProfiles").document(uid).get().await()
                val prevMax = pubSnap.getLong("maxEmotionalLevel") ?: 1L
                if (reachedLevel > prevMax) {
                    db.collection("publicProfiles").document(uid).update(
                        mapOf(
                            "maxEmotionalLevel" to reachedLevel.coerceIn(1L, 999L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
                if (reachedLevel >= 2L) {
                    unlockMedals(uid, listOf("emotional_friendship"))
                }
            }

            Result.success(didLevelUp)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
