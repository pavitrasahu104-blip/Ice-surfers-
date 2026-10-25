package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.game.model.CharacterId
import com.example.game.model.PlayerProfile
import com.example.game.model.SledId

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val bestScore: Int = 0,
    val totalCoins: Int = 0,
    val unlockedCharactersCsv: String = CharacterId.EXPLORER.name,
    val unlockedSledsCsv: String = SledId.ALPINE_GLIDER.name,
    val selectedCharacter: String = CharacterId.EXPLORER.name,
    val selectedSled: String = SledId.ALPINE_GLIDER.name,
    val soundEnabled: Boolean = true,
    val touchButtonsEnabled: Boolean = false
) {
    fun toDomain(): PlayerProfile {
        val chars = unlockedCharactersCsv
            .split(",")
            .filter { it.isNotBlank() }
            .map { CharacterId.fromNameSafe(it.trim()) }
            .toSet() + CharacterId.EXPLORER

        val sleds = unlockedSledsCsv
            .split(",")
            .filter { it.isNotBlank() }
            .map { SledId.fromNameSafe(it.trim()) }
            .toSet() + SledId.ALPINE_GLIDER

        val chosenChar = CharacterId.fromNameSafe(selectedCharacter).let {
            if (it in chars) it else CharacterId.EXPLORER
        }
        val chosenSled = SledId.fromNameSafe(selectedSled).let {
            if (it in sleds) it else SledId.ALPINE_GLIDER
        }

        return PlayerProfile(
            bestScore = bestScore,
            totalCoins = totalCoins,
            unlockedCharacters = chars,
            unlockedSleds = sleds,
            selectedCharacter = chosenChar,
            selectedSled = chosenSled,
            soundEnabled = soundEnabled,
            touchButtonsEnabled = touchButtonsEnabled
        )
    }

    companion object {
        fun fromDomain(profile: PlayerProfile): PlayerProfileEntity = PlayerProfileEntity(
            id = 1,
            bestScore = profile.bestScore,
            totalCoins = profile.totalCoins,
            unlockedCharactersCsv = (profile.unlockedCharacters + CharacterId.EXPLORER)
                .joinToString(",") { it.name },
            unlockedSledsCsv = (profile.unlockedSleds + SledId.ALPINE_GLIDER)
                .joinToString(",") { it.name },
            selectedCharacter = profile.selectedCharacter.name,
            selectedSled = profile.selectedSled.name,
            soundEnabled = profile.soundEnabled,
            touchButtonsEnabled = profile.touchButtonsEnabled
        )
    }
}
