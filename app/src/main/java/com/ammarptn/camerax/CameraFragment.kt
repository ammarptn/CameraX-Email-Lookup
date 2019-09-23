package com.ammarptn.camerax

import android.content.Context
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.view.*
import androidx.camera.core.*
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import kotlinx.android.synthetic.main.fragment_camera.view.*

import java.io.File


class CameraFragment : Fragment() {

    private var listener: OnFragmentInteractionListener? = null
    private var rootView: View? = null
    private var cameraViewFinder: TextureView? = null


    companion object {

        @JvmStatic
        fun newInstance() =
            CameraFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_camera, container, false)
        cameraViewFinder = rootView!!.findViewById(R.id.viewFinder)

        Dexter.withActivity(activity).withPermission(android.Manifest.permission.CAMERA)
            .withListener(object : PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    cameraViewFinder?.apply {

                        post { startCamera() }
                    }

                }

                override fun onPermissionRationaleShouldBeShown(
                    permission: PermissionRequest?,
                    token: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {

                }
            }).check()

        return rootView
    }

    fun startCamera() {

        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(9, 18))
        }
            .build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {
            val viewFinderParent = cameraViewFinder!!.parent as ViewGroup
            viewFinderParent.removeView(cameraViewFinder!!)
            viewFinderParent.addView(cameraViewFinder!!, 0)

            cameraViewFinder!!.surfaceTexture = it.surfaceTexture
            updateTransform()

        }

        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
            setCallbackHandler(Handler(analyzerThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()

        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            analyzer =
                EmailLookupAnalyzer(context!!, object : EmailLookupAnalyzer.OnEmailLookupListener {
                    override fun onFound(email: String) {
                        rootView!!.textPreview.text = email
                    }

                })
        }

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setTargetAspectRatio(Rational(9, 18))
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
        }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)

        rootView!!.capture.setOnClickListener {
            val tempFile = File(context!!.filesDir, System.currentTimeMillis().toString() + ".jpg")

            imageCapture.takePicture(tempFile, object : ImageCapture.OnImageSavedListener {
                override fun onImageSaved(file: File) {

                    Snackbar.make(rootView!!, "Image Saved", Snackbar.LENGTH_SHORT).show()
                }

                override fun onError(
                    useCaseError: ImageCapture.UseCaseError,
                    message: String,
                    cause: Throwable?
                ) {
                    Snackbar.make(rootView!!, "Unable to capture", Snackbar.LENGTH_SHORT).show()
                }

            })
        }

        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)
    }

    fun updateTransform() {
        val matrix = Matrix()


        val centerX = cameraViewFinder!!.width / 2f
        val centerY = cameraViewFinder!!.height / 2f


        val rotationDegree = when (cameraViewFinder!!.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }

        matrix.postRotate(-rotationDegree.toFloat(), centerX, centerY)
        cameraViewFinder!!.setTransform(matrix)
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            listener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }


}
