package com.example.model

import androidx.compose.ui.graphics.Color

enum class AnimeSeries(
    val key: String,
    val displayName: String,
    val malId: Int,
    val primaryColor: Color,
    val secondaryColor: Color,
    val darkPrimaryColor: Color,
    val darkSecondaryColor: Color,
    val primaryHex: String,
    val secondaryHex: String,
    val fallbackImageUrl: String,
    val leadCharacter: String,
    val presetName: String,
    val defaultAmbientSound: String,
    val defaultParticleStyle: String,
    val defaultWaveformStyle: String,
) {
    FRIEREN(
        key = "FRIEREN",
        displayName = "Sousou no Frieren",
        malId = 52991,
        primaryColor = Color(0xFF38BDF8),
        secondaryColor = Color(0xFFC4B5FD),
        darkPrimaryColor = Color(0xFF7DD3FC),
        darkSecondaryColor = Color(0xFFDDD6FE),
        primaryHex = "#7DD3FC",
        secondaryHex = "#C4B5FD",
        fallbackImageUrl = "https://cdn.myanimelist.net/images/anime/1015/138006l.jpg",
        leadCharacter = "Frieren",
        presetName = "Constelación Zoltraak",
        defaultAmbientSound = "ASTRAL_CHIMES",
        defaultParticleStyle = "PETALS",
        defaultWaveformStyle = "ARCANE_CONSTELLATION"
    ),
    APOTHECARY(
        key = "APOTHECARY",
        displayName = "Los Diarios de la Boticaria",
        malId = 54492,
        primaryColor = Color(0xFF0D9488),
        secondaryColor = Color(0xFFF472B6),
        darkPrimaryColor = Color(0xFF2DD4BF),
        darkSecondaryColor = Color(0xFFF9A8D4),
        primaryHex = "#2DD4BF",
        secondaryHex = "#F9A8D4",
        fallbackImageUrl = "https://cdn.myanimelist.net/images/anime/1708/138033l.jpg",
        leadCharacter = "Maomao",
        presetName = "Elixir Imperial",
        defaultAmbientSound = "SERENE_LAB",
        defaultParticleStyle = "FIREFLIES",
        defaultWaveformStyle = "APOTHECARY_RIPPLE"
    ),
    DANDADAN(
        key = "DANDADAN",
        displayName = "Dandadan",
        malId = 57334,
        primaryColor = Color(0xFF06B6D4),
        secondaryColor = Color(0xFFEC4899),
        darkPrimaryColor = Color(0xFF22D3EE),
        darkSecondaryColor = Color(0xFFF472B6),
        primaryHex = "#22D3EE",
        secondaryHex = "#F472B6",
        fallbackImageUrl = "https://cdn.myanimelist.net/images/anime/1584/143719l.jpg",
        leadCharacter = "Momo & Okarun",
        presetName = "Matriz Oculta",
        defaultAmbientSound = "STAR_WAVE",
        defaultParticleStyle = "SPARKS",
        defaultWaveformStyle = "TURBO_MATRIX"
    ),
    LITTLE_WITCH(
        key = "LITTLE_WITCH",
        displayName = "Little Witch Academia",
        malId = 33489,
        primaryColor = Color(0xFF7C3AED),
        secondaryColor = Color(0xFFF59E0B),
        darkPrimaryColor = Color(0xFFA78BFA),
        darkSecondaryColor = Color(0xFFFBBF24),
        primaryHex = "#A78BFA",
        secondaryHex = "#FBBF24",
        fallbackImageUrl = "https://cdn.myanimelist.net/images/anime/5/83047l.jpg",
        leadCharacter = "Akko Kagari",
        presetName = "Prisma Shiny Rod",
        defaultAmbientSound = "MAGIC_FOREST",
        defaultParticleStyle = "STARS",
        defaultWaveformStyle = "SHINY_PRISM"
    ),
    SAKURA(
        key = "SAKURA",
        displayName = "Sakura Card Captor",
        malId = 232,
        primaryColor = Color(0xFFEC4899),
        secondaryColor = Color(0xFF7DD3FC),
        darkPrimaryColor = Color(0xFFF9A8D4),
        darkSecondaryColor = Color(0xFF7DD3FC),
        primaryHex = "#F9A8D4",
        secondaryHex = "#7DD3FC",
        fallbackImageUrl = "https://cdn.myanimelist.net/images/anime/8/60781l.jpg",
        leadCharacter = "Sakura Kinomoto",
        presetName = "Círculo Clow & Sakura",
        defaultAmbientSound = "NIGHT_RAIN",
        defaultParticleStyle = "PETALS",
        defaultWaveformStyle = "CARD_HALO"
    ),
    RANMA(
        key = "RANMA",
        displayName = "Ranma 1/2",
        malId = 210,
        primaryColor = Color(0xFF0284C7),
        secondaryColor = Color(0xFFFB7185),
        darkPrimaryColor = Color(0xFF38BDF8),
        darkSecondaryColor = Color(0xFFFDA4AF),
        primaryHex = "#38BDF8",
        secondaryHex = "#FB7185",
        fallbackImageUrl = "https://cdn.myanimelist.net/images/anime/13/25165l.jpg",
        leadCharacter = "Ranma Saotome",
        presetName = "Vórtice Yin-Yang",
        defaultAmbientSound = "DOJO_BREEZE",
        defaultParticleStyle = "RIPPLES",
        defaultWaveformStyle = "YINYANG_KINETIC"
    );

    companion object {
        fun fromKey(key: String?): AnimeSeries =
            entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: SAKURA
    }
}
