package com.aura.substratecryptotest.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.substratecryptotest.data.models.MilestoneDetails
import com.aura.substratecryptotest.data.models.PhotoDetails
import com.aura.substratecryptotest.data.models.GpsDetails
import java.text.SimpleDateFormat
import java.util.*

/**
 * Componente mejorado para mostrar detalles completos de un milestone
 */
@Composable
fun DetailedMilestoneCard(
    milestoneDetails: MilestoneDetails,
    index: Int,
    onPhotoClick: (PhotoDetails) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (milestoneDetails.milestone.isDraft) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (milestoneDetails.milestone.isDraft) 
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header con título y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$index. ${milestoneDetails.milestone.title}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                // Estado del milestone
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (milestoneDetails.milestone.isDraft) 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else 
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = if (milestoneDetails.milestone.isDraft) "📝 Borrador" else "✅ Completado",
                        fontSize = 10.sp,
                        color = if (milestoneDetails.milestone.isDraft) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            // Descripción
            Text(
                text = milestoneDetails.milestone.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 20.sp
            )
            
            // Separador
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            // Información detallada
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fecha y hora
                InfoRowWithIcon(
                    icon = Icons.Default.Schedule,
                    label = "Fecha y Hora",
                    value = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        .format(milestoneDetails.milestone.timestamp)
                )
                
                // Información GPS
                milestoneDetails.gpsData?.let { gpsData ->
                    GpsInfoSection(gpsData)
                }
                
                // Duración
                milestoneDetails.metadata.durationFromPrevious?.let { duration ->
                    val hours = duration / (1000 * 60 * 60)
                    val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
                    InfoRowWithIcon(
                        icon = Icons.Default.Timer,
                        label = "Duración desde anterior",
                        value = "${hours}h ${minutes}m"
                    )
                }
                
                // Fotos
                if (milestoneDetails.photos.isNotEmpty()) {
                    PhotosSection(
                        photos = milestoneDetails.photos,
                        onPhotoClick = onPhotoClick
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRowWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: $value",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun GpsInfoSection(gpsData: GpsDetails) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        InfoRowWithIcon(
            icon = Icons.Default.LocationOn,
            label = "Coordenadas GPS",
            value = "Lat: ${String.format("%.6f", gpsData.latitude)}, Lng: ${String.format("%.6f", gpsData.longitude)}"
        )
        
        gpsData.altitude?.let { altitude ->
            InfoRowWithIcon(
                icon = Icons.Default.Terrain,
                label = "Altitud",
                value = "${String.format("%.1f", altitude)} m"
            )
        }
        
        InfoRowWithIcon(
            icon = Icons.Default.MyLocation,
            label = "Precisión",
            value = "${String.format("%.1f", gpsData.accuracy)} m"
        )
        
        InfoRowWithIcon(
            icon = Icons.Default.Info,
            label = "Fuente GPS",
            value = when (gpsData.source) {
                com.aura.substratecryptotest.data.models.GpsSource.GPS_MANAGER -> "GPS Manager"
                com.aura.substratecryptotest.data.models.GpsSource.PHOTO_EXIF -> "EXIF de Foto"
                com.aura.substratecryptotest.data.models.GpsSource.MANUAL -> "Manual"
            }
        )
    }
}

@Composable
private fun PhotosSection(
    photos: List<PhotoDetails>,
    onPhotoClick: (PhotoDetails) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Photo,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Fotos (${photos.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                PhotoThumbnail(
                    photo = photo,
                    onClick = { onPhotoClick(photo) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoThumbnail(
    photo: PhotoDetails,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp)),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Mostrar información de la foto
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📸",
                    fontSize = 20.sp
                )
                Text(
                    text = when (photo.photoType) {
                        com.aura.substratecryptotest.data.mountaineering.PhotoType.RECORRIDO -> "R"
                        com.aura.substratecryptotest.data.mountaineering.PhotoType.CUMBRE -> "C"
                        com.aura.substratecryptotest.data.mountaineering.PhotoType.GENERAL -> "G"
                    },
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (photo.gpsData != null) {
                    Text(
                        text = "📍",
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}
