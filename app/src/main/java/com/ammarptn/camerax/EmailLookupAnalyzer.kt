package com.ammarptn.camerax

import android.content.Context
import android.graphics.*
import android.media.Image
import android.util.Log
import android.util.Patterns
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit


class EmailLookupAnalyzer(var context: Context, var callback: OnEmailLookupListener) :
    ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L


    interface OnEmailLookupListener {
        fun onFound(email: String)
    }

    override fun analyze(image: ImageProxy?, rotationDegrees: Int) {
        val currentTimestamp = System.currentTimeMillis()

        if (currentTimestamp - lastAnalyzedTimestamp >= TimeUnit.SECONDS.toMillis(2)
        ) {


            val bitmap = image!!.image!!.toBitmap()
            inspectBitmapForNumber(context, RotateBitmap(bitmap, rotationDegrees.toFloat()))

            lastAnalyzedTimestamp = currentTimestamp
        }
    }

    fun RotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun Image.toBitmap(): Bitmap {
        val yBuffer = planes[0].buffer // Y
        val uBuffer = planes[1].buffer // U
        val vBuffer = planes[2].buffer // V

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 70, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun inspectBitmapForNumber(context: Context, bitmap: Bitmap) {
        val textRecognizer = TextRecognizer.Builder(context).build()
        if (!textRecognizer.isOperational) {
            return
        }


        val frame = Frame.Builder().setBitmap(bitmap).build()
        val origTextBlocks = textRecognizer.detect(frame)
        Log.d("EmailLookup", "Analyzing")
        val textBlocks = ArrayList<TextBlock>()

        for (i in 0 until origTextBlocks.size()) {
            val textBlock = origTextBlocks[i]
            textBlocks.add(textBlock)

        }

        textBlocks.sortWith(Comparator { o1, o2 ->
            val diffOfTops = o1!!.boundingBox.top - o2!!.boundingBox.top
            val diffOfLefts = o1.boundingBox.left - o2.boundingBox.left
            if (diffOfTops != 0) {
                diffOfTops
            } else diffOfLefts
        })

        val detectText = StringBuilder()
        for (i in 0 until textBlocks.size) {
            detectText.append(textBlocks.get(i).value)
            detectText.append("\n")
        }

        val detectTextString = detectText.toString()
        if (detectTextString.isNotEmpty()) {
            Log.d("EmailLookup", "found $detectText")
            val emailMatcher = Patterns.EMAIL_ADDRESS.matcher(detectTextString)


            if (emailMatcher.find()) {
                val email = emailMatcher.group(0)
                callback.onFound(email)
                Log.d("EmailLookup", "email found :$email")
            } else {
                Log.d("EmailLookup", "email not found")

            }
        }


    }

}