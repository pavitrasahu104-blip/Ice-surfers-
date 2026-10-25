package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundSynthesizer
import com.example.data.PlayerRepository
import com.example.game.engine.EngineEventListener
import com.example.game.engine.FrostRushEngine
import com.example.game.model.CharacterId
import com.example.game.model.GameState
import com.example.game.model.HudState
import com.example.game.model.MenuModal
import com.example.game.model.PlayerProfile
import com.example.game.model.PowerUpType
import com.example.game.model.SledId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FrostRushViewModel(
    private val repository: PlayerRepository,
    val engine: FrostRushEngine = FrostRushEngine(),
    private val sound: SoundSynthesizer = SoundSynthesizer()
) : ViewModel() {

    val profileState: StateFlow<PlayerProfile> = repository.profileFlow
        .onEach { profile ->
            sound.soundEnabled = profile.soundEnabled
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PlayerProfile()
        )

    private val _hudState = MutableStateFlow(engine.getHudState(0))
    val hudState: StateFlow<HudState> = _hudState.asStateFlow()

    private val _activeModal = MutableStateFlow(MenuModal.NONE)
    val activeModal: StateFlow<MenuModal> = _activeModal.asStateFlow()

    private val _lastRunSummary = MutableStateFlow(RunSummary())
    val lastRunSummary: StateFlow<RunSummary> = _lastRunSummary.asStateFlow()

    private val _shortfallCoinsNeeded = MutableStateFlow<Int?>(null)
    val shortfallCoinsNeeded: StateFlow<Int?> = _shortfallCoinsNeeded.asStateFlow()

    private val engineListener = object : EngineEventListener {
        override fun onCoinCollected() {
            sound.playCoin()
            syncHudFromEngine()
        }

        override fun onJump() {
            sound.playJump()
        }

        override fun onSlide() {
            sound.playSlide()
        }

        override fun onPowerUpCollected(type: PowerUpType) {
            sound.playPowerUp()
            syncHudFromEngine()
        }

        override fun onShieldAbsorbed() {
            sound.playShieldBreak()
            syncHudFromEngine()
        }

        override fun onCollisionGameOver(
            finalScore: Int,
            distanceMeters: Int,
            coinsCollected: Int
        ) {
            sound.playCollision()
            val previousBest = profileState.value.bestScore
            val isNewBest = finalScore > previousBest && finalScore > 0
            _lastRunSummary.value = RunSummary(
                finalScore = finalScore,
                distanceMeters = distanceMeters,
                coinsCollected = coinsCollected,
                isNewBest = isNewBest
            )
            syncHudFromEngine()
            viewModelScope.launch {
                repository.recordCompletedRun(finalScore = finalScore, collectedCoins = coinsCollected)
                syncHudFromEngine()
            }
        }
    }

    init {
        engine.listener = engineListener
        syncHudFromEngine()
    }

    fun syncHudFromEngine() {
        val currentBest = profileState.value.bestScore
        _hudState.value = engine.getHudState(currentBest)
    }

    fun onPlayClicked() {
        sound.playButton()
        _activeModal.value = MenuModal.NONE
        _shortfallCoinsNeeded.value = null
        engine.startNewRun()
        syncHudFromEngine()
    }

    fun onPauseClicked() {
        if (engine.gameState == GameState.PLAYING) {
            sound.playButton()
            engine.pauseGame()
            syncHudFromEngine()
        }
    }

    fun onLifecyclePause() {
        if (engine.gameState == GameState.PLAYING) {
            engine.pauseGame()
            syncHudFromEngine()
        }
    }

    fun onResumeClicked() {
        if (engine.gameState == GameState.PAUSED) {
            sound.playButton()
            engine.resumeGame()
            syncHudFromEngine()
        }
    }

    fun onRestartClicked() {
        sound.playButton()
        _shortfallCoinsNeeded.value = null
        engine.startNewRun()
        syncHudFromEngine()
    }

    fun onMainMenuClicked() {
        sound.playButton()
        _activeModal.value = MenuModal.NONE
        engine.returnToMenu()
        syncHudFromEngine()
    }

    fun openModal(modal: MenuModal) {
        sound.playButton()
        _shortfallCoinsNeeded.value = null
        _activeModal.value = modal
    }

    fun closeModal() {
        sound.playButton()
        _shortfallCoinsNeeded.value = null
        _activeModal.value = MenuModal.NONE
    }

    fun onMoveLeft() {
        if (engine.moveLeft()) {
            syncHudFromEngine()
        }
    }

    fun onMoveRight() {
        if (engine.moveRight()) {
            syncHudFromEngine()
        }
    }

    fun onJump() {
        if (engine.jump()) {
            syncHudFromEngine()
        }
    }

    fun onSlide() {
        if (engine.slide()) {
            syncHudFromEngine()
        }
    }

    fun onCharacterAction(character: CharacterId) {
        viewModelScope.launch {
            val profile = profileState.value
            val wasLocked = character !in profile.unlockedCharacters
            if (wasLocked && profile.totalCoins < character.costCoins) {
                _shortfallCoinsNeeded.value = character.costCoins - profile.totalCoins
                sound.playButton()
                return@launch
            }
            val success = repository.unlockOrSelectCharacter(character)
            if (success) {
                _shortfallCoinsNeeded.value = null
                if (wasLocked) sound.playPowerUp() else sound.playButton()
            }
        }
    }

    fun onSledAction(sled: SledId) {
        viewModelScope.launch {
            val profile = profileState.value
            val wasLocked = sled !in profile.unlockedSleds
            if (wasLocked && profile.totalCoins < sled.costCoins) {
                _shortfallCoinsNeeded.value = sled.costCoins - profile.totalCoins
                sound.playButton()
                return@launch
            }
            val success = repository.unlockOrSelectSled(sled)
            if (success) {
                _shortfallCoinsNeeded.value = null
                if (wasLocked) sound.playPowerUp() else sound.playButton()
            }
        }
    }

    fun onToggleSound() {
        val next = !profileState.value.soundEnabled
        sound.soundEnabled = next
        if (next) sound.playButton()
        viewModelScope.launch {
            repository.setSoundEnabled(next)
        }
    }

    fun onToggleTouchButtons() {
        sound.playButton()
        val next = !profileState.value.touchButtonsEnabled
        viewModelScope.launch {
            repository.setTouchButtonsEnabled(next)
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.listener = null
        sound.shutdown()
    }

    data class RunSummary(
        val finalScore: Int = 0,
        val distanceMeters: Int = 0,
        val coinsCollected: Int = 0,
        val isNewBest: Boolean = false
    )

    class Factory(
        private val repository: PlayerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FrostRushViewModel(repository = repository) as T
        }
    }
}
