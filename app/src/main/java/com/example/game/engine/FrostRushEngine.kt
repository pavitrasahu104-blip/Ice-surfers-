package com.example.game.engine

import com.example.game.model.GameState
import com.example.game.model.HudState
import com.example.game.model.ObstacleType
import com.example.game.model.PowerUpType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

interface EngineEventListener {
    fun onCoinCollected()
    fun onJump()
    fun onSlide()
    fun onPowerUpCollected(type: PowerUpType)
    fun onShieldAbsorbed()
    fun onCollisionGameOver(finalScore: Int, distanceMeters: Int, coinsCollected: Int)
}

/**
 * Pure Kotlin deterministic 3D endless-runner physics & world recycling engine.
 * Synchronized so both UI input events and the OpenGL ES render thread interact safely.
 */
class FrostRushEngine(
    seed: Long = System.currentTimeMillis()
) {
    companion object {
        const val LANE_WIDTH = 2.35f
        const val SEGMENT_COUNT = 18
        const val SEGMENT_LENGTH = 12.0f
        const val MAX_OBSTACLES = 54
        const val MAX_COINS = 110
        const val MAX_POWERUPS = 10
        const val MAX_SNOW_PARTICLES = 140

        const val BASE_START_SPEED = 16.5f
        const val MAX_BASE_SPEED = 35.5f
        const val BOOST_SPEED_MULTIPLIER = 1.45f

        const val JUMP_VELOCITY = 12.0f
        const val RAMP_LAUNCH_VELOCITY = 15.2f
        const val GRAVITY = -28.5f
        const val SLIDE_DURATION = 0.72f

        const val PLAYER_STANDING_HEIGHT = 1.45f
        const val PLAYER_SLIDING_HEIGHT = 0.56f
        const val PLAYER_HALF_WIDTH = 0.52f
        const val PLAYER_HALF_DEPTH = 0.68f
    }

    private val lock = Any()
    private var rng = Random(seed)

    var listener: EngineEventListener? = null

    var gameState: GameState = GameState.MENU
        private set

    // Player kinematics
    var currentLane: Int = 0
        private set
    var playerX: Float = 0f
        private set
    var playerY: Float = 0f
        private set
    var verticalVelocity: Float = 0f
        private set
    var isJumping: Boolean = false
        private set
    var isSliding: Boolean = false
        private set
    private var slideTimer: Float = 0f
    var leanAngleDeg: Float = 0f
        private set
    var pitchAngleDeg: Float = 0f
        private set

    // Run metrics
    private var distanceExact: Double = 0.0
    private var bonusScore: Int = 0
    var runCoins: Int = 0
        private set
    var baseSpeed: Float = BASE_START_SPEED
        private set
    var effectiveSpeed: Float = BASE_START_SPEED
        private set

    // Power-up timers
    var magnetTimer: Float = 0f
        private set
    var boostTimer: Float = 0f
        private set
    var shieldTimer: Float = 0f
        private set

    private var crashShake: Float = 0f
    var worldTime: Float = 0f
        private set
    private var nextSpawnAllowedZ: Float = -36f
    private var gameOverDispatched: Boolean = false

    // Pre-allocated pools (no unbounded growth)
    val segments: Array<TrackSegment> = Array(SEGMENT_COUNT) { TrackSegment() }
    val obstacles: Array<ObstacleInstance> = Array(MAX_OBSTACLES) { ObstacleInstance() }
    val coins: Array<CoinInstance> = Array(MAX_COINS) { CoinInstance() }
    val powerUps: Array<PowerUpInstance> = Array(MAX_POWERUPS) { PowerUpInstance() }
    val snowParticles: Array<SnowParticle> = Array(MAX_SNOW_PARTICLES) { SnowParticle() }

    init {
        synchronized(lock) {
            initSnowParticles()
            resetWorldScenery(spawnGameplayHazards = false)
        }
    }

    fun reseedForTest(testSeed: Long) = synchronized(lock) {
        rng = Random(testSeed)
    }

    fun startNewRun() = synchronized(lock) {
        currentLane = 0
        playerX = 0f
        playerY = 0f
        verticalVelocity = 0f
        isJumping = false
        isSliding = false
        slideTimer = 0f
        leanAngleDeg = 0f
        pitchAngleDeg = 0f

        distanceExact = 0.0
        bonusScore = 0
        runCoins = 0
        baseSpeed = BASE_START_SPEED
        effectiveSpeed = BASE_START_SPEED

        magnetTimer = 0f
        boostTimer = 0f
        shieldTimer = 0f
        crashShake = 0f
        gameOverDispatched = false
        nextSpawnAllowedZ = -34f

        resetWorldScenery(spawnGameplayHazards = true)
        gameState = GameState.PLAYING
    }

    fun pauseGame() = synchronized(lock) {
        if (gameState == GameState.PLAYING) {
            gameState = GameState.PAUSED
        }
    }

    fun resumeGame() = synchronized(lock) {
        if (gameState == GameState.PAUSED) {
            gameState = GameState.PLAYING
        }
    }

    fun returnToMenu() = synchronized(lock) {
        currentLane = 0
        playerX = 0f
        playerY = 0f
        verticalVelocity = 0f
        isJumping = false
        isSliding = false
        slideTimer = 0f
        leanAngleDeg = 0f
        pitchAngleDeg = 0f
        magnetTimer = 0f
        boostTimer = 0f
        shieldTimer = 0f
        crashShake = 0f
        resetWorldScenery(spawnGameplayHazards = false)
        gameState = GameState.MENU
    }

    fun moveLeft(): Boolean = synchronized(lock) {
        if (gameState != GameState.PLAYING) return false
        if (currentLane > -1) {
            currentLane -= 1
            return true
        }
        false
    }

    fun moveRight(): Boolean = synchronized(lock) {
        if (gameState != GameState.PLAYING) return false
        if (currentLane < 1) {
            currentLane += 1
            return true
        }
        false
    }

    fun jump(): Boolean = synchronized(lock) {
        if (gameState != GameState.PLAYING) return false
        if (!isJumping && playerY <= 0.08f) {
            verticalVelocity = JUMP_VELOCITY
            isJumping = true
            isSliding = false
            slideTimer = 0f
            listener?.onJump()
            return true
        }
        false
    }

    fun slide(): Boolean = synchronized(lock) {
        if (gameState != GameState.PLAYING) return false
        if (isJumping && playerY > 0.1f) {
            // Fast-fall slam into slide
            verticalVelocity = -23.0f
            isSliding = true
            slideTimer = SLIDE_DURATION
            listener?.onSlide()
            return true
        } else if (!isSliding) {
            isSliding = true
            slideTimer = SLIDE_DURATION
            listener?.onSlide()
            return true
        }
        false
    }

    fun getCurrentScore(): Int = synchronized(lock) {
        (distanceExact * 2.5).toInt() + bonusScore + (runCoins * 10)
    }

    fun getDistanceMeters(): Int = synchronized(lock) {
        distanceExact.toInt()
    }

    fun getHudState(currentBestScore: Int): HudState = synchronized(lock) {
        val score = getCurrentScore()
        val speedShown = if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            (effectiveSpeed * 3.6f).toInt()
        } else {
            0
        }
        HudState(
            gameState = gameState,
            score = score,
            distanceMeters = distanceExact.toInt(),
            runCoins = runCoins,
            speedKmh = speedShown,
            magnetRemainingSec = magnetTimer.coerceAtLeast(0f),
            boostRemainingSec = boostTimer.coerceAtLeast(0f),
            shieldRemainingSec = shieldTimer.coerceAtLeast(0f),
            isNewBestOnRun = score > currentBestScore && score > 0
        )
    }

    fun update(rawDeltaSeconds: Float) {
        var eventGameOver: Triple<Int, Int, Int>? = null
        var coinEvents = 0
        var shieldBroken = false
        var collectedPowerUp: PowerUpType? = null

        synchronized(lock) {
            val dt = rawDeltaSeconds.coerceIn(0f, 0.05f)
            if (dt <= 0f) return

            // When PAUSED, genuinely freeze world movement, timers, snow, and collisions
            if (gameState == GameState.PAUSED) {
                return
            }

            worldTime += dt

            if (gameState == GameState.MENU) {
                updateAmbientSnowOnly(dt, forwardSpeed = 4.5f)
                return
            }

            if (gameState == GameState.GAME_OVER) {
                crashShake = (crashShake - dt * 2.4f).coerceAtLeast(0f)
                updateAmbientSnowOnly(dt, forwardSpeed = 1.5f)
                return
            }

            // PLAYING state update
            val difficulty = (distanceExact.toFloat() / 1200f).coerceIn(0f, 1f)
            baseSpeed = (BASE_START_SPEED + difficulty * (MAX_BASE_SPEED - BASE_START_SPEED))
                .coerceAtMost(MAX_BASE_SPEED)

            if (magnetTimer > 0f) magnetTimer = (magnetTimer - dt).coerceAtLeast(0f)
            if (boostTimer > 0f) boostTimer = (boostTimer - dt).coerceAtLeast(0f)
            if (shieldTimer > 0f) shieldTimer = (shieldTimer - dt).coerceAtLeast(0f)

            effectiveSpeed = if (boostTimer > 0f) {
                baseSpeed * BOOST_SPEED_MULTIPLIER
            } else {
                baseSpeed
            }

            val dz = effectiveSpeed * dt
            distanceExact += dz.toDouble()

            // Smooth lateral lane movement
            val targetX = currentLane * LANE_WIDTH
            val dx = targetX - playerX
            val lateralStep = min(1f, dt * 14.5f)
            playerX += dx * lateralStep
            leanAngleDeg = (-dx * 13.5f).coerceIn(-24f, 24f)

            // Vertical jump & gravity physics
            if (isJumping || playerY > 0f || verticalVelocity != 0f) {
                playerY += verticalVelocity * dt
                verticalVelocity += GRAVITY * dt
                if (playerY <= 0f) {
                    playerY = 0f
                    verticalVelocity = 0f
                    isJumping = false
                }
            }
            pitchAngleDeg = when {
                isJumping -> (verticalVelocity * 1.6f).coerceIn(-18f, 22f)
                isSliding -> -8f
                else -> 0f
            }

            // Slide timer
            if (isSliding && !isJumping) {
                slideTimer -= dt
                if (slideTimer <= 0f) {
                    isSliding = false
                    slideTimer = 0f
                }
            }

            // Advance & recycle track segments
            var minZStart = Float.MAX_VALUE
            for (seg in segments) {
                seg.zStart += dz
                if (seg.zStart < minZStart) minZStart = seg.zStart
            }
            nextSpawnAllowedZ += dz

            for (seg in segments) {
                if (seg.zStart > 18.0f) {
                    val recycledZ = minZStart - SEGMENT_LENGTH
                    minZStart = recycledZ
                    configureSegment(seg, recycledZ)
                    if (recycledZ <= nextSpawnAllowedZ) {
                        val spacing = spawnWaveAt(recycledZ, difficulty)
                        nextSpawnAllowedZ = recycledZ - spacing
                    }
                }
            }

            // Update obstacles & check collisions
            val pMinX = playerX - PLAYER_HALF_WIDTH
            val pMaxX = playerX + PLAYER_HALF_WIDTH
            val pMinY = playerY
            val pMaxY = playerY + if (isSliding) PLAYER_SLIDING_HEIGHT else PLAYER_STANDING_HEIGHT
            val pMinZ = -PLAYER_HALF_DEPTH
            val pMaxZ = PLAYER_HALF_DEPTH

            for (obs in obstacles) {
                if (!obs.active) continue
                obs.z += dz
                if (obs.type == ObstacleType.ROLLING_SNOWBALL && !obs.shattered) {
                    obs.z += 7.2f * dt
                    obs.rollAngleDeg = (obs.rollAngleDeg + 380f * dt) % 360f
                }
                if (obs.shattered) {
                    obs.shatterAnim += dt * 3.5f
                    if (obs.shatterAnim >= 1f) {
                        obs.active = false
                    }
                    continue
                }
                if (obs.z > 16f) {
                    obs.active = false
                    continue
                }

                // Check 3D AABB overlap with player
                val oMinX = obs.x - obs.halfWidth
                val oMaxX = obs.x + obs.halfWidth
                val oMinZ = obs.z - obs.halfDepth
                val oMaxZ = obs.z + obs.halfDepth

                val overlapXZ = pMaxX > oMinX && pMinX < oMaxX && pMaxZ > oMinZ && pMinZ < oMaxZ
                if (!overlapXZ) continue

                if (obs.type == ObstacleType.SNOW_RAMP) {
                    // Ramp launches the sled high into the air!
                    if (playerY <= 1.25f && verticalVelocity <= 4.0f) {
                        playerY = 0.65f
                        verticalVelocity = RAMP_LAUNCH_VELOCITY
                        isJumping = true
                        isSliding = false
                        slideTimer = 0f
                        bonusScore += 25
                        listener?.onJump()
                    }
                    continue
                }

                val overlapY = pMaxY > obs.minY && pMinY < obs.maxY
                if (overlapY) {
                    if (shieldTimer > 0f) {
                        shieldTimer = 0f
                        obs.shattered = true
                        obs.shatterAnim = 0.01f
                        shieldBroken = true
                    } else if (!gameOverDispatched) {
                        gameOverDispatched = true
                        gameState = GameState.GAME_OVER
                        crashShake = 1.0f
                        val finalScore = getCurrentScore()
                        eventGameOver = Triple(finalScore, distanceExact.toInt(), runCoins)
                        break
                    }
                }
            }

            // Update coins (including Coin Magnet attraction & collection animation)
            if (gameState == GameState.PLAYING) {
                for (coin in coins) {
                    if (!coin.active) continue
                    coin.z += dz
                    coin.spinPhaseDeg = (coin.spinPhaseDeg + 210f * dt) % 360f

                    if (coin.collected) {
                        coin.collectAnim += dt * 3.2f
                        coin.y += dt * 3.8f
                        if (coin.collectAnim >= 1f) {
                            coin.active = false
                        }
                        continue
                    }

                    if (coin.z > 14f) {
                        coin.active = false
                        continue
                    }

                    if (magnetTimer > 0f && coin.z in -19f..3.5f) {
                        val pull = min(1f, dt * 11.5f)
                        coin.x += (playerX - coin.x) * pull
                        coin.y += ((playerY + 0.75f) - coin.y) * pull
                        if (coin.z < 0f) {
                            coin.z += 12.0f * dt
                        }
                    }

                    val cdx = coin.x - playerX
                    val cdy = coin.y - (playerY + 0.65f)
                    val cdz = coin.z
                    val distSq = cdx * cdx + cdy * cdy + cdz * cdz
                    if (distSq <= 1.32f * 1.32f) {
                        coin.collected = true
                        coin.collectAnim = 0.01f
                        runCoins += 1
                        coinEvents += 1
                    }
                }

                // Update Power-Ups
                for (pu in powerUps) {
                    if (!pu.active) continue
                    pu.z += dz
                    pu.spinPhaseDeg = (pu.spinPhaseDeg + 150f * dt) % 360f
                    if (pu.z > 14f || pu.collected) {
                        pu.active = false
                        continue
                    }

                    val pdx = pu.x - playerX
                    val pdy = pu.y - (playerY + 0.72f)
                    val pdz = pu.z
                    if (pdx * pdx + pdy * pdy + pdz * pdz <= 1.42f * 1.42f) {
                        pu.collected = true
                        pu.active = false
                        activatePowerUpInternal(pu.type)
                        collectedPowerUp = pu.type
                    }
                }
            }

            // Update snow & sled spray particles
            updateParticlesPlaying(dt, dz)
        }

        // Dispatch audio/persistence callbacks outside lock to avoid any lock inversion
        repeat(coinEvents) { listener?.onCoinCollected() }
        if (shieldBroken) listener?.onShieldAbsorbed()
        collectedPowerUp?.let { listener?.onPowerUpCollected(it) }
        eventGameOver?.let { (score, dist, coinsCount) ->
            listener?.onCollisionGameOver(score, dist, coinsCount)
        }
    }

    fun activatePowerUpForTest(type: PowerUpType) = synchronized(lock) {
        activatePowerUpInternal(type)
    }

    fun spawnObstacleForTest(
        type: ObstacleType,
        lane: Int,
        zPos: Float
    ): ObstacleInstance = synchronized(lock) {
        val obs = acquireObstacle()
        setupObstacleGeometry(obs, type, lane, zPos)
        obs
    }

    fun spawnCoinForTest(lane: Int, yPos: Float, zPos: Float): CoinInstance = synchronized(lock) {
        val coin = acquireCoin()
        coin.active = true
        coin.collected = false
        coin.collectAnim = 0f
        coin.x = lane * LANE_WIDTH
        coin.y = yPos
        coin.z = zPos
        coin
    }

    private fun activatePowerUpInternal(type: PowerUpType) {
        when (type) {
            PowerUpType.COIN_MAGNET -> magnetTimer = type.durationSeconds
            PowerUpType.SPEED_BOOST -> boostTimer = type.durationSeconds
            PowerUpType.SHIELD -> shieldTimer = type.durationSeconds
        }
    }

    private fun resetWorldScenery(spawnGameplayHazards: Boolean) {
        obstacles.forEach { it.active = false; it.shattered = false }
        coins.forEach { it.active = false; it.collected = false }
        powerUps.forEach { it.active = false; it.collected = false }

        var zCursor = 12f
        for (i in segments.indices) {
            configureSegment(segments[i], zCursor)
            if (spawnGameplayHazards && zCursor <= nextSpawnAllowedZ) {
                val spacing = spawnWaveAt(zCursor, 0f)
                nextSpawnAllowedZ = zCursor - spacing
            }
            zCursor -= SEGMENT_LENGTH
        }
    }

    private fun configureSegment(seg: TrackSegment, zStart: Float) {
        seg.zStart = zStart
        seg.hasFrozenRiver = rng.nextFloat() < 0.20f
        seg.hasWoodenFenceLeft = !seg.hasFrozenRiver && rng.nextFloat() < 0.65f
        seg.hasWoodenFenceRight = !seg.hasFrozenRiver && rng.nextFloat() < 0.65f

        for (i in seg.props.indices) {
            val side = if (i % 2 == 0) -1f else 1f
            val prop = seg.props[i]
            prop.offsetX = side * (5.6f + rng.nextFloat() * 7.5f)
            prop.offsetZ = -rng.nextFloat() * SEGMENT_LENGTH
            prop.scale = 0.8f + rng.nextFloat() * 0.75f
            val roll = rng.nextFloat()
            prop.kind = when {
                roll < 0.58f -> SidePropKind.PINE_TREE
                roll < 0.82f -> SidePropKind.ICE_SPIRE
                else -> SidePropKind.BOULDER
            }
        }
    }

    private fun spawnWaveAt(zPos: Float, difficulty: Float): Float {
        val patternRoll = rng.nextFloat()
        val lanes = mutableListOf(-1, 0, 1).also { it.shuffle(rng) }

        when {
            // Narrow gap between twin pillars or snow trees (2 lanes blocked, 1 open lane with coins + occasional power-up)
            patternRoll < 0.22f + difficulty * 0.12f -> {
                val openLane = lanes[0]
                val blockerType = if (rng.nextBoolean()) ObstacleType.NARROW_PILLAR else ObstacleType.SNOW_TREE
                setupObstacleGeometry(acquireObstacle(), blockerType, lanes[1], zPos)
                setupObstacleGeometry(acquireObstacle(), blockerType, lanes[2], zPos)
                spawnCoinLine(openLane, zPos + 4.5f, count = 5, stepZ = 2.2f, baseY = 0.65f)
                maybeSpawnPowerUp(openLane, zPos - 7.5f, 0.85f)
            }

            // Overhead wooden gate requiring SLIDE (with low barrier or tree in another lane)
            patternRoll < 0.44f -> {
                val gateLane = lanes[0]
                setupObstacleGeometry(acquireObstacle(), ObstacleType.WOODEN_GATE_HIGH, gateLane, zPos)
                spawnCoinLine(gateLane, zPos + 3.0f, count = 4, stepZ = 2.0f, baseY = 0.42f)
                if (rng.nextFloat() < 0.55f + difficulty * 0.25f) {
                    setupObstacleGeometry(acquireObstacle(), ObstacleType.SNOW_TREE, lanes[1], zPos)
                }
            }

            // Snow Ramp super-jump with aerial gold coin arc!
            patternRoll < 0.60f -> {
                val rampLane = lanes[0]
                setupObstacleGeometry(acquireObstacle(), ObstacleType.SNOW_RAMP, rampLane, zPos)
                // Place obstacle right behind ramp that player vaults effortlessly over, plus high coin arc
                setupObstacleGeometry(acquireObstacle(), ObstacleType.ICE_BLOCK, rampLane, zPos - 5.5f)
                for (i in 0 until 5) {
                    val c = acquireCoin()
                    c.active = true
                    c.collected = false
                    c.collectAnim = 0f
                    c.x = rampLane * LANE_WIDTH
                    val arcT = i / 4f
                    c.y = 1.2f + sin(arcT * Math.PI.toFloat()) * 1.9f
                    c.z = zPos - 2.2f - i * 2.4f
                    c.spinPhaseDeg = i * 32f
                }
                if (rng.nextFloat() < 0.5f) {
                    setupObstacleGeometry(acquireObstacle(), ObstacleType.ROCK, lanes[1], zPos - 2.0f)
                }
            }

            // Rolling snowball downhill hazard!
            patternRoll < 0.78f -> {
                val ballLane = lanes[0]
                setupObstacleGeometry(acquireObstacle(), ObstacleType.ROLLING_SNOWBALL, ballLane, zPos - 6f)
                val secondType = if (rng.nextBoolean()) ObstacleType.WOODEN_BARRIER_LOW else ObstacleType.ICE_BLOCK
                setupObstacleGeometry(acquireObstacle(), secondType, lanes[1], zPos)
                spawnCoinLine(lanes[2], zPos + 3f, count = 4, stepZ = 2.2f, baseY = 0.65f)
                maybeSpawnPowerUp(lanes[2], zPos - 6.5f, 0.85f)
            }

            // Jumpable alpine hurdle / rock / ice block wave with jump coin arc
            else -> {
                val jumpLane = lanes[0]
                val jumpType = when (rng.nextInt(3)) {
                    0 -> ObstacleType.WOODEN_BARRIER_LOW
                    1 -> ObstacleType.ICE_BLOCK
                    else -> ObstacleType.ROCK
                }
                setupObstacleGeometry(acquireObstacle(), jumpType, jumpLane, zPos)
                for (i in -2..2) {
                    val c = acquireCoin()
                    c.active = true
                    c.collected = false
                    c.collectAnim = 0f
                    c.x = jumpLane * LANE_WIDTH
                    val norm = 1f - (abs(i) / 2.5f)
                    c.y = 0.65f + norm * 1.35f
                    c.z = zPos + i * 2.0f
                    c.spinPhaseDeg = (i + 2) * 35f
                }
                if (rng.nextFloat() < 0.5f + difficulty * 0.35f) {
                    setupObstacleGeometry(acquireObstacle(), ObstacleType.SNOW_TREE, lanes[1], zPos)
                }
                maybeSpawnPowerUp(lanes[2], zPos, 0.85f)
            }
        }

        // Spacing shrinks gently as difficulty increases, while always allowing reaction time
        return (20.5f - difficulty * 5.0f).coerceAtLeast(14.5f)
    }

    private fun spawnCoinLine(lane: Int, startZ: Float, count: Int, stepZ: Float, baseY: Float) {
        for (i in 0 until count) {
            val c = acquireCoin()
            c.active = true
            c.collected = false
            c.collectAnim = 0f
            c.x = lane * LANE_WIDTH
            c.y = baseY
            c.z = startZ - i * stepZ
            c.spinPhaseDeg = i * 30f
        }
    }

    private fun maybeSpawnPowerUp(lane: Int, zPos: Float, yPos: Float) {
        if (rng.nextFloat() > 0.24f) return
        val pu = powerUps.firstOrNull { !it.active } ?: return
        pu.active = true
        pu.collected = false
        pu.type = PowerUpType.entries[rng.nextInt(PowerUpType.entries.size)]
        pu.x = lane * LANE_WIDTH
        pu.y = yPos
        pu.z = zPos
        pu.spinPhaseDeg = 0f
    }

    private fun acquireObstacle(): ObstacleInstance {
        return obstacles.firstOrNull { !it.active } ?: obstacles.minBy { -it.z }
    }

    private fun acquireCoin(): CoinInstance {
        return coins.firstOrNull { !it.active } ?: coins.minBy { -it.z }
    }

    private fun setupObstacleGeometry(
        obs: ObstacleInstance,
        type: ObstacleType,
        lane: Int,
        zPos: Float
    ) {
        obs.active = true
        obs.shattered = false
        obs.shatterAnim = 0f
        obs.type = type
        obs.lane = lane
        obs.x = lane * LANE_WIDTH
        obs.y = 0f
        obs.z = zPos
        obs.rollAngleDeg = 0f

        when (type) {
            ObstacleType.SNOW_TREE -> {
                obs.minY = 0f
                obs.maxY = 3.1f
                obs.halfWidth = 0.72f
                obs.halfDepth = 0.72f
            }
            ObstacleType.ICE_BLOCK -> {
                obs.minY = 0f
                obs.maxY = 0.92f
                obs.halfWidth = 0.78f
                obs.halfDepth = 0.62f
            }
            ObstacleType.ROLLING_SNOWBALL -> {
                obs.minY = 0f
                obs.maxY = 1.05f
                obs.halfWidth = 0.74f
                obs.halfDepth = 0.74f
            }
            ObstacleType.WOODEN_BARRIER_LOW -> {
                obs.minY = 0f
                obs.maxY = 0.88f
                obs.halfWidth = 0.92f
                obs.halfDepth = 0.34f
            }
            ObstacleType.WOODEN_GATE_HIGH -> {
                // High crossbeam: standing player (1.45m) hits it; sliding player (0.56m) glides cleanly underneath!
                obs.minY = 0.86f
                obs.maxY = 2.70f
                obs.halfWidth = 0.98f
                obs.halfDepth = 0.38f
            }
            ObstacleType.ROCK -> {
                obs.minY = 0f
                obs.maxY = 0.92f
                obs.halfWidth = 0.78f
                obs.halfDepth = 0.68f
            }
            ObstacleType.NARROW_PILLAR -> {
                obs.minY = 0f
                obs.maxY = 3.0f
                obs.halfWidth = 0.84f
                obs.halfDepth = 0.65f
            }
            ObstacleType.SNOW_RAMP -> {
                obs.minY = 0f
                obs.maxY = 0.85f
                obs.halfWidth = 0.92f
                obs.halfDepth = 1.35f
            }
        }
    }

    private fun initSnowParticles() {
        for (i in snowParticles.indices) {
            val p = snowParticles[i]
            p.isSpray = i < 28
            if (p.isSpray) {
                p.life = rng.nextFloat()
                p.x = 0f
                p.y = -5f
                p.z = 0f
            } else {
                p.x = rng.nextFloat() * 24f - 12f
                p.y = rng.nextFloat() * 11f
                p.z = rng.nextFloat() * 55f - 45f
                p.vx = rng.nextFloat() * 1.4f - 0.7f
                p.vy = -(2.4f + rng.nextFloat() * 2.2f)
                p.size = 0.05f + rng.nextFloat() * 0.06f
            }
        }
    }

    private fun updateAmbientSnowOnly(dt: Float, forwardSpeed: Float) {
        for (p in snowParticles) {
            if (p.isSpray) {
                p.y = -10f
                continue
            }
            p.x += (p.vx + sin(worldTime + p.z * 0.1f) * 0.5f) * dt
            p.y += p.vy * dt
            p.z += forwardSpeed * dt
            if (p.y < 0f || p.z > 10f) {
                p.x = rng.nextFloat() * 22f - 11f
                p.y = 8.5f + rng.nextFloat() * 3.5f
                p.z = -38f + rng.nextFloat() * 38f
            }
        }
    }

    private fun updateParticlesPlaying(dt: Float, dz: Float) {
        for (p in snowParticles) {
            if (p.isSpray) {
                p.life -= dt * 3.2f
                if (p.life <= 0f && !isJumping) {
                    p.life = 1f
                    val side = if (rng.nextBoolean()) -0.45f else 0.45f
                    p.x = playerX + side + (rng.nextFloat() - 0.5f) * 0.2f
                    p.y = playerY + 0.08f
                    p.z = 0.55f
                    p.vx = side * (1.8f + rng.nextFloat() * 1.5f)
                    p.vy = 1.6f + rng.nextFloat() * 1.8f
                    p.vz = 5.5f + rng.nextFloat() * 3.0f
                    p.size = if (boostTimer > 0f) 0.11f else 0.07f
                } else if (p.life > 0f) {
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.vy -= 7.5f * dt
                    p.z += p.vz * dt
                } else {
                    p.y = -10f
                }
            } else {
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.z += dz * 0.65f
                if (p.y < 0f || p.z > 10f) {
                    p.x = rng.nextFloat() * 24f - 12f
                    p.y = 8.5f + rng.nextFloat() * 3.5f
                    p.z = -44f + rng.nextFloat() * 24f
                }
            }
        }
    }

    fun copyForRender(
        outSegments: Array<TrackSegment>,
        outObstacles: Array<ObstacleInstance>,
        outCoins: Array<CoinInstance>,
        outPowerUps: Array<PowerUpInstance>,
        outParticles: Array<SnowParticle>
    ): RenderSnapshot = synchronized(lock) {
        for (i in segments.indices) {
            val s = segments[i]
            val d = outSegments[i]
            d.zStart = s.zStart
            d.hasFrozenRiver = s.hasFrozenRiver
            d.hasWoodenFenceLeft = s.hasWoodenFenceLeft
            d.hasWoodenFenceRight = s.hasWoodenFenceRight
            for (pIdx in s.props.indices) {
                val sp = s.props[pIdx]
                val dp = d.props[pIdx]
                dp.offsetX = sp.offsetX
                dp.offsetZ = sp.offsetZ
                dp.scale = sp.scale
                dp.kind = sp.kind
            }
        }
        for (i in obstacles.indices) {
            val s = obstacles[i]
            val d = outObstacles[i]
            d.active = s.active
            if (s.active) {
                d.shattered = s.shattered
                d.shatterAnim = s.shatterAnim
                d.type = s.type
                d.lane = s.lane
                d.x = s.x
                d.y = s.y
                d.z = s.z
                d.rollAngleDeg = s.rollAngleDeg
            }
        }
        for (i in coins.indices) {
            val s = coins[i]
            val d = outCoins[i]
            d.active = s.active
            if (s.active) {
                d.collected = s.collected
                d.collectAnim = s.collectAnim
                d.x = s.x
                d.y = s.y
                d.z = s.z
                d.spinPhaseDeg = s.spinPhaseDeg
            }
        }
        for (i in powerUps.indices) {
            val s = powerUps[i]
            val d = outPowerUps[i]
            d.active = s.active
            if (s.active) {
                d.collected = s.collected
                d.type = s.type
                d.x = s.x
                d.y = s.y
                d.z = s.z
                d.spinPhaseDeg = s.spinPhaseDeg
            }
        }
        for (i in snowParticles.indices) {
            val s = snowParticles[i]
            val d = outParticles[i]
            d.x = s.x
            d.y = s.y
            d.z = s.z
            d.size = s.size
            d.isSpray = s.isSpray
            d.life = s.life
        }
        RenderSnapshot(
            gameStateOrdinal = gameState.ordinal,
            playerX = playerX,
            playerY = playerY,
            leanAngleDeg = leanAngleDeg,
            pitchAngleDeg = pitchAngleDeg,
            isSliding = isSliding,
            isJumping = isJumping,
            speedRatio = ((effectiveSpeed - BASE_START_SPEED) / (MAX_BASE_SPEED - BASE_START_SPEED)).coerceIn(0f, 1.5f),
            magnetActive = magnetTimer > 0f,
            boostActive = boostTimer > 0f,
            shieldActive = shieldTimer > 0f,
            crashShake = crashShake,
            worldTime = worldTime
        )
    }
}
