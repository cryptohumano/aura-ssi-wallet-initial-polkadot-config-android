package com.aura.substratecryptotest.data.camera

import android.content.Context
import android.net.Uri
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.location.Location
import com.aura.substratecryptotest.data.location.LocationData

/**
 * Metadata de una foto capturada
 */
data class PhotoMetadata(
    val uri: Uri,
    val filePath: String,
    val timestamp: Date,
    val location: LocationData?,
    val photoType: String,
    val width: Int,
    val height: Int,
    val fileSize: Long
)

/**
 * Manager simple para cámara sin Dagger Hilt
 */
class CameraManager(private val context: Context) {
    
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var isInitialized = false
    
    /**
     * Inicia la cámara en el PreviewView
     */
    fun startCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner
    ) {
        if (isInitialized) {
            android.util.Log.d("CameraManager", "Camera already initialized, skipping")
            return
        }
        
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Preview con configuración moderna
                val preview = Preview.Builder()
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // ImageCapture con configuración mejorada
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                    .build()
                
                // Seleccionar cámara trasera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                
                isInitialized = true
                android.util.Log.d("CameraManager", "Camera initialized successfully")
                
            } catch (exc: Exception) {
                android.util.Log.e("CameraManager", "Error starting camera: ${exc.message}")
                isInitialized = false
            }
            
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Captura una foto con metadata
     */
    fun capturePhoto(
        outputDirectory: File,
        photoType: String,
        location: LocationData?,
        onImageCaptured: (PhotoMetadata) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val imageCapture = imageCapture ?: run {
            onError(Exception("ImageCapture no está inicializado"))
            return
        }
        
        if (!isInitialized) {
            onError(Exception("Cámara no está inicializada"))
            return
        }
        
        android.util.Log.d("CameraManager", "Iniciando captura de foto...")
        
        // Crear archivo de salida
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
                .format(System.currentTimeMillis()) + ".jpg"
        )
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    try {
                        android.util.Log.d("CameraManager", "Foto guardada exitosamente: ${photoFile.absolutePath}")
                        val savedUri = Uri.fromFile(photoFile)
                        
                        // Leer metadata de la imagen
                        val metadata = readPhotoMetadata(
                            uri = savedUri,
                            filePath = photoFile.absolutePath,
                            photoType = photoType,
                            location = location
                        )
                        
                        onImageCaptured(metadata)
                    } catch (e: Exception) {
                        android.util.Log.e("CameraManager", "Error procesando metadata: ${e.message}")
                        onError(e)
                    }
                }
                
                override fun onError(exception: ImageCaptureException) {
                    android.util.Log.e("CameraManager", "Error capturando foto: ${exception.message}")
                    onError(exception)
                }
            }
        )
    }
    
    /**
     * Lee la metadata de una foto
     */
    private fun readPhotoMetadata(
        uri: Uri,
        filePath: String,
        photoType: String,
        location: LocationData?
    ): PhotoMetadata {
        val file = File(filePath)
        val timestamp = Date(file.lastModified())
        
        // Leer dimensiones de la imagen
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, options)
        
        val width = options.outWidth
        val height = options.outHeight
        val fileSize = file.length()
        
        return PhotoMetadata(
            uri = uri,
            filePath = filePath,
            timestamp = timestamp,
            location = location,
            photoType = photoType,
            width = width,
            height = height,
            fileSize = fileSize
        )
    }
    
    /**
     * Obtiene la ruta del directorio de fotos
     */
    fun getOutputDirectory(): File {
        val mediaDir = File(context.getExternalFilesDir(null), "AuraMountaineering").apply { mkdirs() }
        return if (mediaDir.exists()) mediaDir else context.filesDir
    }
    
    /**
     * Detiene la cámara correctamente
     */
    fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
            camera = null
            imageCapture = null
            isInitialized = false
            android.util.Log.d("CameraManager", "Cámara detenida correctamente")
        } catch (e: Exception) {
            android.util.Log.e("CameraManager", "Error deteniendo cámara: ${e.message}")
        }
    }
    
    /**
     * Limpia recursos
     */
    fun cleanup() {
        stopCamera()
        cameraExecutor.shutdown()
    }
}
