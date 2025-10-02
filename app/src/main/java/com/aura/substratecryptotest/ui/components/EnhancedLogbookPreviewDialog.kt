package com.aura.substratecryptotest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbook
import com.aura.substratecryptotest.data.models.MilestoneDetails
import com.aura.substratecryptotest.data.models.PhotoDetails
import java.text.SimpleDateFormat
import java.util.*

/**
 * Diálogo mejorado para preview de bitácora con detalles completos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedLogbookPreviewDialog(
    logbook: MountaineeringLogbook,
    milestoneDetails: List<MilestoneDetails>,
    onDismiss: () -> Unit,
    onFinalize: () -> Unit
) {
    var selectedPhotoForPreview by remember { mutableStateOf<PhotoDetails?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 Preview de Bitácora",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Contenido scrolleable
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Información de la bitácora
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📋 Información General",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            InfoRow("Nombre", logbook.name)
                            InfoRow("Club", logbook.club)
                            InfoRow("Asociación", logbook.association)
                            InfoRow("Participantes", logbook.participantsCount.toString())
                            InfoRow("Licencia", logbook.licenseNumber)
                            InfoRow("Ubicación", logbook.location)
                            InfoRow("Fecha Inicio", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(logbook.startDate))
                            InfoRow("Fecha Fin", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(logbook.endDate))
                            
                            if (logbook.observations.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Observaciones:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = logbook.observations,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Estadísticas
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "📊 Estadísticas",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            
                            val totalPhotos = milestoneDetails.sumOf { it.photos.size }
                            val milestonesWithGps = milestoneDetails.count { it.gpsData != null }
                            val milestonesWithPhotos = milestoneDetails.count { it.photos.isNotEmpty() }
                            
                            InfoRow("Total Milestones", milestoneDetails.size.toString())
                            InfoRow("Total Fotos", totalPhotos.toString())
                            InfoRow("Milestones con GPS", milestonesWithGps.toString())
                            InfoRow("Milestones con Fotos", milestonesWithPhotos.toString())
                        }
                    }
                    
                    // Mapa offline y estadísticas GPS
                    OfflineMapViewer(milestoneDetails = milestoneDetails)
                    
                    // Milestones con detalles completos
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🏔️ Milestones Detallados (${milestoneDetails.size})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            if (milestoneDetails.isEmpty()) {
                                Text(
                                    text = "No hay milestones registrados",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            } else {
                                milestoneDetails.forEachIndexed { index, milestoneDetail ->
                                    DetailedMilestoneCard(
                                        milestoneDetails = milestoneDetail,
                                        index = index + 1,
                                        onPhotoClick = { photoDetails ->
                                            selectedPhotoForPreview = photoDetails
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continuar Editando")
                    }
                    
                    Button(
                        onClick = onFinalize,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Finalizar Bitácora")
                    }
                }
            }
        }
    }
    
    // Diálogo de preview de fotos
    PhotoDetailsPreviewDialog(
        photoDetails = selectedPhotoForPreview,
        onDismiss = { selectedPhotoForPreview = null }
    )
}

@Composable
private fun InfoRow(
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
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}
