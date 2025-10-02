package com.aura.substratecryptotest.ui.screens.mountaineering

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import java.io.File
import com.aura.substratecryptotest.data.services.MilestoneDetailsService
import com.aura.substratecryptotest.data.models.PhotoDetails
import com.aura.substratecryptotest.data.camera.PhotoMetadata
import com.aura.substratecryptotest.data.mountaineering.PhotoType
import com.aura.substratecryptotest.data.location.GPSManager
import com.aura.substratecryptotest.data.location.LocationData
import com.aura.substratecryptotest.ui.components.LocationPermissionHandler
import com.aura.substratecryptotest.ui.components.LocationPermissionCard
import com.aura.substratecryptotest.ui.components.CameraPermissionHandler
import com.aura.substratecryptotest.ui.components.CameraPermissionCard
import com.aura.substratecryptotest.ui.components.CameraCaptureScreen
import com.aura.substratecryptotest.ui.components.PhotoPreviewDialog
import kotlinx.coroutines.launch
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Pantalla para capturar milestones de una expedición de montañismo
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilestoneCaptureScreen(
    _logbookId: Long,
    logbookName: String,
    viewModel: com.aura.substratecryptotest.ui.viewmodels.MountaineeringViewModel? = null,
    onMilestoneAdded: () -> Unit,
    onCompleteLogbook: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gpsManager = remember { GPSManager(context) }
    
    // Crear MilestoneDetailsService
    val milestoneDetailsService = remember {
        val database = com.aura.substratecryptotest.data.mountaineering.MountaineeringDatabase.getDatabase(context)
        val repository = com.aura.substratecryptotest.data.mountaineering.MountaineeringRepository(
            logbookDao = database.logbookDao(),
            milestoneDao = database.milestoneDao(),
            photoDao = database.photoDao()
        )
        MilestoneDetailsService(repository)
    }
    
    var milestoneTitle by remember { mutableStateOf("") }
    var milestoneDescription by remember { mutableStateOf("") }
    var currentLocation by remember { mutableStateOf<LocationData?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    
    var showPhotoCapture by remember { mutableStateOf(false) }
    var selectedPhotoType by remember { mutableStateOf(PhotoType.GENERAL) }
    var capturedPhotos by remember { mutableStateOf<List<PhotoMetadata>>(emptyList()) }
    var selectedPhotoForPreview by remember { mutableStateOf<PhotoMetadata?>(null) }
    
    // Función para capturar ubicación GPS
    fun captureLocation() {
        scope.launch {
            isCapturingLocation = true
            try {
                val location = gpsManager.getCurrentLocation()
                currentLocation = location
            } catch (e: Exception) {
                // Manejar error de GPS
                currentLocation = null
            } finally {
                isCapturingLocation = false
            }
        }
    }
    
    // Función para manejar foto capturada
    fun onPhotoCaptured(metadata: PhotoMetadata) {
        capturedPhotos = capturedPhotos + metadata
        showPhotoCapture = false
    }
    
    // Mostrar pantalla de cámara si está activa
    if (showPhotoCapture) {
        CameraCaptureScreen(
            photoType = selectedPhotoType,
            currentLocation = currentLocation,
            onPhotoCaptured = ::onPhotoCaptured,
            onCancel = { showPhotoCapture = false }
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Hiking,
                    contentDescription = "Milestone",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nuevo Milestone",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Bitácora: $logbookName",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
        
        // Información del Milestone
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Información del Milestone",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = milestoneTitle,
                    onValueChange = { milestoneTitle = it },
                    label = { Text("Título del Milestone") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    placeholder = { Text("Ej: Inicio del recorrido, Refugio, Cumbre, etc.") }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = milestoneDescription,
                    onValueChange = { milestoneDescription = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    placeholder = { Text("Describe lo que sucede en este punto de la expedición...") }
                )
            }
        }
        
        // Ubicación GPS
        LocationPermissionHandler(
            onPermissionGranted = {
                // Los permisos fueron concedidos
            },
            onPermissionDenied = {
                // Los permisos fueron denegados
            }
        ) { hasPermission, requestPermission ->
            
            // Card de información de permisos
            LocationPermissionCard(
                hasPermission = hasPermission,
                onRequestPermission = requestPermission
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Ubicación GPS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (currentLocation != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Ubicación",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lat: ${String.format("%.6f", currentLocation!!.latitude)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Lng: ${String.format("%.6f", currentLocation!!.longitude)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                if (currentLocation!!.altitude != 0.0) {
                                    Text(
                                        text = "Alt: ${String.format("%.1f", currentLocation!!.altitude)} m",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            IconButton(onClick = { 
                                if (hasPermission) {
                                    captureLocation()
                                } else {
                                    requestPermission()
                                }
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Actualizar ubicación")
                            }
                        }
                    } else {
                        Button(
                            onClick = { 
                                if (hasPermission) {
                                    captureLocation()
                                } else {
                                    requestPermission()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Capturar Ubicación GPS")
                        }
                    }
                    
                    if (isCapturingLocation) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Capturando ubicación...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        // Captura de Fotos
        CameraPermissionHandler(
            onPermissionGranted = {
                // Los permisos fueron concedidos
            },
            onPermissionDenied = {
                // Los permisos fueron denegados
            }
        ) { hasPermission, requestPermission ->
            
            // Card de información de permisos
            CameraPermissionCard(
                hasPermission = hasPermission,
                onRequestPermission = requestPermission
            )
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Fotografías",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tipos de fotos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(PhotoType.values().size) { index ->
                            val photoType = PhotoType.values()[index]
                            FilterChip(
                                onClick = { 
                                    selectedPhotoType = photoType
                                    if (hasPermission) {
                                        showPhotoCapture = true
                                    } else {
                                        requestPermission()
                                    }
                                },
                                label = { Text(getPhotoTypeLabel(photoType)) },
                                selected = false,
                                leadingIcon = {
                                    Icon(
                                        imageVector = getPhotoTypeIcon(photoType),
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Mostrar fotos capturadas con miniaturas
                    if (capturedPhotos.isNotEmpty()) {
                        Text(
                            text = "Fotos capturadas: ${capturedPhotos.size}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        // Mostrar miniaturas de las fotos
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(capturedPhotos) { photo ->
                                PhotoThumbnail(
                                    photo = photo,
                                    onClick = { selectedPhotoForPreview = photo }
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "Toca un tipo de foto para capturar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Botones de acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Volver")
            }
            
            Button(
                onClick = {
                    // Guardar milestone con fotos usando MilestoneDetailsService
                    if (milestoneTitle.isNotBlank() && milestoneDescription.isNotBlank()) {
                        scope.launch {
                            try {
                                android.util.Log.d("MilestoneCaptureScreen", "Guardando milestone con ${capturedPhotos.size} fotos")
                                
                                // Convertir PhotoMetadata a PhotoType
                                val photoTypes = capturedPhotos.map { 
                                    com.aura.substratecryptotest.data.mountaineering.PhotoType.GENERAL 
                                }
                                
                                // Guardar milestone con fotos usando el servicio
                                val milestoneId = milestoneDetailsService.saveMilestoneWithPhotos(
                                    logbookId = _logbookId,
                                    title = milestoneTitle,
                                    description = milestoneDescription,
                                    locationData = currentLocation,
                                    photoMetadataList = capturedPhotos,
                                    photoTypes = photoTypes,
                                    isDraft = true
                                )
                                
                                android.util.Log.d("MilestoneCaptureScreen", "Milestone guardado con ID: $milestoneId y ${capturedPhotos.size} fotos")
                                
                                // También actualizar el ViewModel para mantener consistencia
                                if (viewModel != null) {
                                    viewModel.loadMilestones(_logbookId)
                                }
                                
                                onMilestoneAdded()
                                
                                // Limpiar campos para el siguiente milestone
                                milestoneTitle = ""
                                milestoneDescription = ""
                                currentLocation = null
                                capturedPhotos = emptyList()
                                
                            } catch (e: Exception) {
                                android.util.Log.e("MilestoneCaptureScreen", "Error guardando milestone con fotos: ${e.message}", e)
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = milestoneTitle.isNotBlank() && milestoneDescription.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Milestone")
            }
        }
        
        // Botón para completar bitácora
        Button(
            onClick = onCompleteLogbook,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Finalizar Bitácora")
        }
    }
    
    // TODO: Implementar PhotoCaptureSheet
    /*
    if (showPhotoCapture) {
        PhotoCaptureSheet(
            photoType = selectedPhotoType,
            onPhotoCaptured = { _ ->
                // TODO: Implementar guardado de foto con metadata
                showPhotoCapture = false
            },
            onDismiss = { showPhotoCapture = false }
        )
    }
    */
    
    // Diálogo de preview de fotos
    PhotoPreviewDialog(
        photoMetadata = selectedPhotoForPreview,
        onDismiss = { selectedPhotoForPreview = null }
    )
}


private fun getPhotoTypeLabel(photoType: PhotoType): String {
    return when (photoType) {
        PhotoType.RECORRIDO -> "Recorrido"
        PhotoType.CUMBRE -> "Cumbre"
        PhotoType.GENERAL -> "General"
    }
}

private fun getPhotoTypeIcon(photoType: PhotoType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (photoType) {
        PhotoType.RECORRIDO -> Icons.Default.Hiking
        PhotoType.CUMBRE -> Icons.Default.Landscape
        PhotoType.GENERAL -> Icons.Default.PhotoCamera
    }
}

@Composable
private fun PhotoThumbnail(
    photo: PhotoMetadata,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Cargar la imagen real
            val imgFile = File(photo.filePath)
            if (imgFile.exists()) {
                val bitmap = BitmapFactory.decodeFile(imgFile.absolutePath)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Foto de ${photo.photoType}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback si no se puede decodificar
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Error cargando foto",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                // Fallback si el archivo no existe
                Icon(
                    Icons.Default.Error,
                    contentDescription = "Archivo no encontrado",
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Superponer información de la foto
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f))
            ) {
                Text(
                    text = when (photo.photoType) {
                        "RECORRIDO" -> "R"
                        "CUMBRE" -> "C"
                        "GENERAL" -> "G"
                        else -> "?"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                if (photo.location != null) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "GPS",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
