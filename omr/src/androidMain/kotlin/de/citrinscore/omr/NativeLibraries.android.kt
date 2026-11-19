package de.citrinscore.omr

import org.opencv.android.OpenCVLoader

internal actual fun loadOpenCv() = check(OpenCVLoader.initLocal()) { "OpenCV could not be loaded" }
