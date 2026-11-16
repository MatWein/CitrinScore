package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerSourceType

@Composable
actual fun isScanAvailable() =
    UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
