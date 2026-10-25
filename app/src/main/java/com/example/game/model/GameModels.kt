package com.example.game.model

enum class GameState {
    MENU,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class MenuModal {
    NONE,
    CHARACTER_SELECT,
    SLED_SELECT,
    SETTINGS
}

enum class CharacterId(
    val displayName: String,
    val roleTitle: String,
    val description: String,
    val costCoins: Int,
    val suitRgb: FloatArray,
    val accentRgb: FloatArray,
    val visorRgb: FloatArray
) {
    EXPLORER(
        displayName = "Koda Frost",
        roleTitle = "Beginner Snow Explorer",
        description = "Outfitted in an insulated cobalt-blue winter jacket, snow goggles, and thermal scarf.",
        costCoins = 0,
        suitRgb = floatArrayOf(0.14f, 0.52f, 0.96f),
        accentRgb = floatArrayOf(0.95f, 0.98f, 1.0f),
        visorRgb = floatArrayOf(1.0f, 0.72f, 0.15f)
    ),
    RACER(
        displayName = "Nyx Vector",
        roleTitle = "Futuristic Snow Racer",
        description = "Engineered purple aero-armor with twin dorsal stabilizers and holographic cyan visor.",
        costCoins = 40,
        suitRgb = floatArrayOf(0.56f, 0.22f, 0.92f),
        accentRgb = floatArrayOf(0.82f, 0.55f, 1.0f),
        visorRgb = floatArrayOf(0.0f, 0.95f, 1.0f)
    ),
    ADVENTURER(
        displayName = "Freya Summit",
        roleTitle = "Arctic Adventurer",
        description = "Heavy-duty crimson winter expedition gear with gold thermal plating and summit crest.",
        costCoins = 90,
        suitRgb = floatArrayOf(0.88f, 0.18f, 0.24f),
        accentRgb = floatArrayOf(1.0f, 0.78f, 0.24f),
        visorRgb = floatArrayOf(0.18f, 0.92f, 0.78f)
    );

    companion object {
        fun fromNameSafe(name: String): CharacterId =
            entries.firstOrNull { it.name == name } ?: EXPLORER
    }
}

enum class SledId(
    val displayName: String,
    val subtitle: String,
    val description: String,
    val costCoins: Int,
    val bodyRgb: FloatArray,
    val runnerRgb: FloatArray,
    val trimRgb: FloatArray
) {
    ALPINE_GLIDER(
        displayName = "Glacier Runner",
        subtitle = "Twin-Rail Alpine Sled",
        description = "Balanced futuristic snow sled with curved steel runners and ice-cyan hull.",
        costCoins = 0,
        bodyRgb = floatArrayOf(0.06f, 0.74f, 0.92f),
        runnerRgb = floatArrayOf(0.82f, 0.90f, 0.98f),
        trimRgb = floatArrayOf(1.0f, 0.78f, 0.22f)
    ),
    PLASMA_WING(
        displayName = "Ion Strider",
        subtitle = "Split-Wing Hover Sled",
        description = "Angular purple aero-chassis with swept delta wings and glowing violet edge rails.",
        costCoins = 35,
        bodyRgb = floatArrayOf(0.48f, 0.18f, 0.86f),
        runnerRgb = floatArrayOf(0.0f, 0.92f, 1.0f),
        trimRgb = floatArrayOf(0.88f, 0.52f, 1.0f)
    ),
    BLAZE_TURBINE(
        displayName = "Solar Vortex",
        subtitle = "Twin-Turbine Heavy Sled",
        description = "Reinforced crimson snow-cruiser sporting dual rear boosters and gold nose armor.",
        costCoins = 80,
        bodyRgb = floatArrayOf(0.86f, 0.15f, 0.22f),
        runnerRgb = floatArrayOf(1.0f, 0.78f, 0.22f),
        trimRgb = floatArrayOf(0.15f, 0.92f, 0.98f)
    );

    companion object {
        fun fromNameSafe(name: String): SledId =
            entries.firstOrNull { it.name == name } ?: ALPINE_GLIDER
    }
}

enum class ObstacleType {
    SNOW_TREE,
    ICE_BLOCK,
    ROLLING_SNOWBALL,
    WOODEN_BARRIER_LOW,
    WOODEN_GATE_HIGH,
    ROCK,
    NARROW_PILLAR,
    SNOW_RAMP
}

enum class PowerUpType(
    val durationSeconds: Float
) {
    COIN_MAGNET(8.0f),
    SPEED_BOOST(5.5f),
    SHIELD(12.0f)
}

data class PlayerProfile(
    val bestScore: Int = 0,
    val totalCoins: Int = 0,
    val unlockedCharacters: Set<CharacterId> = setOf(CharacterId.EXPLORER),
    val unlockedSleds: Set<SledId> = setOf(SledId.ALPINE_GLIDER),
    val selectedCharacter: CharacterId = CharacterId.EXPLORER,
    val selectedSled: SledId = SledId.ALPINE_GLIDER,
    val soundEnabled: Boolean = true,
    val touchButtonsEnabled: Boolean = false
)

data class HudState(
    val gameState: GameState = GameState.MENU,
    val score: Int = 0,
    val distanceMeters: Int = 0,
    val runCoins: Int = 0,
    val speedKmh: Int = 0,
    val magnetRemainingSec: Float = 0f,
    val boostRemainingSec: Float = 0f,
    val shieldRemainingSec: Float = 0f,
    val isNewBestOnRun: Boolean = false
)
