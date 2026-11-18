package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.signOutUser
import com.example.model.FriendRequest
import com.example.model.MedalsAndAvatarsCatalog
import com.example.model.PublicProfile
import com.example.model.UserSettings
import com.example.notifications.QuoteScheduler
import com.example.ui.components.VicdaruBrandHeader
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink
import com.example.util.ProfileAvatarBadge
import com.example.util.ProfileImageUtils
import com.example.viewmodel.VicdaruMainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ProfileSocialScreen(
    viewModel: VicdaruMainViewModel,
    profile: PublicProfile?,
    settings: UserSettings?,
    friendProfiles: List<PublicProfile>,
    incomingRequests: List<FriendRequest>,
    onInviteToGame: (gameType: String, friendUid: String) -> Unit,
    onReplayOnboarding: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var friendCodeInput by remember { mutableStateOf("") }
    var morningHour by remember(settings?.morningHour) { mutableIntStateOf((settings?.morningHour ?: 8L).toInt()) }
    var morningMinute by remember(settings?.morningMinute) { mutableIntStateOf((settings?.morningMinute ?: 0L).toInt()) }

    val achievements by viewModel.achievementsState.collectAsStateWithLifecycle()
    val effectiveAchievements = remember(achievements, profile, friendProfiles) {
        achievements.ifEmpty {
            MedalsAndAvatarsCatalog.evaluateAchievements(
                profile = profile,
                liveFriendsCount = friendProfiles.size
            )
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val base64 = withContext(Dispatchers.IO) {
                    ProfileImageUtils.encodeProfilePhotoToBase64(context, uri)
                }
                if (base64 != null) {
                    viewModel.updateProfilePhoto(base64)
                } else {
                    viewModel.haptics.errorNotice()
                    viewModel.postMessage("No se pudo procesar la foto seleccionada.")
                }
            }
        }
    }

    val selectedAvatar = MedalsAndAvatarsCatalog.avatarById(profile?.selectedAvatarId)
    val unlockedCount = effectiveAchievements.count { it.hasAnyLevel }
    val totalUnlockedLevels = effectiveAchievements.sumOf { it.unlockedLevel }
    val totalPossibleLevels = effectiveAchievements.sumOf { it.definition.maxLevel }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            VicdaruBrandHeader(
                title = "Perfil & Comunidad",
                subtitle = "Medallas, amigos y configuración",
                trailingContent = {
                    IconButton(
                        onClick = {
                            viewModel.haptics.softDelete()
                            signOutUser(credentialManager, scope)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize().testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }

        // User Profile Summary Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
                border = BorderStroke(
                    1.dp,
                    Brush.linearGradient(listOf(VicdaruSkyBlue.copy(alpha = 0.6f), VicdaruSoftPink.copy(alpha = 0.6f)))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProfileAvatarBadge(
                            photoBase64 = profile?.profilePhotoBase64,
                            fallbackSymbol = selectedAvatar.badgeSymbol,
                            size = 64.dp,
                            contentDescription = "Tu foto de perfil",
                            modifier = Modifier
                                .clickable {
                                    viewModel.haptics.subtleTap()
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("profile_avatar_badge")
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile?.displayName ?: "Viajero Estelar",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Nivel ${profile?.activityLevel ?: 1} • ${selectedAvatar.name}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = {
                                        viewModel.haptics.subtleTap()
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.testTag("pick_profile_photo_button")
                                ) {
                                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (profile?.profilePhotoBase64.isNullOrBlank()) "Foto de galería" else "Cambiar foto")
                                }
                                if (!profile?.profilePhotoBase64.isNullOrBlank()) {
                                    TextButton(
                                        onClick = { viewModel.updateProfilePhoto("") },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("clear_profile_photo_button")
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Usar símbolo")
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatChip("Foco", "${profile?.totalFocusMinutes ?: 0}m", Modifier.weight(1f))
                        StatChip("Escalada", "${profile?.climberBestMeters ?: 0}m", Modifier.weight(1f))
                        StatChip("Desafío", "${profile?.dailyChallengeScore ?: 0}pts", Modifier.weight(1f))
                    }

                    val myCode = profile?.friendCode ?: "VD-..."
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tu código de amigo", style = MaterialTheme.typography.labelSmall)
                                Text(myCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            FilledTonalButton(
                                onClick = {
                                    viewModel.haptics.shareOrCopy()
                                    clipboard.setText(AnnotatedString(myCode))
                                    viewModel.postMessage("Código $myCode copiado")
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copiar")
                            }
                        }
                    }
                }
            }
        }

        // Dark Mode & Push Notification Controls
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Modo Oscuro Nocturno", style = MaterialTheme.typography.bodyLarge)
                                Text("Confort visual para noches", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Switch(
                            checked = settings?.darkMode ?: true,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Frase inspiradora matutina", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = String.format(Locale.US, "Cada día a las %02d:%02d", morningHour, morningMinute),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Switch(
                            checked = settings?.morningQuoteEnabled ?: true,
                            onCheckedChange = { enabled ->
                                viewModel.haptics.toggleSwitch(enabled)
                                val rand = settings?.randomQuotesEnabled ?: true
                                viewModel.updateNotificationSettings(enabled, morningHour, morningMinute, rand)
                                QuoteScheduler.syncSchedules(context, enabled, morningHour, morningMinute, rand)
                            }
                        )
                    }

                    // Hour adjuster for morning quote
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hora matutina:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = {
                            morningHour = (morningHour + 23) % 24
                            val mEn = settings?.morningQuoteEnabled ?: true
                            val rEn = settings?.randomQuotesEnabled ?: true
                            viewModel.updateNotificationSettings(mEn, morningHour, morningMinute, rEn)
                            QuoteScheduler.syncSchedules(context, mEn, morningHour, morningMinute, rEn)
                        }) { Text("-1h") }
                        Text(String.format(Locale.US, "%02d:%02d", morningHour, morningMinute), fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = {
                            morningHour = (morningHour + 1) % 24
                            val mEn = settings?.morningQuoteEnabled ?: true
                            val rEn = settings?.randomQuotesEnabled ?: true
                            viewModel.updateNotificationSettings(mEn, morningHour, morningMinute, rEn)
                            QuoteScheduler.syncSchedules(context, mEn, morningHour, morningMinute, rEn)
                        }) { Text("+1h") }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recordatorios aleatorios diurnos", style = MaterialTheme.typography.bodyLarge)
                            Text("Citas + imagen anime en horas productivas", style = MaterialTheme.typography.labelSmall)
                        }
                        Switch(
                            checked = settings?.randomQuotesEnabled ?: true,
                            onCheckedChange = { enabled ->
                                viewModel.haptics.toggleSwitch(enabled)
                                val mEn = settings?.morningQuoteEnabled ?: true
                                viewModel.updateNotificationSettings(mEn, morningHour, morningMinute, enabled)
                                QuoteScheduler.syncSchedules(context, mEn, morningHour, morningMinute, enabled)
                            }
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            viewModel.haptics.subtleTap()
                            onReplayOnboarding()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("replay_onboarding_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Guía interactiva de Vicdaru")
                    }
                }
            }
        }

        // Multi-Tier Medals & Achievements Showcase (up to 10 levels scaling 1.5x)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Medallas y Logros ($unlockedCount/${effectiveAchievements.size} • Rangos $totalUnlockedLevels/$totalPossibleLevels)",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Logros progresivos de hasta 10 niveles con meta creciente 1.5x",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(effectiveAchievements, key = { it.definition.id }) { progress ->
                    val medal = progress.definition
                    val unlocked = progress.hasAnyLevel
                    val tierLabel = when {
                        medal.maxLevel == 1 -> if (unlocked) "Hito completado" else "Hito único"
                        progress.isMaxed -> "Nv. Máx (${medal.maxLevel}/${medal.maxLevel})"
                        else -> "Nv. ${progress.unlockedLevel}/${medal.maxLevel}"
                    }
                    val targetText = when {
                        progress.isMaxed -> "¡Completado! (${progress.currentValue} ${medal.unitLabel})"
                        progress.nextLevelTarget != null -> "${progress.currentValue} / ${progress.nextLevelTarget} ${medal.unitLabel}"
                        else -> "${progress.currentValue}"
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.width(220.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (unlocked) Icons.Filled.EmojiEvents else Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = if (unlocked) VicdaruSoftPink else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (medal.isDuoMedal) "DÚO • ${medal.category}" else medal.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = tierLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(medal.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = medal.description,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            LinearProgressIndicator(
                                progress = { progress.progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = if (unlocked) MaterialTheme.colorScheme.primary else VicdaruSkyBlue,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = targetText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (progress.isMaxed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Friends, Requests & Daily Challenge Competition
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Amistades y Desafío Diario", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = friendCodeInput,
                            onValueChange = { friendCodeInput = it.uppercase().take(20) },
                            label = { Text("Código (ej. VD-XXXXXX)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("add_friend_code_input")
                        )
                        Button(
                            onClick = {
                                viewModel.sendFriendRequest(friendCodeInput)
                                friendCodeInput = ""
                            },
                            enabled = friendCodeInput.length >= 4,
                            modifier = Modifier.height(52.dp).testTag("send_friend_request_button")
                        ) {
                            Text("Agregar")
                        }
                    }

                    if (incomingRequests.isNotEmpty()) {
                        Text("Solicitudes recibidas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        incomingRequests.forEach { req ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(req.fromDisplayName, fontWeight = FontWeight.SemiBold)
                                        Text(req.fromFriendCode, style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { viewModel.respondToFriendRequest(req, true) }) {
                                        Icon(Icons.Filled.Check, contentDescription = "Aceptar amistad", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.respondToFriendRequest(req, false) }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Rechazar amistad", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    if (friendProfiles.isEmpty()) {
                        Text(
                            text = "Comparte tu código de amigo para competir en desafíos diarios y jugar minijuegos en dúo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Tabla de Amigos", style = MaterialTheme.typography.labelLarge)
                        friendProfiles.sortedByDescending { it.dailyChallengeScore }.forEach { friend ->
                            val av = MedalsAndAvatarsCatalog.avatarById(friend.selectedAvatarId)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ProfileAvatarBadge(
                                            photoBase64 = friend.profilePhotoBase64,
                                            fallbackSymbol = av.badgeSymbol,
                                            size = 38.dp,
                                            contentDescription = "Foto de ${friend.displayName}"
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(friend.displayName, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Nvl ${friend.activityLevel} • Desafío: ${friend.dailyChallengeScore} pts • Foco: ${friend.totalFocusMinutes}m",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                viewModel.haptics.socialAction()
                                                onInviteToGame("CLIMBER", friend.userId)
                                            },
                                            leadingIcon = { Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            label = { Text("Retar Escalada") }
                                        )
                                        FilterChip(
                                            selected = false,
                                            onClick = {
                                                viewModel.haptics.socialAction()
                                                onInviteToGame("MUSIC_FOCUS", friend.userId)
                                            },
                                            leadingIcon = { Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                            label = { Text("Foco Dúo") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
