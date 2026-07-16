package com.metrolist.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.desktop.player.EqualizerBand
import com.metrolist.desktop.player.EqualizerConfiguration
import com.metrolist.desktop.player.EqualizerPreset
import com.metrolist.desktop.preferences.AppSettings
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    settings: AppSettings,
    onBack: () -> Unit,
) {
    val enabled by settings.equalizerEnabled.collectAsState()
    val storedPreset by settings.equalizerPreset.collectAsState()
    val gains by settings.equalizerGainsDb.collectAsState()
    val preset = EqualizerPreset.fromStored(storedPreset)

    Column(
        Modifier.fillMaxSize().padding(horizontal = 34.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back to settings") }
            Column {
                Text("Equalizer", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold))
                Text(
                    "Tune every frequency while the music keeps playing.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.widthIn(max = 980.dp).fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Default.Equalizer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column {
                            Text("10-band audio equalizer", style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (enabled) "Applied in realtime" else "Original sound is unchanged",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Switch(checked = enabled, onCheckedChange = settings::setEqualizerEnabled)
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    EqualizerPreset.entries.filter { it != EqualizerPreset.CUSTOM }.forEach { candidate ->
                        FilterChip(
                            selected = preset == candidate,
                            onClick = {
                                settings.setEqualizer(candidate.name, EqualizerBand.entries.map(candidate.gainsDb::getValue))
                            },
                            label = { Text(candidate.displayName) },
                        )
                    }
                    IconButton(
                        onClick = {
                            settings.setEqualizer(
                                EqualizerPreset.FLAT.name,
                                EqualizerBand.entries.map(EqualizerPreset.FLAT.gainsDb::getValue),
                            )
                        },
                    ) {
                        Icon(Icons.Default.RestartAlt, "Reset equalizer")
                    }
                }

                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    EqualizerBand.entries.forEachIndexed { index, band ->
                        EqualizerBandSlider(
                            band = band,
                            gainDb = gains.getOrElse(index) { 0f },
                            enabled = enabled,
                            onGainChanged = { gain ->
                                val updated = gains.toMutableList().apply {
                                    while (size < EqualizerBand.entries.size) add(0f)
                                    this[index] = gain.coerceIn(
                                        EqualizerConfiguration.MIN_GAIN_DB,
                                        EqualizerConfiguration.MAX_GAIN_DB,
                                    )
                                }
                                settings.setEqualizer(EqualizerPreset.CUSTOM.name, updated)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EqualizerBandSlider(
    band: EqualizerBand,
    gainDb: Float,
    enabled: Boolean,
    onGainChanged: (Float) -> Unit,
) {
    Column(
        modifier = Modifier.width(66.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            "${if (gainDb >= 0f) "+" else ""}${gainDb.roundToInt()} dB",
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(Modifier.width(54.dp).height(230.dp), contentAlignment = Alignment.Center) {
            Slider(
                value = gainDb,
                onValueChange = onGainChanged,
                valueRange = EqualizerConfiguration.MIN_GAIN_DB..EqualizerConfiguration.MAX_GAIN_DB,
                steps = 23,
                enabled = enabled,
                modifier = Modifier.width(220.dp).graphicsLayer(rotationZ = -90f),
            )
        }
        Text(band.label, style = MaterialTheme.typography.labelLarge)
        Text("Hz", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
