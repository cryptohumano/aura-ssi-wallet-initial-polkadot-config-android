package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbook
import com.aura.substratecryptotest.data.mountaineering.ExpeditionMilestone
import com.aura.substratecryptotest.ui.viewmodels.SecureDocumentViewModel
import com.aura.substratecryptotest.ui.viewmodels.MountaineeringViewModel
import com.aura.substratecryptotest.ui.viewmodels.UserManagementViewModel
import kotlinx.coroutines.launch
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.data.UserDocument
import com.aura.substratecryptotest.utils.Logger
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de Documentos Segura
 * Integra PDFs existentes con sistema de usuarios seguros
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDocumentsScreen(
    onNavigateBack: () -> Unit,
    secureDocumentViewModel: SecureDocumentViewModel,
    mountaineeringViewModel: MountaineeringViewModel
) {
    val context = LocalContext.current
    val secureUserRepository = SecureUserRepository.getInstance(context)
    val userManager = UserManager(context)
    
    // Estados
    var showCreateForm by remember { mutableStateOf(false) }
    var showUserDialog by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf(userManager.getCurrentUser()) }
    var userDocuments by remember { mutableStateOf<List<UserDocument>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Estados para crear bitácora
    var logbookName by remember { mutableStateOf("") }
    var logbookClub by remember { mutableStateOf("") }
    var logbookLocation by remember { mutableStateOf("") }
    
    // Estados del MountaineeringViewModel
    val logbooks by mountaineeringViewModel.logbooks.collectAsState()
    val isLoadingLogbooks by mountaineeringViewModel.isLoading.collectAsState()
    
    // Recargar bitácoras cuando cambie el usuario
    LaunchedEffect(currentUser?.id) {
        currentUser?.let { user ->
            mountaineeringViewModel.loadUserLogbooks(user.id)
        }
    }
    
    // Cargar datos al iniciar
    LaunchedEffect(Unit) {
        try {
            Logger.debug("SecureDocumentsScreen", "Cargando datos integrados", "")
            
            // 1. Cargar usuario actual
            currentUser = userManager.getCurrentUser()
            
            // 2. Cargar documentos del usuario actual
            if (currentUser != null) {
                val documents = secureUserRepository.getUserWallets()
                // Por ahora usar wallets como ejemplo, después cargar documentos reales
                Logger.debug("SecureDocumentsScreen", "Usuario activo", "ID: ${currentUser!!.id}")
            } else {
                Logger.warning("SecureDocumentsScreen", "No hay usuario activo", "Mostrando pantalla de selección")
            }
            
            // 3. Cargar bitácoras de montañismo del usuario actual
            mountaineeringViewModel.loadUserLogbooks(currentUser?.id)
            
            isLoading = false
        } catch (e: Exception) {
            Logger.error("SecureDocumentsScreen", "Error cargando datos", e.message ?: "Error desconocido", e)
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        if (currentUser != null) "📄 Documentos de ${currentUser!!.name}" 
                        else "📄 Documentos"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (currentUser == null) {
                        Button(
                            onClick = { showUserDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("👤 Seleccionar Usuario", fontSize = 12.sp)
                        }
                    } else {
                        IconButton(onClick = { showCreateForm = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Nueva Bitácora")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (currentUser == null) {
            // Pantalla de selección de usuario
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "👤 Selecciona un Usuario",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Para ver tus documentos PDF y bitácoras, necesitas seleccionar un usuario",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showUserDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Seleccionar Usuario")
                            }
                        }
                    }
                }
            }
        } else {
            // Pantalla principal con documentos del usuario
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Información del usuario
                item {
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
                                text = "👤 Usuario Activo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Nombre: ${currentUser!!.name}")
                            Text("ID: ${currentUser!!.id}")
                            Text("Biometría: ${if (currentUser!!.isActive) "✅ Habilitada" else "❌ Deshabilitada"}")
                        }
                    }
                }
                
                // Documentos seguros del usuario
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "🔐 Documentos Seguros",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Documentos encriptados: ${userDocuments.size}")
                            
                            if (userDocuments.isEmpty()) {
                                Text(
                                    text = "No hay documentos seguros aún",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Button(
                                onClick = { showCreateForm = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Crear Nueva Bitácora")
                            }
                        }
                    }
                }
                
                // Bitácoras de montañismo (PDFs existentes)
                item {
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
                                text = "🏔️ Bitácoras de Montañismo",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bitácoras disponibles: ${logbooks.size}")
                            
                            if (logbooks.isEmpty()) {
                                Text(
                                    text = "No hay bitácoras disponibles",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Lista de bitácoras
                items(logbooks) { logbook ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = logbook.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = logbook.observations.ifEmpty { "Sin observaciones" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.PictureAsPdf,
                                    contentDescription = "PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Creado: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(logbook.createdAt)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = if (logbook.isCompleted) "✅ Completada" else "🔄 En Progreso",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (logbook.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        // Crear documento seguro desde bitácora
                                        secureDocumentViewModel.createSecureDocument(
                                            walletId = "temp_wallet", // Temporal
                                            documentHash = "logbook_${logbook.id}",
                                            documentType = "mountaineering_logbook",
                                            metadata = mapOf(
                                                "logbook_id" to logbook.id,
                                                "logbook_name" to logbook.name,
                                                "is_completed" to logbook.isCompleted
                                            )
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("🔐 Hacer Seguro", fontSize = 12.sp)
                                }
                                
                                Button(
                                    onClick = { 
                                        // Exportar a PDF usando MountaineeringViewModel
                                        Logger.debug("SecureDocumentsScreen", "Exportando bitácora a PDF", "ID: ${logbook.id}")
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("📄 Exportar PDF", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialog para seleccionar usuario
    if (showUserDialog) {
        AlertDialog(
            onDismissRequest = { showUserDialog = false },
            title = { Text("Seleccionar Usuario") },
            text = { Text("Esta funcionalidad se implementará próximamente") },
            confirmButton = {
                TextButton(onClick = { showUserDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
    
    // Dialog para crear nueva bitácora
    if (showCreateForm) {
        AlertDialog(
            onDismissRequest = { 
                showCreateForm = false
                logbookName = ""
                logbookClub = ""
                logbookLocation = ""
            },
            title = { Text("Crear Nueva Bitácora") },
            text = {
                Column {
                    OutlinedTextField(
                        value = logbookName,
                        onValueChange = { logbookName = it },
                        label = { Text("Nombre de la Bitácora") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = logbookClub,
                        onValueChange = { logbookClub = it },
                        label = { Text("Club/Asociación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = logbookLocation,
                        onValueChange = { logbookLocation = it },
                        label = { Text("Ubicación") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (logbookName.isNotEmpty() && currentUser != null) {
                            // Crear bitácora usando MountaineeringViewModel
                            val logbookData = com.aura.substratecryptotest.ui.screens.mountaineering.LogbookData(
                                name = logbookName,
                                club = logbookClub.ifEmpty { "Club por defecto" },
                                association = "Asociación por defecto",
                                participantsCount = 1,
                                licenseNumber = "LIC-${System.currentTimeMillis()}",
                                startDate = java.util.Date(),
                                endDate = java.util.Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000), // 7 días después
                                location = logbookLocation.ifEmpty { "Ubicación por defecto" },
                                observations = "Bitácora creada por usuario: ${currentUser!!.name}"
                            )
                            mountaineeringViewModel.createLogbook(logbookData)
                            
                            Logger.success("SecureDocumentsScreen", "Bitácora creada", "Nombre: $logbookName, Usuario: ${currentUser!!.name}")
                            
                            showCreateForm = false
                            logbookName = ""
                            logbookClub = ""
                            logbookLocation = ""
                        }
                    },
                    enabled = logbookName.isNotEmpty()
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCreateForm = false
                    logbookName = ""
                    logbookClub = ""
                    logbookLocation = ""
                }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
