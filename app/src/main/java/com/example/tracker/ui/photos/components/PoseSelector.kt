package com.example.tracker.ui.photos.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.tracker.R
import com.example.tracker.domain.model.PhotoPose
import com.example.tracker.ui.photos.labelRes

/** Front / Side / Back. Poses in [done] show a check instead of the selection mark. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseSelector(
    selected: PhotoPose,
    onSelect: (PhotoPose) -> Unit,
    modifier: Modifier = Modifier,
    done: Set<PhotoPose> = emptySet(),
    enabled: Boolean = true,
) {
    val poses = PhotoPose.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        poses.forEachIndexed { index, pose ->
            val isDone = pose in done
            SegmentedButton(
                selected = pose == selected,
                onClick = { onSelect(pose) },
                shape = SegmentedButtonDefaults.itemShape(index, poses.size),
                enabled = enabled,
                icon = {
                    if (isDone) {
                        Icon(Icons.Outlined.Check, contentDescription = stringResource(R.string.capture_taken))
                    } else {
                        SegmentedButtonDefaults.Icon(active = pose == selected)
                    }
                },
            ) {
                Text(stringResource(pose.labelRes))
            }
        }
    }
}
