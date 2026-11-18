package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioOutputRoute
import com.example.data.TrackEntity
import com.example.ui.theme.AlabasterWhite
import com.example.ui.theme.AmberWaveform
import com.example.ui.theme.CoralPinAccent
import com.example.ui.theme.GlassBorderSpecular
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.SmokedGlassElevated
import com.example.ui.theme.WarmSandMuted

@Composable
fun PinOptionsPopover(
    track: TrackEntity?,
    onToggleCanvasPin: (TrackEntity) -> Unit,
    onPinShortcutToHome: (TrackEntity) -> Unit,
    onPinWidgetToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SmokedGlassElevated)
            .border(1.dp, GlassBorderSpecular, shape)
            .padding(12.dp)
            .testTag("pin_options_popover")
    ) {
        Text(
            text = "PIN \"${track.title.uppercase()}\"",
            style = MaterialTheme.typography.labelSmall,
            color = AmberWaveform,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
        )

        PopoverActionRow(
            icon = Icons.Default.PushPin,
            iconTint = CoralPinAccent,
            title = if (track.isPinnedToCanvas) {
                stringResource(R.string.unpin_from_canvas)
            } else {
                stringResource(R.string.pin_to_canvas)
            },
            subtitle = "Draggable quick-play badge on backdrop",
            testTag = "pin_canvas_action",
            onClick = { onToggleCanvasPin(track) }
        )

        HorizontalDivider(color = GlassBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

        PopoverActionRow(
            icon = Icons.Default.Home,
            iconTint = AmberWaveform,
            title = stringResource(R.string.pin_to_home_screen),
            subtitle = "One-tap track icon on Android Launcher",
            testTag = "pin_home_shortcut_action",
            onClick = { onPinShortcutToHome(track) }
        )

        HorizontalDivider(color = GlassBorderSubtle, modifier = Modifier.padding(vertical = 4.dp))

        PopoverActionRow(
            icon = Icons.Default.Widgets,
            iconTint = AlabasterWhite,
            title = stringResource(R.string.pin_widget_to_home),
            subtitle = "Add resizable Aura Capsule widget",
            testTag = "pin_home_widget_action",
            onClick = onPinWidgetToHome
        )
    }
}

@Composable
fun AudioRouteAndScalePopover(
    routes: List<AudioOutputRoute>,
    selectedRoute: AudioOutputRoute,
    currentScale: Float,
    onSelectRoute: (AudioOutputRoute) -> Unit,
    onSelectScalePreset: (Float) -> Unit,
    onResetCapsulePosition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SmokedGlassElevated)
            .border(1.dp, GlassBorderSpecular, shape)
            .padding(12.dp)
            .testTag("audio_route_popover")
    ) {
        Text(
            text = "AUDIO OUTPUT & CAPSULE SIZE",
            style = MaterialTheme.typography.labelSmall,
            color = AmberWaveform,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )

        routes.forEach { route ->
            val isSelected = route.label == selectedRoute.label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectRoute(route) }
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (route.isBluetooth) Icons.Default.BluetoothAudio else Icons.Default.Speaker,
                    contentDescription = route.label,
                    tint = if (isSelected) AmberWaveform else WarmSandMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = route.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (isSelected) AlabasterWhite else WarmSandMuted,
                    modifier = Modifier.weight(1f)
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = AmberWaveform,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = GlassBorderSubtle, modifier = Modifier.padding(vertical = 6.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = "Scale",
                style = MaterialTheme.typography.labelMedium,
                color = WarmSandMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0.88f to "88%", 1.0f to "100%", 1.12f to "112%").forEach { (scaleVal, label) ->
                    val active = kotlin.math.abs(currentScale - scaleVal) < 0.04f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) AmberWaveform.copy(alpha = 0.25f) else Color.Transparent)
                            .border(
                                width = 1.dp,
                                color = if (active) AmberWaveform else GlassBorderSubtle,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectScalePreset(scaleVal) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (active) AmberWaveform else AlabasterWhite
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onResetCapsulePosition() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("reset_capsule_position_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CenterFocusStrong,
                    contentDescription = "Center capsule",
                    tint = WarmSandMuted,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Center",
                    style = MaterialTheme.typography.labelSmall,
                    color = WarmSandMuted
                )
            }
        }
    }
}

@Composable
private fun PopoverActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AlabasterWhite
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = WarmSandMuted
            )
        }
    }
}
