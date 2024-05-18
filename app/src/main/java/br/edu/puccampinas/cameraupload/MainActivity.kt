package br.edu.puccampinas.cameraupload

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.edu.puccampinas.cameraupload.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var btnOpenCamera: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onStart() {
        super.onStart()
        initializeOpenCameraButton()
    }

    private fun initializeOpenCameraButton(){
        btnOpenCamera = binding.btnOpenCamera
        btnOpenCamera.setOnClickListener {
            cameraAndStorageProviderResult.launch(
                arrayOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }

    private fun openCameraPreview(){
        val intent = Intent(this, CameraPreviewActivity::class.java)
        startActivity(intent)
    }

    private val cameraAndStorageProviderResult = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.all { it.value }) {
            openCameraPreview()
        } else {
            Snackbar.make(binding.root, "Permission denied", Snackbar.LENGTH_SHORT).show()
        }
    }
}