package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.model.AnimeSeries
import com.example.model.VisualizerProfile
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object VisualizerStyleCatalog {
    data class StyleOption(val key: String, val label: String)

    val allStyles: List<StyleOption> = listOf(
        StyleOption("CARD_HALO", "Círculo Clow & Cartas"),
        StyleOption("ARCANE_CONSTELLATION", "Constelación Zoltraak"),
        StyleOption("APOTHECARY_RIPPLE", "Elixir & Ondas de Seda"),
        StyleOption("TURBO_MATRIX", "Matriz Oculta Turbo"),
        StyleOption("SHINY_PRISM", "Prisma Arcoíris Shiny"),
        StyleOption("YINYANG_KINETIC", "Vórtice Yin-Yang Dojo")
    )

    fun normalizeStyle(raw: String?): String = when (raw?.uppercase()) {
        "CARD_HALO", "RIBBON" -> "CARD_HALO"
        "ARCANE_CONSTELLATION", "ORBITAL" -> "ARCANE_CONSTELLATION"
        "APOTHECARY_RIPPLE", "RIPPLE" -> "APOTHECARY_RIPPLE"
        "TURBO_MATRIX", "BARS" -> "TURBO_MATRIX"
        "SHINY_PRISM" -> "SHINY_PRISM"
        "YINYANG_KINETIC" -> "YINYANG_KINETIC"
        else -> "CARD_HALO"
    }

    fun labelFor(raw: String?): String {
        val norm = normalizeStyle(raw)
        return allStyles.firstOrNull { it.key == norm }?.label ?: norm
    }
}

@Composable
fun AnimeVisualizerCanvas(
    selectedProfile: VisualizerProfile,
    spectrum: List<Float>,
    beatEnergy: Float,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "visualizer_phase"
    )
    val slowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "visualizer_slow_phase"
    )

    val anime = AnimeSeries.fromKey(selectedProfile.baseAnimeKey)
    val style = VisualizerStyleCatalog.normalizeStyle(selectedProfile.waveformStyle)

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = minOf(size.width, size.height) * 0.34f
        val bands = if (spectrum.isNotEmpty()) spectrum else List(16) { 0.25f }

        // Ambient reactive aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.26f * (0.4f + beatEnergy)),
                    secondaryColor.copy(alpha = 0.12f * beatEnergy),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = maxRadius * (1.5f + beatEnergy * 0.65f)
            ),
            radius = maxRadius * (1.5f + beatEnergy * 0.65f),
            center = Offset(cx, cy)
        )

        // 1. Unique geometry per visualizer style (no two styles repeat)
        when (style) {
            "CARD_HALO" -> drawCardHaloVisualizer(cx, cy, maxRadius, bands, beatEnergy, phase, primaryColor, secondaryColor)
            "ARCANE_CONSTELLATION" -> drawArcaneConstellationVisualizer(cx, cy, maxRadius, bands, beatEnergy, phase, primaryColor, secondaryColor)
            "APOTHECARY_RIPPLE" -> drawApothecaryRippleVisualizer(cx, cy, maxRadius, bands, beatEnergy, phase, primaryColor, secondaryColor)
            "TURBO_MATRIX" -> drawTurboMatrixVisualizer(cx, cy, maxRadius, bands, beatEnergy, phase, primaryColor, secondaryColor)
            "SHINY_PRISM" -> drawShinyPrismVisualizer(cx, cy, maxRadius, bands, beatEnergy, phase, primaryColor, secondaryColor)
            "YINYANG_KINETIC" -> drawYinYangKineticVisualizer(cx, cy, maxRadius, bands, beatEnergy, phase, primaryColor, secondaryColor)
        }

        // 2. Decorative motifs of ALL main characters of the selected anime
        drawAnimeMainCharactersMotifs(
            anime = anime,
            phase = phase,
            slowPhase = slowPhase,
            beatEnergy = beatEnergy,
            primaryColor = primaryColor,
            secondaryColor = secondaryColor
        )
    }
}

// --- 6 DISTINCT VISUALIZER GEOMETRIES ---

private fun DrawScope.drawCardHaloVisualizer(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    spectrum: List<Float>,
    beatEnergy: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
) {
    val gold = Color(0xFFFDE68A)
    // Outer double Clow-inspired magic ring
    rotate(degrees = (phase * 180f / PI.toFloat()), pivot = Offset(cx, cy)) {
        drawCircle(
            color = gold.copy(alpha = 0.48f),
            radius = maxRadius * 1.05f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
        drawCircle(
            color = primary.copy(alpha = 0.4f),
            radius = maxRadius * 0.88f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5.dp.toPx())
        )
        // Rotating 6-point star seal
        val starPath = Path()
        for (i in 0..5) {
            val a1 = (i * 60f) * (PI / 180.0)
            val a2 = ((i + 2) * 60f) * (PI / 180.0)
            val r = maxRadius * (0.86f + beatEnergy * 0.08f)
            starPath.moveTo(cx + (cos(a1) * r).toFloat(), cy + (sin(a1) * r).toFloat())
            starPath.lineTo(cx + (cos(a2) * r).toFloat(), cy + (sin(a2) * r).toFloat())
        }
        drawPath(starPath, color = gold.copy(alpha = 0.35f), style = Stroke(width = 1.6.dp.toPx()))
    }

    // Orbiting mini Clow/Sakura cards that rise with spectrum peaks
    val cardCount = 10
    for (i in 0 until cardCount) {
        val amp = spectrum[(i * spectrum.size / cardCount).coerceIn(0, spectrum.lastIndex)]
        val angle = (i.toFloat() / cardCount) * (2.0 * PI) - phase * 0.6f
        val orbitR = maxRadius * (0.68f + amp * 0.52f)
        val cardX = cx + (cos(angle) * orbitR).toFloat()
        val cardY = cy + (sin(angle) * orbitR).toFloat()
        rotate(degrees = ((angle * 180.0 / PI).toFloat() + 90f), pivot = Offset(cardX, cardY)) {
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(primary.copy(alpha = 0.85f), secondary.copy(alpha = 0.85f))),
                topLeft = Offset(cardX - 6.dp.toPx(), cardY - 9.dp.toPx()),
                size = Size(12.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
            drawRoundRect(
                color = gold.copy(alpha = 0.8f),
                topLeft = Offset(cardX - 6.dp.toPx(), cardY - 9.dp.toPx()),
                size = Size(12.dp.toPx(), 18.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }

    // Petal burst bars around inner star core
    spectrum.forEachIndexed { idx, amp ->
        val angle = (idx.toFloat() / spectrum.size) * (2.0 * PI) + phase * 0.3f
        val r0 = maxRadius * 0.3f
        val r1 = r0 + maxRadius * 0.42f * amp
        drawLine(
            brush = Brush.linearGradient(listOf(gold, primary)),
            start = Offset(cx + (cos(angle) * r0).toFloat(), cy + (sin(angle) * r0).toFloat()),
            end = Offset(cx + (cos(angle) * r1).toFloat(), cy + (sin(angle) * r1).toFloat()),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawArcaneConstellationVisualizer(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    spectrum: List<Float>,
    beatEnergy: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
) {
    // Rotating nested hexagons (Zoltraak defensive spell geometry)
    for (ring in 1..3) {
        val hexPath = Path()
        val rot = phase * (if (ring % 2 == 0) 0.5f else -0.4f)
        val r = maxRadius * (0.34f * ring + beatEnergy * 0.1f)
        for (k in 0..5) {
            val a = rot + k * (PI / 3.0).toFloat()
            val px = cx + cos(a) * r
            val py = cy + sin(a) * r
            if (k == 0) hexPath.moveTo(px, py) else hexPath.lineTo(px, py)
        }
        hexPath.close()
        drawPath(
            path = hexPath,
            color = (if (ring == 2) secondary else primary).copy(alpha = 0.42f),
            style = Stroke(width = 1.6.dp.toPx())
        )
    }

    // Constellation polygon linking audio-reactive star nodes
    val polyPath = Path()
    val nodePoints = spectrum.mapIndexed { idx, amp ->
        val a = (idx.toFloat() / spectrum.size) * (2.0 * PI) + phase * 0.25f
        val r = maxRadius * (0.48f + amp * 0.72f)
        Offset(cx + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat())
    }
    nodePoints.forEachIndexed { idx, pt ->
        if (idx == 0) polyPath.moveTo(pt.x, pt.y) else polyPath.lineTo(pt.x, pt.y)
    }
    polyPath.close()
    drawPath(
        path = polyPath,
        brush = Brush.sweepGradient(listOf(primary, secondary, primary), center = Offset(cx, cy)),
        style = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round)
    )

    // Star nodes + delicate threads to center
    nodePoints.forEachIndexed { idx, pt ->
        if (idx % 2 == 0) {
            drawLine(
                color = secondary.copy(alpha = 0.25f),
                start = Offset(cx, cy),
                end = pt,
                strokeWidth = 1.dp.toPx()
            )
        }
        drawCircle(
            color = if (idx % 2 == 0) primary else Color.White,
            radius = (3f + spectrum[idx] * 4f).dp.toPx() * 0.55f,
            center = pt
        )
    }
}

private fun DrawScope.drawApothecaryRippleVisualizer(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    spectrum: List<Float>,
    beatEnergy: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
) {
    // Dual intersecting silk/elixir waves across canvas
    val waveA = Path()
    val waveB = Path()
    val step = size.width / (spectrum.size - 1).coerceAtLeast(1)
    spectrum.forEachIndexed { idx, amp ->
        val x = idx * step
        val env = sin((idx.toFloat() / spectrum.size) * PI).toFloat()
        val yA = cy + sin(idx * 0.7f + phase * 2.2f) * (amp * maxRadius * 0.85f * env + 8f)
        val yB = cy + cos(idx * 0.55f - phase * 1.8f) * (amp * maxRadius * 0.7f * env + 6f)
        if (idx == 0) {
            waveA.moveTo(x, yA)
            waveB.moveTo(x, yB)
        } else {
            waveA.lineTo(x, yA)
            waveB.lineTo(x, yB)
        }
    }
    drawPath(
        path = waveA,
        brush = Brush.horizontalGradient(listOf(primary, secondary, primary)),
        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
    )
    drawPath(
        path = waveB,
        brush = Brush.horizontalGradient(listOf(secondary.copy(alpha = 0.8f), primary.copy(alpha = 0.8f))),
        style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
    )

    // Expanding concentric elixir ripple rings
    for (ring in 1..3) {
        val t = ((phase / (2f * PI.toFloat())) + ring / 3f) % 1f
        val r = maxRadius * (0.25f + t * 0.95f + beatEnergy * 0.12f)
        drawCircle(
            color = (if (ring % 2 == 0) secondary else primary).copy(alpha = (1f - t) * 0.55f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
    }

    // Rising apothecary bubbles synced to spectrum bands
    spectrum.forEachIndexed { idx, amp ->
        if (idx % 2 == 0) {
            val bx = size.width * 0.14f + (idx.toFloat() / spectrum.size) * size.width * 0.72f
            val lift = ((phase * 0.6f + idx * 0.43f) % 1f)
            val by = size.height * (0.86f - lift * 0.72f)
            val br = (3f + amp * 6f)
            drawCircle(
                color = (if (idx % 4 == 0) primary else secondary).copy(alpha = (1f - lift) * 0.7f),
                radius = br,
                center = Offset(bx, by),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }
}

private fun DrawScope.drawTurboMatrixVisualizer(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    spectrum: List<Float>,
    beatEnergy: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
) {
    // UFO tractor-beam tilted rings in center
    for (i in 0..1) {
        val rx = maxRadius * (1.25f + i * 0.25f + beatEnergy * 0.2f)
        val ry = maxRadius * (0.38f + i * 0.1f)
        drawOval(
            color = (if (i == 0) primary else secondary).copy(alpha = 0.35f),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = 1.8.dp.toPx())
        )
    }

    // Mirrored neon equalizer bars (cyber-occult grid)
    val count = spectrum.size
    val totalW = size.width * 0.78f
    val barSlot = totalW / count
    val startX = (size.width - totalW) / 2f
    spectrum.forEachIndexed { idx, amp ->
        val x = startX + idx * barSlot + barSlot / 2f
        val barH = size.height * 0.34f * (0.18f + amp)
        val segColor = if (idx % 2 == 0) primary else secondary
        drawLine(
            brush = Brush.verticalGradient(listOf(segColor, Color.White.copy(alpha = 0.9f), segColor)),
            start = Offset(x, cy - barH),
            end = Offset(x, cy + barH),
            strokeWidth = (barSlot * 0.52f).coerceAtLeast(3.dp.toPx()),
            cap = StrokeCap.Butt
        )
    }

    // High-voltage Turbo lightning bolt cutting across the matrix
    val bolt = Path()
    val segs = 10
    for (s in 0..segs) {
        val fx = size.width * 0.12f + (s.toFloat() / segs) * size.width * 0.76f
        val band = spectrum[(s * spectrum.size / (segs + 1)).coerceIn(0, spectrum.lastIndex)]
        val jag = (if (s % 2 == 0) 1f else -1f) * (10f + band * 34f) * (0.6f + beatEnergy * 0.7f)
        val fy = cy + jag * sin(phase * 3f + s)
        if (s == 0) bolt.moveTo(fx, fy) else bolt.lineTo(fx, fy)
    }
    drawPath(
        path = bolt,
        color = Color(0xFFFEF08A).copy(alpha = 0.85f),
        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Miter)
    )
}

private fun DrawScope.drawShinyPrismVisualizer(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    spectrum: List<Float>,
    beatEnergy: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
) {
    // 7-ray Shiny Arc rainbow fan
    val rainbow = listOf(
        Color(0xFFFB7185),
        Color(0xFFFBBF24),
        Color(0xFF34D399),
        Color(0xFF38BDF8),
        Color(0xFF818CF8),
        Color(0xFFA78BFA),
        Color(0xFFF472B6)
    )
    rainbow.forEachIndexed { idx, col ->
        val band = spectrum[(idx * 2).coerceIn(0, spectrum.lastIndex)]
        val arcR = maxRadius * (0.48f + idx * 0.11f + band * 0.18f)
        val startAngle = 195f + sin(phase + idx * 0.4f) * 14f
        drawArc(
            color = col.copy(alpha = 0.72f),
            startAngle = startAngle,
            sweepAngle = 150f + beatEnergy * 24f,
            useCenter = false,
            topLeft = Offset(cx - arcR, cy - arcR * 0.85f),
            size = Size(arcR * 2f, arcR * 1.7f),
            style = Stroke(width = (3.5f + band * 3f).dp.toPx() * 0.6f, cap = StrokeCap.Round)
        )
    }

    // Spiraling golden broomstick stardust trail
    val trail = Path()
    val pts = 24
    for (i in 0 until pts) {
        val t = i.toFloat() / (pts - 1)
        val amp = spectrum[(i * spectrum.size / pts).coerceIn(0, spectrum.lastIndex)]
        val angle = t * (3.5 * PI).toFloat() + phase * 1.3f
        val r = maxRadius * (0.18f + t * 0.88f + amp * 0.22f)
        val px = cx + cos(angle) * r
        val py = cy + sin(angle) * r * 0.68f + 16.dp.toPx()
        if (i == 0) trail.moveTo(px, py) else trail.lineTo(px, py)
        if (i % 3 == 0) {
            drawStarFour(
                center = Offset(px, py),
                radius = (4f + amp * 6f).dp.toPx() * 0.55f,
                color = if (i % 2 == 0) secondary else primary
            )
        }
    }
    drawPath(
        path = trail,
        brush = Brush.horizontalGradient(listOf(primary.copy(alpha = 0.35f), secondary)),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawYinYangKineticVisualizer(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    spectrum: List<Float>,
    beatEnergy: Float,
    phase: Float,
    primary: Color,
    secondary: Color,
) {
    // Outward splash droplet rays + dojo shockwaves
    spectrum.forEachIndexed { idx, amp ->
        val angle = (idx.toFloat() / spectrum.size) * (2.0 * PI) - phase * 0.35f
        val rStart = maxRadius * 0.56f
        val rEnd = rStart + maxRadius * (0.2f + amp * 0.65f)
        val col = if (idx < spectrum.size / 2) primary else secondary
        drawLine(
            color = col.copy(alpha = 0.85f),
            start = Offset(cx + (cos(angle) * rStart).toFloat(), cy + (sin(angle) * rStart).toFloat()),
            end = Offset(cx + (cos(angle) * rEnd).toFloat(), cy + (sin(angle) * rEnd).toFloat()),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = col,
            radius = 3.dp.toPx() * (0.7f + amp * 0.6f),
            center = Offset(cx + (cos(angle) * (rEnd + 5.dp.toPx())).toFloat(), cy + (sin(angle) * (rEnd + 5.dp.toPx())).toFloat())
        )
    }

    // Spinning Yin-Yang vortex emblem
    val yyRadius = maxRadius * (0.46f + beatEnergy * 0.08f)
    rotate(degrees = (phase * 180f / PI.toFloat()), pivot = Offset(cx, cy)) {
        drawArc(
            color = primary.copy(alpha = 0.88f),
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - yyRadius, cy - yyRadius),
            size = Size(yyRadius * 2f, yyRadius * 2f)
        )
        drawArc(
            color = secondary.copy(alpha = 0.88f),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(cx - yyRadius, cy - yyRadius),
            size = Size(yyRadius * 2f, yyRadius * 2f)
        )
        drawCircle(color = primary, radius = yyRadius / 2f, center = Offset(cx, cy - yyRadius / 2f))
        drawCircle(color = secondary, radius = yyRadius / 2f, center = Offset(cx, cy + yyRadius / 2f))
        drawCircle(color = secondary, radius = yyRadius * 0.16f, center = Offset(cx, cy - yyRadius / 2f))
        drawCircle(color = primary, radius = yyRadius * 0.16f, center = Offset(cx, cy + yyRadius / 2f))
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = yyRadius,
            center = Offset(cx, cy),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// --- DECORATIVE ELEMENTS OF ALL MAIN CHARACTERS PER ANIME ---

private fun DrawScope.drawAnimeMainCharactersMotifs(
    anime: AnimeSeries,
    phase: Float,
    slowPhase: Float,
    beatEnergy: Float,
    primaryColor: Color,
    secondaryColor: Color,
) {
    val tl = Offset(size.width * 0.11f, size.height * 0.18f)
    val tr = Offset(size.width * 0.89f, size.height * 0.18f)
    val bl = Offset(size.width * 0.11f, size.height * 0.78f)
    val br = Offset(size.width * 0.89f, size.height * 0.78f)
    val bob = sin(phase * 1.4f) * 3.5.dp.toPx()
    val pulse = 1f + beatEnergy * 0.18f
    val gold = Color(0xFFFDE047)

    when (anime) {
        AnimeSeries.SAKURA -> {
            // 1. Sakura Kinomoto (top-left): Star Wand head + cherry petal
            val sCenter = Offset(tl.x, tl.y + bob)
            drawCircle(color = secondaryColor, radius = 11.dp.toPx() * pulse, center = sCenter, style = Stroke(2.dp.toPx()))
            drawStarFour(sCenter, 8.dp.toPx() * pulse, gold)
            // Mini angel wings on wand
            drawLine(Color.White.copy(alpha = 0.9f), Offset(sCenter.x - 11.dp.toPx(), sCenter.y), Offset(sCenter.x - 19.dp.toPx(), sCenter.y - 7.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.9f), Offset(sCenter.x + 11.dp.toPx(), sCenter.y), Offset(sCenter.x + 19.dp.toPx(), sCenter.y - 7.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)

            // 2. Syaoran Li (top-right): Emerald Taoist octagonal compass + talisman seal
            val sy = Offset(tr.x, tr.y - bob)
            val emerald = Color(0xFF34D399)
            drawCircle(emerald.copy(alpha = 0.85f), radius = 12.dp.toPx() * pulse, center = sy, style = Stroke(2.dp.toPx()))
            drawLine(emerald, Offset(sy.x, sy.y - 12.dp.toPx()), Offset(sy.x, sy.y + 12.dp.toPx()), 1.5.dp.toPx())
            drawLine(emerald, Offset(sy.x - 12.dp.toPx(), sy.y), Offset(sy.x + 12.dp.toPx(), sy.y), 1.5.dp.toPx())
            drawRoundRect(gold.copy(alpha = 0.85f), Offset(sy.x + 14.dp.toPx(), sy.y - 8.dp.toPx()), Size(7.dp.toPx(), 16.dp.toPx()), CornerRadius(1.dp.toPx()))

            // 3. Kero-chan (bottom-left): Golden sun crest & mini wings
            val kc = Offset(bl.x, bl.y - bob * 0.7f)
            drawCircle(Color(0xFFF59E0B), radius = 10.dp.toPx() * pulse, center = kc)
            drawCircle(gold, radius = 6.dp.toPx(), center = kc)
            drawLine(Color.White, Offset(kc.x - 10.dp.toPx(), kc.y - 2.dp.toPx()), Offset(kc.x - 17.dp.toPx(), kc.y - 9.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
            drawLine(Color.White, Offset(kc.x + 10.dp.toPx(), kc.y - 2.dp.toPx()), Offset(kc.x + 17.dp.toPx(), kc.y - 9.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)

            // 4. Tomoyo & Yue (bottom-right): Camcorder frame + Yue crescent moon
            val ty = Offset(br.x, br.y + bob * 0.7f)
            val lav = Color(0xFFC4B5FD)
            drawRoundRect(lav.copy(alpha = 0.85f), Offset(ty.x - 15.dp.toPx(), ty.y - 10.dp.toPx()), Size(22.dp.toPx(), 16.dp.toPx()), CornerRadius(3.dp.toPx()), style = Stroke(1.8.dp.toPx()))
            drawCircle(Color(0xFFFB7185), radius = 2.5.dp.toPx(), center = Offset(ty.x - 9.dp.toPx(), ty.y - 4.dp.toPx()))
            drawCrescentMoon(Offset(ty.x + 12.dp.toPx(), ty.y), 9.dp.toPx() * pulse, Color(0xFFE0F2FE))
        }

        AnimeSeries.FRIEREN -> {
            // 1. Frieren (top-left): Golden crescent staff + red diamond earring
            val fc = Offset(tl.x, tl.y + bob)
            drawCircle(gold, radius = 11.dp.toPx() * pulse, center = fc, style = Stroke(2.2.dp.toPx()))
            drawCircle(Color(0xFFEF4444), radius = 4.dp.toPx(), center = fc)
            drawLine(gold, Offset(fc.x, fc.y + 11.dp.toPx()), Offset(fc.x, fc.y + 24.dp.toPx()), 2.dp.toPx())

            // 2. Fern (top-right): Violet butterfly hairpin + purple mana ribbon
            val fnc = Offset(tr.x, tr.y - bob)
            val violet = Color(0xFFA78BFA)
            drawCircle(violet, radius = 6.dp.toPx() * pulse, center = Offset(fnc.x - 6.dp.toPx(), fnc.y - 3.dp.toPx()))
            drawCircle(violet, radius = 6.dp.toPx() * pulse, center = Offset(fnc.x + 6.dp.toPx(), fnc.y - 3.dp.toPx()))
            drawCircle(Color(0xFFDDD6FE), radius = 3.5.dp.toPx(), center = Offset(fnc.x - 4.dp.toPx(), fnc.y + 5.dp.toPx()))
            drawCircle(Color(0xFFDDD6FE), radius = 3.5.dp.toPx(), center = Offset(fnc.x + 4.dp.toPx(), fnc.y + 5.dp.toPx()))

            // 3. Stark (bottom-left): Crimson-gold battle axe crescent + red spark
            val stc = Offset(bl.x, bl.y - bob * 0.7f)
            val crimson = Color(0xFFEF4444)
            drawArc(crimson, -50f, 120f, false, Offset(stc.x - 14.dp.toPx(), stc.y - 14.dp.toPx()), Size(28.dp.toPx(), 28.dp.toPx()), style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round))
            drawLine(Color(0xFFFCA5A5), Offset(stc.x - 10.dp.toPx(), stc.y + 12.dp.toPx()), Offset(stc.x + 12.dp.toPx(), stc.y - 12.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)

            // 4. Himmel (bottom-right): Blue Moon Weed flower wreath + hero blade glint
            val hmc = Offset(br.x, br.y + bob * 0.7f)
            val skyFlower = Color(0xFF7DD3FC)
            for (p in 0..4) {
                val a = slowPhase + p * (2.0 * PI / 5.0)
                drawCircle(skyFlower, 3.8.dp.toPx(), Offset(hmc.x + (cos(a) * 8.dp.toPx()).toFloat(), hmc.y + (sin(a) * 8.dp.toPx()).toFloat()))
            }
            drawCircle(gold, 3.dp.toPx(), hmc)
        }

        AnimeSeries.APOTHECARY -> {
            // 1. Maomao (top-left): Emerald mortar & pestle + wood-sorrel 3-leaf herb
            val mc = Offset(tl.x, tl.y + bob)
            val jade = Color(0xFF2DD4BF)
            drawArc(jade, 0f, 180f, true, Offset(mc.x - 12.dp.toPx(), mc.y - 4.dp.toPx()), Size(24.dp.toPx(), 18.dp.toPx()))
            drawLine(Color(0xFFA7F3D0), Offset(mc.x - 4.dp.toPx(), mc.y - 12.dp.toPx()), Offset(mc.x + 6.dp.toPx(), mc.y + 2.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
            drawCircle(Color(0xFF34D399), 3.dp.toPx(), Offset(mc.x - 13.dp.toPx(), mc.y - 8.dp.toPx()))
            drawCircle(Color(0xFF34D399), 3.dp.toPx(), Offset(mc.x - 8.dp.toPx(), mc.y - 12.dp.toPx()))

            // 2. Jinshi (top-right): Silver crescent hairpin & violet silk swirl
            val jc = Offset(tr.x, tr.y - bob)
            drawCrescentMoon(jc, 11.dp.toPx() * pulse, Color(0xFFE2E8F0))
            drawLine(Color(0xFFC084FC), Offset(jc.x - 14.dp.toPx(), jc.y + 10.dp.toPx()), Offset(jc.x + 14.dp.toPx(), jc.y + 4.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)

            // 3. Gyokuyou (bottom-left): Imperial Crimson-Gold Jade Pavilion Lantern
            val gc = Offset(bl.x, bl.y - bob * 0.7f)
            drawRoundRect(Color(0xFFF43F5E), Offset(gc.x - 8.dp.toPx(), gc.y - 11.dp.toPx()), Size(16.dp.toPx(), 20.dp.toPx()), CornerRadius(5.dp.toPx()))
            drawRoundRect(gold, Offset(gc.x - 8.dp.toPx(), gc.y - 11.dp.toPx()), Size(16.dp.toPx(), 20.dp.toPx()), CornerRadius(5.dp.toPx()), style = Stroke(1.5.dp.toPx()))

            // 4. Lihua (bottom-right): Sapphire Imperial Lantern & camellia blossom
            val lc = Offset(br.x, br.y + bob * 0.7f)
            drawRoundRect(Color(0xFF38BDF8), Offset(lc.x - 8.dp.toPx(), lc.y - 11.dp.toPx()), Size(16.dp.toPx(), 20.dp.toPx()), CornerRadius(5.dp.toPx()))
            drawCircle(secondaryColor, 4.dp.toPx(), Offset(lc.x, lc.y))
        }

        AnimeSeries.DANDADAN -> {
            // 1. Momo Ayase (top-left): Glowing Cyan Psychic Hand aura + star earring
            val momo = Offset(tl.x, tl.y + bob)
            val cyan = Color(0xFF22D3EE)
            drawCircle(cyan.copy(alpha = 0.25f), 16.dp.toPx() * pulse, momo)
            drawCircle(cyan, 9.dp.toPx(), momo, style = Stroke(2.dp.toPx()))
            drawStarFour(Offset(momo.x + 12.dp.toPx(), momo.y - 10.dp.toPx()), 5.dp.toPx(), Color(0xFFA3E635))

            // 2. Okarun (top-right): Glowing round spectacles + Turbo jagged teeth
            val ok = Offset(tr.x, tr.y - bob)
            drawCircle(Color.White, 6.5.dp.toPx(), Offset(ok.x - 8.dp.toPx(), ok.y - 4.dp.toPx()), style = Stroke(1.8.dp.toPx()))
            drawCircle(Color.White, 6.5.dp.toPx(), Offset(ok.x + 8.dp.toPx(), ok.y - 4.dp.toPx()), style = Stroke(1.8.dp.toPx()))
            drawLine(Color.White, Offset(ok.x - 1.5.dp.toPx(), ok.y - 4.dp.toPx()), Offset(ok.x + 1.5.dp.toPx(), ok.y - 4.dp.toPx()), 1.8.dp.toPx())
            drawLine(Color(0xFFEF4444), Offset(ok.x - 12.dp.toPx(), ok.y + 8.dp.toPx()), Offset(ok.x + 12.dp.toPx(), ok.y + 8.dp.toPx()), 2.dp.toPx())

            // 3. Turbo Granny (bottom-left): Golden Maneki-neko lucky bell & cat ears
            val tg = Offset(bl.x, bl.y - bob * 0.7f)
            drawCircle(gold, 9.dp.toPx() * pulse, tg)
            drawLine(Color(0xFF78350F), Offset(tg.x - 6.dp.toPx(), tg.y), Offset(tg.x + 6.dp.toPx(), tg.y), 1.8.dp.toPx())
            drawCircle(Color(0xFF78350F), 2.5.dp.toPx(), Offset(tg.x, tg.y + 2.dp.toPx()))

            // 4. Seiko & Aira (bottom-right): Spirit Ofuda Talisman + Pink Silky Ribbon
            val sa = Offset(br.x, br.y + bob * 0.7f)
            drawRoundRect(Color(0xFFFDF2F8), Offset(sa.x - 5.dp.toPx(), sa.y - 12.dp.toPx()), Size(10.dp.toPx(), 20.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawLine(Color(0xFFEF4444), Offset(sa.x, sa.y - 8.dp.toPx()), Offset(sa.x, sa.y + 4.dp.toPx()), 1.8.dp.toPx())
            drawArc(Color(0xFFEC4899), 0f, 220f, false, Offset(sa.x - 14.dp.toPx(), sa.y - 10.dp.toPx()), Size(28.dp.toPx(), 22.dp.toPx()), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        AnimeSeries.LITTLE_WITCH -> {
            // 1. Akko (top-left): Shiny Rod star core + witch hat brim
            val ak = Offset(tl.x, tl.y + bob)
            drawStarFour(ak, 11.dp.toPx() * pulse, gold)
            drawLine(Color(0xFFA855F7), Offset(ak.x - 14.dp.toPx(), ak.y + 10.dp.toPx()), Offset(ak.x + 14.dp.toPx(), ak.y + 10.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)

            // 2. Lotte (top-right): Round glasses + turquoise fairy orb
            val lt = Offset(tr.x, tr.y - bob)
            val mint = Color(0xFF34D399)
            drawCircle(mint, 6.dp.toPx(), Offset(lt.x - 7.dp.toPx(), lt.y), style = Stroke(1.8.dp.toPx()))
            drawCircle(mint, 6.dp.toPx(), Offset(lt.x + 7.dp.toPx(), lt.y), style = Stroke(1.8.dp.toPx()))
            drawCircle(Color(0xFF67E8F9), 3.5.dp.toPx() * pulse, Offset(lt.x, lt.y - 12.dp.toPx()))

            // 3. Sucy (bottom-left): Bubbling purple flask + spotted red mushroom
            val sc = Offset(bl.x, bl.y - bob * 0.7f)
            drawCircle(Color(0xFFA855F7), 9.dp.toPx(), Offset(sc.x - 5.dp.toPx(), sc.y + 3.dp.toPx()))
            drawArc(Color(0xFFEF4444), 180f, 180f, true, Offset(sc.x + 3.dp.toPx(), sc.y - 8.dp.toPx()), Size(16.dp.toPx(), 14.dp.toPx()))
            drawCircle(Color.White, 2.dp.toPx(), Offset(sc.x + 10.dp.toPx(), sc.y - 4.dp.toPx()))

            // 4. Diana (bottom-right): Noble ice-mint crescent crown & silver quill
            val dc = Offset(br.x, br.y + bob * 0.7f)
            drawCrescentMoon(dc, 10.dp.toPx() * pulse, Color(0xFFA7F3D0))
            drawLine(Color(0xFFE0F2FE), Offset(dc.x - 12.dp.toPx(), dc.y + 10.dp.toPx()), Offset(dc.x + 12.dp.toPx(), dc.y - 10.dp.toPx()), 1.8.dp.toPx(), StrokeCap.Round)
        }

        AnimeSeries.RANMA -> {
            // 1. Ranma Saotome (top-left): Red kung-fu frog clasp & braided pigtail curve
            val rc = Offset(tl.x, tl.y + bob)
            drawCircle(Color(0xFFFB7185), 8.dp.toPx(), rc)
            drawCircle(gold, 3.dp.toPx(), rc)
            drawArc(Color(0xFF38BDF8), 270f, 150f, false, Offset(rc.x - 14.dp.toPx(), rc.y - 4.dp.toPx()), Size(20.dp.toPx(), 24.dp.toPx()), style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))

            // 2. Akane Tendou (top-right): Tendou headband ribbon + mini mallet
            val ac = Offset(tr.x, tr.y - bob)
            drawRoundRect(Color(0xFFFDE047), Offset(ac.x - 10.dp.toPx(), ac.y - 8.dp.toPx()), Size(16.dp.toPx(), 10.dp.toPx()), CornerRadius(2.dp.toPx()))
            drawLine(Color(0xFFA16207), Offset(ac.x - 2.dp.toPx(), ac.y + 2.dp.toPx()), Offset(ac.x - 2.dp.toPx(), ac.y + 14.dp.toPx()), 2.5.dp.toPx(), StrokeCap.Round)
            drawCircle(Color(0xFF38BDF8), 3.5.dp.toPx(), Offset(ac.x + 11.dp.toPx(), ac.y - 6.dp.toPx()))

            // 3. Ryoga Hibiki (bottom-left): Yellow-black bandana + tiny P-chan emblem
            val ryc = Offset(bl.x, bl.y - bob * 0.7f)
            drawRoundRect(Color(0xFFFACC15), Offset(ryc.x - 12.dp.toPx(), ryc.y - 6.dp.toPx()), Size(24.dp.toPx(), 10.dp.toPx()), CornerRadius(3.dp.toPx()))
            drawLine(Color.Black, Offset(ryc.x - 6.dp.toPx(), ryc.y - 6.dp.toPx()), Offset(ryc.x - 2.dp.toPx(), ryc.y + 4.dp.toPx()), 2.dp.toPx())
            drawLine(Color.Black, Offset(ryc.x + 4.dp.toPx(), ryc.y - 6.dp.toPx()), Offset(ryc.x + 8.dp.toPx(), ryc.y + 4.dp.toPx()), 2.dp.toPx())

            // 4. Shampoo (bottom-right): Twin Chinese bonbori hair buns with golden bells
            val shc = Offset(br.x, br.y + bob * 0.7f)
            val lilac = Color(0xFFC084FC)
            drawCircle(lilac, 7.dp.toPx() * pulse, Offset(shc.x - 8.dp.toPx(), shc.y - 2.dp.toPx()))
            drawCircle(lilac, 7.dp.toPx() * pulse, Offset(shc.x + 8.dp.toPx(), shc.y - 2.dp.toPx()))
            drawCircle(gold, 3.dp.toPx(), Offset(shc.x - 8.dp.toPx(), shc.y + 8.dp.toPx()))
            drawCircle(gold, 3.dp.toPx(), Offset(shc.x + 8.dp.toPx(), shc.y + 8.dp.toPx()))
        }
    }
}

private fun DrawScope.drawStarFour(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.28f, center.y - radius * 0.28f)
        lineTo(center.x + radius, center.y)
        lineTo(center.x + radius * 0.28f, center.y + radius * 0.28f)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius * 0.28f, center.y + radius * 0.28f)
        lineTo(center.x - radius, center.y)
        lineTo(center.x - radius * 0.28f, center.y - radius * 0.28f)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawCrescentMoon(center: Offset, radius: Float, color: Color) {
    drawArc(
        color = color,
        startAngle = 40f,
        sweepAngle = 220f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
    )
}
