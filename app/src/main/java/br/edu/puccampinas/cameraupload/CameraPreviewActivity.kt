package br.edu.puccampinas.cameraupload

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import br.edu.puccampinas.cameraupload.databinding.ActivityCameraPreviewBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCameraPreviewBinding
    private lateinit var btnTakePhoto: FloatingActionButton
    private val storage by lazy {
        FirebaseStorage.getInstance()
    }

    // controla o ciclo de vida da câmera
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    // seleciona a câmera traseira
    private lateinit var cameraSelector: CameraSelector

    // captura a imagem
    private lateinit var imageCapture: ImageCapture

    // executor para captura de imagem em segundo plano (thread)
    private lateinit var imageCaptureExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeCameraProvider()
        initializeTakePhotoButton()
    }

    private fun initializeCameraProvider() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imageCaptureExecutor = Executors.newSingleThreadExecutor()
    }

    private fun initializeTakePhotoButton() {
        btnTakePhoto = binding.btnTakePhoto
        btnTakePhoto.isEnabled = false
        btnTakePhoto.setOnClickListener {
            takePhoto()
            blinkPreview()
        }
    }

    override fun onStart() {
        super.onStart()
        startCamera()
    }

    private fun startCamera() {
        cameraProviderFuture.addListener({
            try {
                initializeCamera()
            } catch (e: Exception) {
                Log.e("CameraPreviewActivity", "Error starting camera", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun initializeCamera() {
        imageCapture = ImageCapture.Builder().build()
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
        }
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        btnTakePhoto.isEnabled = true
    }

    private fun takePhoto() {
        imageCapture.let { imageCapture ->
            val photoFile = createImageFile()
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {

                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.d(
                            "CameraPreviewActivity",
                            "Photo capture succeeded: ${photoFile.absolutePath}"
                        )
                        uploadImageToFirebase(photoFile)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("CameraPreviewActivity", "Photo capture failed", exception)
                    }
                })
        }
    }

    private fun createImageFile(): File {
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        return File(externalMediaDirs.first(), fileName)
    }

    private fun blinkPreview() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun  uploadImageToFirebase(imageFile: File) {
        val storageRef = storage.getReference("images")
        val imageRef = storageRef.child(imageFile.name)
        val uploadTask = imageRef.putFile(imageFile.toUri())
        uploadTask.addOnSuccessListener {
            Log.d("CameraPreviewActivity", "Image uploaded successfully")
        }.addOnFailureListener {
            Log.e("CameraPreviewActivity", "Image upload failed", it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        imageCaptureExecutor.shutdown()
    }


    override fun onStop() {
        super.onStop()
        cameraProviderFuture.get().unbindAll()
    }
}
