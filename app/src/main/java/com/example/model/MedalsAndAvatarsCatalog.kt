package com.example.model

import kotlin.math.roundToLong

data class ClimberAvatar(
    val id: String,
    val name: String,
    val anime: AnimeSeries,
    val unlockMeters: Int,
    val badgeSymbol: String,
    val description: String,
)

enum class AchievementMetric {
    FIRST_QUOTE,
    QUOTES_COLLECTED,
    FAVORITES_SAVED,
    HABITS_COMPLETED,
    BEST_HABIT_STREAK,
    FOCUS_MINUTES,
    CLIMBER_BEST_METERS,
    DAILY_CHALLENGES_WON,
    FIRST_FRIEND,
    FRIENDS_COUNT,
    DUO_CLIMBER_WINS,
    DUO_MUSIC_WINS,
    MAX_EMOTIONAL_LEVEL,
}

data class MedalDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val metric: AchievementMetric,
    val baseRequirement: Long,
    val maxLevel: Int = 10,
    val unitLabel: String = "",
    val isDuoMedal: Boolean = false,
) {
    /**
     * Computes strictly increasing thresholds scaling at ~1.5x per tier.
     */
    fun thresholdForLevel(level: Int): Long {
        if (level <= 1) return baseRequirement
        var current = baseRequirement
        for (tier in 2..level.coerceAtMost(maxLevel)) {
            val scaled = (current * 1.5).roundToLong()
            current = maxOf(current + 1L, scaled)
        }
        return current
    }
}

data class AchievementProgress(
    val definition: MedalDefinition,
    val currentValue: Long,
    val unlockedLevel: Int,
    val nextLevelTarget: Long?,
) {
    val isMaxed: Boolean get() = unlockedLevel >= definition.maxLevel
    val hasAnyLevel: Boolean get() = unlockedLevel > 0
    val progressFraction: Float
        get() {
            if (isMaxed) return 1f
            val target = nextLevelTarget ?: return 1f
            val prev = if (unlockedLevel <= 0) 0L else definition.thresholdForLevel(unlockedLevel)
            val span = (target - prev).coerceAtLeast(1L)
            return ((currentValue - prev).toFloat() / span.toFloat()).coerceIn(0f, 1f)
        }
}

object MedalsAndAvatarsCatalog {
    val avatars: List<ClimberAvatar> = listOf(
        ClimberAvatar(
            id = "sakura",
            name = "Sakura Estrella",
            anime = AnimeSeries.SAKURA,
            unlockMeters = 0,
            badgeSymbol = "★",
            description = "Desbloqueado desde el inicio. Impulso ligero y sereno."
        ),
        ClimberAvatar(
            id = "frieren",
            name = "Frieren Viajera",
            anime = AnimeSeries.FRIEREN,
            unlockMeters = 120,
            badgeSymbol = "✦",
            description = "Alcanza 120 m en el minijuego de escalada."
        ),
        ClimberAvatar(
            id = "maomao",
            name = "Maomao Boticaria",
            anime = AnimeSeries.APOTHECARY,
            unlockMeters = 250,
            badgeSymbol = "❀",
            description = "Alcanza 250 m en escalada o completa desafíos."
        ),
        ClimberAvatar(
            id = "okarun",
            name = "Okarun Turbo",
            anime = AnimeSeries.DANDADAN,
            unlockMeters = 400,
            badgeSymbol = "⚡",
            description = "Alcanza 400 m en escalada con ritmo veloz."
        ),
        ClimberAvatar(
            id = "akko",
            name = "Akko Luna Nova",
            anime = AnimeSeries.LITTLE_WITCH,
            unlockMeters = 600,
            badgeSymbol = "✧",
            description = "Alcanza 600 m superando nubes de turbulencia."
        ),
        ClimberAvatar(
            id = "ranma",
            name = "Ranma Dojo",
            anime = AnimeSeries.RANMA,
            unlockMeters = 850,
            badgeSymbol = "◈",
            description = "Maestro escalador al llegar a 850 m."
        )
    )

    val medals: List<MedalDefinition> = listOf(
        // Hito único lógico (1 nivel)
        MedalDefinition(
            id = "first_quote",
            title = "Primer Rayo",
            description = "Recibiste tu primera frase motivacional.",
            category = "Bienestar",
            metric = AchievementMetric.FIRST_QUOTE,
            baseRequirement = 1L,
            maxLevel = 1,
            unitLabel = "frase"
        ),
        // Logros progresivos de hasta 10 niveles (escala 1.5x por nivel)
        MedalDefinition(
            id = "quotes_collector",
            title = "Coleccionista de Luz",
            description = "Acumula frases inspiradoras en tu historial.",
            category = "Bienestar",
            metric = AchievementMetric.QUOTES_COLLECTED,
            baseRequirement = 4L,
            maxLevel = 10,
            unitLabel = "frases"
        ),
        MedalDefinition(
            id = "favorite_keeper",
            title = "Guardián de Luz",
            description = "Guarda tus citas anime más queridas en favoritos.",
            category = "Bienestar",
            metric = AchievementMetric.FAVORITES_SAVED,
            baseRequirement = 3L,
            maxLevel = 10,
            unitLabel = "favoritas"
        ),
        MedalDefinition(
            id = "habit_spark",
            title = "Ritmo Constante",
            description = "Completa registros diarios de hábitos saludables.",
            category = "Hábitos",
            metric = AchievementMetric.HABITS_COMPLETED,
            baseRequirement = 5L,
            maxLevel = 10,
            unitLabel = "registros"
        ),
        MedalDefinition(
            id = "habit_streak",
            title = "Racha Inquebrantable",
            description = "Mantén rachas continuas de hábitos día tras día.",
            category = "Hábitos",
            metric = AchievementMetric.BEST_HABIT_STREAK,
            baseRequirement = 3L,
            maxLevel = 10,
            unitLabel = "días"
        ),
        MedalDefinition(
            id = "deep_flow_focus",
            title = "Mente Serena",
            description = "Acumula minutos de concentración en el estudio musical.",
            category = "Enfoque",
            metric = AchievementMetric.FOCUS_MINUTES,
            baseRequirement = 15L,
            maxLevel = 10,
            unitLabel = "min"
        ),
        MedalDefinition(
            id = "climber_altitude",
            title = "Escalador Astral",
            description = "Alcanza nuevas alturas récord en el minijuego de escalada.",
            category = "Escalada",
            metric = AchievementMetric.CLIMBER_BEST_METERS,
            baseRequirement = 120L,
            maxLevel = 10,
            unitLabel = "m"
        ),
        MedalDefinition(
            id = "daily_victor",
            title = "Campeón Diario",
            description = "Supera desafíos diarios de escalada.",
            category = "Escalada",
            metric = AchievementMetric.DAILY_CHALLENGES_WON,
            baseRequirement = 1L,
            maxLevel = 10,
            unitLabel = "desafíos"
        ),
        // Hito único lógico (1 nivel)
        MedalDefinition(
            id = "social_bond",
            title = "Lazo Estelar",
            description = "Conectaste con tu primer amigo en Vicdaru Project.",
            category = "Social",
            metric = AchievementMetric.FIRST_FRIEND,
            baseRequirement = 1L,
            maxLevel = 1,
            unitLabel = "amigo"
        ),
        // Logro social escalado a 5 niveles lógicos (escala 1.5x)
        MedalDefinition(
            id = "social_circle",
            title = "Círculo Estelar",
            description = "Amplía tu red de amigos cercanos.",
            category = "Social",
            metric = AchievementMetric.FRIENDS_COUNT,
            baseRequirement = 2L,
            maxLevel = 5,
            unitLabel = "amigos"
        ),
        // Logros de Amistad Emocional y Multijugador (10 niveles, escala 1.5x)
        MedalDefinition(
            id = "emotional_friendship",
            title = "Vínculo Emocional",
            description = "Sube el nivel de Amistad Emocional en el chat musical.",
            category = "Social",
            metric = AchievementMetric.MAX_EMOTIONAL_LEVEL,
            baseRequirement = 2L,
            maxLevel = 10,
            unitLabel = "nv."
        ),
        MedalDefinition(
            id = "duo_summit",
            title = "Cima Compartida",
            description = "Completen partidas multijugador de Escalada juntos.",
            category = "Multijugador",
            metric = AchievementMetric.DUO_CLIMBER_WINS,
            baseRequirement = 1L,
            maxLevel = 10,
            unitLabel = "duelos",
            isDuoMedal = true
        ),
        MedalDefinition(
            id = "duo_harmony",
            title = "Armonía Dual",
            description = "Completen sesiones multijugador de Enfoque Musical.",
            category = "Multijugador",
            metric = AchievementMetric.DUO_MUSIC_WINS,
            baseRequirement = 1L,
            maxLevel = 10,
            unitLabel = "sesiones",
            isDuoMedal = true
        )
    )

    fun evaluateAchievements(
        profile: PublicProfile?,
        liveQuotesCount: Int = 0,
        liveFavoritesCount: Int = 0,
        liveHabitsCompleted: Int = 0,
        liveBestStreak: Int = 0,
        liveFriendsCount: Int = 0,
    ): List<AchievementProgress> {
        val legacyMedals = profile?.medals.orEmpty().toSet()

        fun metricValue(metric: AchievementMetric): Long = when (metric) {
            AchievementMetric.FIRST_QUOTE -> maxOf(
                if (legacyMedals.contains("first_quote")) 1L else 0L,
                profile?.quotesCount ?: 0L,
                liveQuotesCount.toLong()
            )
            AchievementMetric.QUOTES_COLLECTED -> maxOf(profile?.quotesCount ?: 0L, liveQuotesCount.toLong())
            AchievementMetric.FAVORITES_SAVED -> maxOf(
                profile?.favoritesCount ?: 0L,
                liveFavoritesCount.toLong(),
                if (legacyMedals.contains("favorite_keeper")) 3L else 0L
            )
            AchievementMetric.HABITS_COMPLETED -> maxOf(
                profile?.habitsCompletedTotal ?: 0L,
                liveHabitsCompleted.toLong(),
                if (legacyMedals.contains("habit_spark")) 5L else 0L
            )
            AchievementMetric.BEST_HABIT_STREAK -> maxOf(profile?.bestHabitStreak ?: 0L, liveBestStreak.toLong())
            AchievementMetric.FOCUS_MINUTES -> maxOf(
                profile?.totalFocusMinutes ?: 0L,
                when {
                    legacyMedals.contains("deep_flow_60") -> 60L
                    legacyMedals.contains("deep_flow_15") -> 15L
                    else -> 0L
                }
            )
            AchievementMetric.CLIMBER_BEST_METERS -> maxOf(
                profile?.climberBestMeters ?: 0L,
                when {
                    legacyMedals.contains("climber_600") -> 600L
                    legacyMedals.contains("climber_250") -> 250L
                    else -> 0L
                }
            )
            AchievementMetric.DAILY_CHALLENGES_WON -> maxOf(
                profile?.dailyChallengesCompleted ?: 0L,
                if (legacyMedals.contains("daily_victor")) 1L else 0L
            )
            AchievementMetric.FIRST_FRIEND -> maxOf(
                if (legacyMedals.contains("social_bond")) 1L else 0L,
                profile?.friendsCount ?: 0L,
                liveFriendsCount.toLong()
            )
            AchievementMetric.FRIENDS_COUNT -> maxOf(profile?.friendsCount ?: 0L, liveFriendsCount.toLong())
            AchievementMetric.DUO_CLIMBER_WINS -> maxOf(
                profile?.duoClimberWins ?: 0L,
                if (legacyMedals.contains("duo_summit")) 1L else 0L
            )
            AchievementMetric.DUO_MUSIC_WINS -> maxOf(
                profile?.duoMusicWins ?: 0L,
                if (legacyMedals.contains("duo_harmony")) 1L else 0L
            )
            AchievementMetric.MAX_EMOTIONAL_LEVEL -> profile?.maxEmotionalLevel ?: 1L
        }

        return medals.map { def ->
            val currentVal = metricValue(def.metric)
            var unlockedLevel = 0
            for (tier in 1..def.maxLevel) {
                if (currentVal >= def.thresholdForLevel(tier)) {
                    unlockedLevel = tier
                } else {
                    break
                }
            }
            val nextTarget = if (unlockedLevel < def.maxLevel) def.thresholdForLevel(unlockedLevel + 1) else null
            AchievementProgress(
                definition = def,
                currentValue = currentVal,
                unlockedLevel = unlockedLevel,
                nextLevelTarget = nextTarget
            )
        }
    }

    fun avatarById(id: String?): ClimberAvatar =
        avatars.firstOrNull { it.id == id } ?: avatars.first()
}
