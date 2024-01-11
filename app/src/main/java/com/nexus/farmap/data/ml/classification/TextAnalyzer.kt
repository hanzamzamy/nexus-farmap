package com.nexus.farmap.data.ml.classification

import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import com.nexus.farmap.data.ml.classification.utils.ImageUtils
import com.nexus.farmap.data.model.DetectedObjectResult
import com.nexus.farmap.domain.ml.ObjectDetector
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dev.romainguy.kotlin.math.Float2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Analyzes the frames passed in from the camera and returns any detected text within the requested
 * crop region.
 */
class TextAnalyzer : ObjectDetector {

    private val options = TextRecognizerOptions.Builder().build()

    private val detector = TextRecognition.getClient(options)

    override suspend fun analyze(
        mediaImage: Image, rotationDegrees: Int, imageCropPercentages: Pair<Int, Int>, displaySize: Pair<Int, Int>
    ): Result<DetectedObjectResult> {

        var text: Text?
        var cropRect: Rect?
        var croppedBit: Bitmap?

        withContext(Dispatchers.Default) {

            // Calculate the actual ratio from the frame to appropriately
            // crop the image.
            val imageHeight = mediaImage.height
            val imageWidth = mediaImage.width

            val actualAspectRatio = imageWidth / imageHeight

            val convertImageToBitmap = ImageUtils.convertYuv420888ImageToBitmap(mediaImage)
            cropRect = Rect(0, 0, imageWidth, imageHeight)

            // In case image has wider aspect ratio, then crop less the height.
            // In case image has taller aspect ratio, just handle it as is.
            var currentCropPercentages = imageCropPercentages
            if (actualAspectRatio > 3) {
                val originalHeightCropPercentage = currentCropPercentages.first
                val originalWidthCropPercentage = currentCropPercentages.second
                currentCropPercentages = Pair(originalHeightCropPercentage / 2, originalWidthCropPercentage)
            }

            // Image rotation compensation. Swapping height and width on landscape orientation.
            val cropPercentages = currentCropPercentages
            val heightCropPercent = cropPercentages.first
            val widthCropPercent = cropPercentages.second
            val (widthCrop, heightCrop) = when (rotationDegrees) {
                90, 270 -> Pair(heightCropPercent / 100f, widthCropPercent / 100f)
                else -> Pair(widthCropPercent / 100f, heightCropPercent / 100f)
            }

            cropRect!!.inset(
                (imageWidth * widthCrop / 2).toInt(), (imageHeight * heightCrop / 2).toInt()
            )

            val croppedBitmap = ImageUtils.rotateAndCrop(convertImageToBitmap, rotationDegrees, cropRect!!)
            croppedBit = croppedBitmap

            text = detector.process(InputImage.fromBitmap(croppedBitmap, 0)).await()
        }
        return if (text != null) {
            if (text!!.textBlocks.isNotEmpty()) {
                val textBlock = text!!.textBlocks.firstOrNull { textBlock ->
                    usingFormat(textBlock)
                }
                if (textBlock != null) {
                    val boundingBox = textBlock.boundingBox
                    if (boundingBox != null) {
                        val croppedRatio = Float2(
                            boundingBox.centerX() / croppedBit!!.width.toFloat(),
                            boundingBox.centerY() / croppedBit!!.height.toFloat()
                        )

                        val x = displaySize.first * croppedRatio.x
                        val y = displaySize.second * croppedRatio.y

                        Result.success(
                            DetectedObjectResult(
                                label = textBlock.text, centerCoordinate = Float2(x, y)
                            )
                        )
                    } else {
                        Result.failure(Exception("Cant detect bounding box"))
                    }
                } else {
                    Result.failure(Exception("No digits found"))
                }

            } else {
                Result.failure(Exception("No detected objects"))
            }
        } else {
            Result.failure(Exception("Null text"))
        }

    }

    private fun usingFormat(text: Text.TextBlock): Boolean {
        return text.text[0].isLetterOrDigit()
    }

}