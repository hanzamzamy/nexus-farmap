package com.nexus.farmap.data.model

import dev.romainguy.kotlin.math.Float2

data class DetectedObjectResult(
    val label: String,
    val centerCoordinate: Float2,
)