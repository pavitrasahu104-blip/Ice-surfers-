package com.example

import com.example.game.engine.EngineEventListener
import com.example.game.engine.FrostRushEngine
import com.example.game.model.GameState
import com.example.game.model.ObstacleType
import com.example.game.model.PowerUpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun engine_forwardMovement_laneChanges_and_increasingSpeed() {
        val engine = FrostRushEngine(seed = 12345L)
        assertEquals(GameState.MENU, engine.gameState)

        engine.startNewRun()
        assertEquals(GameState.PLAYING, engine.gameState)
        // Clear random hazards so we test pure kinematics & speed ramp
        engine.obstacles.forEach { it.active = false }

        val initialSpeed = engine.effectiveSpeed
        assertTrue(engine.moveLeft())
        assertEquals(-1, engine.currentLane)

        repeat(40) {
            engine.obstacles.forEach { it.active = false }
            engine.update(0.025f)
        }
        assertTrue("Player X should smoothly move into left lane", engine.playerX < -2.0f)
        assertTrue("Distance should advance forward", engine.getDistanceMeters() > 10)
        assertTrue("Speed should increase with distance", engine.baseSpeed > initialSpeed)

        assertTrue(engine.moveRight())
        assertTrue(engine.moveRight())
        assertEquals(1, engine.currentLane)
        repeat(40) {
            engine.obstacles.forEach { it.active = false }
            engine.update(0.025f)
        }
        assertTrue("Player X should smoothly move into right lane", engine.playerX > 2.0f)
    }

    @Test
    fun engine_jumpClearsLowObstacle_and_slideClearsHighGate() {
        val engine = FrostRushEngine(seed = 99L)
        engine.startNewRun()
        engine.obstacles.forEach { it.active = false }

        // 1. Jump over ICE_BLOCK at z = -3.0f
        engine.spawnObstacleForTest(ObstacleType.ICE_BLOCK, lane = 0, zPos = -3.0f)
        assertTrue(engine.jump())
        repeat(20) { engine.update(0.02f) }
        assertEquals("Jumping should safely clear low ice block", GameState.PLAYING, engine.gameState)

        // Wait until landed
        repeat(35) {
            engine.obstacles.forEach { it.active = false }
            engine.update(0.02f)
        }
        assertFalse(engine.isJumping)

        // 2. Slide under WOODEN_GATE_HIGH at z = -2.5f
        engine.spawnObstacleForTest(ObstacleType.WOODEN_GATE_HIGH, lane = 0, zPos = -2.5f)
        assertTrue(engine.slide())
        assertTrue(engine.isSliding)
        repeat(18) { engine.update(0.02f) }
        assertEquals("Sliding should pass cleanly beneath high wooden gate", GameState.PLAYING, engine.gameState)
    }

    @Test
    fun engine_collisionStopsRunOnce_and_shieldSurvivesOneHit() {
        var gameOverCalls = 0
        var shieldAbsorbCalls = 0
        val engine = FrostRushEngine(seed = 77L)
        engine.listener = object : EngineEventListener {
            override fun onCoinCollected() {}
            override fun onJump() {}
            override fun onSlide() {}
            override fun onPowerUpCollected(type: PowerUpType) {}
            override fun onShieldAbsorbed() { shieldAbsorbCalls++ }
            override fun onCollisionGameOver(finalScore: Int, distanceMeters: Int, coinsCollected: Int) {
                gameOverCalls++
            }
        }

        engine.startNewRun()
        engine.obstacles.forEach { it.active = false }

        // Activate shield and hit a tall SNOW_TREE
        engine.activatePowerUpForTest(PowerUpType.SHIELD)
        engine.spawnObstacleForTest(ObstacleType.SNOW_TREE, lane = 0, zPos = -1.2f)
        repeat(10) { engine.update(0.02f) }

        assertEquals("Shield should absorb first obstacle hit", GameState.PLAYING, engine.gameState)
        assertEquals(1, shieldAbsorbCalls)
        assertEquals(0f, engine.shieldTimer, 0.001f)

        // Now without shield, hit another SNOW_TREE -> GAME_OVER exactly once
        engine.spawnObstacleForTest(ObstacleType.SNOW_TREE, lane = 0, zPos = -1.2f)
        repeat(15) { engine.update(0.02f) }

        assertEquals(GameState.GAME_OVER, engine.gameState)
        assertEquals("Game over event must not fire multiple times", 1, gameOverCalls)
    }

    @Test
    fun engine_coinsCollectOnce_magnetAttracts_boostSpeedsUp_and_pauseFreezesWorld() {
        val engine = FrostRushEngine(seed = 555L)
        engine.startNewRun()
        engine.obstacles.forEach { it.active = false }
        engine.coins.forEach { it.active = false }

        // Spawn single coin right ahead in lane 0
        engine.spawnCoinForTest(lane = 0, yPos = 0.65f, zPos = -0.8f)
        repeat(15) { engine.update(0.02f) }
        assertEquals("Coin should be collected exactly once", 1, engine.runCoins)

        // Activate COIN_MAGNET and spawn coin in far right lane (lane = 1)
        engine.activatePowerUpForTest(PowerUpType.COIN_MAGNET)
        engine.spawnCoinForTest(lane = 1, yPos = 0.65f, zPos = -8.0f)
        repeat(35) {
            engine.obstacles.forEach { it.active = false }
            engine.update(0.02f)
        }
        assertEquals("Magnet should pull adjacent-lane coin into player", 2, engine.runCoins)

        // Activate SPEED_BOOST and verify effectiveSpeed > baseSpeed, then expires
        engine.activatePowerUpForTest(PowerUpType.SPEED_BOOST)
        engine.obstacles.forEach { it.active = false }
        engine.update(0.02f)
        assertTrue(engine.effectiveSpeed > engine.baseSpeed * 1.3f)

        // Pause freezes movement & timers
        engine.pauseGame()
        val pausedDist = engine.getDistanceMeters()
        val pausedBoost = engine.boostTimer
        repeat(20) { engine.update(0.03f) }
        assertEquals(pausedDist, engine.getDistanceMeters())
        assertEquals(pausedBoost, engine.boostTimer, 0.0001f)

        // Resume continues run and lets power-up expire naturally
        engine.resumeGame()
        repeat(140) {
            engine.obstacles.forEach { it.active = false }
            engine.update(0.05f)
        }
        assertEquals(0f, engine.boostTimer, 0.001f)
    }
}
