package com.example.game.engine

import com.example.game.model.ObstacleType
import com.example.game.model.PowerUpType

enum class SidePropKind {
    PINE_TREE,
    ICE_SPIRE,
    BOULDER
}

class SideProp(
    var offsetX: Float = 0f,
    var offsetZ: Float = 0f,
    var scale: Float = 1f,
    var kind: SidePropKind = SidePropKind.PINE_TREE
)

class TrackSegment(
    var zStart: Float = 0f,
    var hasFrozenRiver: Boolean = false,
    var hasWoodenFenceLeft: Boolean = true,
    var hasWoodenFenceRight: Boolean = true,
    val props: Array<SideProp> = Array(6) { SideProp() }
)

class ObstacleInstance(
    var active: Boolean = false,
    var shattered: Boolean = false,
    var shatterAnim: Float = 0f,
    var type: ObstacleType = ObstacleType.ICE_BLOCK,
    var lane: Int = 0,
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var minY: Float = 0f,
    var maxY: Float = 1f,
    var halfWidth: Float = 0.7f,
    var halfDepth: Float = 0.6f,
    var rollAngleDeg: Float = 0f
)

class CoinInstance(
    var active: Boolean = false,
    var collected: Boolean = false,
    var collectAnim: Float = 0f,
    var x: Float = 0f,
    var y: Float = 0.65f,
    var z: Float = 0f,
    var spinPhaseDeg: Float = 0f
)

class PowerUpInstance(
    var active: Boolean = false,
    var collected: Boolean = false,
    var type: PowerUpType = PowerUpType.COIN_MAGNET,
    var x: Float = 0f,
    var y: Float = 0.85f,
    var z: Float = 0f,
    var spinPhaseDeg: Float = 0f
)

class SnowParticle(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = -3.5f,
    var vz: Float = 4f,
    var size: Float = 0.08f,
    var isSpray: Boolean = false,
    var life: Float = 1f
)

data class RenderSnapshot(
    val gameStateOrdinal: Int,
    val playerX: Float,
    val playerY: Float,
    val leanAngleDeg: Float,
    val pitchAngleDeg: Float,
    val isSliding: Boolean,
    val isJumping: Boolean,
    val speedRatio: Float,
    val magnetActive: Boolean,
    val boostActive: Boolean,
    val shieldActive: Boolean,
    val crashShake: Float,
    val worldTime: Float
)
