package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.audio.HapticsHelper
import com.example.model.AnimeSeries
import com.example.model.QuoteHistoryEntry
import com.example.share.QuoteShareHelper
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class AmbientParticle(
    val xRatio: Float,
    val yRatio: Float,
    val radiusDp: Float,
    val speedFactor: Float,
    val phase: Float,
    val isPink: Boolean,
)

@Composable
fun SoftCyberParticleCanvas(
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    energyMultiplier: Float = 1f,
) {
    val particles = remember {
        List(22) { idx ->
            AmbientParticle(
                xRatio = Random(idx * 31 + 7).nextFloat(),
                yRatio = Random(idx * 53 + 11).nextFloat(),
                radiusDp = 2.5f + Random(idx * 17).nextFloat() * 5.5f,
                speedFactor = 0.5f + Random(idx * 19).nextFloat() * 1.1f,
                phase = Random(idx * 23).nextFloat() * 6.28f,
                isPink = idx % 2 == 1
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "ambientParticles")
    val tick by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleTick"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        // Soft radial aurora glow in top-left and bottom-right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.15f),
                radius = size.width * 0.65f
            ),
            radius = size.width * 0.65f,
            center = Offset(size.width * 0.18f, size.height * 0.15f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.12f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.82f),
                radius = size.width * 0.6f
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width * 0.85f, size.height * 0.82f)
        )

        particles.forEach { p ->
            val driftX = sin(tick * p.speedFactor + p.phase) * 26f * energyMultiplier
            val driftY = cos(tick * p.speedFactor * 0.8f + p.phase) * 32f * energyMultiplier
            val cx = ((p.xRatio * size.width) + driftX).coerceIn(0f, size.width)
            val cy = ((p.yRatio * size.height) + driftY).coerceIn(0f, size.height)
            val color = if (p.isPink) secondaryColor else primaryColor
            drawCircle(
                color = color.copy(alpha = 0.24f),
                radius = p.radiusDp.dp.toPx() * (0.85f + 0.25f * energyMultiplier),
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
fun VicdaruBrandHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(VicdaruSkyBlue, VicdaruSoftPink))
                    )
                    .padding(2.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vicdaru_app_logo_1794974223268),
                    contentDescription = "Logo de Vicdaru Project",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

@Composable
fun QuoteCardItem(
    entry: QuoteHistoryEntry,
    isHero: Boolean = false,
    showArchiveDetails: Boolean = false,
    onStatusMessage: ((String) -> Unit)? = null,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val anime = AnimeSeries.fromKey(entry.animeKey)
    val context = LocalContext.current
    val haptics = remember(context) { HapticsHelper(context) }
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    val resolvedUrl = entry.imageUrl.ifBlank { anime.fallbackImageUrl }
    val resolvedSource = remember(entry.imageSource, resolvedUrl) {
        if (entry.imageSource.isNotBlank()) entry.imageSource
        else when {
            resolvedUrl.contains("danbooru", ignoreCase = true) -> "Danbooru (Fanart & Arte)"
            resolvedUrl.contains("safebooru", ignoreCase = true) -> "Safebooru (Fanart)"
            resolvedUrl.contains("konachan", ignoreCase = true) -> "Konachan (Ilustración)"
            resolvedUrl.contains("yande.re", ignoreCase = true) -> "Yande.re (Fanart & Scans)"
            resolvedUrl.contains("wallhaven", ignoreCase = true) -> "Wallhaven (Wallpapers)"
            resolvedUrl.contains("anilist", ignoreCase = true) -> "AniList (Oficial)"
            else -> "Jikan / MyAnimeList"
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHero) 6.dp else 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isHero) 210.dp else 156.dp)
            ) {
                AsyncImage(
                    model = entry.imageUrl.ifBlank { anime.fallbackImageUrl },
                    contentDescription = "Ilustración de ${anime.displayName}",
                    placeholder = painterResource(id = R.drawable.hero_cyber_zen_1794974224612),
                    error = painterResource(id = R.drawable.hero_cyber_zen_1794974224612),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = anime.displayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)
                    ) {
                        Text(
                            text = entry.moodTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "“${entry.quoteText}”",
                    style = if (isHero) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "— ${entry.characterName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("favorite_quote_button_${entry.id}")
                        ) {
                            Icon(
                                imageVector = if (entry.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = if (entry.favorite) "Quitar de favoritos" else "Guardar en favoritos",
                                tint = if (entry.favorite) VicdaruSoftPink else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                haptics.shareOrCopy()
                                onShare()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("share_quote_button_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "Compartir cita en redes sociales",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (showArchiveDetails) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val sourceTypeLabel = when (entry.sourceType) {
                        "MORNING" -> "Frase Matutina"
                        "RANDOM" -> "Recordatorio Diario"
                        else -> "Dosis Manual"
                    }
                    val formattedDate = remember(entry.createdAt) {
                        val date = entry.createdAt?.toDate()
                        if (date != null) {
                            SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("es", "ES")).format(date)
                        } else {
                            "Reciente"
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().testTag("archive_quote_info_${entry.id}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "Información de la cita",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Anime: ${anime.displayName} • Personaje: ${entry.characterName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Origen: $sourceTypeLabel • Guardada: $formattedDate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Fuente de imagen: $resolvedSource",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Enlace para descargar imagen:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = resolvedUrl,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptics.subtleTap()
                                        QuoteShareHelper.openImageLink(context, resolvedUrl)
                                    }
                                    .padding(vertical = 2.dp)
                                    .testTag("quote_image_download_link_${entry.id}")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        haptics.downloadStart()
                                        val msg = QuoteShareHelper.downloadQuoteImage(context, entry)
                                        onStatusMessage?.invoke(msg)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("download_quote_image_button_${entry.id}")
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Descargar")
                                }
                                OutlinedButton(
                                    onClick = {
                                        haptics.shareOrCopy()
                                        clipboard.setText(AnnotatedString(resolvedUrl))
                                        onStatusMessage?.invoke("Link copiado al portapapeles")
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("copy_quote_image_link_${entry.id}")
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar link", modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copiar")
                                }
                                IconButton(
                                    onClick = {
                                        haptics.subtleTap()
                                        QuoteShareHelper.openImageLink(context, resolvedUrl)
                                    },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.OpenInNew,
                                        contentDescription = "Abrir imagen en navegador",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
