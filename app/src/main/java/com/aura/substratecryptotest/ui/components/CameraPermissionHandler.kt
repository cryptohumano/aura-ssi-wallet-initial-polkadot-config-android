package com.aura.substratecryptotest.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

/**
 * Composable para manejar permisos de cámara
 */
@Composable
fun CameraPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    
    // Determinar qué permisos necesitamos según la versión de Android
    val permissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            listOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }
    
    // Verificar si ya tenemos todos los permisos
    val hasAllPermissions = remember {
        permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // Launcher para solicitar permisos múltiples
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }
    
    // Función para solicitar permisos
    fun requestCameraPermissions() {
        permissionLauncher.launch(permissions.toTypedArray())
    }
    
    content(hasAllPermissions, ::requestCameraPermissions)
}

/**
 * Card que muestra información sobre permisos de cámara
 */
@Composable
fun CameraPermissionCard(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPermission) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = if (hasPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasPermission) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = if (hasPermission) "Permisos de cámara concedidos" else "Permisos de cámara requeridos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasPermission) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onErrorContainer
                )
            }
            
            Text(
                text = if (hasPermission) 
                    "La aplicación puede acceder a la cámara y almacenamiento para capturar y guardar fotos de los milestones."
                else 
                    "Para capturar y guardar fotos en los milestones, necesitamos permisos de cámara y almacenamiento.",
                fontSize = 14.sp,
                color = if (hasPermission) 
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                else 
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            if (!hasPermission) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Conceder Permisos de Cámara y Almacenamiento")
                }
            }
        }
    }
}
