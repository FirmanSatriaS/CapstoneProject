
package com.example.culinaryndo.ui.scan

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.OrientationEventListener
import android.view.Surface
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.example.culinaryndo.R
import com.example.culinaryndo.ViewModelFactory
import com.example.culinaryndo.databinding.ActivityScanBinding
import com.example.culinaryndo.ui.profile.ProfileViewModel
import com.example.culinaryndo.utils.createCustomTempFile

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var imageCapture: ImageCapture? = null
    private var currentImageUri: Uri? = null
    private val viewModel by viewModels<ScanViewModel>{
        ViewModelFactory.getInstance(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        startCamera()

        binding.btnFlash.setOnClickListener{
            if (viewModel.flashMode.value == ImageCapture.FLASH_MODE_ON){
                viewModel.flashMode.value = ImageCapture.FLASH_MODE_OFF
                startCamera()
            }else{
                viewModel.flashMode.value = ImageCapture.FLASH_MODE_ON
                startCamera()
            }
        }

        viewModel.flashMode.observe(this){ value ->
            when(value){
                ImageCapture.FLASH_MODE_ON -> {
                    binding.btnFlash.setImageResource(R.drawable.flash)
                }
                ImageCapture.FLASH_MODE_OFF -> {
                    binding.btnFlash.setImageResource(R.drawable.flash_off)
                }
            }
        }

        binding.btnCaptureImage.setOnClickListener{ takePhoto() }

        binding.btnChoseImage.setOnClickListener{ choseFromGalery() }
    }

    private fun choseFromGalery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            currentImageUri = uri
            val intent = Intent()
            intent.putExtra(EXTRA_CAMERAX_IMAGE,currentImageUri.toString())
            setResult(CAMERAX_RESULT,intent)
            finish()
        } else {
            Log.d("Photo Picker", "No media selected")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> finish()
        }
        return true
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(viewModel.flashMode.value!!)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(
                    this@ScanActivity,
                    "Gagal memunculkan kamera.",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "startCamera: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = createCustomTempFile(application)

        val outputOption = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@ScanActivity,
                        "Gagal Mengambil Gambar",Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val intent = Intent()
                    intent.putExtra(EXTRA_CAMERAX_IMAGE,outputFileResults.savedUri.toString())
                    setResult(CAMERAX_RESULT,intent)
                    finish()
                }
            }
        )
    }

    private val orientationEventListener by lazy {
        object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    return
                }
                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rotation
            }
        }
    }
    override fun onStart() {
        super.onStart()
        orientationEventListener.enable()
    }
    override fun onStop() {
        super.onStop()
        orientationEventListener.disable()
    }

    companion object {
        private const val TAG = "ScanActivity"
        const val EXTRA_CAMERAX_IMAGE = "CameraX Image"
        const val CAMERAX_RESULT = 200
    }
}