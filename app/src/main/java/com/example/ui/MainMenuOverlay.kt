package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.MenuModal
import com.example.game.model.PlayerProfile
import com.example.ui.theme.FrostedCardSurface
import com.example.ui.theme.GlacierMidnight
import com.example.ui.theme.IceCyan
import com.example.ui.theme.SnowWhite
import com.example.ui.theme.SolarCoinGold

@Composable
fun MainMenuOverlay(
    profile: PlayerProfile,
    onPlay: () -> Unit,
    onOpenModal: (MenuModal) -> Unit,
    onToggleSound: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0x99071322),
                        Color(0x18071322),
                        Color(0xCC071322)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxSize()
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Best Score, Coin Counter, Sound Toggle, Settings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatPill(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = IceCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = stringResource(R.string.label_best_score),
                        value = profile.bestScore.toString(),
                        tag = "best_score_badge"
                    )
                    StatPill(
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.MonetizationOn,
                                contentDescription = null,
                                tint = SolarCoinGold,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = stringResource(R.string.label_coins),
                        value = profile.totalCoins.toString(),
                        tag = "menu_coins_counter"
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = onToggleSound,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = FrostedCardSurface,
                            contentColor = if (profile.soundEnabled) IceCyan else SnowWhite.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (profile.soundEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = if (profile.soundEnabled) {
                                stringResource(R.string.btn_sound_on)
                            } else {
                                stringResource(R.string.btn_sound_off)
                            }
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { onOpenModal(MenuModal.SETTINGS) },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = FrostedCardSurface,
                            contentColor = SnowWhite
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.btn_settings)
                        )
                    }
                }
            }

            // Title Crest Banner at Upper-Middle leaving center 3D character & sled clearly visible
            Surface(
                shape = MaterialTheme.shapes.large,
                color = FrostedCardSurface,
                border = BorderStroke(1.5.dp, IceCyan.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_frost_peak_banner_1792897188299),
                        contentDescription = stringResource(R.string.cd_alpine_banner),
                        contentScale = ContentScale.Crop,
                        alpha = 0.34f,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(MaterialTheme.shapes.large)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.game_title),
                            style = MaterialTheme.typography.displayMedium,
                            color = SnowWhite,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("game_title_text")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.game_subtitle),
                            style = MaterialTheme.typography.labelMedium,
                            color = IceCyan,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LoadoutChip(text = profile.selectedCharacter.displayName)
                            LoadoutChip(text = profile.selectedSled.displayName)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Primary Action Dock
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onPlay,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyan,
                        contentColor = GlacierMidnight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .border(2.dp, SnowWhite.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                        .testTag("play_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_play),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onOpenModal(MenuModal.CHARACTER_SELECT) },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.5.dp, IceCyan.copy(alpha = 0.7f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = FrostedCardSurface,
                            contentColor = SnowWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("character_select_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = IceCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_characters),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    OutlinedButton(
                        onClick = { onOpenModal(MenuModal.SLED_SELECT) },
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.5.dp, SolarCoinGold.copy(alpha = 0.8f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = FrostedCardSurface,
                            contentColor = SnowWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("sled_select_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DownhillSkiing,
                            contentDescription = null,
                            tint = SolarCoinGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_sleds),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: @Composable () -> Unit,
    label: String,
    value: String,
    tag: String
) {
    Surface(
        shape = CircleShape,
        color = FrostedCardSurface,
        border = BorderStroke(1.dp, SnowWhite.copy(alpha = 0.22f)),
        modifier = Modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            icon()
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = SnowWhite.copy(alpha = 0.72f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = SnowWhite
                )
            }
        }
    }
}

@Composable
private fun LoadoutChip(text: String) {
    Surface(
        shape = CircleShape,
        color = GlacierMidnight.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, IceCyan.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = SnowWhite,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}
