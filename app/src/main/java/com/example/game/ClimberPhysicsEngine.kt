package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.VicdaruSkyBlue
import com.example.ui.theme.VicdaruSoftPink
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class PlatformNode(
    val xRatio: Float,
    var yRatio: Float,
    val widthRatio: Float,
    val isBoost: Boolean,
)

enum class ClimberItemType {
    SPEED_BOOST,
    PAUSE_TRAP,
    STUN,
}

data class ClimberItem(
    val id: Long,
    val xRatio: Float,
    var yRatio: Float,
    val type: ClimberItemType,
)

data class BounceRipple(
    val xRatio: Float,
    val yRatio: Float,
    var progress: Float,
    val color: Color,
)

sealed interface ClimberPhysicsEvent {
    data object GameStarted : ClimberPhysicsEvent
    data object SteerMoved : ClimberPhysicsEvent
    data class Bounced(val isBoostPlatform: Boolean, val impactIntensity: Float) : ClimberPhysicsEvent
    data object SpeedBoostPickedUp : ClimberPhysicsEvent
    data object PauseTrapTriggered : ClimberPhysicsEvent
    data object PauseTrapReleased : ClimberPhysicsEvent
    data object StunTriggered : ClimberPhysicsEvent
    data object DailyChallengeReached : ClimberPhysicsEvent
    data class GameOver(val finalMeters: Int) : ClimberPhysicsEvent
}

/**
 * Pure Kotlin frame-independent (delta-time) physics & swept-collision engine
 * designed to be driven by Jetpack Compose's `withFrameNanos` clock and `Animatable` springs.
 */
class ClimberPhysicsEngine(
    val dailyGoalMeters: Int = 250,
    private val random: Random = Random.Default,
) {
    companion object {
        const val GRAVITY_NORMAL = 3.78f
        const val GRAVITY_SPEED_BOOST = 2.59f
        const val BOUNCE_VELOCITY_NORMAL = -1.53f
        const val BOUNCE_VELOCITY_BOOST_PLATFORM = -2.16f
        const val BOUNCE_VELOCITY_SPEED_BOOST_ITEM = -2.52f
        const val SPEED_BOOST_MIN_RISE_VY = -1.32f
        const val TRAP_RELEASE_VELOCITY = -1.38f
        const val HORIZONTAL_MAX_SPEED = 0.96f
        const val HORIZONTAL_STEER_STEP = 0.088f
        const val SCROLL_THRESHOLD_Y = 0.42f
        const val ITEM_HIT_RADIUS = 0.070f
        const val ORB_FOOT_OFFSET = 0.026f
        const val PAUSE_TRAP_DURATION_MS = 2000L
        const val SPEED_BOOST_DURATION_MS = 2600L
        const val STUN_DURATION_MS = 1800L
    }

    var isPlaying: Boolean = false
        private set
    var isGameOver: Boolean = false
        private set

    var playerX: Float = 0.5f
    var playerY: Float = 0.76f
    var velocityX: Float = 0f
    var velocityY: Float = -1.53f
    var meters: Int = 0
        private set

    var speedBoostRemainingMs: Long = 0L
    var freezeRemainingMs: Long = 0L
    var stunRemainingMs: Long = 0L
    var animTimeSec: Float = 0f
        private set

    private var dailyChallengeAnnounced: Boolean = false
    private var nextItemId: Long = 100L

    val platforms: MutableList<PlatformNode> = mutableListOf()
    val items: MutableList<ClimberItem> = mutableListOf()
    val bounceRipples: MutableList<BounceRipple> = mutableListOf()

    init {
        seedInitialWorld(preserveStoppedState = true)
    }

    fun startNewRun(): List<ClimberPhysicsEvent> {
        seedInitialWorld(preserveStoppedState = false)
        isPlaying = true
        return listOf(ClimberPhysicsEvent.GameStarted)
    }

    fun stopRun() {
        isPlaying = false
    }

    fun tickIdle(dtSec: Float) {
        animTimeSec += dtSec.coerceIn(0f, 0.05f)
    }

    fun applySteerImpulse(goLeft: Boolean): ClimberPhysicsEvent? {
        if (!isPlaying || freezeRemainingMs > 0L) return null
        val effectiveLeft = if (stunRemainingMs > 0L) !goLeft else goLeft
        val dir = if (effectiveLeft) -1f else 1f
        playerX = (playerX + dir * HORIZONTAL_STEER_STEP).coerceIn(0.06f, 0.94f)
        velocityX = dir * 0.48f
        return ClimberPhysicsEvent.SteerMoved
    }

    /**
     * Advances the physical simulation by [dtSec] (clamped to prevent tunneling on frame drops).
     * [horizontalInput] is -1 (left), 0 (idle), or +1 (right).
     */
    fun step(dtSec: Float, horizontalInput: Int = 0): List<ClimberPhysicsEvent> {
        val dt = dtSec.coerceIn(0.001f, 0.035f)
        animTimeSec += dt
        val events = mutableListOf<ClimberPhysicsEvent>()

        // Decay ripples
        for (idx in bounceRipples.indices.reversed()) {
            val ripple = bounceRipples[idx]
            val nextProg = ripple.progress + dt * 3.6f
            if (nextProg >= 1f) {
                bounceRipples.removeAt(idx)
            } else {
                bounceRipples[idx] = ripple.copy(progress = nextProg)
            }
        }

        if (!isPlaying) return events

        val dtMs = (dt * 1000f).toLong().coerceAtLeast(1L)
        if (speedBoostRemainingMs > 0L) {
            speedBoostRemainingMs = (speedBoostRemainingMs - dtMs).coerceAtLeast(0L)
        }
        if (stunRemainingMs > 0L) {
            stunRemainingMs = (stunRemainingMs - dtMs).coerceAtLeast(0L)
        }

        // Pause trap holds the climber suspended in place for ~2 seconds
        if (freezeRemainingMs > 0L) {
            freezeRemainingMs = (freezeRemainingMs - dtMs).coerceAtLeast(0L)
            if (freezeRemainingMs == 0L) {
                velocityY = TRAP_RELEASE_VELOCITY
                bounceRipples.add(BounceRipple(playerX, playerY, 0f, Color(0xFF7DD3FC)))
                events.add(ClimberPhysicsEvent.PauseTrapReleased)
            }
            return events
        }

        val prevX = playerX
        val prevY = playerY

        // Horizontal movement with inertial damping & stun wobble
        val targetVx = if (horizontalInput != 0) {
            val effectiveDir = if (stunRemainingMs > 0L) -horizontalInput else horizontalInput
            effectiveDir * HORIZONTAL_MAX_SPEED
        } else {
            0f
        }
        val damping = (dt * 12f).coerceIn(0f, 1f)
        velocityX += (targetVx - velocityX) * damping
        val stunWobble = if (stunRemainingMs > 0L) sin(animTimeSec * 18f) * 0.45f * dt else 0f
        playerX = (playerX + velocityX * dt + stunWobble).coerceIn(0.06f, 0.94f)

        // Vertical gravity + speed boost anti-gravity lift
        val gravity = if (speedBoostRemainingMs > 0L) GRAVITY_SPEED_BOOST else GRAVITY_NORMAL
        velocityY += gravity * dt
        if (speedBoostRemainingMs > 0L && velocityY > -0.84f) {
            velocityY = SPEED_BOOST_MIN_RISE_VY
        }
        playerY += velocityY * dt

        // Swept platform collision when descending
        if (velocityY > 0f) {
            val prevFoot = prevY + ORB_FOOT_OFFSET
            val currFoot = playerY + ORB_FOOT_OFFSET
            for (p in platforms) {
                val withinX = abs(playerX - p.xRatio) <= (p.widthRatio * 0.56f)
                val crossedTop = prevFoot <= (p.yRatio + 0.032f) && currFoot >= (p.yRatio - 0.015f)
                if (withinX && crossedTop) {
                    playerY = (p.yRatio - ORB_FOOT_OFFSET).coerceAtMost(playerY)
                    val impact = (velocityY / 2.0f).coerceIn(0.6f, 1.35f)
                    velocityY = when {
                        speedBoostRemainingMs > 0L -> -2.34f
                        p.isBoost -> BOUNCE_VELOCITY_BOOST_PLATFORM
                        else -> BOUNCE_VELOCITY_NORMAL
                    }
                    bounceRipples.add(
                        BounceRipple(
                            xRatio = playerX,
                            yRatio = p.yRatio,
                            progress = 0f,
                            color = if (p.isBoost) VicdaruSoftPink else VicdaruSkyBlue
                        )
                    )
                    if (p.isBoost) {
                        meters += 12
                    }
                    events.add(ClimberPhysicsEvent.Bounced(isBoostPlatform = p.isBoost, impactIntensity = impact))
                    break
                }
            }
        }

        // Swept segment-to-circle collision for traps & boosters
        for (idx in items.indices.reversed()) {
            val item = items[idx]
            val dist = pointToSegmentDistance(
                px = item.xRatio,
                py = item.yRatio,
                ax = prevX,
                ay = prevY,
                bx = playerX,
                by = playerY
            )
            if (dist <= ITEM_HIT_RADIUS) {
                items.removeAt(idx)
                when (item.type) {
                    ClimberItemType.SPEED_BOOST -> {
                        speedBoostRemainingMs = SPEED_BOOST_DURATION_MS
                        velocityY = BOUNCE_VELOCITY_SPEED_BOOST_ITEM
                        meters += 25
                        bounceRipples.add(BounceRipple(playerX, playerY, 0f, Color(0xFF38BDF8)))
                        events.add(ClimberPhysicsEvent.SpeedBoostPickedUp)
                    }
                    ClimberItemType.PAUSE_TRAP -> {
                        freezeRemainingMs = PAUSE_TRAP_DURATION_MS
                        velocityY = 0f
                        velocityX = 0f
                        speedBoostRemainingMs = 0L
                        bounceRipples.add(BounceRipple(playerX, playerY, 0f, Color(0xFFF59E0B)))
                        events.add(ClimberPhysicsEvent.PauseTrapTriggered)
                    }
                    ClimberItemType.STUN -> {
                        stunRemainingMs = STUN_DURATION_MS
                        velocityY = maxOf(velocityY, -0.60f)
                        bounceRipples.add(BounceRipple(playerX, playerY, 0f, Color(0xFFC084FC)))
                        events.add(ClimberPhysicsEvent.StunTriggered)
                    }
                }
            }
        }

        // Scroll camera upward as climber ascends past threshold
        if (playerY < SCROLL_THRESHOLD_Y) {
            val shift = SCROLL_THRESHOLD_Y - playerY
            playerY = SCROLL_THRESHOLD_Y
            meters += (shift * 140f).toInt().coerceAtLeast(1)

            for (idx in platforms.indices) {
                val p = platforms[idx]
                val newY = p.yRatio + shift
                if (newY > 0.98f) {
                    val spawnedX = 0.14f + random.nextFloat() * 0.72f
                    val spawnedY = newY - 1.0f
                    platforms[idx] = PlatformNode(
                        xRatio = spawnedX,
                        yRatio = spawnedY,
                        widthRatio = (0.24f - (meters / 3000f)).coerceAtLeast(0.15f),
                        isBoost = random.nextFloat() < 0.24f
                    )
                    maybeSpawnItem(spawnedX, spawnedY)
                } else {
                    platforms[idx] = p.copy(yRatio = newY)
                }
            }

            for (idx in items.indices.reversed()) {
                val item = items[idx]
                val newItemY = item.yRatio + shift
                if (newItemY > 1.02f) {
                    items.removeAt(idx)
                } else {
                    items[idx] = item.copy(yRatio = newItemY)
                }
            }

            for (idx in bounceRipples.indices) {
                val r = bounceRipples[idx]
                bounceRipples[idx] = r.copy(yRatio = r.yRatio + shift)
            }
        }

        if (!dailyChallengeAnnounced && meters >= dailyGoalMeters) {
            dailyChallengeAnnounced = true
            events.add(ClimberPhysicsEvent.DailyChallengeReached)
        }

        if (playerY > 1.04f) {
            isPlaying = false
            isGameOver = true
            events.add(ClimberPhysicsEvent.GameOver(meters))
        }

        return events
    }

    private fun seedInitialWorld(preserveStoppedState: Boolean) {
        playerX = 0.5f
        playerY = 0.76f
        velocityX = 0f
        velocityY = BOUNCE_VELOCITY_NORMAL
        meters = 0
        isGameOver = false
        if (preserveStoppedState) isPlaying = false
        speedBoostRemainingMs = 0L
        freezeRemainingMs = 0L
        stunRemainingMs = 0L
        dailyChallengeAnnounced = false
        bounceRipples.clear()
        items.clear()
        platforms.clear()

        platforms.add(PlatformNode(0.5f, 0.86f, 0.36f, false))
        for (i in 1..8) {
            val px = 0.16f + random.nextFloat() * 0.68f
            val py = 0.86f - i * 0.105f
            platforms.add(
                PlatformNode(
                    xRatio = px,
                    yRatio = py,
                    widthRatio = 0.24f,
                    isBoost = i % 4 == 0
                )
            )
            if (i == 3) {
                nextItemId += 1L
                items.add(ClimberItem(nextItemId, px, py - 0.055f, ClimberItemType.SPEED_BOOST))
            } else if (i == 5) {
                nextItemId += 1L
                items.add(ClimberItem(nextItemId, px, py - 0.055f, ClimberItemType.PAUSE_TRAP))
            } else if (i >= 6) {
                maybeSpawnItem(px, py)
            }
        }
    }

    private fun maybeSpawnItem(xRatio: Float, yRatio: Float) {
        val roll = random.nextFloat()
        val type = when {
            roll < 0.26f -> ClimberItemType.SPEED_BOOST // Potenciador de velocidad
            roll < 0.45f -> ClimberItemType.PAUSE_TRAP  // Trampa que detiene 2s
            roll < 0.53f -> ClimberItemType.STUN        // Aturdimiento en menor cantidad (~8%)
            else -> null
        }
        if (type != null) {
            nextItemId += 1L
            items.add(
                ClimberItem(
                    id = nextItemId,
                    xRatio = xRatio.coerceIn(0.10f, 0.90f),
                    yRatio = (yRatio - 0.055f).coerceIn(-0.15f, 0.92f),
                    type = type
                )
            )
        }
    }

    private fun pointToSegmentDistance(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val abLenSq = abx * abx + aby * aby
        if (abLenSq <= 1e-7f) {
            return hypot((px - ax).toDouble(), (py - ay).toDouble()).toFloat()
        }
        val t = (((px - ax) * abx + (py - ay) * aby) / abLenSq).coerceIn(0f, 1f)
        val closestX = ax + t * abx
        val closestY = ay + t * aby
        return hypot((px - closestX).toDouble(), (py - closestY).toDouble()).toFloat()
    }
}
