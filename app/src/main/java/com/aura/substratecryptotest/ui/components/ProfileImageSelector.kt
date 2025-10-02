package com.aura.substratecryptotest.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.graphics.drawable.toBitmap
import com.aura.substratecryptotest.data.ProfileImageManager
import kotlinx.coroutines.launch

/**
 * Componente para mostrar y editar la imagen de perfil
 */
@Composable
fun ProfileImageSelector(
    didAddress: String,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profileImageManager = remember { ProfileImageManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var currentImagePath by remember { mutableStateOf<String?>(null) }
    var showImageOptions by remember { mutableStateOf(false) }
    
    // Cargar imagen existente al inicializar
    LaunchedEffect(didAddress) {
        val result = profileImageManager.loadProfileImage(didAddress)
        result.onSuccess { bitmap ->
            currentImagePath = if (bitmap != null) {
                profileImageManager.getProfileImagePath(didAddress)
            } else null
        }
    }
    
    // Launcher para seleccionar imagen de la galería
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                val result = profileImageManager.saveProfileImage(didAddress, selectedUri)
                result.onSuccess { path ->
                    currentImagePath = path
                    onImageSelected(selectedUri)
                }
            }
        }
    }
    
    // Launcher para tomar foto con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // La imagen se guarda automáticamente en el URI temporal
            // Aquí podrías procesar la imagen si es necesario
        }
    }
    
    Box(
        modifier = modifier
            .size(50.dp)
            .clickable { showImageOptions = true }
    ) {
        // Imagen de perfil o placeholder
        Card(
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (currentImagePath != null) {
                    // Mostrar imagen guardada
                    val bitmap = remember(currentImagePath) {
                        try {
                            android.graphics.BitmapFactory.decodeFile(currentImagePath)
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    // Placeholder
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Foto de perfil",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        
        // Botón de cámara
        FloatingActionButton(
            onClick = { showImageOptions = true },
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.BottomEnd),
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Cambiar foto",
                modifier = Modifier.size(12.dp)
            )
        }
    }
    
    // Bottom Sheet para opciones de imagen
    if (showImageOptions) {
        ImageOptionsBottomSheet(
            onDismiss = { showImageOptions = false },
            onSelectFromGallery = {
                showImageOptions = false
                galleryLauncher.launch("image/*")
            },
            onTakePhoto = {
                showImageOptions = false
                // Aquí necesitarías crear un URI temporal para la cámara
                // cameraLauncher.launch(temporaryUri)
            },
            onRemoveImage = {
                showImageOptions = false
                coroutineScope.launch {
                    profileImageManager.deleteProfileImage(didAddress)
                    currentImagePath = null
                }
            },
            hasImage = currentImagePath != null
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageOptionsBottomSheet(
    onDismiss: () -> Unit,
    onSelectFromGallery: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemoveImage: () -> Unit,
    hasImage: Boolean
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Opciones de imagen",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ListItem(
                headlineContent = { Text("Seleccionar de galería") },
                leadingContent = {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                },
                modifier = Modifier.clickable { onSelectFromGallery() }
            )
            
            ListItem(
                headlineContent = { Text("Tomar foto") },
                leadingContent = {
                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                },
                modifier = Modifier.clickable { onTakePhoto() }
            )
            
            if (hasImage) {
                ListItem(
                    headlineContent = { Text("Eliminar imagen") },
                    leadingContent = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onRemoveImage() }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
