package com.example.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.HudState
import com.example.game.model.PowerUpType
import com.example.ui.theme.AuroraPurple
import com.example.ui.theme.CrimsonPeak
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.FrostedCardSurface
import com.example.ui.theme.GlacierMidnight
import com.example.ui.theme.IceCyan
import com.example.ui.theme.SnowWhite
import com.example.ui.theme.SolarCoinGold
import java.util.Locale

@Composable
fun GameplayHudOverlay(
    hud: HudState,
    touchButtonsEnabled: Boolean,
    onPause: () -> Unit,
    onToggleTouchButtons: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onJump: () -> Unit,
    onSlide: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top HUD Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score + Distance + Speed Card
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = FrostedCardSurface,
                    border = BorderStroke(1.dp, IceCyan.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.label_score),
                                style = MaterialTheme.typography.labelMedium,
                                color = IceCyan
                            )
                            Text(
                                text = hud.score.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                color = SnowWhite,
                                modifier = Modifier.testTag("hud_score_text")
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.label_distance),
                                style = MaterialTheme.typography.labelMedium,
                                color = SnowWhite.copy(alpha = 0.72f)
                            )
                            Text(
                                text = "${hud.distanceMeters} m",
                                style = MaterialTheme.typography.labelLarge,
                                color = SnowWhite,
                                modifier = Modifier.testTag("hud_distance_text")
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.label_speed),
                                style = MaterialTheme.typography.labelMedium,
                                color = AuroraPurple
                            )
                            Text(
                                text = "${hud.speedKmh} km/h",
                                style = MaterialTheme.typography.labelLarge,
                                color = SnowWhite
                            )
                        }
                    }
                }

                // Coins + Touch Pad Toggle + Pause Button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FrostedCardSurface,
                        border = BorderStroke(1.dp, SolarCoinGold.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
                                text = hud.runCoins.toString(),
                                style = MaterialTheme.typography.labelLarge,
                                color = SnowWhite,
                                modifier = Modifier.testTag("hud_coins_text")
                            )
                        }
                    }

                    FilledTonalIconButton(
                        onClick = onToggleTouchButtons,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = FrostedCardSurface,
                            contentColor = if (touchButtonsEnabled) IceCyan else SnowWhite.copy(alpha = 0.75f)
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("hud_touchpad_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Gamepad,
                            contentDescription = stringResource(R.string.setting_touch_buttons)
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onPause,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = FrostedCardSurface,
                            contentColor = SnowWhite
                        ),
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Pause,
                            contentDescription = stringResource(R.string.btn_pause)
                        )
                    }
                }
            }

            // Active Power-Up Countdown Indicators
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.widthIn(max = 260.dp)
            ) {
                if (hud.magnetRemainingSec > 0f) {
                    PowerUpBanner(
                        title = stringResource(R.string.powerup_magnet),
                        remainingSec = hud.magnetRemainingSec,
                        totalSec = PowerUpType.COIN_MAGNET.durationSeconds,
                        color = CrimsonPeak,
                        icon = Icons.Filled.TrackChanges,
                        tag = "powerup_magnet_indicator"
                    )
                }
                if (hud.boostRemainingSec > 0f) {
                    PowerUpBanner(
                        title = stringResource(R.string.powerup_boost),
                        remainingSec = hud.boostRemainingSec,
                        totalSec = PowerUpType.SPEED_BOOST.durationSeconds,
                        color = IceCyan,
                        icon = Icons.Filled.Bolt,
                        tag = "powerup_boost_indicator"
                    )
                }
                if (hud.shieldRemainingSec > 0f) {
                    PowerUpBanner(
                        title = stringResource(R.string.powerup_shield),
                        remainingSec = hud.shieldRemainingSec,
                        totalSec = PowerUpType.SHIELD.durationSeconds,
                        color = EmeraldShield,
                        icon = Icons.Filled.Shield,
                        tag = "powerup_shield_indicator"
                    )
                }
            }
        }

        // Optional Large Ergonomic Touch Control Buttons at Bottom
        if (touchButtonsEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Left / Right lane buttons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlPadButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDesc = stringResource(R.string.control_left),
                        accent = IceCyan,
                        onClick = onMoveLeft,
                        tag = "control_left_button"
                    )
                    ControlPadButton(
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDesc = stringResource(R.string.control_right),
                        accent = IceCyan,
                        onClick = onMoveRight,
                        tag = "control_right_button"
                    )
                }

                // Jump / Slide action buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlPadButton(
                        icon = Icons.Filled.KeyboardArrowUp,
                        contentDesc = stringResource(R.string.control_jump),
                        accent = SolarCoinGold,
                        onClick = onJump,
                        tag = "control_jump_button"
                    )
                    ControlPadButton(
                        icon = Icons.Filled.KeyboardArrowDown,
                        contentDesc = stringResource(R.string.control_slide),
                        accent = AuroraPurple,
                        onClick = onSlide,
                        tag = "control_slide_button"
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerUpBanner(
    title: String,
    remainingSec: Float,
    totalSec: Float,
    color: Color,
    icon: ImageVector,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = FrostedCardSurface,
        border = BorderStroke(1.2.dp, color.copy(alpha = 0.8f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = SnowWhite
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.1fs", remainingSec),
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { (remainingSec / totalSec).coerceIn(0f, 1f) },
                color = color,
                trackColor = GlacierMidnight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun ControlPadButton(
    icon: ImageVector,
    contentDesc: String,
    accent: Color,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = FrostedCardSurface,
        border = BorderStroke(1.8.dp, accent.copy(alpha = 0.85f))
    ) {
        FilledIconButton(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = Color.Transparent,
                contentColor = SnowWhite
            ),
            modifier = Modifier
                .size(66.dp)
                .testTag(tag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}
