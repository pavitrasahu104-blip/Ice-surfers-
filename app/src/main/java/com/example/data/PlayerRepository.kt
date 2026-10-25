package com.example.data

import com.example.game.model.CharacterId
import com.example.game.model.PlayerProfile
import com.example.game.model.SledId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PlayerRepository(private val dao: PlayerProfileDao) {

    private val mutex = Mutex()

    val profileFlow: Flow<PlayerProfile> = dao.observeProfile().map { entity ->
        entity?.toDomain() ?: PlayerProfile()
    }

    private suspend fun currentDomain(): PlayerProfile {
        return dao.getProfileOnce()?.toDomain() ?: PlayerProfile()
    }

    suspend fun recordCompletedRun(finalScore: Int, collectedCoins: Int): Boolean = mutex.withLock {
        val current = currentDomain()
        val safeCoins = collectedCoins.coerceAtLeast(0)
        val safeScore = finalScore.coerceAtLeast(0)
        val isNewBest = safeScore > current.bestScore
        val updated = current.copy(
            bestScore = maxOf(current.bestScore, safeScore),
            totalCoins = current.totalCoins + safeCoins
        )
        dao.upsertProfile(PlayerProfileEntity.fromDomain(updated))
        isNewBest
    }

    suspend fun unlockOrSelectCharacter(character: CharacterId): Boolean = mutex.withLock {
        val current = currentDomain()
        if (character in current.unlockedCharacters) {
            dao.upsertProfile(
                PlayerProfileEntity.fromDomain(current.copy(selectedCharacter = character))
            )
            return true
        }
        if (current.totalCoins < character.costCoins) {
            return false
        }
        val updated = current.copy(
            totalCoins = current.totalCoins - character.costCoins,
            unlockedCharacters = current.unlockedCharacters + character,
            selectedCharacter = character
        )
        dao.upsertProfile(PlayerProfileEntity.fromDomain(updated))
        true
    }

    suspend fun unlockOrSelectSled(sled: SledId): Boolean = mutex.withLock {
        val current = currentDomain()
        if (sled in current.unlockedSleds) {
            dao.upsertProfile(
                PlayerProfileEntity.fromDomain(current.copy(selectedSled = sled))
            )
            return true
        }
        if (current.totalCoins < sled.costCoins) {
            return false
        }
        val updated = current.copy(
            totalCoins = current.totalCoins - sled.costCoins,
            unlockedSleds = current.unlockedSleds + sled,
            selectedSled = sled
        )
        dao.upsertProfile(PlayerProfileEntity.fromDomain(updated))
        true
    }

    suspend fun setSoundEnabled(enabled: Boolean) = mutex.withLock {
        val current = currentDomain()
        dao.upsertProfile(PlayerProfileEntity.fromDomain(current.copy(soundEnabled = enabled)))
    }

    suspend fun setTouchButtonsEnabled(enabled: Boolean) = mutex.withLock {
        val current = currentDomain()
        dao.upsertProfile(PlayerProfileEntity.fromDomain(current.copy(touchButtonsEnabled = enabled)))
    }
}
