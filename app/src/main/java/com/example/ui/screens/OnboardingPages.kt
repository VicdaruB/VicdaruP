package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.HapticsHelper
import com.example.model.AnimeSeries
import com.example.model.MedalsAndAvatarsCatalog
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
internal fun OnboardingPulsoPage(
    selectedAnime: AnimeSeries,
    accentPrimary: Color,
    accentSecondary: Color,
    onSelectAnime: (AnimeSeries) -> Unit,
) {
    val sampleQuote = remember(selectedAnime) {
        when (selectedAnime) {
            AnimeSeries.SAKURA -> "«Todo estará bien sin importar lo difícil que parezca hoy.»" to "Sakura Kinomoto"
            AnimeSeries.FRIEREN -> "«Los pequeños momentos de hoy se vuelven tesoros eternos.»" to "Frieren"
            AnimeSeries.APOTHECARY -> "«Observa cada detalle con calma y hallarás la respuesta.»" to "Maomao"
            AnimeSeries.DANDADAN -> "«¡Da el salto incluso cuando el ritmo parezca imposible!»" to "Momo & Okarun"
            AnimeSeries.LITTLE_WITCH -> "«Un corazón que cree en su magia nunca deja de brillar.»" to "Akko Kagari"
            AnimeSeries.RANMA -> "«Cada tropiezo en el dojo te vuelve más ágil y fuerte.»" to "Ranma Saotome"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = accentPrimary.copy(alpha = 0.16f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = accentPrimary, modifier = Modifier.size(16.dp))
                Text("1 de 4 • Pulso Diario & Hábitos", style = MaterialTheme.typography.labelLarge, color = accentPrimary)
            }
        }

        Text(
            text = "Inspiración anime que transforma tu app",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Cada cita motivacional descarga arte nuevo de múltiples fuentes y cambia automáticamente los colores de Vicdaru según el anime. Además, registra tus hábitos en el tablero mensual.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(accentPrimary, accentSecondary))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Pruébalo ahora: toca un anime para ver el cambio de tema",
                    style = MaterialTheme.typography.labelLarge,
                    color = accentPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(AnimeSeries.entries, key = { it.key }) { series ->
                        FilterChip(
                            selected = selectedAnime == series,
                            onClick = { onSelectAnime(series) },
                            label = { Text(series.leadCharacter) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    }
                }
                AnimatedContent(targetState = selectedAnime, label = "onboarding_quote_preview") { anime ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = anime.primaryColor.copy(alpha = 0.13f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "${anime.displayName} • ${anime.presetName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = anime.primaryColor
                            )
                            Text(
                                text = sampleQuote.first,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                            Text(
                                text = "— ${sampleQuote.second}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnboardingArcadeFocusPage(
    haptics: HapticsHelper,
    accentPrimary: Color,
    accentSecondary: Color,
) {
    var pulseCount by remember { mutableIntStateOf(0) }
    var isDeepFlow by remember { mutableStateOf(false) }
    val pulseScale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = accentSecondary.copy(alpha = 0.16f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = accentSecondary, modifier = Modifier.size(16.dp))
                Text("2 de 4 • Arcade & Enfoque Musical", style = MaterialTheme.typography.labelLarge, color = accentSecondary)
            }
        }

        Text(
            text = "Juega, concéntrate y desbloquea estilos",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Sube de altura en el Minijuego de Escalada con tus orbes anime o entra al estudio con 6 visualizadores únicos que reaccionan al Pomodoro y a tu música.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1528)),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(accentPrimary, accentSecondary))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(125.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF090F1E))
                        .clickable(role = Role.Button) {
                            pulseCount++
                            haptics.subtleBounce()
                            scope.launch {
                                pulseScale.snapTo(1.38f)
                                pulseScale.animateTo(
                                    targetValue = 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        }
                        .testTag("onboarding_pulse_preview"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = 32.dp.toPx() * pulseScale.value
                        val bars = 14
                        for (i in 0 until bars) {
                            val angle = (i.toFloat() / bars) * (2.0 * PI) + pulseCount * 0.25
                            val inner = r * 0.72f
                            val outer = r * (1.15f + ((i + pulseCount) % 3) * 0.18f)
                            drawLine(
                                brush = Brush.linearGradient(listOf(accentPrimary, accentSecondary)),
                                start = Offset(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat()),
                                end = Offset(cx + (cos(angle) * outer).toFloat(), cy + (sin(angle) * outer).toFloat()),
                                strokeWidth = 3.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(
                            color = accentPrimary.copy(alpha = 0.85f),
                            radius = r * 0.56f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.5.dp.toPx())
                        )
                    }
                    Text(
                        text = if (isDeepFlow) "★ Enfoque Profundo" else "⚡ Toca para pulsar ($pulseCount)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = !isDeepFlow,
                        onClick = {
                            haptics.subtleTap()
                            isDeepFlow = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Terrain, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Escalada & Impulsos") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                    FilterChip(
                        selected = isDeepFlow,
                        onClick = {
                            haptics.focusTimerToggle(true)
                            isDeepFlow = true
                        },
                        leadingIcon = { Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("6 Visualizadores") },
                        modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                    )
                }
            }
        }
    }
}

@Composable
internal fun OnboardingMedalsPage(
    haptics: HapticsHelper,
    accentPrimary: Color,
    accentSecondary: Color,
) {
    val sampleMedal = remember {
        MedalsAndAvatarsCatalog.medals.first { it.id == "quotes_collector" }
    }
    var simulatedLevel by remember { mutableFloatStateOf(2f) }
    val levelInt = simulatedLevel.roundToInt().coerceIn(1, sampleMedal.maxLevel)
    val requiredForLevel = sampleMedal.thresholdForLevel(levelInt)
    val nextLevelReq = if (levelInt < sampleMedal.maxLevel) sampleMedal.thresholdForLevel(levelInt + 1) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = accentPrimary.copy(alpha = 0.16f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = accentPrimary, modifier = Modifier.size(16.dp))
                Text("3 de 4 • Sistema de Medallas (10 Niveles)", style = MaterialTheme.typography.labelLarge, color = accentPrimary)
            }
        }

        Text(
            text = "Medallas de hasta 10 niveles con escala 1.5x",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Los hitos únicos (como tu primera frase o primer amigo) tienen 1 nivel. Los logros continuos (citas, hábitos, enfoque, escalada y amistad) escalan hasta Nivel 10 multiplicando cada meta por 1.5x.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)),
            border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(accentPrimary, accentSecondary))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = VicdaruSoftPink, modifier = Modifier.size(22.dp))
                        Column {
                            Text(sampleMedal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Simulador interactivo 1.5x", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(shape = RoundedCornerShape(50), color = accentPrimary.copy(alpha = 0.18f)) {
                        Text(
                            text = "Nv. $levelInt / ${sampleMedal.maxLevel}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = accentPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { levelInt / sampleMedal.maxLevel.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50)),
                    color = accentPrimary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Text(
                    text = if (nextLevelReq != null) {
                        "Meta en Nv. $levelInt: $requiredForLevel ${sampleMedal.unitLabel}  •  Siguiente Nv. ${levelInt + 1}: $nextLevelReq ${sampleMedal.unitLabel}"
                    } else {
                        "¡Nivel Máximo 10/10! Meta final: $requiredForLevel ${sampleMedal.unitLabel}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Slider(
                    value = simulatedLevel,
                    onValueChange = { newVal ->
                        val nextInt = newVal.roundToInt()
                        if (nextInt != levelInt) {
                            haptics.subtleTap()
                        }
                        simulatedLevel = newVal
                    },
                    valueRange = 1f..10f,
                    steps = 8,
                    modifier = Modifier.testTag("onboarding_medal_slider")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            haptics.climberUnlock()
                            simulatedLevel = if (levelInt >= 10) 1f else (levelInt + 1).toFloat()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("onboarding_medal_step_button")
                    ) {
                        Text(if (levelInt >= 10) "Reiniciar demo" else "Subir a Nv. ${levelInt + 1}")
                    }
                }
            }
        }
    }
}

@Composable
internal fun OnboardingFriendsPage(
    haptics: HapticsHelper,
    accentPrimary: Color,
    accentSecondary: Color,
) {
    var vibePoints by remember { mutableIntStateOf(50) }
    var emotionalLevel by remember { mutableIntStateOf(1) }
    val animatedVibe by animateFloatAsState(
        targetValue = (vibePoints / 100f).coerceIn(0f, 1f),
        animationSpec = tween(360),
        label = "onboarding_vibe_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = accentSecondary.copy(alpha = 0.16f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = accentSecondary, modifier = Modifier.size(16.dp))
                Text("4 de 4 • Conectar con Amigos", style = MaterialTheme.typography.labelLarge, color = accentSecondary)
            }
        }

        Text(
            text = "Comparte tu código y sube la Buena Vibra",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            border = BorderStroke(1.dp, accentPrimary.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StepHintRow(
                    icon = Icons.Filled.ContentCopy,
                    title = "1. Código de Amigo único (VD-XXXXXX)",
                    detail = "En Perfil copia tu código o escribe el código de tu amigo para enviarle solicitud."
                )
                StepHintRow(
                    icon = Icons.Filled.SportsEsports,
                    title = "2. Salas en Tiempo Real y Foto de Galería",
                    detail = "Acepta solicitudes para retarse en Escalada Dúo, Enfoque Musical y ver sus avatares."
                )
                StepHintRow(
                    icon = Icons.Filled.Forum,
                    title = "3. Chat Musical + Amistad Emocional",
                    detail = "Cada mensaje en el Chat en Vivo suma +25 de Buena Vibra. ¡Al llenar 100/100 ambos suben de nivel!"
                )
            }
        }

        // Interactive Good Vibe simulator
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
            border = BorderStroke(1.dp, VicdaruSoftPink.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = VicdaruSoftPink, modifier = Modifier.size(18.dp))
                        Text("Amistad Emocional • Nv. $emotionalLevel", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text("$vibePoints / 100", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedVibe)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Brush.horizontalGradient(listOf(VicdaruSkyBlue, VicdaruSoftPink)))
                    )
                }

                Button(
                    onClick = {
                        val total = vibePoints + 25
                        if (total >= 100) {
                            vibePoints = total % 100
                            emotionalLevel += 1
                            haptics.vibeLevelUp()
                        } else {
                            vibePoints = total
                            haptics.chatMessageSent()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("onboarding_good_vibe_button")
                ) {
                    Text(
                        text = if (vibePoints >= 75) "¡Enviar mensaje y subir a Nv. ${emotionalLevel + 1} (+25)!"
                        else "Probar mensaje en chat (+25 Buena Vibra)",
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHintRow(
    icon: ImageVector,
    title: String,
    detail: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
