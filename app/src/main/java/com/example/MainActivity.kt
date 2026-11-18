package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.AmbientFocusEngine
import com.example.audio.HapticsHelper
import com.example.data.PreferencesRepository
import com.example.data.VicdaruRepository
import com.example.data.authStateFlow
import com.example.data.local.QuoteCacheRepository
import com.example.data.local.VicdaruLocalDatabase
import com.example.model.AnimeSeries
import com.example.model.UiState
import com.example.network.NetworkMonitor
import com.example.notifications.QuoteNotificationHelper
import com.example.notifications.QuoteScheduler
import com.example.ui.navigation.VicdaruMainScaffold
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.VicdaruTheme
import com.example.viewmodel.VicdaruMainViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        QuoteNotificationHelper.ensureChannels(this)
        setContent {
            VicdaruRoot()
        }
    }
}

@Composable
fun VicdaruRoot() {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val rootHaptics = remember(context) { HapticsHelper(context.applicationContext) }
    val localPrefs by prefsRepo.prefsFlow.collectAsStateWithLifecycle(initialValue = null)

    var isReplayingOnboarding by rememberSaveable { mutableStateOf(false) }
    var onboardingPreviewAnime by remember { mutableStateOf<AnimeSeries?>(null) }

    val prefs = localPrefs
    if (prefs == null) {
        VicdaruTheme(activeAnime = AnimeSeries.SAKURA, darkTheme = true) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
        return
    }

    if (!prefs.onboardingCompleted || isReplayingOnboarding) {
        val activeAnime = onboardingPreviewAnime ?: AnimeSeries.fromKey(prefs.activeThemeAnime)
        VicdaruTheme(activeAnime = activeAnime, darkTheme = prefs.darkMode) {
            if (isReplayingOnboarding) {
                BackHandler {
                    isReplayingOnboarding = false
                    onboardingPreviewAnime = null
                }
            }
            OnboardingScreen(
                haptics = rootHaptics,
                initialAnime = activeAnime,
                onPreviewAnimeChanged = { onboardingPreviewAnime = it },
                onFinish = {
                    scope.launch {
                        prefsRepo.setOnboardingCompleted(true)
                    }
                    isReplayingOnboarding = false
                    onboardingPreviewAnime = null
                }
            )
        }
        return
    }

    val user = currentUser
    if (user == null) {
        val fallbackAnime = AnimeSeries.fromKey(prefs.activeThemeAnime)
        VicdaruTheme(activeAnime = fallbackAnime, darkTheme = prefs.darkMode) {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            AuthenticatedVicdaruShell(
                currentUserId = user.uid,
                displayName = user.displayName ?: "Viajero Estelar",
                onReplayOnboarding = { isReplayingOnboarding = true }
            )
        }
    }
}

@Composable
private fun AuthenticatedVicdaruShell(
    currentUserId: String,
    displayName: String,
    onReplayOnboarding: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel: VicdaruMainViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val localDb = VicdaruLocalDatabase.getInstance(app)
                VicdaruMainViewModel(
                    repository = VicdaruRepository(db),
                    prefsRepository = PreferencesRepository(app),
                    quoteCacheRepository = QuoteCacheRepository(localDb.cachedQuoteDao()),
                    networkMonitor = NetworkMonitor(app),
                    currentUserId = currentUserId,
                    initialDisplayName = displayName,
                    audioEngine = AmbientFocusEngine(app),
                    haptics = HapticsHelper(app)
                )
            }
        }
    )

    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val quotesState by viewModel.quotesState.collectAsStateWithLifecycle()
    val habitsState by viewModel.habitsState.collectAsStateWithLifecycle()
    val visualizerProfilesState by viewModel.visualizerProfilesState.collectAsStateWithLifecycle()
    val friendProfiles by viewModel.friendProfiles.collectAsStateWithLifecycle()
    val incomingRequestsState by viewModel.incomingRequestsState.collectAsStateWithLifecycle()
    val invitedRoomsState by viewModel.invitedRoomsState.collectAsStateWithLifecycle()
    val activeRoom by viewModel.activeGameRoomState.collectAsStateWithLifecycle()
    val isFetchingQuote by viewModel.isFetchingQuote.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val pendingCacheSyncCount by viewModel.pendingCacheSyncCount.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

    val settings = (settingsState as? UiState.Success)?.data
    val profile = (profileState as? UiState.Success)?.data
    val incomingRequests = (incomingRequestsState as? UiState.Success)?.data.orEmpty()
    val invitedRooms = (invitedRoomsState as? UiState.Success)?.data.orEmpty()

    val latestQuoteAnime = (quotesState as? UiState.Success)?.data?.firstOrNull()?.animeKey
    val activeAnime = AnimeSeries.fromKey(latestQuoteAnime ?: settings?.activeThemeAnime)
    val darkMode = settings?.darkMode ?: true

    LaunchedEffect(
        settings?.morningQuoteEnabled,
        settings?.morningHour,
        settings?.morningMinute,
        settings?.randomQuotesEnabled
    ) {
        if (settings != null) {
            QuoteScheduler.syncSchedules(
                context = context,
                morningEnabled = settings.morningQuoteEnabled,
                morningHour = settings.morningHour.toInt(),
                morningMinute = settings.morningMinute.toInt(),
                randomEnabled = settings.randomQuotesEnabled
            )
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    VicdaruTheme(activeAnime = activeAnime, darkTheme = darkMode) {
        VicdaruMainScaffold(
            viewModel = viewModel,
            currentUserId = currentUserId,
            activeAnime = activeAnime,
            darkMode = darkMode,
            settings = settings,
            profile = profile,
            quotesState = quotesState,
            habitsState = habitsState,
            visualizerProfilesState = visualizerProfilesState,
            friendProfiles = friendProfiles,
            incomingRequests = incomingRequests,
            invitedRooms = invitedRooms,
            activeRoom = activeRoom,
            isFetchingQuote = isFetchingQuote,
            isOffline = isOffline,
            pendingCacheSyncCount = pendingCacheSyncCount,
            snackbarHostState = snackbarHostState,
            onReplayOnboarding = onReplayOnboarding
        )
    }
}
