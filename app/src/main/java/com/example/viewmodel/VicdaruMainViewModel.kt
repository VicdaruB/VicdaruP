package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AmbientFocusEngine
import com.example.audio.FocusState
import com.example.audio.HapticsHelper
import com.example.data.PreferencesRepository
import com.example.data.VicdaruRepository
import com.example.data.local.QuoteCacheRepository
import com.example.model.AchievementProgress
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
import com.example.model.UiState
import com.example.model.UserSettings
import com.example.model.VisualizerProfile
import com.example.network.AnimeImageFetcher
import com.example.network.NetworkMonitor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class VicdaruMainViewModel(
    private val repository: VicdaruRepository,
    private val prefsRepository: PreferencesRepository,
    private val quoteCacheRepository: QuoteCacheRepository,
    private val networkMonitor: NetworkMonitor,
    private val currentUserId: String,
    private val initialDisplayName: String,
    val audioEngine: AmbientFocusEngine,
    val haptics: HapticsHelper,
) : ViewModel() {

    private companion object {
        const val TAG = "VicdaruMainVM"
        const val STOP_TIMEOUT_MS = 5000L
        const val POMODORO_WORK_SECONDS = 25 * 60
        const val POMODORO_BREAK_SECONDS = 5 * 60
    }

    private fun <T> Flow<T>.toUiStateFlow(): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Stream error", rootError)
                emit(UiState.Error(rootError.message ?: "Error al sincronizar con la nube"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = networkMonitor.currentIsOnline()
        )

    val isOffline: StateFlow<Boolean> = isOnline
        .map { !it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = !networkMonitor.currentIsOnline()
        )

    val pendingCacheSyncCount: StateFlow<Int> = quoteCacheRepository.observePendingSyncCount(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), 0)

    private val _remoteQuotesSyncedOnce = MutableStateFlow(false)
    private val _remoteQuotesError = MutableStateFlow<String?>(null)
    private val cacheSyncMutex = Mutex()

    val settingsState: StateFlow<UiState<UserSettings?>> =
        repository.observeUserSettings(currentUserId).toUiStateFlow()

    val profileState: StateFlow<UiState<PublicProfile?>> =
        repository.observePublicProfile(currentUserId).toUiStateFlow()

    val quotesState: StateFlow<UiState<List<QuoteHistoryEntry>>> = combine(
        quoteCacheRepository.observeQuotes(currentUserId),
        _remoteQuotesSyncedOnce,
        _remoteQuotesError,
        isOnline
    ) { cached, syncedOnce, remoteErr, online ->
        when {
            cached.isNotEmpty() -> UiState.Success(cached)
            remoteErr != null && online -> UiState.Error(remoteErr)
            syncedOnce || !online -> UiState.Success(emptyList())
            else -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading
    )

    val habitsState: StateFlow<UiState<List<HabitItem>>> =
        repository.observeHabits(currentUserId).toUiStateFlow()

    val visualizerProfilesState: StateFlow<UiState<List<VisualizerProfile>>> =
        repository.observeVisualizerProfiles(currentUserId).toUiStateFlow()

    val friendsState: StateFlow<UiState<List<FriendLink>>> =
        repository.observeFriends(currentUserId).toUiStateFlow()

    val incomingRequestsState: StateFlow<UiState<List<FriendRequest>>> =
        repository.observeIncomingFriendRequests(currentUserId).toUiStateFlow()

    val invitedRoomsState: StateFlow<UiState<List<GameRoom>>> =
        repository.observeInvitedRooms(currentUserId).toUiStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val friendProfiles: StateFlow<List<PublicProfile>> = friendsState
        .flatMapLatest { state ->
            val links = (state as? UiState.Success)?.data.orEmpty()
            repository.observeFriendPublicProfiles(links)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val achievementsState: StateFlow<List<AchievementProgress>> = combine(
        profileState,
        quotesState,
        habitsState,
        friendsState
    ) { profUi, quotesUi, habitsUi, friendsUi ->
        val prof = (profUi as? UiState.Success)?.data
        val quotes = (quotesUi as? UiState.Success)?.data.orEmpty()
        val habits = (habitsUi as? UiState.Success)?.data.orEmpty()
        val friends = (friendsUi as? UiState.Success)?.data.orEmpty()
        MedalsAndAvatarsCatalog.evaluateAchievements(
            profile = prof,
            liveQuotesCount = quotes.size,
            liveFavoritesCount = quotes.count { it.favorite },
            liveHabitsCompleted = habits.sumOf { it.completedDates.size },
            liveBestStreak = (habits.maxOfOrNull { it.currentStreak } ?: 0L).toInt(),
            liveFriendsCount = friends.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeGameRoomState: StateFlow<GameRoom?> = _activeRoomId
        .flatMapLatest { roomId ->
            if (roomId.isNullOrBlank()) flowOf(null)
            else repository.observeGameRoom(roomId, currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Game room observe error", error)
                    emit(null)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    private val _selectedChatFriendUid = MutableStateFlow<String?>(null)
    val selectedChatFriendUid: StateFlow<String?> = _selectedChatFriendUid.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeFriendChatState: StateFlow<FriendChat?> = _selectedChatFriendUid
        .flatMapLatest { friendUid ->
            if (friendUid.isNullOrBlank()) flowOf(null)
            else repository.observeFriendChat(currentUserId, friendUid)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "FriendChat observe error", error)
                    emit(null)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val friendChatMessagesState: StateFlow<List<FriendChatMessage>> = _selectedChatFriendUid
        .flatMapLatest { friendUid ->
            if (friendUid.isNullOrBlank()) flowOf(emptyList())
            else repository.observeFriendChatMessages(currentUserId, friendUid)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "FriendChat messages observe error", error)
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isFetchingQuote = MutableStateFlow(false)
    val isFetchingQuote: StateFlow<Boolean> = _isFetchingQuote.asStateFlow()

    // Focus & Pomodoro State Detector
    private val _focusState = MutableStateFlow(FocusState.WARMUP)
    val focusState: StateFlow<FocusState> = _focusState.asStateFlow()

    private val _pomodoroRemainingSeconds = MutableStateFlow(POMODORO_WORK_SECONDS)
    val pomodoroRemainingSeconds: StateFlow<Int> = _pomodoroRemainingSeconds.asStateFlow()

    private val _pomodoroRunning = MutableStateFlow(false)
    val pomodoroRunning: StateFlow<Boolean> = _pomodoroRunning.asStateFlow()

    private val _isBreakPhase = MutableStateFlow(false)
    val isBreakPhase: StateFlow<Boolean> = _isBreakPhase.asStateFlow()

    private var elapsedWorkSecondsInSession = 0
    private var lastTouchTimestampMs = System.currentTimeMillis()
    private var pomodoroJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureUserBootstrapped(initialDisplayName)
        }
        // Auto-sync accepted outgoing friend requests into user's friend list
        viewModelScope.launch {
            repository.observeOutgoingAcceptedRequests(currentUserId)
                .catch { error -> if (error is CancellationException) throw error }
                .collect { accepted ->
                    accepted.forEach { req ->
                        repository.syncAcceptedOutgoingFriend(req)
                    }
                }
        }
        // Select default chat friend automatically as friend profiles arrive live
        viewModelScope.launch {
            friendProfiles.collect { profiles ->
                if (_selectedChatFriendUid.value == null && profiles.isNotEmpty()) {
                    _selectedChatFriendUid.value = profiles.first().userId
                }
            }
        }
        // Sync remote quotes into Room local cache so history & favorites work offline
        viewModelScope.launch {
            repository.observeQuotes(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    Log.w(TAG, "Remote quotes stream error; keeping Room cache active", error)
                    _remoteQuotesError.value = error.message ?: "Error al sincronizar citas"
                }
                .collect { remoteQuotes ->
                    _remoteQuotesError.value = null
                    quoteCacheRepository.syncFromRemote(currentUserId, remoteQuotes)
                    _remoteQuotesSyncedOnce.value = true
                }
        }
        // Reconcile queued offline favorites & offline-delivered quotes when connectivity returns
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    syncPendingCacheChanges()
                }
            }
        }
        // Mirror settings locally for WorkManager notifications
        viewModelScope.launch {
            settingsState.collect { state ->
                val s = (state as? UiState.Success)?.data ?: return@collect
                prefsRepository.saveSettingsMirror(
                    darkMode = s.darkMode,
                    morningEnabled = s.morningQuoteEnabled,
                    morningHour = s.morningHour.toInt(),
                    morningMinute = s.morningMinute.toInt(),
                    randomEnabled = s.randomQuotesEnabled,
                    activeThemeAnime = s.activeThemeAnime
                )
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun deliverNewMotivationalQuote() {
        if (_isFetchingQuote.value) return
        viewModelScope.launch {
            _isFetchingQuote.value = true
            val currentQuotes = (quotesState.value as? UiState.Success)?.data
                ?: quoteCacheRepository.getCachedQuotes(currentUserId)
            val latest = currentQuotes.firstOrNull()
            val picked = CuratedQuotesCatalog.randomQuote(excludeText = latest?.quoteText)

            if (!isOnline.value) {
                val cachedSameAnimeImg = currentQuotes
                    .firstOrNull { it.animeKey == picked.anime.key && it.imageUrl.isNotBlank() }
                    ?.imageUrl
                    ?: picked.anime.fallbackImageUrl
                val now = Timestamp.now()
                val localEntry = QuoteHistoryEntry(
                    id = "local_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
                    userId = currentUserId,
                    quoteText = picked.text,
                    animeKey = picked.anime.key,
                    characterName = picked.characterName,
                    moodTag = picked.moodTag,
                    imageUrl = cachedSameAnimeImg,
                    imageSource = "Caché Offline Vicdaru",
                    favorite = false,
                    sourceType = "MANUAL",
                    createdAt = now,
                    updatedAt = now
                )
                quoteCacheRepository.cacheQuote(currentUserId, localEntry, pendingUpload = true)
                prefsRepository.saveLastDeliveredQuote(
                    animeKey = picked.anime.key,
                    quoteText = picked.text,
                    characterName = picked.characterName,
                    imageUrl = cachedSameAnimeImg
                )
                _statusMessage.value = "Cita guardada en caché local (se sincronizará al volver internet)"
                haptics.quoteDelivered()
                _isFetchingQuote.value = false
                return@launch
            }

            val historyUrls = currentQuotes.mapNotNull { it.imageUrl.takeIf { url -> url.isNotBlank() } }.toSet()
            val localUsed = prefsRepository.getSnapshot().usedImageUrls
            val fetched = AnimeImageFetcher.fetchFreshImage(
                anime = picked.anime,
                excludeUrls = historyUrls + localUsed,
                characterHint = picked.characterName
            )

            repository.saveQuoteEntry(
                quoteText = picked.text,
                anime = picked.anime,
                characterName = picked.characterName,
                moodTag = picked.moodTag,
                imageUrl = fetched.imageUrl,
                imageSource = fetched.sourceLabel,
                sourceType = "MANUAL"
            ).onSuccess { saved ->
                val now = Timestamp.now()
                quoteCacheRepository.cacheQuote(
                    userId = currentUserId,
                    entry = saved.copy(createdAt = saved.createdAt ?: now, updatedAt = saved.updatedAt ?: now),
                    pendingUpload = false
                )
                prefsRepository.saveLastDeliveredQuote(
                    animeKey = picked.anime.key,
                    quoteText = picked.text,
                    characterName = picked.characterName,
                    imageUrl = fetched.imageUrl
                )
                _statusMessage.value = "Tema adaptado a ${picked.anime.displayName} • ${fetched.sourceLabel}"
                haptics.quoteDelivered()
            }.onFailure {
                // Save locally when remote save fails so the quote is preserved offline
                val now = Timestamp.now()
                val fallbackEntry = QuoteHistoryEntry(
                    id = "local_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(5)}",
                    userId = currentUserId,
                    quoteText = picked.text,
                    animeKey = picked.anime.key,
                    characterName = picked.characterName,
                    moodTag = picked.moodTag,
                    imageUrl = fetched.imageUrl,
                    imageSource = fetched.sourceLabel,
                    favorite = false,
                    sourceType = "MANUAL",
                    createdAt = now,
                    updatedAt = now
                )
                quoteCacheRepository.cacheQuote(currentUserId, fallbackEntry, pendingUpload = true)
                prefsRepository.saveLastDeliveredQuote(
                    animeKey = picked.anime.key,
                    quoteText = picked.text,
                    characterName = picked.characterName,
                    imageUrl = fetched.imageUrl
                )
                _statusMessage.value = "Guardada en caché local; pendiente de subir a la nube"
                haptics.quoteDelivered()
            }
            _isFetchingQuote.value = false
        }
    }

    fun toggleQuoteFavorite(entry: QuoteHistoryEntry) {
        val currentList = (quotesState.value as? UiState.Success)?.data.orEmpty()
        val newFav = !entry.favorite
        haptics.favoriteToggle(newFav)
        val delta = if (newFav) 1 else -1
        val favCount = (currentList.count { it.favorite } + delta).coerceAtLeast(0)
        val isLocalTemp = entry.id.startsWith("local_")
        val onlineNow = isOnline.value

        viewModelScope.launch {
            quoteCacheRepository.updateFavoriteLocal(
                userId = currentUserId,
                quoteId = entry.id,
                favorite = newFav,
                pendingSync = !onlineNow || isLocalTemp
            )
            if (onlineNow && !isLocalTemp) {
                repository.toggleQuoteFavorite(entry.id, newFav, favCount)
                    .onSuccess {
                        quoteCacheRepository.markFavoriteSynced(currentUserId, entry.id)
                    }
                    .onFailure {
                        quoteCacheRepository.updateFavoriteLocal(
                            userId = currentUserId,
                            quoteId = entry.id,
                            favorite = newFav,
                            pendingSync = true
                        )
                    }
            }
        }
    }

    private suspend fun syncPendingCacheChanges() = cacheSyncMutex.withLock {
        val pending = quoteCacheRepository.getPendingSyncQuotes(currentUserId)
        if (pending.isEmpty()) return@withLock

        var syncedCount = 0
        for (entity in pending) {
            if (!isOnline.value) break
            if (entity.pendingUpload) {
                val anime = AnimeSeries.fromKey(entity.animeKey)
                val result = repository.saveQuoteEntry(
                    quoteText = entity.quoteText,
                    anime = anime,
                    characterName = entity.characterName,
                    moodTag = entity.moodTag,
                    imageUrl = entity.imageUrl,
                    imageSource = entity.imageSource,
                    sourceType = entity.sourceType
                )
                result.onSuccess { remoteEntry ->
                    quoteCacheRepository.replacePendingUploadWithSynced(
                        userId = currentUserId,
                        localTempId = entity.id,
                        syncedEntry = remoteEntry,
                        preserveFavorite = entity.favorite
                    )
                    if (entity.favorite) {
                        val totalFavs = quoteCacheRepository.getCachedQuotes(currentUserId).count { it.favorite }
                        repository.toggleQuoteFavorite(remoteEntry.id, true, totalFavs)
                            .onSuccess {
                                quoteCacheRepository.markFavoriteSynced(currentUserId, remoteEntry.id)
                            }
                    }
                    syncedCount++
                }
            } else if (entity.pendingFavoriteSync && !entity.id.startsWith("local_")) {
                val totalFavs = quoteCacheRepository.getCachedQuotes(currentUserId).count { it.favorite }
                repository.toggleQuoteFavorite(entity.id, entity.favorite, totalFavs)
                    .onSuccess {
                        quoteCacheRepository.markFavoriteSynced(currentUserId, entity.id)
                        syncedCount++
                    }
            }
        }

        if (syncedCount > 0) {
            _statusMessage.value = "$syncedCount citas y favoritos sincronizados con la nube"
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        haptics.toggleSwitch(enabled)
        viewModelScope.launch {
            repository.updateUserSettings(darkMode = enabled)
        }
    }

    fun updateNotificationSettings(
        morningEnabled: Boolean,
        morningHour: Int,
        morningMinute: Int,
        randomEnabled: Boolean,
    ) {
        viewModelScope.launch {
            repository.updateUserSettings(
                morningQuoteEnabled = morningEnabled,
                morningHour = morningHour,
                morningMinute = morningMinute,
                randomQuotesEnabled = randomEnabled
            ).onSuccess {
                _statusMessage.value = "Horarios de notificación actualizados"
                haptics.subtleTap()
            }
        }
    }

    fun createHabit(title: String, animeTag: String) {
        viewModelScope.launch {
            repository.createHabit(title, animeTag)
                .onSuccess {
                    _statusMessage.value = "Hábito agregado al tablero mensual"
                    haptics.habitCreated()
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "Error al crear hábito"
                    haptics.errorNotice()
                }
        }
    }

    fun toggleHabitDate(habit: HabitItem, isoDate: String) {
        val willComplete = !habit.completedDates.contains(isoDate)
        if (willComplete) {
            haptics.habitComplete()
        } else {
            haptics.habitUndo()
        }
        viewModelScope.launch {
            repository.toggleHabitDate(habit, isoDate)
        }
    }

    fun deleteHabit(habitId: String) {
        haptics.softDelete()
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    fun selectVisualizerProfile(profileId: String) {
        haptics.subtleTap()
        viewModelScope.launch {
            repository.updateUserSettings(selectedVisualizerProfileId = profileId)
        }
    }

    fun saveCustomVisualizerProfile(profile: VisualizerProfile) {
        viewModelScope.launch {
            repository.saveVisualizerProfile(profile)
                .onSuccess { savedId ->
                    repository.updateUserSettings(selectedVisualizerProfileId = savedId)
                    _statusMessage.value = "Perfil visual '${profile.name}' guardado"
                    haptics.shareOrCopy()
                }
                .onFailure {
                    _statusMessage.value = it.localizedMessage ?: "No se pudo guardar el perfil"
                    haptics.errorNotice()
                }
        }
    }

    fun selectAvatar(avatarId: String) {
        haptics.socialAction()
        viewModelScope.launch {
            repository.selectAvatar(avatarId)
                .onSuccess { _statusMessage.value = "Avatar actualizado" }
        }
    }

    fun updateProfilePhoto(photoBase64: String) {
        viewModelScope.launch {
            repository.updateProfilePhoto(photoBase64)
                .onSuccess {
                    if (photoBase64.isNotBlank()) {
                        haptics.socialAction()
                        _statusMessage.value = "Foto de perfil actualizada"
                    } else {
                        haptics.subtleTap()
                        _statusMessage.value = "Foto restaurada al símbolo anime"
                    }
                }
                .onFailure { err ->
                    haptics.errorNotice()
                    _statusMessage.value = err.localizedMessage ?: "No se pudo actualizar la foto"
                }
        }
    }

    fun recordClimberResult(meters: Int, completedDailyChallenge: Boolean) {
        viewModelScope.launch {
            repository.recordClimberRun(meters, completedDailyChallenge)
                .onSuccess { unlocked ->
                    if (unlocked.isNotEmpty()) {
                        haptics.climberUnlock()
                        _statusMessage.value = "¡Nuevo avatar desbloqueado: ${unlocked.joinToString()}!"
                    }
                }
        }
    }

    fun sendFriendRequest(code: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sendFriendRequestByCode(profile.displayName, profile.friendCode, code)
                .onSuccess { targetName ->
                    haptics.socialAction()
                    _statusMessage.value = "Solicitud enviada a $targetName"
                }
                .onFailure { err ->
                    haptics.errorNotice()
                    _statusMessage.value = err.localizedMessage ?: "No se pudo enviar solicitud"
                }
        }
    }

    fun respondToFriendRequest(req: FriendRequest, accept: Boolean) {
        if (accept) haptics.socialAction() else haptics.softDelete()
        viewModelScope.launch {
            repository.respondToFriendRequest(req, accept)
                .onSuccess {
                    _statusMessage.value = if (accept) "¡${req.fromDisplayName} ahora es tu amigo!" else "Solicitud rechazada"
                }
        }
    }

    // --- MULTIPLAYER ROOMS ---

    fun createMultiplayerRoom(gameType: String, targetGoal: Long, invitedUid: String = "") {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.createMultiplayerRoom(profile, gameType, targetGoal, invitedUid)
                .onSuccess { code ->
                    haptics.socialAction()
                    _activeRoomId.value = code
                    _statusMessage.value = "Sala creada: $code"
                }
                .onFailure { err ->
                    haptics.errorNotice()
                    _statusMessage.value = err.localizedMessage ?: "No se pudo crear la sala"
                }
        }
    }

    fun joinMultiplayerRoom(roomCode: String) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.joinMultiplayerRoom(profile, roomCode)
                .onSuccess { room ->
                    haptics.socialAction()
                    _activeRoomId.value = room.roomCode
                    _statusMessage.value = "Conectado a la sala ${room.roomCode}"
                }
                .onFailure { err ->
                    haptics.errorNotice()
                    _statusMessage.value = err.localizedMessage ?: "No se pudo unir a la sala"
                }
        }
    }

    fun updateMultiplayerProgress(progress: Long, finishRoom: Boolean = false) {
        val room = activeGameRoomState.value ?: return
        viewModelScope.launch {
            repository.updateMultiplayerProgress(room, progress, finishRoom)
                .onSuccess {
                    if (finishRoom) {
                        haptics.vibeLevelUp()
                        _statusMessage.value = "¡Medalla Dúo y recompensa desbloqueadas para ambos perfiles!"
                    }
                }
        }
    }

    fun leaveMultiplayerRoom() {
        haptics.subtleTap()
        _activeRoomId.value = null
    }

    // --- LIVE FRIEND CHAT & GOOD VIBE COUNTER ---

    fun selectChatFriend(friendUid: String) {
        haptics.subtleTap()
        _selectedChatFriendUid.value = friendUid
    }

    fun sendFriendChatMessage(text: String) {
        val friendUid = _selectedChatFriendUid.value ?: return
        val myName = (profileState.value as? UiState.Success)?.data?.displayName ?: initialDisplayName
        haptics.chatMessageSent()
        viewModelScope.launch {
            repository.sendFriendChatMessage(friendUid, myName, text)
                .onSuccess { leveledUp ->
                    if (leveledUp) {
                        haptics.vibeLevelUp()
                        val friendName = friendProfiles.value.firstOrNull { it.userId == friendUid }?.displayName ?: "tu amigo"
                        val nextLevel = (activeFriendChatState.value?.emotionalLevel ?: 1L) + 1L
                        _statusMessage.value = "¡Amistad Emocional con $friendName subió a Nivel $nextLevel!"
                    }
                }
                .onFailure { err ->
                    haptics.errorNotice()
                    _statusMessage.value = err.localizedMessage ?: "No se pudo enviar el mensaje"
                }
        }
    }

    // --- POMODORO & DYNAMIC FOCUS DETECTION ---

    fun registerFocusTap() {
        val now = System.currentTimeMillis()
        val delta = now - lastTouchTimestampMs
        lastTouchTimestampMs = now
        // Rapid repetitive tapping resets deep flow back to warmup; calm steady pace maintains deep flow
        if (_pomodoroRunning.value && !_isBreakPhase.value && delta < 4000L && elapsedWorkSecondsInSession > 20) {
            _focusState.value = FocusState.WARMUP
        }
    }

    fun togglePomodoro() {
        if (_pomodoroRunning.value) {
            haptics.focusTimerToggle(started = false)
            pausePomodoro()
        } else {
            haptics.focusTimerToggle(started = true)
            startPomodoro()
        }
    }

    private fun startPomodoro() {
        if (_pomodoroRunning.value) return
        _pomodoroRunning.value = true
        if (!audioEngine.isPlaying.value) {
            audioEngine.startPlayback()
        }
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            var minuteAccumulator = 0
            while (isActive && _pomodoroRunning.value) {
                delay(1000L)
                val remaining = _pomodoroRemainingSeconds.value
                if (remaining > 1) {
                    _pomodoroRemainingSeconds.value = remaining - 1
                    if (!_isBreakPhase.value) {
                        elapsedWorkSecondsInSession++
                        minuteAccumulator++
                        val calmDuration = System.currentTimeMillis() - lastTouchTimestampMs
                        _focusState.value = if (elapsedWorkSecondsInSession >= 12 && calmDuration >= 6000L) {
                            FocusState.DEEP_FLOW
                        } else {
                            FocusState.WARMUP
                        }
                        if (minuteAccumulator >= 60) {
                            minuteAccumulator = 0
                            repository.recordFocusMinutes(1)
                            // Sync focus progress if inside active MUSIC_FOCUS multiplayer room
                            val room = activeGameRoomState.value
                            if (room != null && room.gameType == "MUSIC_FOCUS" && room.status == "active") {
                                val mine = if (room.hostUid == currentUserId) room.hostProgress else room.guestProgress
                                val nextVal = mine + 20L
                                val done = nextVal >= room.targetGoal
                                repository.updateMultiplayerProgress(room, nextVal, finishRoom = done)
                            }
                        }
                    } else {
                        _focusState.value = FocusState.BREAK_RECHARGE
                    }
                } else {
                    // Phase transition
                    val nextBreak = !_isBreakPhase.value
                    _isBreakPhase.value = nextBreak
                    _pomodoroRemainingSeconds.value = if (nextBreak) POMODORO_BREAK_SECONDS else POMODORO_WORK_SECONDS
                    _focusState.value = if (nextBreak) FocusState.BREAK_RECHARGE else FocusState.WARMUP
                    haptics.focusPhaseTransition()
                    if (nextBreak) {
                        repository.recordFocusMinutes(1)
                    }
                }
            }
        }
    }

    private fun pausePomodoro() {
        _pomodoroRunning.value = false
        pomodoroJob?.cancel()
        pomodoroJob = null
    }

    fun resetPomodoro() {
        haptics.subtleTap()
        pausePomodoro()
        _isBreakPhase.value = false
        elapsedWorkSecondsInSession = 0
        _pomodoroRemainingSeconds.value = POMODORO_WORK_SECONDS
        _focusState.value = FocusState.WARMUP
    }

    fun logQuickFocusBoost(minutes: Int = 5) {
        haptics.habitComplete()
        viewModelScope.launch {
            repository.recordFocusMinutes(minutes)
            val room = activeGameRoomState.value
            if (room != null && room.gameType == "MUSIC_FOCUS" && room.status == "active") {
                val mine = if (room.hostUid == currentUserId) room.hostProgress else room.guestProgress
                val nextVal = mine + 25L
                val done = nextVal >= room.targetGoal
                repository.updateMultiplayerProgress(room, nextVal, finishRoom = done)
            }
            _statusMessage.value = "+$minutes min de enfoque sincronizados"
        }
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
        audioEngine.stopPlayback()
    }
}
