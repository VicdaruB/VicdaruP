package com.example.ui.screens

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AmbientSoundType
import com.example.model.AnimeSeries
import com.example.model.GameRoom
import com.example.model.MedalsAndAvatarsCatalog
import com.example.model.PublicProfile
import com.example.model.UiState
import com.example.model.UserSettings
import com.example.model.VisualizerProfile
import com.example.ui.components.AnimeVisualizerCanvas
import com.example.ui.components.VisualizerStyleCatalog
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink
import com.example.util.ProfileAvatarBadge
import com.example.viewmodel.VicdaruMainViewModel
import java.util.Locale

@Composable
fun MusicFocusScreen(
    viewModel: VicdaruMainViewModel,
    profile: PublicProfile?,
    settings: UserSettings?,
    visualizerProfilesState: UiState<List<VisualizerProfile>>,
    friendProfiles: List<PublicProfile>,
    activeRoom: GameRoom?,
    currentUserId: String,
) {
    val context = LocalContext.current
    val audioEngine = viewModel.audioEngine

    val isPlaying by audioEngine.isPlaying.collectAsStateWithLifecycle()
    val trackName by audioEngine.selectedTrackName.collectAsStateWithLifecycle()
    val hasLocalTrack by audioEngine.hasLocalTrack.collectAsStateWithLifecycle()
    val spectrum by audioEngine.spectrumBands.collectAsStateWithLifecycle()
    val beatEnergy by audioEngine.beatEnergy.collectAsStateWithLifecycle()

    val focusState by viewModel.focusState.collectAsStateWithLifecycle()
    val pomodoroSec by viewModel.pomodoroRemainingSeconds.collectAsStateWithLifecycle()
    val pomodoroRunning by viewModel.pomodoroRunning.collectAsStateWithLifecycle()
    val isBreak by viewModel.isBreakPhase.collectAsStateWithLifecycle()

    val selectedChatFriendUid by viewModel.selectedChatFriendUid.collectAsStateWithLifecycle()
    val activeFriendChat by viewModel.activeFriendChatState.collectAsStateWithLifecycle()
    val chatMessages by viewModel.friendChatMessagesState.collectAsStateWithLifecycle()

    val profiles = (visualizerProfilesState as? UiState.Success)?.data.orEmpty()
    val selectedProfile = profiles.firstOrNull { it.id == settings?.selectedVisualizerProfileId }
        ?: profiles.firstOrNull()
        ?: VisualizerProfile(
            id = "preset_sakura",
            name = AnimeSeries.SAKURA.presetName,
            baseAnimeKey = AnimeSeries.SAKURA.key,
            particleStyle = AnimeSeries.SAKURA.defaultParticleStyle,
            waveformStyle = AnimeSeries.SAKURA.defaultWaveformStyle,
            ambientSound = AnimeSeries.SAKURA.defaultAmbientSound,
            sensitivity = 1.15,
            primaryHex = AnimeSeries.SAKURA.primaryHex,
            secondaryHex = AnimeSeries.SAKURA.secondaryHex
        )

    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var joinRoomCode by remember { mutableStateOf("") }
    var chatMessageInput by remember { mutableStateOf("") }

    // Sync FocusState + Selected Visualizer Profile into the live audio synthesizer
    LaunchedEffect(selectedProfile, focusState) {
        audioEngine.updateParameters(
            soundKey = selectedProfile.ambientSound,
            focusState = focusState,
            sensitivity = selectedProfile.sensitivity.toFloat()
        )
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            var name = "Pista local seleccionada"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIdx >= 0) {
                    name = cursor.getString(nameIdx) ?: name
                }
            }
            audioEngine.selectLocalTrack(uri, name)
            viewModel.haptics.socialAction()
            viewModel.registerFocusTap()
        }
    }

    val primaryColor = remember(selectedProfile.primaryHex) {
        runCatching { Color(android.graphics.Color.parseColor(selectedProfile.primaryHex)) }
            .getOrDefault(VicdaruSkyBlue)
    }
    val secondaryColor = remember(selectedProfile.secondaryHex) {
        runCatching { Color(android.graphics.Color.parseColor(selectedProfile.secondaryHex)) }
            .getOrDefault(VicdaruSoftPink)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Focus Detection & Pomodoro Header
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Estado: ${focusState.label}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${focusState.subtitle} • ${if (isBreak) "Pausa" else "Sesión Pomodoro"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val min = pomodoroSec / 60
                        val sec = pomodoroSec % 60
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", min, sec),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { viewModel.togglePomodoro() },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f).testTag("pomodoro_toggle_button")
                        ) {
                            Icon(if (pomodoroRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (pomodoroRunning) "Pausar foco" else "Iniciar foco")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.resetPomodoro() },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar Pomodoro")
                        }
                        FilledTonalButton(
                            onClick = { viewModel.logQuickFocusBoost(5) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("quick_focus_boost_button")
                        ) {
                            Text("+5m")
                        }
                    }
                }
            }
        }

        // Interactive Reactive Audio Visualizer Canvas (6 unique character-decorated styles)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF090F1E), Color(0xFF131A2E))))
                    .testTag("music_visualizer_canvas")
            ) {
                AnimeVisualizerCanvas(
                    selectedProfile = selectedProfile,
                    spectrum = spectrum,
                    beatEnergy = beatEnergy,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    modifier = Modifier.fillMaxSize()
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                ) {
                    Text(
                        text = "${selectedProfile.name} • $trackName",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Audio Controls (Play/Pause + Local Device Music Picker + Built-in Ambient)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        viewModel.haptics.focusTimerToggle(started = !isPlaying)
                        viewModel.registerFocusTap()
                        audioEngine.togglePlayPause()
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(48.dp).testTag("audio_play_pause_button")
                ) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.GraphicEq, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isPlaying) "Pausar audio" else "Activar audio")
                }
                FilledTonalButton(
                    onClick = {
                        viewModel.haptics.subtleTap()
                        audioPickerLauncher.launch(arrayOf("audio/*"))
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(48.dp).testTag("pick_local_music_button")
                ) {
                    Icon(Icons.Filled.AudioFile, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Tu música")
                }
                if (hasLocalTrack) {
                    OutlinedButton(
                        onClick = {
                            viewModel.haptics.subtleTap()
                            audioEngine.useBuiltInAmbientOnly()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Ambiental")
                    }
                }
            }
        }

        // Character Presets & Custom Profiles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Perfiles Visuales y Sonoros", style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = {
                        viewModel.haptics.subtleTap()
                        showCreateProfileDialog = true
                    },
                    modifier = Modifier.testTag("create_visualizer_profile_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Personalizar")
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(profiles, key = { it.id }) { prof ->
                    FilterChip(
                        selected = prof.id == selectedProfile.id,
                        onClick = {
                            viewModel.registerFocusTap()
                            viewModel.selectVisualizerProfile(prof.id)
                        },
                        label = { Text(prof.name) }
                    )
                }
            }
        }

        // Live Friend Chat + Good Vibe Counter + Emotional Friendship Level
        item {
            val selectedFriend = friendProfiles.firstOrNull { it.userId == selectedChatFriendUid }
                ?: friendProfiles.firstOrNull()
            val goodVibePoints = (activeFriendChat?.goodVibePoints ?: 0L).coerceIn(0L, 100L)
            val emotionalLevel = (activeFriendChat?.emotionalLevel ?: 1L).coerceAtLeast(1L)
            val animatedVibeFraction by animateFloatAsState(
                targetValue = (goodVibePoints.toFloat() / 100f).coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 450),
                label = "good_vibe_progress"
            )
            val chatListState = rememberLazyListState()
            var lastSeenMessageId by remember(selectedFriend?.userId) { mutableStateOf<String?>(null) }
            LaunchedEffect(chatMessages.size, selectedFriend?.userId) {
                val latest = chatMessages.lastOrNull()
                if (latest != null) {
                    if (lastSeenMessageId != null && latest.id != lastSeenMessageId && latest.senderUid != currentUserId) {
                        viewModel.haptics.chatMessageReceived()
                    }
                    lastSeenMessageId = latest.id
                    chatListState.animateScrollToItem(chatMessages.lastIndex)
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                border = BorderStroke(1.dp, VicdaruSoftPink.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().testTag("music_live_chat_card")
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Chat Musical en Vivo", style = MaterialTheme.typography.titleMedium)
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = "Nivel de Amistad Emocional",
                                    tint = VicdaruSoftPink,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Amistad Emocional • Nv. $emotionalLevel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.testTag("emotional_friendship_level_badge")
                                )
                            }
                        }
                    }

                    // Friend Selector
                    if (friendProfiles.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(friendProfiles, key = { it.userId }) { friend ->
                                FilterChip(
                                    selected = selectedFriend?.userId == friend.userId,
                                    onClick = { viewModel.selectChatFriend(friend.userId) },
                                    label = { Text(friend.displayName) }
                                )
                            }
                        }
                    }

                    // Contador de Buena Vibra
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = VicdaruSkyBlue,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Contador de Buena Vibra",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text(
                                text = "$goodVibePoints / 100",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("good_vibe_progress_bar")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedVibeFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(Brush.horizontalGradient(listOf(VicdaruSkyBlue, VicdaruSoftPink)))
                            )
                        }
                        Text(
                            text = "Cada mensaje suma +25 de Buena Vibra. ¡Al llegar a 100 sube la Amistad Emocional de ambos!",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (selectedFriend == null) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Agrega amigos en la pestaña Perfil con su Código de Amigo para chatear en vivo mientras escuchan música.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        // Live Chat Message Feed
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(185.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f))
                                .padding(10.dp)
                        ) {
                            if (chatMessages.isEmpty()) {
                                Text(
                                    text = "Saluda a ${selectedFriend.displayName} para iniciar el Contador de Buena Vibra ✨",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                val myAvatarSymbol = MedalsAndAvatarsCatalog.avatarById(profile?.selectedAvatarId).badgeSymbol
                                val friendAvatarSymbol = MedalsAndAvatarsCatalog.avatarById(selectedFriend.selectedAvatarId).badgeSymbol
                                LazyColumn(
                                    state = chatListState,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(chatMessages, key = { it.id.ifBlank { "${it.senderUid}_${it.text.hashCode()}" } }) { msg ->
                                        val isMine = msg.senderUid == currentUserId
                                        val senderProfile = if (isMine) profile else {
                                            friendProfiles.firstOrNull { it.userId == msg.senderUid } ?: selectedFriend
                                        }
                                        val senderPhoto = senderProfile?.profilePhotoBase64
                                        val senderSymbol = if (isMine) myAvatarSymbol else friendAvatarSymbol

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            if (!isMine) {
                                                ProfileAvatarBadge(
                                                    photoBase64 = senderPhoto,
                                                    fallbackSymbol = senderSymbol,
                                                    size = 30.dp,
                                                    contentDescription = "Foto de ${msg.senderName}",
                                                    modifier = Modifier.testTag("chat_avatar_${msg.senderUid}")
                                                )
                                                Spacer(Modifier.width(6.dp))
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (isMine) {
                                                    MaterialTheme.colorScheme.primaryContainer
                                                } else {
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                },
                                                modifier = Modifier.widthIn(max = 228.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                                    Text(
                                                        text = "${msg.senderName} • +${msg.vibeBoost} vibra",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = msg.text,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            if (isMine) {
                                                Spacer(Modifier.width(6.dp))
                                                ProfileAvatarBadge(
                                                    photoBase64 = senderPhoto,
                                                    fallbackSymbol = senderSymbol,
                                                    size = 30.dp,
                                                    contentDescription = "Tu foto de perfil",
                                                    modifier = Modifier.testTag("chat_avatar_mine")
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Message Input & Send
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = chatMessageInput,
                                onValueChange = { if (it.length <= 300) chatMessageInput = it },
                                label = { Text("Mensaje para ${selectedFriend.displayName}") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("music_chat_input")
                            )
                            FilledIconButton(
                                onClick = {
                                    val msg = chatMessageInput.trim()
                                    if (msg.isNotEmpty()) {
                                        viewModel.sendFriendChatMessage(msg)
                                        chatMessageInput = ""
                                        viewModel.registerFocusTap()
                                    }
                                },
                                enabled = chatMessageInput.isNotBlank(),
                                modifier = Modifier.size(50.dp).testTag("music_chat_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Enviar mensaje"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Multiplayer Co-Focus / Rhythm Sync Section
        if (activeRoom != null && activeRoom.gameType == "MUSIC_FOCUS") {
            item {
                val isHost = activeRoom.hostUid == currentUserId
                val oppName = if (isHost) activeRoom.guestName.ifBlank { "Esperando amigo..." } else activeRoom.hostName
                val myPts = if (isHost) activeRoom.hostProgress else activeRoom.guestProgress
                val oppPts = if (isHost) activeRoom.guestProgress else activeRoom.hostProgress
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sesión Dúo ${activeRoom.roomCode} • Meta: ${activeRoom.targetGoal} pts", style = MaterialTheme.typography.labelLarge)
                            OutlinedButton(onClick = { viewModel.leaveMultiplayerRoom() }) { Text("Salir") }
                        }
                        Text("Tú: $myPts pts  •  $oppName: $oppPts pts", style = MaterialTheme.typography.bodyMedium)
                        if (activeRoom.status == "active") {
                            Button(
                                onClick = {
                                    viewModel.haptics.socialAction()
                                    val next = myPts + 25L
                                    viewModel.updateMultiplayerProgress(next, finishRoom = next >= activeRoom.targetGoal)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enviar pulso de armonía (+25 pts)")
                            }
                        } else if (activeRoom.status == "finished") {
                            Text(
                                "¡Sesión completada! Medalla 'Armonía Dual' desbloqueada para ambos.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Enfoque Musical Multijugador", style = MaterialTheme.typography.titleMedium)
                        }
                        Button(
                            onClick = {
                                val firstFriend = friendProfiles.firstOrNull()?.userId.orEmpty()
                                viewModel.createMultiplayerRoom("MUSIC_FOCUS", 100L, firstFriend)
                            },
                            modifier = Modifier.fillMaxWidth().testTag("create_music_room_button")
                        ) {
                            Text("Crear sesión sincronizada con amigo")
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinRoomCode,
                                onValueChange = { joinRoomCode = it.uppercase().take(10) },
                                label = { Text("Código de sala") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = { viewModel.joinMultiplayerRoom(joinRoomCode) },
                                enabled = joinRoomCode.length >= 4,
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text("Unirse")
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showCreateProfileDialog) {
        CreateVisualizerProfileDialog(
            onChipTap = { viewModel.haptics.subtleTap() },
            onDismiss = {
                viewModel.haptics.subtleTap()
                showCreateProfileDialog = false
            },
            onSave = { prof ->
                viewModel.saveCustomVisualizerProfile(prof)
                showCreateProfileDialog = false
            }
        )
    }
}

@Composable
private fun CreateVisualizerProfileDialog(
    onChipTap: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (VisualizerProfile) -> Unit,
) {
    var name by remember { mutableStateOf("Mi Vibra Anime") }
    var selectedAnime by remember { mutableStateOf(AnimeSeries.SAKURA) }
    var selectedWave by remember { mutableStateOf(AnimeSeries.SAKURA.defaultWaveformStyle) }
    var selectedSound by remember { mutableStateOf(AmbientSoundType.NIGHT_RAIN) }
    var sensitivity by remember { mutableFloatStateOf(1.25f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalizar Perfil Visual") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text("Nombre del perfil") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Visualizador único:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(VisualizerStyleCatalog.allStyles, key = { it.key }) { styleOpt ->
                        FilterChip(
                            selected = selectedWave == styleOpt.key,
                            onClick = {
                                onChipTap()
                                selectedWave = styleOpt.key
                            },
                            label = { Text(styleOpt.label) }
                        )
                    }
                }
                Text("Sonido relajante base:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AmbientSoundType.entries) { sound ->
                        FilterChip(
                            selected = selectedSound == sound,
                            onClick = {
                                onChipTap()
                                selectedSound = sound
                            },
                            label = { Text(sound.displayName) }
                        )
                    }
                }
                Text("Decoración y personajes de:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AnimeSeries.entries) { series ->
                        FilterChip(
                            selected = selectedAnime == series,
                            onClick = {
                                onChipTap()
                                selectedAnime = series
                                selectedWave = series.defaultWaveformStyle
                            },
                            label = { Text(series.displayName) }
                        )
                    }
                }
                Text("Sensibilidad al ritmo: ${String.format(Locale.US, "%.1fx", sensitivity)}", style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.5f..2.2f
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        VisualizerProfile(
                            name = name.ifBlank { "Perfil Personal" },
                            baseAnimeKey = selectedAnime.key,
                            particleStyle = selectedAnime.defaultParticleStyle,
                            waveformStyle = selectedWave,
                            ambientSound = selectedSound.key,
                            sensitivity = sensitivity.toDouble(),
                            primaryHex = selectedAnime.primaryHex,
                            secondaryHex = selectedAnime.secondaryHex
                        )
                    )
                }
            ) {
                Text("Guardar perfil")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
