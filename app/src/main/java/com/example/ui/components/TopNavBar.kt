package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EmeraldTertiary
import com.example.ui.theme.OfflineEngineColor
import com.example.ui.theme.OnlineGeminiColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopNavBar(
    isOnline: Boolean,
    forceOffline: Boolean,
    ttsEnabled: Boolean,
    onToggleTts: () -> Unit,
    onToggleForceOffline: () -> Unit,
    onRunAutomations: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "ActionFlow Logo",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "ActionFlow",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Engine badge
                            val effectiveOffline = forceOffline || !isOnline
                            val badgeColor = if (effectiveOffline) OfflineEngineColor else OnlineGeminiColor
                            val badgeText = if (forceOffline) "Offline Forced" else if (!isOnline) "Offline Mode" else "AI Online"
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onToggleForceOffline() }
                                    .testTag("engine_toggle_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(badgeColor)
                                    )
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Voice & Text Action Engine",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            },
            actions = {
                // Run scheduled automations button
                IconButton(
                    onClick = onRunAutomations,
                    modifier = Modifier.testTag("run_automations_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = "Trigger Scheduled Automations",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Text To Speech toggle button
                IconButton(
                    onClick = onToggleTts,
                    modifier = Modifier.testTag("tts_toggle_button")
                ) {
                    Icon(
                        imageVector = if (ttsEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        contentDescription = if (ttsEnabled) "Voice Speech Enabled" else "Voice Speech Muted",
                        tint = if (ttsEnabled) EmeraldTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Online/Offline status icon
                IconButton(
                    onClick = onToggleForceOffline,
                    modifier = Modifier.testTag("connection_status_button")
                ) {
                    Icon(
                        imageVector = if (forceOffline || !isOnline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                        contentDescription = if (forceOffline || !isOnline) "Offline Mode" else "Online Mode",
                        tint = if (forceOffline || !isOnline) OfflineEngineColor else OnlineGeminiColor
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}
