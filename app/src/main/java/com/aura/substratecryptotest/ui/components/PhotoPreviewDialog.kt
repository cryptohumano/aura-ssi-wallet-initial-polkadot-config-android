package com.aura.substratecryptotest.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.substratecryptotest.data.camera.PhotoMetadata
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PhotoPreviewDialog(
    photoMetadata: PhotoMetadata?,
    onDismiss: () -> Unit
) {
    if (photoMetadata != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preview de Foto",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                    
                    // Contenido scrolleable
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Imagen
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val bitmap = remember(photoMetadata.filePath) {
                                    try {
                                        BitmapFactory.decodeFile(photoMetadata.filePath)
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                                
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Foto capturada",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } else {
                                    Text("No se pudo cargar la imagen")
                                }
                            }
                        }
                        
                        // Metadata
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Información de la Foto",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                MetadataRow("Tipo", photoMetadata.photoType)
                                MetadataRow("Fecha", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(photoMetadata.timestamp))
                                MetadataRow("Dimensiones", "${photoMetadata.width}x${photoMetadata.height}")
                                MetadataRow("Tamaño", "${String.format("%.1f", photoMetadata.fileSize / 1024.0)} KB")
                                MetadataRow("Archivo", photoMetadata.filePath)
                                
                                if (photoMetadata.location != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Ubicación GPS",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    MetadataRow("Latitud", String.format("%.6f", photoMetadata.location.latitude))
                                    MetadataRow("Longitud", String.format("%.6f", photoMetadata.location.longitude))
                                    MetadataRow("Altitud", "${String.format("%.1f", photoMetadata.location.altitude)} m")
                                    MetadataRow("Precisión", "${String.format("%.1f", photoMetadata.location.accuracy)} m")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
