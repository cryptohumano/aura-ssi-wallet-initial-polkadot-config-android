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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.aura.substratecryptotest.ui.screens.mountaineering.CreateLogbookForm
import com.aura.substratecryptotest.ui.screens.mountaineering.LogbookData
import com.aura.substratecryptotest.ui.screens.mountaineering.MilestoneCaptureScreen
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbook
import com.aura.substratecryptotest.data.DocumentTypeData
import com.aura.substratecryptotest.ui.viewmodels.UserDocumentsViewModel
import com.aura.substratecryptotest.data.UserMountaineeringLogbook
import com.aura.substratecryptotest.data.UserExpeditionMilestone
import com.aura.substratecryptotest.data.UserExpeditionPhoto
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.stringResource
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.ui.context.LanguageAware
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.*
import com.aura.substratecryptotest.ui.components.PDFCryptographicSignatureDialog
import com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager
import java.io.File
import com.aura.substratecryptotest.ui.components.EnhancedLogbookPreviewDialog
import com.aura.substratecryptotest.data.models.MilestoneDetails

/**
 * Pantalla de Documentos
 */

data class DocumentTypeData(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val fields: List<String>
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(
    onNavigateBack: () -> Unit,
    viewModel: UserDocumentsViewModel? = null
) {
    val context = LocalContext.current
    LanguageAware {
    var showCreateForm by remember { mutableStateOf(false) }
    var showMilestoneCapture by remember { mutableStateOf(false) }
    var showLogbookPreview by remember { mutableStateOf(false) }
    var previewLogbook by remember { mutableStateOf<UserMountaineeringLogbook?>(null) }
    
    // Estados para exportación PDF
    var isExportingPDF by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf<String?>(null) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var currentLogbook by remember { mutableStateOf<UserMountaineeringLogbook?>(null) }
    var pendingLogbookName by remember { mutableStateOf<String?>(null) }
    var generatedPDFFile by remember { mutableStateOf<java.io.File?>(null) }
    var showPDFViewer by remember { mutableStateOf(false) }
    var savedPDFs by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    
    // Estados para firma criptográfica
    var showCryptographicSignature by remember { mutableStateOf(false) }
    var signatureResult by remember { mutableStateOf<String?>(null) }
    var showSignatureVerification by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<String?>(null) }
    var shouldSavePDFPermanently by remember { mutableStateOf(false) }
    
    // Detalles de milestones para preview
    var milestoneDetails by remember { mutableStateOf<List<MilestoneDetails>>(emptyList()) }
    
    // Inicializar ViewModel si no se proporciona
    val userDocumentsViewModel = viewModel ?: remember { UserDocumentsViewModel() }
    
    // Inicializar ViewModel
    LaunchedEffect(context) {
        userDocumentsViewModel.initialize(context)
    }
    
    // Obtener datos del usuario actual
    val currentUser by userDocumentsViewModel.currentUser.collectAsState()
    val existingLogbooks by userDocumentsViewModel.userLogbooks.collectAsState()
    val milestoneCounts by userDocumentsViewModel.milestoneCounts.collectAsState()
    val userMilestones by userDocumentsViewModel.userMilestones.collectAsState()
    val userPhotos by userDocumentsViewModel.userPhotos.collectAsState()
    
    // Estado del filtro
    var showCompletedLogbooks by remember { mutableStateOf(true) }
    
    // Filtrar bitácoras según el estado del filtro
    val filteredLogbooks = if (showCompletedLogbooks) {
        existingLogbooks // Mostrar todas
    } else {
        existingLogbooks.filter { !it.isCompleted } // Solo activas
    }
    
    // Función para mostrar preview de bitácora
    fun showLogbookPreview(logbook: UserMountaineeringLogbook) {
        android.util.Log.d("DocumentsScreen", "Cargando detalles completos para preview de bitácora ${logbook.id}")
        previewLogbook = logbook
        showLogbookPreview = true
    }
    
    // Función para generar vista previa de bitácora a PDF
    fun generateLogbookPreview(logbook: UserMountaineeringLogbook) {
        previewLogbook = logbook
        isExportingPDF = true
        exportError = null
        exportSuccess = null
        generatedPDFFile = null
    }
    
    // Función para extraer ID de bitácora del nombre del archivo PDF
    fun extractLogbookIdFromFileName(fileName: String): Long? {
        return try {
            // Patrón: bitacora_[nombre]_[id].pdf
            val regex = Regex("bitacora_.*_(\\d+)\\.pdf")
            val matchResult = regex.find(fileName)
            matchResult?.groupValues?.get(1)?.toLong()
        } catch (e: Exception) {
            android.util.Log.w("DocumentsScreen", "No se pudo extraer ID de bitácora del nombre: $fileName")
            null
        }
    }
    
    // Manejar la generación de vista previa con LaunchedEffect
    LaunchedEffect(isExportingPDF) {
        if (isExportingPDF && previewLogbook != null) {
            try {
                val pdfFile = userDocumentsViewModel.generateLogbookPreview(previewLogbook!!.id)
                if (pdfFile != null) {
                    generatedPDFFile = pdfFile
                    exportSuccess = "Vista previa generada exitosamente: ${pdfFile.name}"
                    android.util.Log.d("DocumentsScreen", "Vista previa generada: ${pdfFile.absolutePath}")
                } else {
                    exportError = "Error al generar la vista previa"
                }
            } catch (e: Exception) {
                exportError = "Error al generar vista previa: ${e.message}"
                android.util.Log.e("DocumentsScreen", "Error generando vista previa", e)
            } finally {
                isExportingPDF = false
            }
        }
    }
    
    // Manejar el guardado permanente del PDF
    LaunchedEffect(shouldSavePDFPermanently) {
        if (shouldSavePDFPermanently && previewLogbook != null) {
            android.util.Log.d("DocumentsScreen", "=== INICIANDO GUARDADO PERMANENTE ===")
            android.util.Log.d("DocumentsScreen", "Bitácora: ${previewLogbook!!.name}")
            android.util.Log.d("DocumentsScreen", "ID Bitácora: ${previewLogbook!!.id}")
            android.util.Log.d("DocumentsScreen", "Usuario actual: ${currentUser}")
            
            try {
                android.util.Log.d("DocumentsScreen", "Llamando exportLogbookToPDF...")
                val permanentPDF = userDocumentsViewModel.exportLogbookToPDF(previewLogbook!!.id)
                
                if (permanentPDF != null) {
                    val fileDate = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(permanentPDF.lastModified()))
                    val fileSize = "${permanentPDF.length() / 1024} KB"
                    val fileName = permanentPDF.name
                    
                    android.util.Log.d("DocumentsScreen", "=== PDF GUARDADO EXITOSAMENTE ===")
                    android.util.Log.d("DocumentsScreen", "Archivo: $fileName")
                    android.util.Log.d("DocumentsScreen", "Fecha: $fileDate")
                    android.util.Log.d("DocumentsScreen", "Tamaño: $fileSize")
                    android.util.Log.d("DocumentsScreen", "Ubicación: ${permanentPDF.parent}")
                    android.util.Log.d("DocumentsScreen", "Path completo: ${permanentPDF.absolutePath}")
                    
                    exportSuccess = "PDF guardado exitosamente:\n\n📄 Archivo: $fileName\n📅 Fecha: $fileDate\n💾 Tamaño: $fileSize\n📍 Ubicación: ${permanentPDF.parent}"
                    
                    // Generar firma DID del PDF guardado
                    android.util.Log.d("DocumentsScreen", "=== INICIANDO GENERACIÓN DE FIRMA DID ===")
                    android.util.Log.d("DocumentsScreen", "PDF para firmar: ${permanentPDF.name}")
                    
                    try {
                        // Obtener el mnemonic del usuario actual
                        android.util.Log.d("DocumentsScreen", "Obteniendo mnemonic del usuario actual...")
                        
                        val secureUserRepository = com.aura.substratecryptotest.data.SecureUserRepository.getInstance(context)
                        val userWallets = secureUserRepository.getUserWallets()
                        
                        if (userWallets.isNotEmpty()) {
                            val firstWallet = userWallets.first()
                            android.util.Log.d("DocumentsScreen", "Wallet encontrada: ${firstWallet.name} (${firstWallet.id.take(8)}...)")
                            
                            // Obtener mnemonic de la primera wallet (requiere autenticación biométrica)
                            val mnemonicResult = secureUserRepository.getWalletMnemonic(
                                walletId = firstWallet.id,
                                requireBiometric = false // Temporalmente deshabilitado para testing
                            )
                            
                            if (mnemonicResult.isSuccess) {
                                val mnemonic = mnemonicResult.getOrNull()
                                android.util.Log.d("DocumentsScreen", "Mnemonic obtenido exitosamente")
                                
                                // Crear PDFSignatureManager
                                val pdfSignatureManager = com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager(context)
                                
                                // Generar firma DID
                                android.util.Log.d("DocumentsScreen", "Llamando PDFSignatureManager.signPDF...")
                                val signatureResult = pdfSignatureManager.signPDF(
                                    pdfFile = permanentPDF,
                                    mnemonic = mnemonic!!,
                                    signerName = "Usuario Aura",
                                    logbookId = previewLogbook!!.id
                                )
                            
                                when (signatureResult) {
                                    is com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager.SignatureResult.Success -> {
                                        android.util.Log.d("DocumentsScreen", "=== FIRMA DID GENERADA EXITOSAMENTE ===")
                                        android.util.Log.d("DocumentsScreen", "Archivo de firma: ${signatureResult.signatureFile.name}")
                                        android.util.Log.d("DocumentsScreen", "Path: ${signatureResult.signatureFile.absolutePath}")
                                        android.util.Log.d("DocumentsScreen", "DID URI: ${signatureResult.signature.didKeyUri}")
                                        android.util.Log.d("DocumentsScreen", "Dirección: ${signatureResult.signature.signerAddress}")
                                        
                                        exportSuccess = "PDF guardado y firmado exitosamente:\n\n📄 Archivo: $fileName\n📅 Fecha: $fileDate\n💾 Tamaño: $fileSize\n📍 Ubicación: ${permanentPDF.parent}\n\n🔐 Firma DID generada:\n📁 ${signatureResult.signatureFile.name}\n🔑 ${signatureResult.signature.didKeyUri}"
                                    }
                                    is com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager.SignatureResult.Error -> {
                                        android.util.Log.e("DocumentsScreen", "❌ Error generando firma DID: ${signatureResult.message}")
                                        exportSuccess = "PDF guardado exitosamente:\n\n📄 Archivo: $fileName\n📅 Fecha: $fileDate\n💾 Tamaño: $fileSize\n📍 Ubicación: ${permanentPDF.parent}\n\n⚠️ Error en firma DID: ${signatureResult.message}"
                                    }
                                }
                            } else {
                                android.util.Log.e("DocumentsScreen", "❌ No se pudo obtener el mnemonic de la wallet")
                                val errorMsg = mnemonicResult.exceptionOrNull()?.message ?: "Error desconocido"
                                exportSuccess = "PDF guardado exitosamente:\n\n📄 Archivo: $fileName\n📅 Fecha: $fileDate\n💾 Tamaño: $fileSize\n📍 Ubicación: ${permanentPDF.parent}\n\n⚠️ No se pudo generar firma DID: $errorMsg"
                            }
                        } else {
                            android.util.Log.e("DocumentsScreen", "❌ No hay wallets disponibles para el usuario")
                            exportSuccess = "PDF guardado exitosamente:\n\n📄 Archivo: $fileName\n📅 Fecha: $fileDate\n💾 Tamaño: $fileSize\n📍 Ubicación: ${permanentPDF.parent}\n\n⚠️ No se pudo generar firma DID: No hay wallets disponibles"
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("DocumentsScreen", "❌ ERROR FATAL generando firma DID", e)
                        android.util.Log.e("DocumentsScreen", "Mensaje: ${e.message}")
                        android.util.Log.e("DocumentsScreen", "Stack trace:", e)
                        exportSuccess = "PDF guardado exitosamente:\n\n📄 Archivo: $fileName\n📅 Fecha: $fileDate\n💾 Tamaño: $fileSize\n📍 Ubicación: ${permanentPDF.parent}\n\n⚠️ Error en firma DID: ${e.message}"
                    }
                    
                } else {
                    android.util.Log.e("DocumentsScreen", "❌ Error: exportLogbookToPDF retornó null")
                    exportError = "Error al guardar el PDF permanentemente"
                }
            } catch (e: Exception) {
                android.util.Log.e("DocumentsScreen", "❌ ERROR FATAL en guardado permanente", e)
                android.util.Log.e("DocumentsScreen", "Mensaje: ${e.message}")
                android.util.Log.e("DocumentsScreen", "Stack trace:", e)
                exportError = "Error al guardar PDF: ${e.message}"
            } finally {
                shouldSavePDFPermanently = false
                android.util.Log.d("DocumentsScreen", "Guardado permanente completado")
            }
        }
    }
    
    // Logging de datos cargados
    LaunchedEffect(currentUser, existingLogbooks, milestoneCounts, userMilestones, userPhotos) {
        android.util.Log.d("DocumentsScreen", "=== DATOS CARGADOS PARA USUARIO $currentUser ===")
        android.util.Log.d("DocumentsScreen", "Bitácoras totales: ${existingLogbooks.size}")
        android.util.Log.d("DocumentsScreen", "Bitácoras completadas: ${existingLogbooks.count { it.isCompleted }}")
        android.util.Log.d("DocumentsScreen", "Bitácoras activas: ${existingLogbooks.count { !it.isCompleted }}")
        existingLogbooks.forEach { logbook ->
            android.util.Log.d("DocumentsScreen", "Bitácora: ${logbook.name} (ID: ${logbook.id}) - Completada: ${logbook.isCompleted}")
        }
        android.util.Log.d("DocumentsScreen", "Conteos de milestones: $milestoneCounts")
        android.util.Log.d("DocumentsScreen", "Milestones actuales: ${userMilestones.size}")
        userMilestones.forEach { milestone ->
            android.util.Log.d("DocumentsScreen", "Milestone: ${milestone.title} - Draft: ${milestone.isDraft}")
        }
        android.util.Log.d("DocumentsScreen", "Fotos actuales: ${userPhotos.size}")
        userPhotos.forEach { photo ->
            android.util.Log.d("DocumentsScreen", "Foto: ${photo.photoPath} - Tipo: ${photo.photoType}")
        }
        android.util.Log.d("DocumentsScreen", "===================")
    }
    
    // Cargar PDFs guardados y vincularlos con bitácoras
    LaunchedEffect(existingLogbooks) {
        try {
            val logbooksDir = context.getExternalFilesDir("logbooks")
            if (logbooksDir?.exists() == true) {
                // Migrar firmas existentes al nuevo formato
                val pdfSignatureManager = com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager(context)
                val migratedCount = pdfSignatureManager.migrateExistingSignatures(logbooksDir)
                if (migratedCount > 0) {
                    android.util.Log.d("DocumentsScreen", "✅ Migración de firmas: $migratedCount firmas migradas")
                }
                
                // Limpiar firmas huérfanas
                val cleanedCount = pdfSignatureManager.cleanupOrphanedSignatures(logbooksDir)
                if (cleanedCount > 0) {
                    android.util.Log.d("DocumentsScreen", "🧹 Limpieza de firmas: $cleanedCount firmas huérfanas eliminadas")
                }
                
                val pdfFiles: List<java.io.File> = logbooksDir.listFiles()?.filter { file -> file.name.endsWith(".pdf") }?.sortedByDescending { file -> file.lastModified() } ?: emptyList()
                savedPDFs = pdfFiles
                android.util.Log.d("DocumentsScreen", "PDFs guardados encontrados: ${pdfFiles.size}")
                
                // Vincular PDFs con bitácoras existentes
                pdfFiles.forEach { pdf: java.io.File ->
                    val logbookId = extractLogbookIdFromFileName(pdf.name)
                    val associatedLogbook = existingLogbooks.find { it.id == logbookId }
                    
                    android.util.Log.d("DocumentsScreen", "PDF: ${pdf.name}")
                    android.util.Log.d("DocumentsScreen", "  - ID extraído: $logbookId")
                    android.util.Log.d("DocumentsScreen", "  - Bitácora asociada: ${associatedLogbook?.name ?: "No encontrada"}")
                    android.util.Log.d("DocumentsScreen", "  - Fecha: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(pdf.lastModified()))}")
                    
                    // Verificar si tiene firma correspondiente
                    val hasSignature = pdfSignatureManager.hasCorrespondingSignature(pdf)
                    android.util.Log.d("DocumentsScreen", "  - Tiene firma DID: $hasSignature")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DocumentsScreen", "Error al cargar PDFs guardados: ${e.message}")
        }
    }
    
    // Manejar navegación después de crear bitácora
    LaunchedEffect(existingLogbooks, pendingLogbookName) {
        if (pendingLogbookName != null) {
            android.util.Log.d("DocumentsScreen", "Buscando bitácora más reciente con nombre: $pendingLogbookName")
            
            // Buscar todas las bitácoras con ese nombre y tomar la más reciente (mayor ID)
            val logbooksWithName = existingLogbooks.filter { it.name == pendingLogbookName }
            val realLogbook = logbooksWithName.maxByOrNull { it.id }
            
            if (realLogbook != null) {
                android.util.Log.d("DocumentsScreen", "Bitácora encontrada: ${realLogbook.name} (ID: ${realLogbook.id})")
                android.util.Log.d("DocumentsScreen", "Bitácora completada: ${realLogbook.isCompleted}")
                
                // Verificar que la bitácora no esté completada
                if (realLogbook.isCompleted) {
                    android.util.Log.w("DocumentsScreen", "⚠️ La bitácora encontrada ya está completada. Buscando una activa...")
                    
                    // Buscar una bitácora activa (no completada) con ese nombre
                    val activeLogbook = logbooksWithName.find { !it.isCompleted }
                    if (activeLogbook != null) {
                        android.util.Log.d("DocumentsScreen", "✅ Bitácora activa encontrada: ${activeLogbook.name} (ID: ${activeLogbook.id})")
                        currentLogbook = activeLogbook
                    } else {
                        android.util.Log.e("DocumentsScreen", "❌ No hay bitácoras activas con ese nombre")
                        // Crear una nueva bitácora con nombre único
                        val uniqueName = "${pendingLogbookName}_${System.currentTimeMillis()}"
                        android.util.Log.d("DocumentsScreen", "Creando nueva bitácora con nombre único: $uniqueName")
                        // Aquí podrías crear una nueva bitácora o mostrar un error
                    }
                } else {
                    currentLogbook = realLogbook
                }
                
                showCreateForm = false
                showMilestoneCapture = true
                pendingLogbookName = null
            } else {
                android.util.Log.w("DocumentsScreen", "No se encontró bitácora con nombre: $pendingLogbookName")
            }
        }
    }
    
    when {
        showCreateForm -> {
            CreateLogbookForm(
                onLogbookCreated = { logbookData ->
                    // Generar nombre único para evitar conflictos
                    val timestamp = System.currentTimeMillis()
                    val uniqueLogbookData = logbookData.copy(
                        name = "${logbookData.name}_${timestamp}"
                    )
                    
                    android.util.Log.d("DocumentsScreen", "Creando bitácora con nombre único: ${uniqueLogbookData.name}")
                    
                    // Usar ViewModel para crear la bitácora
                    userDocumentsViewModel.createLogbook(uniqueLogbookData)
                    
                    // Establecer el nombre pendiente para que LaunchedEffect maneje la navegación
                    pendingLogbookName = uniqueLogbookData.name
                },
                onCancel = {
                    showCreateForm = false
                }
            )
        }
        
        showMilestoneCapture && currentLogbook != null -> {
            MilestoneCaptureScreen(
                _logbookId = currentLogbook!!.id,
                logbookName = currentLogbook!!.name,
                viewModel = null, // No usar MountaineeringViewModel aquí
                onMilestoneAdded = {
                    // TODO: Implementar agregado de milestone
                },
                onCompleteLogbook = {
                    if (currentLogbook != null) {
                        // Mostrar preview antes de finalizar
                        showLogbookPreview(currentLogbook!!)
                    }
                },
                onNavigateBack = {
                    // No hacer nada - el usuario debe usar el botón "Finalizar Bitácora"
                    // para salir de la captura de milestones
                }
            )
        }
        
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.documents_title)) },
                        navigationIcon = {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                            }
                        },
                        actions = {
                            // Filtro para mostrar bitácoras completadas
                            FilterChip(
                                onClick = { 
                                    android.util.Log.d("DocumentsScreen", "Cambiando filtro de $showCompletedLogbooks a ${!showCompletedLogbooks}")
                                    showCompletedLogbooks = !showCompletedLogbooks 
                                },
                                label = { Text(if (showCompletedLogbooks) stringResource(R.string.documents_all) else stringResource(R.string.documents_active_only)) },
                                selected = showCompletedLogbooks,
                                leadingIcon = {
                                    Icon(
                                        if (showCompletedLogbooks) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null
                                    )
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { 
                                // Recargar documentos disponibles
                                android.util.Log.d("DocumentsScreen", "🔄 Recargando documentos...")
                                // TODO: Implementar recarga de documentos
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Recargar")
                            }
                            IconButton(onClick = { showCreateForm = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Nueva bitácora")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showCreateForm = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Crear Documento")
                    }
                }
            ) { paddingValues ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Información sobre documentos
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "📄 Gestión de Documentos",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Text(
                                    text = "Firma documentos PDF para ti mismo o con otros y envíalos a attesters que requieran esta información.",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    
                    // Tipos de documentos disponibles
                    item {
                        Text(
                            text = "Tipos de Documentos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    val documentTypes = listOf(
                        DocumentTypeData(
                            title = "Bitácora de Montañismo",
                            description = "Registra expediciones de montañismo con fotos, GPS y milestones",
                            icon = Icons.Default.Hiking,
                            fields = listOf(
                                "Nombre Completo",
                                "Club",
                                "Asociación",
                                "Cantidad de participantes",
                                "Número de Licencia Deportiva Federada",
                                "Fechas de inicio y término",
                                "Lugar",
                                "Fotos del recorrido",
                                "Fotos de cumbre",
                                "Archivo GPS (GPX o KMZ)",
                                "Observaciones"
                            )
                        ),
                        DocumentTypeData(
                            title = "Documento Personalizado",
                            description = "Crea documentos PDF personalizados para cualquier propósito",
                            icon = Icons.Default.Description,
                            fields = listOf("Campos personalizables")
                        )
                    )
                    
                    items(documentTypes) { documentType ->
                        DocumentTypeCard(
                            documentType = documentType,
                            onCreate = { 
                                if (documentType.title == "Bitácora de Montañismo") {
                                    showCreateForm = true
                                }
                            }
                        )
                    }
                    
                    // Documentos existentes
                    item {
                        Text(
                            text = "Mis Documentos",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                        )
                    }
                    
                    // Bitácoras creadas
                    if (filteredLogbooks.isNotEmpty()) {
                        items(filteredLogbooks) { logbook ->
                            LogbookCard(
                                logbook = logbook,
                                milestoneCount = milestoneCounts[logbook.id] ?: 0,
                                onContinueLogbook = {
                                    currentLogbook = logbook
                                    showMilestoneCapture = true
                                },
                                onViewCompleted = {
                                    // Mostrar preview de la bitácora
                                    showLogbookPreview(logbook)
                                },
                                onDeleteLogbook = {
                                    userDocumentsViewModel.deleteLogbook(logbook)
                                    // TODO: Mostrar confirmación antes de eliminar
                                },
                                onExportPDF = {
                                    generateLogbookPreview(logbook)
                                },
                                onViewPDF = { pdfFile ->
                                    android.util.Log.d("DocumentsScreen", "=== CALLBACK onViewPDF EJECUTADO ===")
                                    android.util.Log.d("DocumentsScreen", "PDF recibido: ${pdfFile.name}")
                                    android.util.Log.d("DocumentsScreen", "PDF path: ${pdfFile.absolutePath}")
                                    android.util.Log.d("DocumentsScreen", "PDF exists: ${pdfFile.exists()}")
                                    
                                    generatedPDFFile = pdfFile
                                    android.util.Log.d("DocumentsScreen", "generatedPDFFile asignado")
                                    
                                    showPDFViewer = true
                                    android.util.Log.d("DocumentsScreen", "showPDFViewer = true")
                                    android.util.Log.d("DocumentsScreen", "=== FIN CALLBACK onViewPDF ===")
                                }
                            )
                            
                            // Mostrar PDF exportado asociado a esta bitácora
                            val associatedPDF = savedPDFs.find { pdf ->
                                extractLogbookIdFromFileName(pdf.name) == logbook.id
                            }
                            
                            if (associatedPDF != null) {
                                // Título de PDF exportado
                                Text(
                                    text = "📄 PDF Exportado de ${logbook.name}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                                
                                // Mostrar el PDF exportado
                                ExportedPDFCard(
                                    pdfFile = associatedPDF,
                                    logbook = logbook,
                                    onViewPDF = { pdfFile: File ->
                                        generatedPDFFile = pdfFile
                                        showPDFViewer = true
                                    },
                                    onSignPDF = { pdfFile: File ->
                                        generatedPDFFile = pdfFile
                                        showCryptographicSignature = true
                                    },
                                    onVerifySignature = { pdfFile: File ->
                                        generatedPDFFile = pdfFile
                                        showSignatureVerification = true
                                    }
                                )
                            } else {
                                // Mostrar información de que no hay PDF exportado
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "No hay PDF exportado",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Usa 'Ver PDF' para exportar",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Placeholder para documentos existentes
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Text(
                                        text = "No hay documentos creados",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Text(
                                        text = "Usa el botón + para crear tu primer documento",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    // PDFs guardados organizados por carpetas
                    if (savedPDFs.isNotEmpty()) {
                        item {
                            Text(
                                text = "📁 Documentos PDF Organizados",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        
                        // Agrupar PDFs por directorio
                        val pdfsByDirectory = savedPDFs.groupBy { it.parentFile?.name ?: "Sin categoría" }
                        
                        pdfsByDirectory.forEach { (directoryName, pdfFiles) ->
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "📁 $directoryName",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "${pdfFiles.size} archivo${if (pdfFiles.size != 1) "s" else ""}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        pdfFiles.forEach { pdfFile: File ->
                                            DocumentFileCard(
                                                pdfFile = pdfFile,
                                                onViewPDF = { pdfFile: File ->
                                                    generatedPDFFile = pdfFile
                                                    showPDFViewer = true
                                                },
                                                onSignPDF = { pdfFile: File ->
                                                    generatedPDFFile = pdfFile
                                                    showCryptographicSignature = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Diálogo de preview mejorado de bitácora
    if (showLogbookPreview && previewLogbook != null) {
        // Convertir UserMountaineeringLogbook a MountaineeringLogbook para el diálogo
        val mountaineeringLogbook = com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbook(
            id = previewLogbook!!.id,
            name = previewLogbook!!.name,
            club = previewLogbook!!.club,
            association = previewLogbook!!.association,
            participantsCount = previewLogbook!!.participantsCount,
            licenseNumber = previewLogbook!!.licenseNumber,
            location = previewLogbook!!.location,
            startDate = java.util.Date(previewLogbook!!.startDate),
            endDate = java.util.Date(previewLogbook!!.endDate),
            observations = previewLogbook!!.observations,
            createdAt = java.util.Date(previewLogbook!!.createdAt),
            isCompleted = previewLogbook!!.isCompleted
        )
        
        EnhancedLogbookPreviewDialog(
            logbook = mountaineeringLogbook,
            milestoneDetails = milestoneDetails,
            onDismiss = {
                showLogbookPreview = false
                previewLogbook = null
            },
            onFinalize = {
                if (viewModel != null && previewLogbook != null) {
                    // Finalizar borradores y marcar bitácora como completada
                    viewModel.finalizeDraftMilestones(previewLogbook!!.id)
                    viewModel.completeLogbook(previewLogbook!!.id, "/storage/bitacora_${previewLogbook!!.id}.pdf")
                }
                showLogbookPreview = false
                showMilestoneCapture = false
                currentLogbook = null
                previewLogbook = null
            }
        )
    }
    
    // Diálogo de exportación exitosa
    if (exportSuccess != null) {
        AlertDialog(
            onDismissRequest = { exportSuccess = null },
            content = {
                Column {
                    Text(
                        text = "Exportación Exitosa",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = exportSuccess!!)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { exportSuccess = null }) {
                            Text(stringResource(R.string.documents_close))
                        }
                        if (generatedPDFFile != null) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    showPDFViewer = true
                                    exportSuccess = null
                                }
                            ) {
                                Text(stringResource(R.string.documents_view_pdf))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { 
                                    showCryptographicSignature = true
                                    exportSuccess = null
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Firmar Criptográficamente")
                            }
                        }
                    }
                }
            }
        )
    }
    
    // Diálogo de error de exportación
    if (exportError != null) {
        AlertDialog(
            onDismissRequest = { exportError = null },
            content = {
                Column {
                    Text(
                        text = "Error de Exportación",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = exportError!!)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { exportError = null }) {
                            Text(stringResource(R.string.documents_ok))
                        }
                    }
                }
            }
        )
    }
    
    // Indicador de carga para exportación
    if (isExportingPDF) {
        AlertDialog(
            onDismissRequest = { },
            content = {
                Column {
                    Text(
                        text = "Generando PDF",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(stringResource(R.string.documents_please_wait))
                    }
                }
            }
        )
    }
    
    // Visor de PDF nativo
    if (showPDFViewer && generatedPDFFile != null) {
        android.util.Log.d("DocumentsScreen", "=== MOSTRANDO PDFViewerDialog ===")
        android.util.Log.d("DocumentsScreen", "showPDFViewer: $showPDFViewer")
        android.util.Log.d("DocumentsScreen", "generatedPDFFile: ${generatedPDFFile?.name}")
        
        com.aura.substratecryptotest.ui.components.PDFViewerDialog(
            pdfFile = generatedPDFFile,
            onDismiss = { 
                android.util.Log.d("DocumentsScreen", "PDFViewerDialog cerrado")
                showPDFViewer = false 
            },
            onSignatureComplete = { signatureBitmap: android.graphics.Bitmap ->
                // Procesar la firma guardada
                android.util.Log.d("DocumentsScreen", "=== FIRMA AUTOGRÁFICA COMPLETADA ===")
                android.util.Log.d("DocumentsScreen", "Tamaño de firma: ${signatureBitmap.width}x${signatureBitmap.height}")
                android.util.Log.d("DocumentsScreen", "PDF actual: ${generatedPDFFile?.name}")
                android.util.Log.d("DocumentsScreen", "Bitácora: ${previewLogbook?.name}")
                android.util.Log.d("DocumentsScreen", "ID Bitácora: ${previewLogbook?.id}")
                // TODO: Aquí se debería generar la firma DID después del guardado
            },
               onSavePDF = { pdfFile ->
                   // Marcar que se debe guardar el PDF permanentemente
                   android.util.Log.d("DocumentsScreen", "=== GUARDADO DE PDF SOLICITADO ===")
                   android.util.Log.d("DocumentsScreen", "PDF recibido: ${pdfFile.name}")
                   android.util.Log.d("DocumentsScreen", "PDF path: ${pdfFile.absolutePath}")
                   android.util.Log.d("DocumentsScreen", "PDF existe: ${pdfFile.exists()}")
                   android.util.Log.d("DocumentsScreen", "PDF tamaño: ${pdfFile.length()} bytes")
                   android.util.Log.d("DocumentsScreen", "Bitácora asociada: ${previewLogbook?.name}")
                   android.util.Log.d("DocumentsScreen", "ID Bitácora: ${previewLogbook?.id}")
                   
                   shouldSavePDFPermanently = true
                   showPDFViewer = false
                   android.util.Log.d("DocumentsScreen", "Marcando para guardado permanente...")
               }
        )
    } else {
        android.util.Log.d("DocumentsScreen", "=== NO MOSTRANDO PDFViewerDialog ===")
        android.util.Log.d("DocumentsScreen", "showPDFViewer: $showPDFViewer")
        android.util.Log.d("DocumentsScreen", "generatedPDFFile: ${generatedPDFFile?.name}"        )
    }
    
    // Diálogo de resultado de firma criptográfica
    if (signatureResult != null) {
        AlertDialog(
            onDismissRequest = { signatureResult = null },
            content = {
                Column {
                    Text(
                        text = "Firma Criptográfica",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = signatureResult!!)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { signatureResult = null }) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        )
    }
    
    // Diálogo de firma criptográfica
    if (showCryptographicSignature && generatedPDFFile != null) {
        val logbookId = generatedPDFFile?.let { extractLogbookIdFromFileName(it.name) }
        
        PDFCryptographicSignatureDialog(
            pdfFile = generatedPDFFile,
            logbookId = logbookId,
            onDismiss = { 
                showCryptographicSignature = false
                signatureResult = null
            },
            onSignatureComplete = { signature, signatureFile ->
                signatureResult = "✅ PDF firmado criptográficamente con Sr25519\n\n" +
                    "🔑 DID URI: ${signature.didKeyUri}\n" +
                    "📍 Dirección: ${signature.signerAddress}\n" +
                    "📁 Archivo de firma: ${signatureFile.name}\n" +
                    "📋 Bitácora ID: ${signature.logbookId}\n" +
                    "👤 Firmante: ${signature.signerName}\n" +
                    "⏰ Timestamp: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(signature.timestamp))}"
                showCryptographicSignature = false
                android.util.Log.d("DocumentsScreen", "Firma criptográfica completada: ${signatureFile.name}")
            },
            onError = { errorMessage ->
                signatureResult = "❌ Error en firma criptográfica: $errorMessage"
                showCryptographicSignature = false
                android.util.Log.e("DocumentsScreen", "Error en firma criptográfica: $errorMessage")
            }
        )
    }
    
    // Diálogo de verificación de firma
    if (showSignatureVerification && generatedPDFFile != null) {
        val logbookId = generatedPDFFile?.let { extractLogbookIdFromFileName(it.name) }
        
        PDFCryptographicSignatureDialog(
            pdfFile = generatedPDFFile,
            logbookId = logbookId,
            verificationMode = true,
            onDismiss = { 
                showSignatureVerification = false
                verificationResult = null
            },
            onSignatureComplete = { signature, signatureFile ->
                // No debería ocurrir en modo verificación
            },
            onError = { errorMessage ->
                verificationResult = "❌ Error verificando firma: $errorMessage"
                showSignatureVerification = false
            }
        )
    }
    
    // Diálogo de resultado de verificación
    if (verificationResult != null) {
        AlertDialog(
            onDismissRequest = { verificationResult = null },
            content = {
                Column {
                    Text(
                        text = "Verificación de Firma",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = verificationResult!!)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { verificationResult = null }) {
                            Text("Cerrar")
                        }
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookCard(
        logbook: UserMountaineeringLogbook,
    milestoneCount: Int = 0,
        onContinueLogbook: () -> Unit,
        onViewCompleted: () -> Unit,
    onDeleteLogbook: () -> Unit,
    onExportPDF: () -> Unit,
    onViewPDF: (File) -> Unit = {}
    ) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var showDeleteDialog by remember { mutableStateOf(false) }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (logbook.isCompleted) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
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
                        imageVector = Icons.Default.Hiking,
                        contentDescription = "Bitácora",
                        modifier = Modifier.size(24.dp),
                        tint = if (logbook.isCompleted) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = logbook.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (logbook.isCompleted) 
                                MaterialTheme.colorScheme.onPrimaryContainer 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${logbook.club} - ${logbook.association}",
                            fontSize = 12.sp,
                            color = if (logbook.isCompleted) 
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    Text(
                        text = "📊 $milestoneCount milestones",
                        fontSize = 11.sp,
                        color = if (logbook.isCompleted) 
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    
                    // Estado de la bitácora
                    if (logbook.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completada",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "En progreso",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Botón de eliminar
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar bitácora",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Información adicional
                Text(
                    text = "📍 ${logbook.location}",
                    fontSize = 12.sp,
                    color = if (logbook.isCompleted) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "👥 ${logbook.participantsCount} participantes",
                    fontSize = 12.sp,
                    color = if (logbook.isCompleted) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = "📅 ${dateFormat.format(Date(logbook.startDate))} - ${dateFormat.format(Date(logbook.endDate))}",
                    fontSize = 12.sp,
                    color = if (logbook.isCompleted) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Botones de acción según el estado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (logbook.isCompleted) {
                        // Bitácoras completadas: Vista rápida, Generar PDF y Ver PDF
                        Button(
                            onClick = onViewCompleted,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.documents_quick_view))
                        }
                        
                        Button(
                            onClick = onExportPDF,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.documents_generate_pdf))
                        }
                        
                        // Botón para ver PDF si ya está generado
                        if (logbook.pdfPath != null) {
                            Button(
                                onClick = {
                                    android.util.Log.d("DocumentsScreen", "=== INTENTANDO ABRIR PDF ===")
                                    android.util.Log.d("DocumentsScreen", "PDF Path: ${logbook.pdfPath}")
                                    
                                    // Buscar el archivo PDF generado
                                    val pdfFile = File(logbook.pdfPath)
                                    android.util.Log.d("DocumentsScreen", "PDF File exists: ${pdfFile.exists()}")
                                    android.util.Log.d("DocumentsScreen", "PDF File path: ${pdfFile.absolutePath}")
                                    
                                    if (pdfFile.exists()) {
                                        android.util.Log.d("DocumentsScreen", "Llamando onViewPDF con archivo: ${pdfFile.name}")
                                        onViewPDF(pdfFile)
                                    } else {
                                        android.util.Log.e("DocumentsScreen", "ERROR: Archivo PDF no encontrado en ${pdfFile.absolutePath}")
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.documents_view_pdf))
                            }
                        }
                    } else {
                        // Bitácoras activas: Vista rápida y Editar
                        Button(
                            onClick = onViewCompleted,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.documents_quick_view))
                        }
                        
                        Button(
                            onClick = onContinueLogbook,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.documents_edit))
                        }
                    }
                }
            }
        }
        
        // Diálogo de confirmación de eliminación
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                content = {
                    Column {
                        Text(
                            text = "Eliminar Bitácora",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.documents_delete_confirm, logbook.name))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(stringResource(R.string.documents_cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = {
                                    onDeleteLogbook()
                                    showDeleteDialog = false
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text(stringResource(R.string.documents_delete))
                            }
                        }
                    }
                }
            )
        }
    } // Cierre de LanguageAware
}

@Composable
fun DocumentTypeCard(
    documentType: DocumentTypeData,
    onCreate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = documentType.icon,
                    contentDescription = documentType.title,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = documentType.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = documentType.description,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Campos del documento
            Text(
                text = "Campos requeridos:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            documentType.fields.forEach { field ->
                Text(
                    text = "• $field",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear ${documentType.title}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogbookCard(
    logbook: UserMountaineeringLogbook,
    milestoneCount: Int = 0,
    onContinueLogbook: () -> Unit,
    onViewCompleted: () -> Unit,
    onDeleteLogbook: () -> Unit,
    onExportPDF: () -> Unit,
    onViewPDF: (File) -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${logbook.club} - ${logbook.association}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${dateFormat.format(Date(logbook.startDate))} - ${dateFormat.format(Date(logbook.endDate))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (logbook.isCompleted) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Completada",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Milestones: $milestoneCount",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Participantes: ${logbook.participantsCount}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!logbook.isCompleted) {
                    Button(
                        onClick = onContinueLogbook,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Continuar")
                    }
                } else {
                    Button(
                        onClick = onViewCompleted,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver")
                    }
                }
                
                Button(
                    onClick = onExportPDF,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF")
                }
                
                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            if (logbook.pdfPath != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onViewPDF(File(logbook.pdfPath)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver PDF")
                }
            }
        }
    }
    
    // Diálogo de confirmación de eliminación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            content = {
                Column {
                    Text(
                        text = "Eliminar Bitácora",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("¿Estás seguro de que quieres eliminar esta bitácora?")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancelar")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onDeleteLogbook()
                                showDeleteDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Eliminar")
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun ExportedPDFCard(
    pdfFile: java.io.File,
    logbook: UserMountaineeringLogbook,
    onViewPDF: (java.io.File) -> Unit,
    onSignPDF: (java.io.File) -> Unit,
    onVerifySignature: (java.io.File) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val pdfSignatureManager = remember { com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager(context) }
    var hasSignature by remember { mutableStateOf(false) }
    
    // Verificar si el PDF tiene firma DID
    LaunchedEffect(pdfFile) {
        try {
            hasSignature = pdfSignatureManager.hasCorrespondingSignature(pdfFile)
        } catch (e: Exception) {
            android.util.Log.e("ExportedPDFCard", "Error verificando firma", e)
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pdfFile.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Bitácora: ${logbook.name}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(pdfFile.length() / 1024)} KB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (hasSignature) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Firmado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de ver PDF
                Button(
                    onClick = { onViewPDF(pdfFile) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ver", fontSize = 12.sp)
                }
                
                if (hasSignature) {
                    // Botón de verificar firma
                    Button(
                        onClick = { onVerifySignature(pdfFile) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Verificar", fontSize = 12.sp)
                    }
                } else {
                    // Botón de firma para PDFs sin firmar
                    Button(
                        onClick = { onSignPDF(pdfFile) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Firmar DID", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentFileCard(
    pdfFile: java.io.File,
    onViewPDF: (java.io.File) -> Unit,
    onSignPDF: (java.io.File) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Description,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pdfFile.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${(pdfFile.length() / 1024)} KB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onViewPDF(pdfFile) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "Ver PDF",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                IconButton(
                    onClick = { onSignPDF(pdfFile) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Firmar PDF",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
