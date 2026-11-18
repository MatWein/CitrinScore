package de.citrinscore.ui.viewer.files

import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AttachmentName(name: String) = Text(name, maxLines = 1, modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE))
