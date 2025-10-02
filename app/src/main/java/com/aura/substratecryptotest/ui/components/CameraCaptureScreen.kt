package com.aura.substratecryptotest.ui.components

import android.net.Uri
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.aura.substratecryptotest.data.camera.CameraManager
import com.aura.substratecryptotest.data.camera.PhotoMetadata
import com.aura.substratecryptotest.data.mountaineering.PhotoType
import com.aura.substratecryptotest.data.location.LocationData
import java.io.File

/**
 * Composable para la interfaz de cámara
 */
@Composable
fun CameraCaptureScreen(
    photoType: PhotoType,
    currentLocation: LocationData?,
    onPhotoCaptured: (PhotoMetadata) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraManager = remember { CameraManager(context) }
    
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Función para capturar foto
    fun capturePhoto() {
        if (isCapturing) return
        
        isCapturing = true
        errorMessage = null
        
        try {
            val outputDirectory = cameraManager.getOutputDirectory()
            cameraManager.capturePhoto(
                outputDirectory = outputDirectory,
                photoType = photoType.name,
                location = currentLocation,
                onImageCaptured = { metadata ->
                    isCapturing = false
                    onPhotoCaptured(metadata)
                },
                onError = { exception ->
                    isCapturing = false
                    errorMessage = "Error al capturar foto: ${exception.message}"
                }
            )
        } catch (e: Exception) {
            isCapturing = false
            errorMessage = "Error al capturar foto: ${e.message}"
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Cancelar")
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Capturar Foto",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Tipo: ${photoType.name}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Vista previa de la cámara
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { context ->
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { previewView ->
                    cameraManager.startCamera(
                        previewView = previewView,
                        lifecycleOwner = lifecycleOwner
                    )
                }
            )
            
            // Overlay con información
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getPhotoTypeIcon(photoType),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = photoType.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Botón de captura
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(32.dp)
            ) {
                FloatingActionButton(
                    onClick = { capturePhoto() },
                    modifier = Modifier.size(80.dp),
                    containerColor = if (isCapturing) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Capturar foto",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        
        // Mensaje de error
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
    
    // Cleanup al desmontar
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.cleanup()
        }
    }
}

/**
 * Obtiene el icono para el tipo de foto
 */
private fun getPhotoTypeIcon(photoType: PhotoType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (photoType) {
        PhotoType.RECORRIDO -> Icons.Default.Hiking
        PhotoType.CUMBRE -> Icons.Default.Landscape
        PhotoType.GENERAL -> Icons.Default.PhotoCamera
    }
}
