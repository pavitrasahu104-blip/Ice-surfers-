package com.example

import android.content.Context
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeUp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.audio.SoundSynthesizer
import com.example.data.FrostRushDatabase
import com.example.data.PlayerRepository
import com.example.game.engine.FrostRushEngine
import com.example.game.model.CharacterId
import com.example.game.model.GameState
import com.example.game.model.ObstacleType
import com.example.game.model.SledId
import com.example.ui.FrostRushApp
import com.example.ui.FrostRushViewModel
import com.example.ui.theme.FrostRushTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: FrostRushDatabase
    private lateinit var repository: PlayerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, FrostRushDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = PlayerRepository(db.playerProfileDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun appName_matchesFrostRush3D() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Frost Rush 3D", context.getString(R.string.app_name))
    }

    @Test
    fun mainMenu_play_swipeControls_pauseResume_collisionGameOver_restart_and_unlockPersistence() {
        val engine = FrostRushEngine(seed = 42L)
        val vm = FrostRushViewModel(
            repository = repository,
            engine = engine,
            sound = SoundSynthesizer(initialEnabled = false)
        )

        composeTestRule.setContent {
            FrostRushTheme {
                FrostRushApp(viewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // 1. Verify Main Menu renders title, coins counter, and PLAY button
        composeTestRule.onNodeWithTag("game_title_text").assertIsDisplayed()
        composeTestRule.onNodeWithTag("menu_coins_counter").assertIsDisplayed()
        composeTestRule.onNodeWithTag("play_button").assertIsDisplayed()

        // 2. Start Run via PLAY button
        composeTestRule.onNodeWithTag("play_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameState.PLAYING, engine.gameState)
        composeTestRule.onNodeWithTag("hud_score_text").assertIsDisplayed()

        // Clear obstacles and test mobile touch swipe left & swipe up gestures
        engine.obstacles.forEach { it.active = false }
        composeTestRule.onNodeWithTag("game_touch_area").performTouchInput {
            swipeLeft(startX = centerX + 100f, endX = centerX - 150f, durationMillis = 120)
        }
        composeTestRule.waitForIdle()
        assertEquals(-1, engine.currentLane)

        composeTestRule.onNodeWithTag("game_touch_area").performTouchInput {
            swipeUp(startY = centerY + 120f, endY = centerY - 150f, durationMillis = 120)
        }
        composeTestRule.waitForIdle()
        assertTrue(engine.isJumping)

        // 3. Test Pause and Resume
        composeTestRule.onNodeWithTag("pause_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameState.PAUSED, engine.gameState)
        composeTestRule.onNodeWithTag("pause_modal").assertIsDisplayed()

        composeTestRule.onNodeWithTag("resume_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameState.PLAYING, engine.gameState)

        // 4. Collect coins legitimately during run, then hit obstacle -> Game Over & persist coins
        engine.obstacles.forEach { it.active = false }
        engine.coins.forEach { it.active = false }
        // Land player first
        repeat(40) { engine.update(0.025f) }
        for (i in 0 until 5) {
            engine.spawnCoinForTest(lane = -1, yPos = 0.65f, zPos = -0.5f - i * 0.6f)
        }
        repeat(15) { engine.update(0.02f) }
        assertTrue("Should have collected 5 coins", engine.runCoins >= 5)

        // Trigger obstacle collision in current lane (-1)
        engine.spawnObstacleForTest(ObstacleType.SNOW_TREE, lane = -1, zPos = -0.8f)
        repeat(10) { engine.update(0.02f) }
        vm.syncHudFromEngine()
        composeTestRule.waitForIdle()

        assertEquals(GameState.GAME_OVER, engine.gameState)
        composeTestRule.onNodeWithTag("game_over_modal").assertIsDisplayed()

        // Verify Restart button starts a clean active run
        composeTestRule.onNodeWithTag("restart_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameState.PLAYING, engine.gameState)
        assertEquals(0, engine.runCoins)

        // Return to Main Menu via Pause -> Main Menu
        composeTestRule.onNodeWithTag("pause_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("pause_menu_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(GameState.MENU, engine.gameState)

        // 5. Verify persistent coin balance & unlocking Character B and Sled 2
        runBlocking {
            repository.recordCompletedRun(finalScore = 450, collectedCoins = 80)
            val saved = repository.profileFlow.first { it.totalCoins >= 80 }
            assertTrue(saved.bestScore >= 450)
        }
        composeTestRule.waitForIdle()

        // Open Character Select & unlock RACER (cost 40)
        composeTestRule.onNodeWithTag("character_select_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("character_action_RACER").performClick()
        composeTestRule.waitForIdle()
        runBlocking {
            val updated = repository.profileFlow.first { it.selectedCharacter == CharacterId.RACER }
            assertTrue(CharacterId.RACER in updated.unlockedCharacters)
        }
        composeTestRule.onNodeWithTag("modal_close_button").performClick()
        composeTestRule.waitForIdle()

        // Open Sled Select & unlock PLASMA_WING (cost 35)
        composeTestRule.onNodeWithTag("sled_select_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("sled_action_PLASMA_WING").performClick()
        composeTestRule.waitForIdle()
        runBlocking {
            val updated = repository.profileFlow.first { it.selectedSled == SledId.PLASMA_WING }
            assertTrue(SledId.PLASMA_WING in updated.unlockedSleds)
        }
    }
}
