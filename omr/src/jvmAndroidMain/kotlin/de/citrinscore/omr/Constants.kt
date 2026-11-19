package de.citrinscore.omr

internal const val LINES_ON_A_STAFF = 5
internal const val STAFF_LINE_SEGMENT_X_TOLERANCE = 10
internal const val STAFF_POSITION_TOLERANCE = 50
internal const val IMAGE_NOISE_LIMIT = 50
internal const val MAX_ANGLE_FOR_PARALLEL_LINES = 10

internal fun toleranceForStaffLineDetection(unitSize: Double) = unitSize / 3

internal fun maxLineGapSize(unitSize: Double) = 5 * unitSize

internal fun isShortLine(unitSize: Double) = unitSize / 5

internal fun isShortConnectedLine(unitSize: Double) = 2 * unitSize

internal fun barLineMaxWidth(unitSize: Double) = 2 * unitSize

internal fun barLineMinHeight(unitSize: Double) = 3 * unitSize

internal fun blackSpotRemovalThreshold(unitSize: Double) = 2 * unitSize
