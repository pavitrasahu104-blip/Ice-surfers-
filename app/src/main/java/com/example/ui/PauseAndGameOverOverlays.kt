package com.example.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.HudState
import com.example.ui.theme.CrimsonPeak
import com.example.ui.theme.FrostedSlate
import com.example.ui.theme.GlacierMidnight
import com.example.ui.theme.IceCyan
import com.example.ui.theme.SnowWhite
import com.example.ui.theme.SolarCoinGold

@Composable
fun PauseOverlay(
    hud: HudState,
    soundEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xCC06111F))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = FrostedSlate,
            border = BorderStroke(1.5.dp, IceCyan.copy(alpha = 0.65f)),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .testTag("pause_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_paused),
                    style = MaterialTheme.typography.headlineLarge,
                    color = IceCyan
                )
                Text(
                    text = stringResource(R.string.label_paused_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SnowWhite.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SummaryMetric(
                        label = stringResource(R.string.label_score),
                        value = hud.score.toString(),
                        accent = SnowWhite
                    )
                    SummaryMetric(
                        label = stringResource(R.string.label_distance),
                        value = "${hud.distanceMeters} m",
                        accent = IceCyan
                    )
                    SummaryMetric(
                        label = stringResource(R.string.label_coins),
                        value = "+${hud.runCoins}",
                        accent = SolarCoinGold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyan,
                        contentColor = GlacierMidnight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("resume_button")
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_resume),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedButton(
                    onClick = onRestart,
                    border = BorderStroke(1.2.dp, SnowWhite.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SnowWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pause_restart_button")
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_restart),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onToggleSound,
                        border = BorderStroke(1.2.dp, IceCyan.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SnowWhite),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("pause_sound_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) {
                                Icons.AutoMirrored.Filled.VolumeUp
                            } else {
                                Icons.AutoMirrored.Filled.VolumeOff
                            },
                            contentDescription = null,
                            tint = IceCyan
                        )
                    }

                    OutlinedButton(
                        onClick = onMainMenu,
                        border = BorderStroke(1.2.dp, SnowWhite.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SnowWhite),
                        modifier = Modifier
                            .weight(2.2f)
                            .height(50.dp)
                            .testTag("pause_menu_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_main_menu),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    summary: FrostRushViewModel.RunSummary,
    bestScore: Int,
    totalCoins: Int,
    onRestart: () -> Unit,
    onMainMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD406111F))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = FrostedSlate,
            border = BorderStroke(1.8.dp, CrimsonPeak.copy(alpha = 0.8f)),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .testTag("game_over_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_game_over),
                    style = MaterialTheme.typography.displayMedium,
                    color = CrimsonPeak,
                    modifier = Modifier.testTag("game_over_title")
                )

                if (summary.isNewBest) {
                    Surface(
                        shape = CircleShape,
                        color = SolarCoinGold.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, SolarCoinGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = SolarCoinGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.label_new_best),
                                style = MaterialTheme.typography.labelLarge,
                                color = SolarCoinGold
                            )
                        }
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = GlacierMidnight.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, SnowWhite.copy(alpha = 0.16f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SummaryMetric(
                                label = stringResource(R.string.label_score),
                                value = summary.finalScore.toString(),
                                accent = SnowWhite,
                                valueTag = "gameover_score_text"
                            )
                            SummaryMetric(
                                label = stringResource(R.string.label_best_score),
                                value = maxOf(bestScore, summary.finalScore).toString(),
                                accent = IceCyan,
                                valueTag = "gameover_best_text"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            SummaryMetric(
                                label = stringResource(R.string.label_distance),
                                value = "${summary.distanceMeters} m",
                                accent = IceCyan,
                                valueTag = "gameover_distance_text"
                            )
                            SummaryMetric(
                                label = stringResource(R.string.label_coins_collected),
                                value = "+${summary.coinsCollected} ($totalCoins)",
                                accent = SolarCoinGold,
                                valueTag = "gameover_coins_text"
                            )
                        }
                    }
                }

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyan,
                        contentColor = GlacierMidnight
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("restart_button")
                ) {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_restart),
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                OutlinedButton(
                    onClick = onMainMenu,
                    border = BorderStroke(1.5.dp, SnowWhite.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SnowWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("gameover_menu_button")
                ) {
                    Icon(imageVector = Icons.Filled.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_main_menu),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: String,
    accent: Color,
    valueTag: String? = null
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = SnowWhite.copy(alpha = 0.72f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
            modifier = if (valueTag != null) Modifier.testTag(valueTag) else Modifier
        )
    }
}
