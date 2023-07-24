package com.mycam.mycamera

import android.Manifest
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.mycam.mycamera.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var defaultCamera = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private val REQUEST_CODE_PERMISSIONS = 123
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        previewView = binding.previewView
        setContentView(binding.root)

        requestCameraPermission()

        binding.btnCapture.setOnClickListener {
            takePicture()
            animateFlash()
        }
        binding.btnSwitch.setOnClickListener {
            switch()
        }
        binding.btnGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray) {
        if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestCameraPermission()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initPreview() {
        preview = Preview.Builder().build()
        preview?.setSurfaceProvider(previewView.surfaceProvider)
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            startCamera()
        }
    }

    private fun startCamera(){
        initPreview()
        imageCapture = ImageCapture.Builder().build()
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(defaultCamera)
            .build()
        lifecycleScope.launch {
            val cameraProvider = ProcessCameraProvider
                .getInstance(this@MainActivity)
                .await()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@MainActivity,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception){
                Log.e(TAG, "Error: binding usecases $e")
            }
        }
    }

    private fun takePicture() {
        val fileName = "JPEG_${System.currentTimeMillis()}.jpeg"
        val file = File(externalMediaDirs[0],fileName)
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Log.i(TAG,"The image has been saved in ${file.toUri()}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG,"Image capture failed ${exception.message}")
                }
            }
        )
    }

    private fun switch(){
        defaultCamera = if (defaultCamera == CameraSelector.LENS_FACING_BACK){
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera()
    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }
}