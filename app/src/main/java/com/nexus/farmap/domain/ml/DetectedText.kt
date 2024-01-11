package com.nexus.farmap.domain.ml

import com.nexus.farmap.data.model.DetectedObjectResult
import com.google.ar.core.Frame

data class DetectedText(
    val detectedObjectResult: DetectedObjectResult, val frame: Frame
)
