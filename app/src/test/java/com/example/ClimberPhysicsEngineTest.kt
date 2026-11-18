package com.example

import com.example.game.ClimberItem
import com.example.game.ClimberItemType
import com.example.game.ClimberPhysicsEngine
import com.example.game.ClimberPhysicsEvent
import com.example.game.PlatformNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ClimberPhysicsEngineTest {

    @Test
    fun sweptPlatformCollision_bouncesNormalAndBoostPlatform() {
        val engine = ClimberPhysicsEngine(random = Random(42))
        engine.startNewRun()
        engine.items.clear()
        engine.platforms.clear()
        engine.platforms.add(PlatformNode(xRatio = 0.5f, yRatio = 0.80f, widthRatio = 0.30f, isBoost = true))

        // Place orb right above the platform falling downward fast
        engine.playerX = 0.5f
        engine.playerY = 0.76f
        engine.velocityY = 1.6f

        val events = engine.step(0.025f)
        assertTrue(events.any { it is ClimberPhysicsEvent.Bounced && it.isBoostPlatform })
        assertTrue("Velocity should reverse upward after bounce", engine.velocityY < 0f)
        assertTrue("Boost platform awards bonus meters", engine.meters >= 12)
    }

    @Test
    fun pauseTrapCollision_freezesForTwoSecondsAndReleases() {
        val engine = ClimberPhysicsEngine(random = Random(7))
        engine.startNewRun()
        engine.platforms.clear()
        engine.items.clear()
        engine.items.add(ClimberItem(id = 1L, xRatio = 0.5f, yRatio = 0.65f, type = ClimberItemType.PAUSE_TRAP))

        engine.playerX = 0.5f
        engine.playerY = 0.65f
        engine.velocityY = 0.5f
        engine.speedBoostRemainingMs = 1500L

        val hitEvents = engine.step(0.016f)
        assertTrue(hitEvents.contains(ClimberPhysicsEvent.PauseTrapTriggered))
        assertEquals(0L, engine.speedBoostRemainingMs)
        assertTrue(engine.freezeRemainingMs > 1900L)

        // Advance time while frozen -> position stays locked
        val lockedY = engine.playerY
        engine.step(0.030f)
        assertEquals(lockedY, engine.playerY, 0.0001f)

        // Advance ~2 seconds in small steps until trap releases
        val collected = mutableListOf<ClimberPhysicsEvent>()
        repeat(80) {
            collected += engine.step(0.030f)
        }
        assertTrue(collected.contains(ClimberPhysicsEvent.PauseTrapReleased))
        assertEquals(0L, engine.freezeRemainingMs)
    }

    @Test
    fun speedBoostAndStunCollisions_emitEventsAndInvertSteerWhenStunned() {
        val engine = ClimberPhysicsEngine(random = Random(99))
        engine.startNewRun()
        engine.platforms.clear()
        engine.items.clear()
        engine.items.add(ClimberItem(id = 2L, xRatio = 0.5f, yRatio = 0.60f, type = ClimberItemType.SPEED_BOOST))
        engine.items.add(ClimberItem(id = 3L, xRatio = 0.5f, yRatio = 0.58f, type = ClimberItemType.STUN))

        engine.playerX = 0.5f
        engine.playerY = 0.59f

        val events = engine.step(0.016f)
        assertTrue(events.contains(ClimberPhysicsEvent.SpeedBoostPickedUp))
        assertTrue(events.contains(ClimberPhysicsEvent.StunTriggered))
        assertTrue(engine.speedBoostRemainingMs > 0L)
        assertTrue(engine.stunRemainingMs > 0L)

        val beforeX = engine.playerX
        val steerEvent = engine.applySteerImpulse(goLeft = true)
        assertEquals(ClimberPhysicsEvent.SteerMoved, steerEvent)
        assertTrue("Stun should invert left tap to move right", engine.playerX > beforeX)
    }

    @Test
    fun fallingBelowScreen_triggersGameOver() {
        val engine = ClimberPhysicsEngine(random = Random(123))
        engine.startNewRun()
        engine.platforms.clear()
        engine.items.clear()
        engine.playerY = 1.03f
        engine.velocityY = 1.2f

        val events = engine.step(0.030f)
        assertTrue(events.any { it is ClimberPhysicsEvent.GameOver })
        assertFalse(engine.isPlaying)
        assertTrue(engine.isGameOver)
    }
}
