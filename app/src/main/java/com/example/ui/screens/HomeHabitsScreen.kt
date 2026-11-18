package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.model.AnimeSeries
import com.example.model.HabitItem
import com.example.model.QuoteHistoryEntry
import com.example.model.UiState
import com.example.notifications.QuoteNotificationHelper
import com.example.share.QuoteShareHelper
import com.example.ui.components.QuoteCardItem
import com.example.ui.components.VicdaruBrandHeader
import com.example.viewmodel.VicdaruMainViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeHabitsScreen(
    viewModel: VicdaruMainViewModel,
    quotesState: UiState<List<QuoteHistoryEntry>>,
    habitsState: UiState<List<HabitItem>>,
    isFetchingQuote: Boolean,
    isOffline: Boolean = false,
    pendingCacheSyncCount: Int = 0,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddHabitDialog by remember { mutableStateOf(false) }
    val todayIso = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    }
    var selectedIsoDate by remember { mutableStateOf(todayIso) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scope.launch {
                QuoteNotificationHelper.deliverQuoteNotification(context, isMorning = false)
                viewModel.postMessage("Notificación enviada con imagen anime")
            }
        } else {
            viewModel.postMessage("Permiso de notificaciones denegado en el sistema")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            VicdaruBrandHeader(
                title = "Vicdaru Project",
                subtitle = "Pulso motivacional y hábitos del mes",
                trailingContent = {
                    IconButton(
                        onClick = {
                            viewModel.haptics.subtleTap()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                scope.launch {
                                    QuoteNotificationHelper.deliverQuoteNotification(context, isMorning = false)
                                    viewModel.postMessage("Notificación push entregada")
                                }
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("test_push_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Enviar notificación motivacional de prueba",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }

        if (isOffline || pendingCacheSyncCount > 0) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().testTag("home_offline_cache_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOffline) Icons.Filled.CloudOff else Icons.Filled.Sync,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isOffline) {
                                "Modo sin conexión activo • Historial y favoritos disponibles en caché local"
                            } else {
                                "Sincronizando $pendingCacheSyncCount cambios guardados offline..."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // Latest Quote Hero Section
        item {
            when (quotesState) {
                is UiState.Loading -> {
                    Card(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                is UiState.Error -> {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = quotesState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                is UiState.Success -> {
                    val latest = quotesState.data.firstOrNull()
                    if (latest != null) {
                        QuoteCardItem(
                            entry = latest,
                            isHero = true,
                            onToggleFavorite = { viewModel.toggleQuoteFavorite(latest) },
                            onShare = {
                                scope.launch { QuoteShareHelper.shareQuoteCard(context, latest) }
                            }
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Toca 'Nueva dosis' para recibir tu primera frase anime.", textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        // Action row for new motivational quote
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.deliverNewMotivationalQuote() },
                    enabled = !isFetchingQuote,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("new_quote_button")
                ) {
                    if (isFetchingQuote) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Buscando imagen...")
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nueva dosis anime")
                    }
                }
                FilledTonalButton(
                    onClick = {
                        viewModel.haptics.subtleTap()
                        showAddHabitDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("open_add_habit_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Hábito")
                }
            }
        }

        // Monthly Interactive Habit Progress Visualizer
        item {
            val habits = (habitsState as? UiState.Success)?.data.orEmpty()
            MonthlyHabitVisualizerCard(
                habits = habits,
                selectedIsoDate = selectedIsoDate,
                onSelectDate = {
                    viewModel.haptics.subtleTap()
                    selectedIsoDate = it
                }
            )
        }

        // Daily Habits Checklist for selected date
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedIsoDate == todayIso) "Hábitos de hoy" else "Hábitos del $selectedIsoDate",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Toca para completar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when (habitsState) {
            is UiState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(habitsState.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is UiState.Success -> {
                if (habitsState.data.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Aún no tienes hábitos creados. Toca '+ Hábito' para iniciar tu progreso mensual.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(habitsState.data, key = { it.id }) { habit ->
                        val completed = habit.completedDates.contains(selectedIsoDate)
                        val anime = AnimeSeries.fromKey(habit.animeTag)
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (completed) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                } else {
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                                }
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleHabitDate(habit, selectedIsoDate) }
                                .testTag("habit_card_${habit.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (completed) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                    contentDescription = if (completed) "Completado" else "Pendiente",
                                    tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${anime.displayName} • Racha: ${habit.currentStreak} días",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteHabit(habit.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = "Eliminar hábito",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showAddHabitDialog) {
        AddHabitDialog(
            onChipTap = { viewModel.haptics.subtleTap() },
            onDismiss = {
                viewModel.haptics.subtleTap()
                showAddHabitDialog = false
            },
            onConfirm = { title, animeKey ->
                viewModel.createHabit(title, animeKey)
                showAddHabitDialog = false
            }
        )
    }
}

@Composable
private fun MonthlyHabitVisualizerCard(
    habits: List<HabitItem>,
    selectedIsoDate: String,
    onSelectDate: (String) -> Unit,
) {
    val calendar = remember { Calendar.getInstance() }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val monthLabel = remember {
        SimpleDateFormat("MMMM yyyy", Locale("es", "ES")).format(calendar.time)
            .replaceFirstChar { it.uppercase() }
    }
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val monthPrefix = remember { String.format(Locale.US, "%04d-%02d-", year, month + 1) }

    val totalPossible = (habits.size * daysInMonth).coerceAtLeast(1)
    val totalCompletedInMonth = habits.sumOf { h ->
        h.completedDates.count { it.startsWith(monthPrefix) }
    }
    val monthlyRatio = (totalCompletedInMonth.toFloat() / totalPossible.toFloat()).coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Progreso Mensual",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$monthLabel • $totalCompletedInMonth registros completados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(
                            color = primary.copy(alpha = 0.16f),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(primary, secondary, primary)),
                            startAngle = -90f,
                            sweepAngle = 360f * monthlyRatio,
                            useCenter = false,
                            style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "${(monthlyRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive 7-column calendar heatmap
            val rows = (1..daysInMonth).chunked(7)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                rows.forEach { weekDays ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weekDays.forEach { day ->
                            val iso = String.format(Locale.US, "%s%02d", monthPrefix, day)
                            val doneCount = habits.count { it.completedDates.contains(iso) }
                            val intensity = if (habits.isEmpty()) 0f else (doneCount.toFloat() / habits.size.toFloat()).coerceIn(0f, 1f)
                            val isSelected = iso == selectedIsoDate
                            val cellColor = when {
                                intensity >= 0.99f -> primary
                                intensity > 0.45f -> primary.copy(alpha = 0.62f)
                                intensity > 0f -> secondary.copy(alpha = 0.48f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cellColor)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .clickable { onSelectDate(iso) }
                                    .testTag("habit_day_$iso"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$day",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected || doneCount > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (intensity > 0.45f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        repeat(7 - weekDays.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = habits.isNotEmpty()) {
                val doneForSelected = habits.count { it.completedDates.contains(selectedIsoDate) }
                Text(
                    text = "Día $selectedIsoDate: $doneForSelected de ${habits.size} hábitos cumplidos",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun AddHabitDialog(
    onChipTap: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedAnime by remember { mutableStateOf(AnimeSeries.SAKURA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo hábito positivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("Ej. Leer 15 min, Hidratarme") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("habit_title_input")
                )
                Text("Inspiración anime:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AnimeSeries.entries) { series ->
                        FilterChip(
                            selected = selectedAnime == series,
                            onClick = {
                                onChipTap()
                                selectedAnime = series
                            },
                            label = { Text(series.leadCharacter) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onConfirm(title, selectedAnime.key) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_habit_button")
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
