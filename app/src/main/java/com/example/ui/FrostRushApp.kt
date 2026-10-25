package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.model.GameState
import com.example.game.model.MenuModal
import com.example.game.render.FrostRush3DViewport
import kotlin.math.abs

@Composable
fun FrostRushApp(
    viewModel: FrostRushViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profileState.collectAsStateWithLifecycle()
    val hud by viewModel.hudState.collectAsStateWithLifecycle()
    val activeModal by viewModel.activeModal.collectAsStateWithLifecycle()
    val lastRunSummary by viewModel.lastRunSummary.collectAsStateWithLifecycle()
    val shortfallCoins by viewModel.shortfallCoinsNeeded.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val swipeThresholdPx = with(LocalDensity.current) { 26.dp.toPx() }

    LaunchedEffect(hud.gameState) {
        focusRequester.requestFocus()
    }

    // Automatically pause an active run if the app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                viewModel.onLifecyclePause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // System back button handling for gameplay states
    when (hud.gameState) {
        GameState.PLAYING -> BackHandler { viewModel.onPauseClicked() }
        GameState.PAUSED -> BackHandler { viewModel.onResumeClicked() }
        GameState.GAME_OVER -> BackHandler { viewModel.onMainMenuClicked() }
        GameState.MENU -> Unit
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (keyEvent.key) {
                    Key.DirectionLeft, Key.A -> {
                        viewModel.onMoveLeft()
                        true
                    }
                    Key.DirectionRight, Key.D -> {
                        viewModel.onMoveRight()
                        true
                    }
                    Key.DirectionUp, Key.W, Key.Spacebar -> {
                        viewModel.onJump()
                        true
                    }
                    Key.DirectionDown, Key.S -> {
                        viewModel.onSlide()
                        true
                    }
                    Key.Escape, Key.P -> {
                        when (hud.gameState) {
                            GameState.PLAYING -> viewModel.onPauseClicked()
                            GameState.PAUSED -> viewModel.onResumeClicked()
                            else -> Unit
                        }
                        true
                    }
                    else -> false
                }
            }
            .pointerInput(hud.gameState, swipeThresholdPx) {
                if (hud.gameState != GameState.PLAYING) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var startPos = down.position
                    var gestureTriggered = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break

                        val dx = change.position.x - startPos.x
                        val dy = change.position.y - startPos.y
                        if (!gestureTriggered && (abs(dx) >= swipeThresholdPx || abs(dy) >= swipeThresholdPx)) {
                            gestureTriggered = true
                            change.consume()
                            if (abs(dx) > abs(dy)) {
                                if (dx < 0f) viewModel.onMoveLeft() else viewModel.onMoveRight()
                            } else {
                                if (dy < 0f) viewModel.onJump() else viewModel.onSlide()
                            }
                            startPos = change.position
                        } else if (gestureTriggered) {
                            change.consume()
                        }
                    }
                }
            }
            .testTag("game_touch_area")
    ) {
        // Real-time 3D OpenGL ES 2.0 Mountain Sled Environment & Character Renderer
        FrostRush3DViewport(
            engine = viewModel.engine,
            selectedCharacter = profile.selectedCharacter,
            selectedSled = profile.selectedSled,
            onHudTick = viewModel::syncHudFromEngine,
            modifier = Modifier.fillMaxSize()
        )

        // State-driven UI Overlays
        when (hud.gameState) {
            GameState.MENU -> {
                MainMenuOverlay(
                    profile = profile,
                    onPlay = viewModel::onPlayClicked,
                    onOpenModal = viewModel::openModal,
                    onToggleSound = viewModel::onToggleSound
                )
                MenuModalSheet(
                    modal = activeModal,
                    profile = profile,
                    shortfallCoinsNeeded = shortfallCoins,
                    onClose = viewModel::closeModal,
                    onCharacterAction = viewModel::onCharacterAction,
                    onSledAction = viewModel::onSledAction,
                    onToggleSound = viewModel::onToggleSound,
                    onToggleTouchButtons = viewModel::onToggleTouchButtons
                )
            }

            GameState.PLAYING -> {
                GameplayHudOverlay(
                    hud = hud,
                    touchButtonsEnabled = profile.touchButtonsEnabled,
                    onPause = viewModel::onPauseClicked,
                    onToggleTouchButtons = viewModel::onToggleTouchButtons,
                    onMoveLeft = viewModel::onMoveLeft,
                    onMoveRight = viewModel::onMoveRight,
                    onJump = viewModel::onJump,
                    onSlide = viewModel::onSlide
                )
            }

            GameState.PAUSED -> {
                GameplayHudOverlay(
                    hud = hud,
                    touchButtonsEnabled = false,
                    onPause = viewModel::onPauseClicked,
                    onToggleTouchButtons = viewModel::onToggleTouchButtons,
                    onMoveLeft = {},
                    onMoveRight = {},
                    onJump = {},
                    onSlide = {}
                )
                PauseOverlay(
                    hud = hud,
                    soundEnabled = profile.soundEnabled,
                    onResume = viewModel::onResumeClicked,
                    onRestart = viewModel::onRestartClicked,
                    onToggleSound = viewModel::onToggleSound,
                    onMainMenu = viewModel::onMainMenuClicked
                )
            }

            GameState.GAME_OVER -> {
                GameOverOverlay(
                    summary = lastRunSummary,
                    bestScore = profile.bestScore,
                    totalCoins = profile.totalCoins,
                    onRestart = viewModel::onRestartClicked,
                    onMainMenu = viewModel::onMainMenuClicked
                )
            }
        }
    }
}
