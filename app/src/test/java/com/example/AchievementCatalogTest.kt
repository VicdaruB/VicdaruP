package com.example

import com.example.model.AnimeSeries
import com.example.model.MedalsAndAvatarsCatalog
import com.example.model.PublicProfile
import com.example.ui.components.VisualizerStyleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementCatalogTest {

    @Test
    fun `multi-level achievements scale strictly at 1_5x and logical milestones stay single-level`() {
        val firstQuote = MedalsAndAvatarsCatalog.medals.first { it.id == "first_quote" }
        val socialBond = MedalsAndAvatarsCatalog.medals.first { it.id == "social_bond" }
        val socialCircle = MedalsAndAvatarsCatalog.medals.first { it.id == "social_circle" }

        assertEquals(1, firstQuote.maxLevel)
        assertEquals(1, socialBond.maxLevel)
        assertEquals(5, socialCircle.maxLevel)

        val tenTierMedals = MedalsAndAvatarsCatalog.medals.filter { it.maxLevel == 10 }
        assertTrue("Expected at least 10 progressive 10-level achievements", tenTierMedals.size >= 10)

        MedalsAndAvatarsCatalog.medals.filter { it.maxLevel > 1 }.forEach { def ->
            var prev = def.thresholdForLevel(1)
            assertEquals(def.baseRequirement, prev)
            for (tier in 2..def.maxLevel) {
                val curr = def.thresholdForLevel(tier)
                assertTrue(
                    "Threshold for ${def.id} tier $tier ($curr) must exceed tier ${tier - 1} ($prev)",
                    curr > prev
                )
                assertTrue(
                    "Threshold for ${def.id} tier $tier ($curr) should scale ~1.5x from $prev",
                    curr >= (prev * 1.4).toLong()
                )
                prev = curr
            }
        }
    }

    @Test
    fun `evaluateAchievements computes unlocked level, next target, and maxed state accurately`() {
        val profile = PublicProfile(
            userId = "u1",
            displayName = "Sakura",
            friendCode = "VD-123456",
            totalFocusMinutes = 60L,
            climberBestMeters = 600L,
            quotesCount = 10L,
            maxEmotionalLevel = 5L,
            medals = listOf("first_quote")
        )

        val evaluated = MedalsAndAvatarsCatalog.evaluateAchievements(
            profile = profile,
            liveQuotesCount = 10,
            liveFavoritesCount = 3,
            liveHabitsCompleted = 12,
            liveBestStreak = 5,
            liveFriendsCount = 2
        ).associateBy { it.definition.id }

        val firstQuote = evaluated.getValue("first_quote")
        assertEquals(1, firstQuote.unlockedLevel)
        assertTrue(firstQuote.isMaxed)
        assertNull(firstQuote.nextLevelTarget)

        val quotesCollector = evaluated.getValue("quotes_collector")
        // base 4 -> Nv1=4, Nv2=6, Nv3=9, Nv4=14; 10 quotes => Nv. 3
        assertEquals(3, quotesCollector.unlockedLevel)
        assertEquals(14L, quotesCollector.nextLevelTarget)

        val emotional = evaluated.getValue("emotional_friendship")
        // base 2 -> Nv1=2, Nv2=3, Nv3=5, Nv4=8; level 5 => Nv. 3
        assertEquals(3, emotional.unlockedLevel)
        assertEquals(8L, emotional.nextLevelTarget)
    }

    @Test
    fun `all 6 anime presets have unique non-repeating visualizer styles`() {
        val styles = AnimeSeries.entries.map { it.defaultWaveformStyle }
        assertEquals(6, styles.distinct().size)
        styles.forEach { style ->
            assertTrue(
                "Style $style must exist in VisualizerStyleCatalog",
                VisualizerStyleCatalog.allStyles.any { it.key == style }
            )
        }
    }
}
