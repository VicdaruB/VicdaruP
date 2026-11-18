package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.HapticsHelper
import com.example.model.AnimeSeries
import com.example.ui.components.SoftCyberParticleCanvas
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    haptics: HapticsHelper,
    initialAnime: AnimeSeries = AnimeSeries.SAKURA,
    onPreviewAnimeChanged: ((AnimeSeries) -> Unit)? = null,
    onFinish: () -> Unit,
) {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    var previewAnime by remember { mutableStateOf(initialAnime) }
    val accentPrimary by animateColorAsState(
        targetValue = previewAnime.primaryColor,
        animationSpec = tween(420),
        label = "onboarding_primary"
    )
    val accentSecondary by animateColorAsState(
        targetValue = previewAnime.secondaryColor,
        animationSpec = tween(420),
        label = "onboarding_secondary"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        accentPrimary.copy(alpha = 0.08f),
                        accentSecondary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("onboarding_screen")
    ) {
        SoftCyberParticleCanvas()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 620.dp)
                .align(Alignment.Center)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Brand header + Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vicdaru_app_logo_1794974223268),
                        contentDescription = "Logo de Vicdaru Project",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(34.dp).clip(CircleShape)
                    )
                    Column {
                        Text(
                            text = "Vicdaru Project",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Guía Interactiva",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentPrimary
                        )
                    }
                }

                TextButton(
                    onClick = {
                        haptics.subtleTap()
                        onFinish()
                    },
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("onboarding_skip_button")
                ) {
                    Text("Saltar")
                }
            }

            // Interactive HorizontalPager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPulsoPage(
                        selectedAnime = previewAnime,
                        accentPrimary = accentPrimary,
                        accentSecondary = accentSecondary,
                        onSelectAnime = { series ->
                            haptics.subtleTap()
                            previewAnime = series
                            onPreviewAnimeChanged?.invoke(series)
                        }
                    )
                    1 -> OnboardingArcadeFocusPage(
                        haptics = haptics,
                        accentPrimary = accentPrimary,
                        accentSecondary = accentSecondary
                    )
                    2 -> OnboardingMedalsPage(
                        haptics = haptics,
                        accentPrimary = accentPrimary,
                        accentSecondary = accentSecondary
                    )
                    else -> OnboardingFriendsPage(
                        haptics = haptics,
                        accentPrimary = accentPrimary,
                        accentSecondary = accentSecondary
                    )
                }
            }

            // Bottom pagination dots & navigation controls
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pageCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 26.dp else 8.dp,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "dot_width"
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(CircleShape)
                                .clickable(role = Role.Tab) {
                                    haptics.navTabSwitch()
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                }
                                .semantics {
                                    contentDescription = "Ir a la página ${index + 1} de $pageCount"
                                }
                                .testTag("onboarding_dot_$index")
                        ) {
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(dotWidth)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) {
                                            Brush.horizontalGradient(listOf(accentPrimary, accentSecondary))
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                                )
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                haptics.navTabSwitch()
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(50.dp).testTag("onboarding_prev_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior")
                            Spacer(Modifier.width(6.dp))
                            Text("Anterior")
                        }
                    }

                    val isLastPage = pagerState.currentPage == pageCount - 1
                    Button(
                        onClick = {
                            if (isLastPage) {
                                haptics.quoteDelivered()
                                onFinish()
                            } else {
                                haptics.navTabSwitch()
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag(if (isLastPage) "onboarding_finish_button" else "onboarding_next_button")
                    ) {
                        Text(
                            text = if (isLastPage) "¡Empezar en Vicdaru!" else "Siguiente",
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (isLastPage) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}
