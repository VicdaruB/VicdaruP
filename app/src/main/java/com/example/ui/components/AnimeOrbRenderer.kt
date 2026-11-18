package com.example.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.model.AnimeSeries
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the climber orb in the app's luminous cyber-zen style, enriched with
 * iconic motifs of the main character for each of the 6 featured anime series.
 */
fun DrawScope.drawAnimeCharacterOrb(
    anime: AnimeSeries,
    center: Offset,
    baseRadiusPx: Float,
    scaleX: Float,
    scaleY: Float,
    animTimeSec: Float,
    isFrozen: Boolean = false,
    isStunned: Boolean = false,
    isSpeedBoosted: Boolean = false,
    alpha: Float = 1f,
) {
    val rx = baseRadiusPx * scaleX
    val ry = baseRadiusPx * scaleY
    val orbW = rx * 2f
    val orbH = ry * 2f

    val orbColors = when {
        isFrozen -> listOf(Color(0xFFBAE6FD), Color(0xFFFBBF24))
        else -> when (anime) {
            AnimeSeries.SAKURA -> listOf(Color(0xFFF9A8D4), Color(0xFF7DD3FC))
            AnimeSeries.FRIEREN -> listOf(Color(0xFFE0F2FE), Color(0xFFC4B5FD))
            AnimeSeries.APOTHECARY -> listOf(Color(0xFF2DD4BF), Color(0xFFF9A8D4))
            AnimeSeries.DANDADAN -> listOf(Color(0xFF22D3EE), Color(0xFFEC4899))
            AnimeSeries.LITTLE_WITCH -> listOf(Color(0xFFA78BFA), Color(0xFFFBBF24))
            AnimeSeries.RANMA -> listOf(Color(0xFF38BDF8), Color(0xFFFB7185))
        }
    }

    val haloColor = when {
        isFrozen -> Color(0xFFFBBF24)
        isStunned -> Color(0xFFD946EF)
        isSpeedBoosted -> Color(0xFF38BDF8)
        else -> orbColors.last()
    }

    // Soft glowing cyber-zen aura
    drawCircle(
        brush = Brush.radialGradient(
            listOf(haloColor.copy(alpha = 0.52f * alpha), Color.Transparent),
            center = center,
            radius = baseRadiusPx * 2.05f
        ),
        radius = baseRadiusPx * 2.05f,
        center = center
    )

    // Back-layer character traits (wings, twin-tails, braids, magic staff ring)
    drawBackMotifs(anime, center, rx, ry, animTimeSec, alpha)

    // Main luminous squash-and-stretch orb body
    drawOval(
        brush = Brush.linearGradient(orbColors.map { it.copy(alpha = alpha) }),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(orbW, orbH)
    )

    // Glossy specular highlight preserving the minimalist glass-orb look
    drawCircle(
        color = Color.White.copy(alpha = 0.72f * alpha),
        radius = baseRadiusPx * 0.28f,
        center = Offset(center.x - rx * 0.25f, center.y - ry * 0.32f)
    )

    // Subtle rim glow
    drawOval(
        color = Color.White.copy(alpha = 0.35f * alpha),
        topLeft = Offset(center.x - rx, center.y - ry),
        size = Size(orbW, orbH),
        style = Stroke(width = 1.4.dp.toPx())
    )

    // Front-layer character motifs (stars, ribbons, hat, mask, kanzashi, martial collar)
    drawFrontMotifs(anime, center, rx, ry, animTimeSec, alpha)
}

private fun DrawScope.drawBackMotifs(
    anime: AnimeSeries,
    c: Offset,
    rx: Float,
    ry: Float,
    t: Float,
    alpha: Float,
) {
    when (anime) {
        AnimeSeries.SAKURA -> {
            // Luminous white angel/Cardcaptor wings fluttering gently
            val flap = sin(t * 7f) * ry * 0.14f
            for (side in listOf(-1f, 1f)) {
                val wing = Path().apply {
                    moveTo(c.x + side * rx * 0.7f, c.y - ry * 0.15f)
                    cubicTo(
                        c.x + side * rx * 1.85f, c.y - ry * 0.95f + flap,
                        c.x + side * rx * 1.9f, c.y + ry * 0.1f + flap * 0.6f,
                        c.x + side * rx * 0.8f, c.y + ry * 0.38f
                    )
                    close()
                }
                drawPath(wing, color = Color.White.copy(alpha = 0.88f * alpha))
                drawPath(
                    wing,
                    color = Color(0xFFF9A8D4).copy(alpha = 0.6f * alpha),
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }

        AnimeSeries.FRIEREN -> {
            // Golden arcane crescent ring & twin silver-white twin-tails + subtle elf ears
            drawCircle(
                color = Color(0xFFFDE047).copy(alpha = 0.55f * alpha),
                radius = rx * 1.42f,
                center = c,
                style = Stroke(width = 1.8.dp.toPx())
            )
            for (side in listOf(-1f, 1f)) {
                // Pointed elf ear silhouette
                val ear = Path().apply {
                    moveTo(c.x + side * rx * 0.82f, c.y - ry * 0.18f)
                    lineTo(c.x + side * rx * 1.46f, c.y - ry * 0.34f)
                    lineTo(c.x + side * rx * 0.88f, c.y + ry * 0.16f)
                    close()
                }
                drawPath(ear, color = Color(0xFFE0F2FE).copy(alpha = 0.9f * alpha))

                // Flowing silver twin-tail
                val sway = sin(t * 4f + side) * rx * 0.1f
                val tail = Path().apply {
                    moveTo(c.x + side * rx * 0.68f, c.y - ry * 0.55f)
                    quadraticTo(
                        c.x + side * rx * 1.45f, c.y + ry * 0.15f,
                        c.x + side * rx * 1.15f + sway, c.y + ry * 1.12f
                    )
                    quadraticTo(
                        c.x + side * rx * 0.82f, c.y + ry * 0.5f,
                        c.x + side * rx * 0.55f, c.y + ry * 0.15f
                    )
                    close()
                }
                drawPath(tail, color = Color(0xFFF1F5F9).copy(alpha = 0.88f * alpha))
                // Crimson diamond earring accent
                drawCircle(
                    color = Color(0xFFEF4444).copy(alpha = 0.9f * alpha),
                    radius = rx * 0.12f,
                    center = Offset(c.x + side * rx * 0.98f, c.y + ry * 0.22f)
                )
            }
        }

        AnimeSeries.APOTHECARY -> {
            // Maomao's twin odango buns on top with jade & blossom hairpins
            for (side in listOf(-1f, 1f)) {
                val bunCenter = Offset(c.x + side * rx * 0.62f, c.y - ry * 0.84f)
                drawCircle(
                    color = Color(0xFF134E4A).copy(alpha = 0.92f * alpha),
                    radius = rx * 0.34f,
                    center = bunCenter
                )
                // Kanzashi hairpin stick with jade gem
                drawLine(
                    color = Color(0xFFFDE047).copy(alpha = 0.9f * alpha),
                    start = Offset(bunCenter.x - side * rx * 0.2f, bunCenter.y + ry * 0.15f),
                    end = Offset(bunCenter.x + side * rx * 0.42f, bunCenter.y - ry * 0.35f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = if (side < 0) Color(0xFF34D399) else Color(0xFFF9A8D4),
                    radius = rx * 0.13f,
                    center = Offset(bunCenter.x + side * rx * 0.42f, bunCenter.y - ry * 0.35f)
                )
            }
        }

        AnimeSeries.DANDADAN -> {
            // Electric turbo aura & Momo's green-blue psychic waves
            for (k in 0..5) {
                val angle = t * 5f + k * (PI / 3.0)
                val sx = c.x + (cos(angle) * rx * 1.05f).toFloat()
                val sy = c.y + (sin(angle) * ry * 1.05f).toFloat()
                val ex = c.x + (cos(angle) * rx * 1.48f).toFloat()
                val ey = c.y + (sin(angle) * ry * 1.48f).toFloat()
                drawLine(
                    color = (if (k % 2 == 0) Color(0xFF22D3EE) else Color(0xFFF472B6)).copy(alpha = 0.8f * alpha),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        AnimeSeries.LITTLE_WITCH -> {
            // Orbiting Shiny Rod golden magic dust stars
            for (s in 0..2) {
                val angle = t * 3.8f + s * (2.0 * PI / 3.0)
                val sx = c.x + (cos(angle) * rx * 1.42f).toFloat()
                val sy = c.y + (sin(angle) * ry * 1.25f).toFloat()
                drawStarShape(
                    center = Offset(sx, sy),
                    outerR = rx * 0.22f,
                    innerR = rx * 0.09f,
                    points = 4,
                    color = Color(0xFFFDE047).copy(alpha = 0.9f * alpha)
                )
            }
        }

        AnimeSeries.RANMA -> {
            // Ranma's iconic braided pigtail + rotating Cold/Hot Yin-Yang splash ring
            val sway = sin(t * 5.2f) * rx * 0.14f
            val braid = Path().apply {
                moveTo(c.x + rx * 0.35f, c.y - ry * 0.72f)
                quadraticTo(
                    c.x + rx * 1.35f, c.y - ry * 0.1f,
                    c.x + rx * 1.05f + sway, c.y + ry * 0.95f
                )
                lineTo(c.x + rx * 0.78f + sway, c.y + ry * 0.92f)
                quadraticTo(
                    c.x + rx * 1.02f, c.y + ry * 0.0f,
                    c.x + rx * 0.18f, c.y - ry * 0.62f
                )
                close()
            }
            drawPath(braid, color = Color(0xFF0F172A).copy(alpha = 0.9f * alpha))
            // Red hair ribbon tie
            drawCircle(
                color = Color(0xFFFB7185).copy(alpha = alpha),
                radius = rx * 0.16f,
                center = Offset(c.x + rx * 0.5f, c.y - ry * 0.66f)
            )
            // Cold/Hot water Yin-Yang orbital dots
            val angle = t * 3.5f
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.85f * alpha),
                radius = rx * 0.16f,
                center = Offset(c.x + (cos(angle) * rx * 1.38f).toFloat(), c.y + (sin(angle) * ry * 1.38f).toFloat())
            )
            drawCircle(
                color = Color(0xFFFB7185).copy(alpha = 0.85f * alpha),
                radius = rx * 0.16f,
                center = Offset(c.x - (cos(angle) * rx * 1.38f).toFloat(), c.y - (sin(angle) * ry * 1.38f).toFloat())
            )
        }
    }
}

private fun DrawScope.drawFrontMotifs(
    anime: AnimeSeries,
    c: Offset,
    rx: Float,
    ry: Float,
    t: Float,
    alpha: Float,
) {
    when (anime) {
        AnimeSeries.SAKURA -> {
            // Sakura's golden Star Wand emblem at the center + top pink ribbon bow
            drawStarShape(
                center = Offset(c.x, c.y + ry * 0.08f),
                outerR = rx * 0.42f,
                innerR = rx * 0.19f,
                points = 5,
                color = Color(0xFFFDE047).copy(alpha = 0.95f * alpha)
            )
            // Pink bow on top
            val leftBow = Path().apply {
                moveTo(c.x, c.y - ry * 0.88f)
                lineTo(c.x - rx * 0.42f, c.y - ry * 1.15f)
                lineTo(c.x - rx * 0.35f, c.y - ry * 0.72f)
                close()
            }
            val rightBow = Path().apply {
                moveTo(c.x, c.y - ry * 0.88f)
                lineTo(c.x + rx * 0.42f, c.y - ry * 1.15f)
                lineTo(c.x + rx * 0.35f, c.y - ry * 0.72f)
                close()
            }
            drawPath(leftBow, color = Color(0xFFEC4899).copy(alpha = 0.92f * alpha))
            drawPath(rightBow, color = Color(0xFFEC4899).copy(alpha = 0.92f * alpha))
        }

        AnimeSeries.FRIEREN -> {
            // Frieren's golden staff crescent ring + ruby core emblem on front
            drawCircle(
                color = Color(0xFFFBBF24).copy(alpha = 0.95f * alpha),
                radius = rx * 0.34f,
                center = Offset(c.x, c.y + ry * 0.12f),
                style = Stroke(width = 2.4.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFEF4444).copy(alpha = 0.95f * alpha),
                radius = rx * 0.16f,
                center = Offset(c.x, c.y + ry * 0.12f)
            )
        }

        AnimeSeries.APOTHECARY -> {
            // Maomao's signature subtle freckles + glowing medicinal herb leaf emblem
            for (side in listOf(-1f, 1f)) {
                for (d in -1..1) {
                    drawCircle(
                        color = Color(0xFF134E4A).copy(alpha = 0.65f * alpha),
                        radius = rx * 0.045f,
                        center = Offset(c.x + side * rx * 0.42f + d * rx * 0.08f, c.y + ry * 0.06f + (d % 2) * ry * 0.05f)
                    )
                }
            }
            val leaf = Path().apply {
                moveTo(c.x, c.y + ry * 0.56f)
                quadraticTo(c.x - rx * 0.36f, c.y + ry * 0.24f, c.x, c.y - ry * 0.02f)
                quadraticTo(c.x + rx * 0.36f, c.y + ry * 0.24f, c.x, c.y + ry * 0.56f)
                close()
            }
            drawPath(leaf, color = Color(0xFF047857).copy(alpha = 0.85f * alpha))
            drawLine(
                color = Color(0xFFA7F3D0).copy(alpha = 0.9f * alpha),
                start = Offset(c.x, c.y + ry * 0.52f),
                end = Offset(c.x, c.y + ry * 0.06f),
                strokeWidth = 1.4.dp.toPx()
            )
        }

        AnimeSeries.DANDADAN -> {
            // Okarun's glowing round glasses + Turbo Granny jagged teeth mask on lower half
            // Mask lower half
            drawArc(
                color = Color.White.copy(alpha = 0.92f * alpha),
                startAngle = 15f,
                sweepAngle = 150f,
                useCenter = true,
                topLeft = Offset(c.x - rx * 0.82f, c.y - ry * 0.2f),
                size = Size(rx * 1.64f, ry * 1.05f)
            )
            // Jagged mask teeth lines
            for (i in -2..2) {
                val tx = c.x + i * rx * 0.24f
                drawLine(
                    color = Color(0xFF0F172A).copy(alpha = 0.9f * alpha),
                    start = Offset(tx, c.y + ry * 0.34f),
                    end = Offset(tx, c.y + ry * 0.62f),
                    strokeWidth = 1.6.dp.toPx()
                )
            }
            // Round glasses
            val glassColor = Color(0xFF38BDF8).copy(alpha = 0.95f * alpha)
            drawCircle(
                color = glassColor,
                radius = rx * 0.24f,
                center = Offset(c.x - rx * 0.32f, c.y - ry * 0.16f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = glassColor,
                radius = rx * 0.24f,
                center = Offset(c.x + rx * 0.32f, c.y - ry * 0.16f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawLine(
                color = glassColor,
                start = Offset(c.x - rx * 0.08f, c.y - ry * 0.16f),
                end = Offset(c.x + rx * 0.08f, c.y - ry * 0.16f),
                strokeWidth = 2.dp.toPx()
            )
        }

        AnimeSeries.LITTLE_WITCH -> {
            // Akko's iconic Luna Nova witch hat perched on top + golden Shiny Arc star
            val brimY = c.y - ry * 0.66f
            val hat = Path().apply {
                moveTo(c.x - rx * 0.86f, brimY)
                lineTo(c.x + rx * 0.86f, brimY)
                lineTo(c.x + rx * 0.48f, brimY - ry * 0.22f)
                lineTo(c.x - rx * 0.12f, brimY - ry * 1.08f)
                lineTo(c.x - rx * 0.52f, brimY - ry * 0.22f)
                close()
            }
            drawPath(hat, color = Color(0xFF312E81).copy(alpha = 0.96f * alpha))
            // Crimson-gold hat band
            drawLine(
                color = Color(0xFFF59E0B).copy(alpha = 0.95f * alpha),
                start = Offset(c.x - rx * 0.52f, brimY - ry * 0.14f),
                end = Offset(c.x + rx * 0.48f, brimY - ry * 0.14f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawStarShape(
                center = Offset(c.x, c.y + ry * 0.15f),
                outerR = rx * 0.34f,
                innerR = rx * 0.14f,
                points = 4,
                color = Color(0xFFFDE047).copy(alpha = 0.95f * alpha)
            )
        }

        AnimeSeries.RANMA -> {
            // Ranma's red Chinese martial arts tunic collar + golden frog button clasp
            drawArc(
                color = Color(0xFFE11D48).copy(alpha = 0.92f * alpha),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(c.x - rx * 0.84f, c.y + ry * 0.18f),
                size = Size(rx * 1.68f, ry * 0.72f),
                style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
            )
            // Golden tunic clasp at center
            drawLine(
                color = Color(0xFFFDE047).copy(alpha = 0.95f * alpha),
                start = Offset(c.x - rx * 0.24f, c.y + ry * 0.36f),
                end = Offset(c.x + rx * 0.24f, c.y + ry * 0.36f),
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawCircle(
                color = Color(0xFFFDE047).copy(alpha = 0.95f * alpha),
                radius = rx * 0.1f,
                center = Offset(c.x, c.y + ry * 0.36f)
            )
        }
    }
}

private fun DrawScope.drawStarShape(
    center: Offset,
    outerR: Float,
    innerR: Float,
    points: Int,
    color: Color,
) {
    val path = Path()
    val total = points * 2
    for (i in 0 until total) {
        val r = if (i % 2 == 0) outerR else innerR
        val angle = -PI / 2.0 + i * (PI / points)
        val px = center.x + (cos(angle) * r).toFloat()
        val py = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color = color)
}
