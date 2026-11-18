package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.ClimberSoundEngine
import com.example.game.ClimberItemType
import com.example.game.ClimberPhysicsEngine
import com.example.game.ClimberPhysicsEvent
import com.example.model.GameRoom
import com.example.model.MedalsAndAvatarsCatalog
import com.example.model.PublicProfile
import com.example.ui.components.drawAnimeCharacterOrb
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink
import com.example.viewmodel.VicdaruMainViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClimberGameScreen(
    viewModel: VicdaruMainViewModel,
    profile: PublicProfile?,
    friendProfiles: List<PublicProfile>,
    activeRoom: GameRoom?,
    currentUserId: String,
) {
    val profileAvatar = MedalsAndAvatarsCatalog.avatarById(profile?.selectedAvatarId)
    val unlockedIds = profile?.unlockedAvatars ?: listOf("sakura")
    var activeBallAvatarId by remember(profileAvatar.id) { mutableStateOf(profileAvatar.id) }
    val selectedAvatar = MedalsAndAvatarsCatalog.avatarById(activeBallAvatarId)
    val dailyGoalMeters = 250

    val haptics = viewModel.haptics
    val soundEngine = remember { ClimberSoundEngine() }
    val engine = remember { ClimberPhysicsEngine(dailyGoalMeters = dailyGoalMeters) }
    val scope = rememberCoroutineScope()

    var isSoundMuted by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var meters by remember { mutableIntStateOf(0) }
    var speedBoostRemainingMs by remember { mutableLongStateOf(0L) }
    var freezeRemainingMs by remember { mutableLongStateOf(0L) }
    var stunRemainingMs by remember { mutableLongStateOf(0L) }
    var renderFrameTick by remember { mutableLongStateOf(0L) }
    var lastSyncedMeters by remember { mutableIntStateOf(0) }
    var joinCodeInput by remember { mutableStateOf("") }

    // Jetpack Compose Spring physics animatables for elastic orb squash-stretch & hazard recoil
    val bounceScaleY = remember { Animatable(1f) }
    val impactTremor = remember { Animatable(0f) }

    DisposableEffect(Unit) {
        onDispose { soundEngine.release() }
    }

    fun triggerElasticBounce(squashFactor: Float = 0.68f) {
        scope.launch {
            bounceScaleY.snapTo(squashFactor)
            bounceScaleY.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    fun triggerImpactTremor(strength: Float = 1f) {
        scope.launch {
            impactTremor.snapTo(strength)
            impactTremor.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
        }
    }

    fun handlePhysicsEvents(events: List<ClimberPhysicsEvent>) {
        events.forEach { event ->
            when (event) {
                ClimberPhysicsEvent.GameStarted -> {
                    haptics.subtleBounce()
                    soundEngine.playStart()
                    triggerElasticBounce(0.72f)
                }
                ClimberPhysicsEvent.SteerMoved -> {
                    soundEngine.playMove()
                    haptics.climberSteer()
                }
                is ClimberPhysicsEvent.Bounced -> {
                    triggerElasticBounce(if (event.isBoostPlatform) 0.62f else 0.72f)
                    if (event.isBoostPlatform) {
                        haptics.speedBoost()
                        soundEngine.playBounce(isBoostPlatform = true)
                    } else {
                        haptics.subtleBounce()
                        soundEngine.playBounce(isBoostPlatform = false)
                    }
                }
                ClimberPhysicsEvent.SpeedBoostPickedUp -> {
                    triggerElasticBounce(0.58f)
                    haptics.speedBoost()
                    soundEngine.playSpeedBoost()
                }
                ClimberPhysicsEvent.PauseTrapTriggered -> {
                    triggerImpactTremor(1.1f)
                    haptics.trapFreeze()
                    soundEngine.playTrapFreeze()
                }
                ClimberPhysicsEvent.PauseTrapReleased -> {
                    triggerElasticBounce(0.70f)
                    haptics.subtleBounce()
                    soundEngine.playTrapRelease()
                }
                ClimberPhysicsEvent.StunTriggered -> {
                    triggerImpactTremor(1.3f)
                    haptics.stunEffect()
                    soundEngine.playStun()
                }
                ClimberPhysicsEvent.DailyChallengeReached -> {
                    haptics.climberUnlock()
                    soundEngine.playChallengeComplete()
                }
                is ClimberPhysicsEvent.GameOver -> {
                    isPlaying = false
                    isGameOver = true
                    triggerImpactTremor(0.9f)
                    haptics.climberGameOver()
                    soundEngine.playGameOver()
                    viewModel.recordClimberResult(
                        meters = event.finalMeters,
                        completedDailyChallenge = event.finalMeters >= dailyGoalMeters
                    )
                }
            }
        }
    }

    fun resetGame() {
        lastSyncedMeters = 0
        val startEvents = engine.startNewRun()
        isPlaying = true
        isGameOver = false
        meters = engine.meters
        speedBoostRemainingMs = 0L
        freezeRemainingMs = 0L
        stunRemainingMs = 0L
        handlePhysicsEvents(startEvents)
    }

    // Jetpack Compose Choreographer frame clock (`withFrameNanos`) physics loop
    LaunchedEffect(isPlaying) {
        var prevFrameNanos = 0L
        while (isActive) {
            withFrameNanos { frameNanos ->
                val dtSec = if (prevFrameNanos == 0L) 0.016f
                else ((frameNanos - prevFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.035f)
                prevFrameNanos = frameNanos

                if (isPlaying) {
                    val events = engine.step(dtSec)
                    meters = engine.meters
                    speedBoostRemainingMs = engine.speedBoostRemainingMs
                    freezeRemainingMs = engine.freezeRemainingMs
                    stunRemainingMs = engine.stunRemainingMs
                    renderFrameTick += 1L

                    if (events.isNotEmpty()) {
                        handlePhysicsEvents(events)
                    }

                    val room = activeRoom
                    if (room != null && room.gameType == "CLIMBER" && room.status == "active" &&
                        (meters - lastSyncedMeters >= 20 || meters >= room.targetGoal.toInt())
                    ) {
                        lastSyncedMeters = meters
                        val done = meters >= room.targetGoal.toInt()
                        viewModel.updateMultiplayerProgress(meters.toLong(), finishRoom = done)
                        if (done) {
                            engine.stopRun()
                            isPlaying = false
                        }
                    }
                } else {
                    engine.tickIdle(dtSec)
                    renderFrameTick += 1L
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header stats & Daily Challenge bar
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
                            Text("Escalada: ${selectedAvatar.name}", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${selectedAvatar.anime.displayName} • Récord: ${profile?.climberBestMeters ?: 0} m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    isSoundMuted = !isSoundMuted
                                    soundEngine.isMuted = isSoundMuted
                                    haptics.toggleSwitch(!isSoundMuted)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("climber_sound_toggle")
                            ) {
                                Icon(
                                    imageVector = if (isSoundMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = if (isSoundMuted) "Activar efectos de sonido" else "Silenciar efectos de sonido",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "$meters m",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    LinearProgressIndicator(
                        progress = { (meters.toFloat() / dailyGoalMeters.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50))
                    )
                }
            }
        }

        // Active Multiplayer Duel/Co-op Banner if connected
        if (activeRoom != null && activeRoom.gameType == "CLIMBER") {
            item {
                val isHost = activeRoom.hostUid == currentUserId
                val opponentName = if (isHost) activeRoom.guestName.ifBlank { "Esperando amigo..." } else activeRoom.hostName
                val opponentMeters = if (isHost) activeRoom.guestProgress else activeRoom.hostProgress
                val myRoomMeters = if (isHost) activeRoom.hostProgress else activeRoom.guestProgress

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sala ${activeRoom.roomCode} • Meta: ${activeRoom.targetGoal} m",
                                style = MaterialTheme.typography.labelLarge
                            )
                            OutlinedButton(onClick = { viewModel.leaveMultiplayerRoom() }) {
                                Text("Salir")
                            }
                        }
                        Text(
                            text = "Tú: $myRoomMeters m  vs  $opponentName: $opponentMeters m",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (activeRoom.status == "finished") {
                            Text(
                                text = "¡Meta compartida completada! Medalla 'Cima Compartida' otorgada.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Interactive Climbing Canvas
        item {
            val primary = selectedAvatar.anime.darkPrimaryColor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(355.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF0A1224), Color(0xFF131D35), Color(0xFF0B1120))))
                    .pointerInput(isPlaying) {
                        detectTapGestures { offset ->
                            val ev = engine.applySteerImpulse(goLeft = offset.x < size.width / 2f)
                            if (ev != null) handlePhysicsEvents(listOf(ev))
                        }
                    }
                    .testTag("climber_game_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Read frame tick so Compose redraws smoothly on every Choreographer frame
                    @Suppress("UNUSED_VARIABLE")
                    val frame = renderFrameTick
                    val shakeOffset = sin(engine.animTimeSec * 42f) * impactTremor.value * 5.dp.toPx()

                    // Subtle sky-blue to soft-pink summit grid
                    for (g in 1..4) {
                        val y = size.height * (g / 5f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.06f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Bounce ripples
                    engine.bounceRipples.forEach { ripple ->
                        val rx = size.width * ripple.xRatio + shakeOffset
                        val ry = size.height * ripple.yRatio
                        val radius = (14f + ripple.progress * 34f).dp.toPx()
                        drawCircle(
                            color = ripple.color.copy(alpha = (1f - ripple.progress) * 0.7f),
                            radius = radius,
                            center = Offset(rx, ry),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }

                    // Platforms
                    engine.platforms.forEach { p ->
                        val pw = size.width * p.widthRatio
                        val ph = 11.dp.toPx()
                        val px = (size.width * p.xRatio) - pw / 2f + shakeOffset
                        val py = size.height * p.yRatio
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                if (p.isBoost) listOf(VicdaruSoftPink, Color(0xFFFDE047))
                                else listOf(VicdaruSkyBlue, primary)
                            ),
                            topLeft = Offset(px, py),
                            size = Size(pw, ph),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }

                    // Boosters, pause traps, and rare stun hazards
                    engine.items.forEach { item ->
                        val ix = size.width * item.xRatio + shakeOffset
                        val bob = sin(engine.animTimeSec * 4.5f + item.id) * 4.dp.toPx()
                        val iy = size.height * item.yRatio + bob
                        val itemRadius = 12.dp.toPx()
                        when (item.type) {
                            ClimberItemType.SPEED_BOOST -> {
                                drawCircle(Color(0xFF38BDF8).copy(alpha = 0.25f), itemRadius * 1.6f, Offset(ix, iy))
                                val arrow = Path().apply {
                                    moveTo(ix, iy - itemRadius)
                                    lineTo(ix + itemRadius * 0.85f, iy + itemRadius * 0.65f)
                                    lineTo(ix, iy + itemRadius * 0.2f)
                                    lineTo(ix - itemRadius * 0.85f, iy + itemRadius * 0.65f)
                                    close()
                                }
                                drawPath(arrow, Brush.verticalGradient(listOf(Color(0xFF7DD3FC), Color(0xFFFDE047))))
                            }
                            ClimberItemType.PAUSE_TRAP -> {
                                drawCircle(Color(0xFFF59E0B).copy(alpha = 0.26f), itemRadius * 1.55f, Offset(ix, iy))
                                val r = itemRadius * 0.9f
                                drawRoundRect(
                                    color = Color(0xFFFBBF24),
                                    topLeft = Offset(ix - r, iy - r),
                                    size = Size(r * 2f, r * 2f),
                                    cornerRadius = CornerRadius(6f, 6f),
                                    style = Stroke(width = 2.6.dp.toPx())
                                )
                                drawLine(Color(0xFFFDE68A), Offset(ix - 3.dp.toPx(), iy - 5.dp.toPx()), Offset(ix - 3.dp.toPx(), iy + 5.dp.toPx()), 2.5.dp.toPx())
                                drawLine(Color(0xFFFDE68A), Offset(ix + 3.dp.toPx(), iy - 5.dp.toPx()), Offset(ix + 3.dp.toPx(), iy + 5.dp.toPx()), 2.5.dp.toPx())
                            }
                            ClimberItemType.STUN -> {
                                drawCircle(Color(0xFFC084FC).copy(alpha = 0.28f), itemRadius * 1.65f, Offset(ix, iy))
                                val diamond = Path().apply {
                                    moveTo(ix, iy - itemRadius)
                                    lineTo(ix + itemRadius * 0.9f, iy)
                                    lineTo(ix, iy + itemRadius)
                                    lineTo(ix - itemRadius * 0.9f, iy)
                                    close()
                                }
                                drawPath(diamond, Color(0xFFD946EF))
                                drawCircle(Color.White, 3.5.dp.toPx(), Offset(ix, iy))
                            }
                        }
                    }

                    // Multiplayer opponent orb
                    if (activeRoom != null && activeRoom.status == "active") {
                        val isHost = activeRoom.hostUid == currentUserId
                        val oppMeters = if (isHost) activeRoom.guestProgress else activeRoom.hostProgress
                        val oppAvatarId = if (isHost) activeRoom.guestAvatarId else activeRoom.hostAvatarId
                        val oppAvatar = MedalsAndAvatarsCatalog.avatarById(oppAvatarId)
                        val diffRatio = ((meters - oppMeters) / 120f).coerceIn(-0.4f, 0.4f)
                        val oppY = (engine.playerY + diffRatio).coerceIn(0.1f, 0.9f)
                        drawAnimeCharacterOrb(
                            anime = oppAvatar.anime,
                            center = Offset(size.width * 0.78f + shakeOffset, size.height * oppY),
                            baseRadiusPx = 14.dp.toPx(),
                            scaleX = 1f,
                            scaleY = 1f,
                            animTimeSec = engine.animTimeSec,
                            alpha = 0.62f
                        )
                    }

                    // Player anime character orb driven by Compose Spring squash-stretch + velocity stretch
                    val cx = size.width * engine.playerX + shakeOffset
                    val cy = size.height * engine.playerY
                    val baseR = 18.dp.toPx()

                    val springSy = bounceScaleY.value
                    val velStretch = (-engine.velocityY * 0.08f).coerceIn(-0.10f, 0.16f)
                    val scaleY = (springSy + velStretch).coerceIn(0.60f, 1.38f)
                    val scaleX = (1f / scaleY).coerceIn(0.72f, 1.42f)

                    if (speedBoostRemainingMs > 0L) {
                        for (t in 1..3) {
                            drawCircle(
                                color = Color(0xFF38BDF8).copy(alpha = 0.36f - t * 0.09f),
                                radius = (baseR * (1f - t * 0.18f)),
                                center = Offset(cx, cy + t * 14.dp.toPx())
                            )
                        }
                    }

                    drawAnimeCharacterOrb(
                        anime = selectedAvatar.anime,
                        center = Offset(cx, cy),
                        baseRadiusPx = baseR,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        animTimeSec = engine.animTimeSec,
                        isFrozen = freezeRemainingMs > 0L,
                        isStunned = stunRemainingMs > 0L,
                        isSpeedBoosted = speedBoostRemainingMs > 0L
                    )

                    if (freezeRemainingMs > 0L) {
                        val sweep = 360f * (freezeRemainingMs.toFloat() / ClimberPhysicsEngine.PAUSE_TRAP_DURATION_MS.toFloat())
                        drawArc(
                            color = Color(0xFFFBBF24),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(cx - 26.dp.toPx(), cy - 26.dp.toPx()),
                            size = Size(52.dp.toPx(), 52.dp.toPx()),
                            style = Stroke(width = 3.5.dp.toPx())
                        )
                    }

                    if (stunRemainingMs > 0L) {
                        for (s in 0..2) {
                            val angle = engine.animTimeSec * 7f + s * (2.0 * PI / 3.0)
                            val sx = cx + (cos(angle) * 26.dp.toPx()).toFloat()
                            val sy = cy - 20.dp.toPx() + (sin(angle) * 7.dp.toPx()).toFloat()
                            drawCircle(Color(0xFFF0ABFC), 4.dp.toPx(), Offset(sx, sy))
                        }
                    }
                }

                // Active Status HUD Pills inside canvas
                if (isPlaying) {
                    Row(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (speedBoostRemainingMs > 0L) {
                            StatusPill("⚡ Velocidad (${((speedBoostRemainingMs + 999L) / 1000L)}s)", Color(0xFF0284C7))
                        }
                        if (freezeRemainingMs > 0L) {
                            StatusPill("⏸ Detenido (${((freezeRemainingMs + 999L) / 1000L)}s)", Color(0xFFD97706))
                        }
                        if (stunRemainingMs > 0L) {
                            StatusPill("💫 ¡Aturdido! (${((stunRemainingMs + 999L) / 1000L)}s)", Color(0xFFA21CAF))
                        }
                    }
                }

                if (!isPlaying) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(20.dp)
                        ) {
                            if (isGameOver) {
                                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = VicdaruSoftPink, modifier = Modifier.size(38.dp))
                            } else {
                                Canvas(modifier = Modifier.size(64.dp)) {
                                    @Suppress("UNUSED_VARIABLE")
                                    val tick = renderFrameTick
                                    drawAnimeCharacterOrb(
                                        anime = selectedAvatar.anime,
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        baseRadiusPx = 19.dp.toPx(),
                                        scaleX = 1f,
                                        scaleY = 1f,
                                        animTimeSec = engine.animTimeSec
                                    )
                                }
                            }
                            Text(
                                text = if (isGameOver) "¡Alcanzaste $meters m!" else "Escala con ${selectedAvatar.name}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = "Toca izquierda/derecha o usa los botones. Atrapa ⚡ impulsos y esquiva ⏸ trampas y 💫 aturdimientos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFCBD5E1)
                            )
                            Button(
                                onClick = { resetGame() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.testTag("start_climber_button")
                            ) {
                                Icon(if (isGameOver) Icons.Filled.Refresh else Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text(if (isGameOver) "Volver a escalar" else "Iniciar escalada")
                            }
                        }
                    }
                }
            }
        }

        // Legend & Steering Controls
        item {
            Text(
                text = "⚡ Impulso rápido  •  ⏸ Trampa (detiene 2s)  •  💫 Aturdimiento (raro)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    onClick = {
                        val ev = engine.applySteerImpulse(goLeft = true)
                        if (ev != null) handlePhysicsEvents(listOf(ev))
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(52.dp).testTag("climber_left_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mover izquierda")
                    Text("Izquierda")
                }
                FilledTonalButton(
                    onClick = {
                        val ev = engine.applySteerImpulse(goLeft = false)
                        if (ev != null) handlePhysicsEvents(listOf(ev))
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(52.dp).testTag("climber_right_button")
                ) {
                    Text("Derecha")
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mover derecha")
                }
            }
        }

        // Unlockable Anime Character Orbs Carousel
        item {
            Text("Diseños de Bola por Anime y Avatares", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Cada anime tiene sus rasgos icónicos. Desbloquea avatares permanentes al subir metros.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MedalsAndAvatarsCatalog.avatars) { avatar ->
                    val unlocked = unlockedIds.contains(avatar.id)
                    val selected = activeBallAvatarId == avatar.id
                    FilterChip(
                        selected = selected,
                        onClick = {
                            activeBallAvatarId = avatar.id
                            soundEngine.playStart()
                            haptics.subtleBounce()
                            if (unlocked) {
                                viewModel.selectAvatar(avatar.id)
                            } else {
                                viewModel.postMessage("Vista previa de ${avatar.name} (desbloqueo permanente a los ${avatar.unlockMeters} m)")
                            }
                        },
                        leadingIcon = {
                            Canvas(modifier = Modifier.size(22.dp)) {
                                drawAnimeCharacterOrb(
                                    anime = avatar.anime,
                                    center = Offset(size.width / 2f, size.height / 2f),
                                    baseRadiusPx = 7.dp.toPx(),
                                    scaleX = 1f,
                                    scaleY = 1f,
                                    animTimeSec = engine.animTimeSec
                                )
                            }
                        },
                        label = {
                            Text(if (unlocked) avatar.name else "${avatar.name} (${avatar.unlockMeters}m)")
                        },
                        modifier = Modifier.testTag("anime_orb_chip_${avatar.id}")
                    )
                }
            }
        }

        // Multiplayer Lobby Controls
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
                        Text("Multijugador con Amigos (Meta 300 m)", style = MaterialTheme.typography.titleMedium)
                    }
                    Button(
                        onClick = {
                            val firstFriendUid = friendProfiles.firstOrNull()?.userId.orEmpty()
                            viewModel.createMultiplayerRoom("CLIMBER", 300L, firstFriendUid)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("create_climber_room_button")
                    ) {
                        Text("Crear sala")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = joinCodeInput,
                            onValueChange = { joinCodeInput = it.uppercase().take(10) },
                            label = { Text("Código de sala") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("climber_room_code_input")
                        )
                        FilledTonalButton(
                            onClick = { viewModel.joinMultiplayerRoom(joinCodeInput) },
                            enabled = joinCodeInput.length >= 4,
                            modifier = Modifier.height(52.dp).testTag("join_climber_room_button")
                        ) {
                            Text("Unirse")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatusPill(text: String, bgColor: Color) {
    Surface(shape = RoundedCornerShape(50), color = bgColor.copy(alpha = 0.90f)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
