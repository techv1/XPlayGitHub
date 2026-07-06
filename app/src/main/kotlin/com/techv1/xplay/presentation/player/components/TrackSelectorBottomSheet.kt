package com.techv1.xplay.presentation.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import com.techv1.xplay.ui.theme.BackgroundSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSelectorBottomSheet(
    player: Player?,
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible || player == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BackgroundSurface
    ) {
        TrackSelectorContent(player = player)
    }
}

@Composable
private fun TrackSelectorContent(player: Player) {
    val groups = player.currentTracks.groups

    LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
        groups.forEachIndexed { groupIndex, group ->
            val typeName = when (group.type) {
                C.TRACK_TYPE_VIDEO -> "Video"
                C.TRACK_TYPE_AUDIO -> "Audio"
                C.TRACK_TYPE_TEXT -> "Subtitles"
                else -> "Other"
            }

            item(key = "header_$groupIndex") {
                Text(
                    text = typeName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            itemsIndexed(
                items = (0 until group.length).toList(),
                key = { trackIndex, _ -> "track_${groupIndex}_$trackIndex" }
            ) { trackIndex, _ ->
                if (!group.isTrackSupported(trackIndex)) return@itemsIndexed

                val format = group.getTrackFormat(trackIndex)
                val label = when (group.type) {
                    C.TRACK_TYPE_VIDEO -> videoTrackLabel(format)
                    C.TRACK_TYPE_AUDIO -> format.label ?: format.language?.uppercase() ?: "Audio ${trackIndex + 1}"
                    C.TRACK_TYPE_TEXT -> format.label ?: format.language?.uppercase() ?: "Subtitle ${trackIndex + 1}"
                    else -> "Track ${trackIndex + 1}"
                }
                val isSelected = group.isTrackSelected(trackIndex)

                TrackOptionRow(
                    label = label,
                    isSelected = isSelected,
                    onClick = {
                        val trackGroup = group.mediaTrackGroup
                        player.trackSelectionParameters = player.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(TrackSelectionOverride(trackGroup, listOf(trackIndex)))
                            .build()
                    }
                )
            }
        }
    }
}

private fun videoTrackLabel(format: androidx.media3.common.Format): String {
    val height = format.height
    val quality = when {
        height >= 2160 -> "4K"
        height >= 1440 -> "1440p"
        height >= 1080 -> "1080p"
        height >= 720 -> "720p"
        height >= 480 -> "480p"
        height > 0 -> "${height}p"
        else -> "Auto"
    }
    val codecs = format.codecs?.substringBefore(".")?.uppercase()
    return if (codecs != null) "$quality ($codecs)" else quality
}

@Composable
private fun TrackOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = isSelected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
