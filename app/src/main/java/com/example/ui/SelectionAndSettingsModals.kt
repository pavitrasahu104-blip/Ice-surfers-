package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.CharacterId
import com.example.game.model.MenuModal
import com.example.game.model.PlayerProfile
import com.example.game.model.SledId
import com.example.ui.theme.CrimsonPeak
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.FrostedCardSurface
import com.example.ui.theme.FrostedSlate
import com.example.ui.theme.GlacierMidnight
import com.example.ui.theme.IceCyan
import com.example.ui.theme.SnowWhite
import com.example.ui.theme.SolarCoinGold

@Composable
fun MenuModalSheet(
    modal: MenuModal,
    profile: PlayerProfile,
    shortfallCoinsNeeded: Int?,
    onClose: () -> Unit,
    onCharacterAction: (CharacterId) -> Unit,
    onSledAction: (SledId) -> Unit,
    onToggleSound: () -> Unit,
    onToggleTouchButtons: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (modal == MenuModal.NONE) return

    BackHandler(onBack = onClose)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC06111F))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = FrostedSlate.copy(alpha = 0.96f),
            border = BorderStroke(1.5.dp, IceCyan.copy(alpha = 0.5f)),
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = onClose,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = GlacierMidnight,
                                contentColor = SnowWhite
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("modal_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.btn_back)
                            )
                        }
                        Text(
                            text = when (modal) {
                                MenuModal.CHARACTER_SELECT -> stringResource(R.string.label_character_select)
                                MenuModal.SLED_SELECT -> stringResource(R.string.label_sled_select)
                                MenuModal.SETTINGS -> stringResource(R.string.label_settings_title)
                                MenuModal.NONE -> ""
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = SnowWhite
                        )
                    }

                    // Coin wallet pill
                    Surface(
                        shape = CircleShape,
                        color = GlacierMidnight,
                        border = BorderStroke(1.dp, SolarCoinGold.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = null,
                                tint = SolarCoinGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = profile.totalCoins.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = SnowWhite,
                                modifier = Modifier.testTag("modal_coins_text")
                            )
                        }
                    }
                }

                if (shortfallCoinsNeeded != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = CrimsonPeak.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, CrimsonPeak)
                    ) {
                        Text(
                            text = stringResource(R.string.insufficient_coins, shortfallCoinsNeeded),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SnowWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("insufficient_coins_banner")
                        )
                    }
                }

                when (modal) {
                    MenuModal.CHARACTER_SELECT -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(CharacterId.entries) { character ->
                                val isUnlocked = character in profile.unlockedCharacters
                                val isSelected = profile.selectedCharacter == character
                                UnlockableItemCard(
                                    title = character.displayName,
                                    subtitle = character.roleTitle,
                                    description = character.description,
                                    primarySwatch = Color(
                                        character.suitRgb[0],
                                        character.suitRgb[1],
                                        character.suitRgb[2]
                                    ),
                                    accentSwatch = Color(
                                        character.visorRgb[0],
                                        character.visorRgb[1],
                                        character.visorRgb[2]
                                    ),
                                    costCoins = character.costCoins,
                                    isUnlocked = isUnlocked,
                                    isSelected = isSelected,
                                    actionTag = "character_action_${character.name}",
                                    onAction = { onCharacterAction(character) }
                                )
                            }
                        }
                    }

                    MenuModal.SLED_SELECT -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SledId.entries) { sled ->
                                val isUnlocked = sled in profile.unlockedSleds
                                val isSelected = profile.selectedSled == sled
                                UnlockableItemCard(
                                    title = sled.displayName,
                                    subtitle = sled.subtitle,
                                    description = sled.description,
                                    primarySwatch = Color(
                                        sled.bodyRgb[0],
                                        sled.bodyRgb[1],
                                        sled.bodyRgb[2]
                                    ),
                                    accentSwatch = Color(
                                        sled.runnerRgb[0],
                                        sled.runnerRgb[1],
                                        sled.runnerRgb[2]
                                    ),
                                    costCoins = sled.costCoins,
                                    isUnlocked = isUnlocked,
                                    isSelected = isSelected,
                                    actionTag = "sled_action_${sled.name}",
                                    onAction = { onSledAction(sled) }
                                )
                            }
                        }
                    }

                    MenuModal.SETTINGS -> {
                        SettingsContent(
                            soundEnabled = profile.soundEnabled,
                            touchButtonsEnabled = profile.touchButtonsEnabled,
                            onToggleSound = onToggleSound,
                            onToggleTouchButtons = onToggleTouchButtons
                        )
                    }

                    MenuModal.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun UnlockableItemCard(
    title: String,
    subtitle: String,
    description: String,
    primarySwatch: Color,
    accentSwatch: Color,
    costCoins: Int,
    isUnlocked: Boolean,
    isSelected: Boolean,
    actionTag: String,
    onAction: () -> Unit
) {
    val borderColor = when {
        isSelected -> EmeraldShield
        isUnlocked -> IceCyan.copy(alpha = 0.55f)
        else -> SolarCoinGold.copy(alpha = 0.45f)
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = FrostedCardSurface,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // 3D Color Swatch Emblem
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(primarySwatch),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(accentSwatch)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = SnowWhite
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = IceCyan
                        )
                    }
                }

                Button(
                    onClick = onAction,
                    enabled = !isSelected,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isSelected -> EmeraldShield.copy(alpha = 0.25f)
                            isUnlocked -> IceCyan
                            else -> SolarCoinGold
                        },
                        contentColor = GlacierMidnight,
                        disabledContainerColor = EmeraldShield.copy(alpha = 0.24f),
                        disabledContentColor = EmeraldShield
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag(actionTag)
                ) {
                    when {
                        isSelected -> {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_selected),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        isUnlocked -> {
                            Text(
                                text = stringResource(R.string.btn_select),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.btn_unlock_format, costCoins),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = SnowWhite.copy(alpha = 0.82f)
            )
        }
    }
}

@Composable
private fun SettingsContent(
    soundEnabled: Boolean,
    touchButtonsEnabled: Boolean,
    onToggleSound: () -> Unit,
    onToggleTouchButtons: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = FrostedCardSurface,
            border = BorderStroke(1.dp, SnowWhite.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setting_sound_effects),
                        style = MaterialTheme.typography.titleMedium,
                        color = SnowWhite
                    )
                    Text(
                        text = stringResource(R.string.setting_sound_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowWhite.copy(alpha = 0.72f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = soundEnabled,
                    onCheckedChange = { onToggleSound() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GlacierMidnight,
                        checkedTrackColor = IceCyan
                    ),
                    modifier = Modifier.testTag("settings_sound_switch")
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = FrostedCardSurface,
            border = BorderStroke(1.dp, SnowWhite.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.setting_touch_buttons),
                        style = MaterialTheme.typography.titleMedium,
                        color = SnowWhite
                    )
                    Text(
                        text = stringResource(R.string.setting_touch_buttons_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SnowWhite.copy(alpha = 0.72f)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = touchButtonsEnabled,
                    onCheckedChange = { onToggleTouchButtons() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GlacierMidnight,
                        checkedTrackColor = IceCyan
                    ),
                    modifier = Modifier.testTag("settings_touch_buttons_switch")
                )
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = GlacierMidnight.copy(alpha = 0.8f),
            border = BorderStroke(1.dp, IceCyan.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_controls_guide),
                    style = MaterialTheme.typography.labelLarge,
                    color = IceCyan
                )
                Text(
                    text = stringResource(R.string.controls_guide_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowWhite.copy(alpha = 0.85f)
                )
            }
        }
    }
}
