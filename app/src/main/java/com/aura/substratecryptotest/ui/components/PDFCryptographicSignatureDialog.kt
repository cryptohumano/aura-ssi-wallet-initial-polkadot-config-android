package com.aura.substratecryptotest.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aura.substratecryptotest.crypto.pdf.PDFSignatureManager
import com.aura.substratecryptotest.security.SecureWalletFlowManager
import kotlinx.coroutines.launch
import java.io.File

/**
 * Componente para firma criptográfica de PDFs
 * Integra la funcionalidad de firma Sr25519 con cuenta derivada //did//0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PDFCryptographicSignatureDialog(
    pdfFile: File?,
    logbookId: Long? = null,
    verificationMode: Boolean = false,
    onDismiss: () -> Unit,
    onSignatureComplete: (PDFSignatureManager.PDFSignature, File) -> Unit,
    onError: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(pdfFile != null) }
    var isLoading by remember { mutableStateOf(false) }
    var signatureResult by remember { mutableStateOf<PDFSignatureManager.SignatureResult?>(null) }
    var verificationResult by remember { mutableStateOf<PDFSignatureManager.VerificationResult?>(null) }
    var showVerification by remember { mutableStateOf(false) }
    var showSignatureHistory by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val pdfSignatureManager = remember { PDFSignatureManager(context) }
    
    // Mnemonic de prueba - en producción debería venir del SecureWalletManager
    val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
    
    LaunchedEffect(pdfFile, verificationMode) {
        showDialog = pdfFile != null
        if (verificationMode && pdfFile != null) {
            showVerification = true
        }
    }
    
    if (showDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Firma Criptográfica PDF",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Información del PDF
                    pdfFile?.let { file ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "📄 Archivo PDF",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nombre: ${file.name}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Tamaño: ${file.length() / 1024} KB",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Ubicación: ${file.parent}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Botones de acción
                        if (!showVerification && !showSignatureHistory) {
                            // Botón de firma
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val result = pdfSignatureManager.signPDF(
                                                pdfFile = file,
                                                mnemonic = testMnemonic,
                                                signerName = "Usuario Test",
                                                logbookId = logbookId ?: extractLogbookIdFromFileName(file.name) ?: 0L
                                            )
                                            signatureResult = result
                                            
                                            when (result) {
                                                is PDFSignatureManager.SignatureResult.Success -> {
                                                    onSignatureComplete(result.signature, result.signatureFile)
                                                }
                                                is PDFSignatureManager.SignatureResult.Error -> {
                                                    onError(result.message)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            onError("Error inesperado: ${e.message}")
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Icon(Icons.Default.Security, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Firmar con Sr25519")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Botón de verificación
                            OutlinedButton(
                                onClick = {
                                    showVerification = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Verificar Firma Existente")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Botón de historial de firmas
                            OutlinedButton(
                                onClick = {
                                    showSignatureHistory = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.History, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ver Historial de Firmas")
                            }
                        } else if (showVerification) {
                            // Panel de verificación
                            VerificationPanel(
                                pdfFile = file,
                                pdfSignatureManager = pdfSignatureManager,
                                onVerificationComplete = { result ->
                                    verificationResult = result
                                },
                                onBack = {
                                    showVerification = false
                                    verificationResult = null
                                }
                            )
                        } else if (showSignatureHistory) {
                            // Panel de historial de firmas
                            SignatureHistoryPanel(
                                pdfSignatureManager = pdfSignatureManager,
                                onBack = {
                                    showSignatureHistory = false
                                },
                                onSignatureSelected = { signature, signatureFile ->
                                    // Verificar la firma seleccionada
                                    scope.launch {
                                        try {
                                            val result = pdfSignatureManager.verifyPDFSignature(
                                                pdfFile = file,
                                                signatureFile = signatureFile
                                            )
                                            verificationResult = result
                                            showSignatureHistory = false
                                            showVerification = true
                                        } catch (e: Exception) {
                                            onError("Error verificando firma: ${e.message}")
                                        }
                                    }
                                }
                            )
                        }
                        
                        // Mostrar resultado de firma
                        signatureResult?.let { result ->
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            when (result) {
                                is PDFSignatureManager.SignatureResult.Success -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "✅ Firma Exitosa",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "DID URI: ${result.signature.didKeyUri}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Dirección: ${result.signature.signerAddress}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Archivo: ${result.signatureFile.name}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                
                                is PDFSignatureManager.SignatureResult.Error -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Error,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD32F2F)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "❌ Error en Firma",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD32F2F)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = result.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFD32F2F)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Mostrar resultado de verificación
                        verificationResult?.let { result ->
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            when (result) {
                                is PDFSignatureManager.VerificationResult.Valid -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Verified,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "✅ Firma Válida",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "Firmante: ${result.signerInfo.address}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Algoritmo: ${result.signerInfo.algorithm}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Timestamp: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(result.signerInfo.timestamp))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Button(
                                                onClick = {
                                                    // Mostrar información detallada en logs
                                                    android.util.Log.d("PDFSignatureDialog", "📋 Información detallada de la firma:")
                                                    android.util.Log.d("PDFSignatureDialog", "   DID URI: ${result.signature.didKeyUri}")
                                                    android.util.Log.d("PDFSignatureDialog", "   Dirección: ${result.signature.signerAddress}")
                                                    android.util.Log.d("PDFSignatureDialog", "   Hash: ${result.signature.hashes.firstOrNull()}")
                                                    android.util.Log.d("PDFSignatureDialog", "   JWS: ${result.signature.jws.take(50)}...")
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ver Detalles en Logs")
                                            }
                                        }
                                    }
                                }
                                
                                is PDFSignatureManager.VerificationResult.Invalid -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Cancel,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD32F2F)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "❌ Firma Inválida",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD32F2F)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "Razón: ${result.reason}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFD32F2F),
                                                fontWeight = FontWeight.Medium
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "Posibles causas:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD32F2F)
                                            )
                                            
                                            Text(
                                                text = "• El archivo PDF fue modificado después de la firma\n• El archivo de firma (.didsign) está corrupto\n• Los nombres de archivo no coinciden\n• La firma criptográfica no es válida",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFD32F2F)
                                            )
                                        }
                                    }
                                }
                                
                                is PDFSignatureManager.VerificationResult.Error -> {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Error,
                                                    contentDescription = null,
                                                    tint = Color(0xFFD32F2F)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "❌ Error en Verificación",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD32F2F)
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "Error: ${result.message}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFD32F2F),
                                                fontWeight = FontWeight.Medium
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "Solución:",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD32F2F)
                                            )
                                            
                                            Text(
                                                text = "• Verifica que ambos archivos existan\n• Asegúrate de que el archivo .didsign no esté corrupto\n• Intenta regenerar la firma del PDF\n• Revisa los logs para más detalles",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFFD32F2F)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerificationPanel(
    pdfFile: File,
    pdfSignatureManager: PDFSignatureManager,
    onVerificationComplete: (PDFSignatureManager.VerificationResult) -> Unit,
    onBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var signatureFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedSignatureFile by remember { mutableStateOf<File?>(null) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        // Buscar archivos de firma en el mismo directorio
        val directory = pdfFile.parentFile ?: return@LaunchedEffect
        
        // Buscar la firma correspondiente al PDF actual
        val correspondingSignature = pdfSignatureManager.findCorrespondingSignatureFile(pdfFile)
        if (correspondingSignature != null) {
            // Si existe la firma correspondiente, seleccionarla automáticamente
            selectedSignatureFile = correspondingSignature
            android.util.Log.d("PDFSignatureDialog", "✅ Firma correspondiente encontrada: ${correspondingSignature.name}")
        } else {
            // Si no existe, mostrar todas las firmas disponibles pero con advertencia
            android.util.Log.w("PDFSignatureDialog", "⚠️ No hay firma correspondiente para: ${pdfFile.name}")
        }
        
        signatureFiles = pdfSignatureManager.listSignaturesInDirectory(directory)
    }
    
    Column {
        // Botón de regreso
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Regresar")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Seleccionar Archivo de Firma",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (signatureFiles.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se encontraron archivos de firma (.didsign)",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Mostrar advertencia si no hay firma correspondiente
            if (selectedSignatureFile == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "⚠️ No se encontró firma correspondiente para este PDF.\nSelecciona manualmente un archivo de firma.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF856404)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            LazyColumn(
                modifier = Modifier.height(200.dp)
            ) {
                items(signatureFiles) { signatureFile ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            selectedSignatureFile = signatureFile
                        }
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = signatureFile.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                // Mostrar indicador si es la firma correspondiente
                                val isCorrespondingSignature = signatureFile.nameWithoutExtension == pdfFile.nameWithoutExtension
                                if (isCorrespondingSignature) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Firma correspondiente",
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "✓ Correspondiente",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Text(
                                text = "Tamaño: ${signatureFile.length() / 1024} KB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Modificado: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(signatureFile.lastModified()))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
                            Button(
                                onClick = {
                                    selectedSignatureFile?.let { signatureFile ->
                                        scope.launch {
                                            isLoading = true
                                            try {
                                                android.util.Log.d("PDFSignatureDialog", "🔍 Iniciando verificación de firma")
                                                android.util.Log.d("PDFSignatureDialog", "📄 PDF: ${pdfFile.name}")
                                                android.util.Log.d("PDFSignatureDialog", "📁 Archivo de firma: ${signatureFile.name}")
                                                
                                                val result = pdfSignatureManager.verifyPDFSignature(
                                                    pdfFile = pdfFile,
                                                    signatureFile = signatureFile
                                                )
                                                
                                                android.util.Log.d("PDFSignatureDialog", "✅ Resultado de verificación: ${result::class.simpleName}")
                                                onVerificationComplete(result)
                                            } catch (e: Exception) {
                                                android.util.Log.e("PDFSignatureDialog", "❌ Error en verificación: ${e.message}", e)
                                                onVerificationComplete(
                                                    PDFSignatureManager.VerificationResult.Error("Error inesperado: ${e.message}")
                                                )
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading && selectedSignatureFile != null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verificar Firma")
            }
        }
    }
}

/**
 * Componente para mostrar el historial completo de firmas almacenadas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignatureHistoryPanel(
    pdfSignatureManager: PDFSignatureManager,
    onBack: () -> Unit,
    onSignatureSelected: (PDFSignatureManager.PDFSignature, File) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var allSignatures by remember { mutableStateOf<List<Pair<PDFSignatureManager.PDFSignature, File>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedLogbookId by remember { mutableStateOf<Long?>(null) }
    
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Cargar todas las firmas disponibles
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val logbooksDir = context.getExternalFilesDir("logbooks")
                if (logbooksDir != null) {
                    val signatureFiles = pdfSignatureManager.listSignaturesInDirectory(logbooksDir)
                    val signaturesWithFiles = signatureFiles.mapNotNull { signatureFile ->
                        try {
                            val signature = pdfSignatureManager.loadSignatureFile(signatureFile)
                            if (signature != null) signature to signatureFile else null
                        } catch (e: Exception) {
                            android.util.Log.w("SignatureHistoryPanel", "Error cargando firma ${signatureFile.name}: ${e.message}")
                            null
                        }
                    }.sortedByDescending { (signature, _) -> signature.timestamp }
                    
                    allSignatures = signaturesWithFiles
                    android.util.Log.d("SignatureHistoryPanel", "📋 Cargadas ${signaturesWithFiles.size} firmas")
                }
            } catch (e: Exception) {
                android.util.Log.e("SignatureHistoryPanel", "❌ Error cargando firmas: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // Filtrar firmas según búsqueda y logbook seleccionado
    val filteredSignatures = allSignatures.filter { (signature, _) ->
        val matchesSearch = searchQuery.isEmpty() || 
            signature.signerName.contains(searchQuery, ignoreCase = true) ||
            signature.pdfFileName.contains(searchQuery, ignoreCase = true) ||
            signature.signerAddress.contains(searchQuery, ignoreCase = true)
        
        val matchesLogbook = selectedLogbookId == null || signature.logbookId == selectedLogbookId
        
        matchesSearch && matchesLogbook
    }
    
    Column {
        // Header con controles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Regresar")
            }
            
            Text(
                text = "📋 Historial de Firmas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${filteredSignatures.size} firmas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Barra de búsqueda
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Buscar firmas...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Filtro por bitácora
        val uniqueLogbookIds = allSignatures.map { (signature, _) -> signature.logbookId }.distinct().sorted()
        if (uniqueLogbookIds.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = { selectedLogbookId = null },
                    label = { Text("Todas") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedLogbookId == null) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
                
                uniqueLogbookIds.take(5).forEach { logbookId ->
                    AssistChip(
                        onClick = { selectedLogbookId = logbookId },
                        label = { Text("Bitácora $logbookId") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedLogbookId == logbookId) 
                                MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Lista de firmas
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (filteredSignatures.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "No hay firmas almacenadas" else "No se encontraron firmas",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSignatures) { (signature, signatureFile) ->
                    SignatureHistoryCard(
                        signature = signature,
                        signatureFile = signatureFile,
                        onClick = { onSignatureSelected(signature, signatureFile) }
                    )
                }
            }
        }
    }
}

/**
 * Tarjeta para mostrar información de una firma en el historial
 */
@Composable
private fun SignatureHistoryCard(
    signature: PDFSignatureManager.PDFSignature,
    signatureFile: File,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header con información principal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = signature.signerName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bitácora ${signature.logbookId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Icon(
                    Icons.Default.Verified,
                    contentDescription = "Firma verificada",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Información del archivo PDF
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = signature.pdfFileName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Dirección del firmante
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = signature.signerAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Footer con timestamp y tamaño
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(signature.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = formatFileSize(signatureFile.length()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Formatea un timestamp en formato legible
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return formatter.format(date)
}

/**
 * Formatea el tamaño de archivo en formato legible
 */
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}

/**
 * Extrae el ID de bitácora del nombre del archivo PDF
 */
private fun extractLogbookIdFromFileName(fileName: String): Long? {
    return try {
        // Patrón: bitacora_[nombre]_[id].pdf
        val regex = Regex("bitacora_.*_(\\d+)\\.pdf")
        val matchResult = regex.find(fileName)
        matchResult?.groupValues?.get(1)?.toLong()
    } catch (e: Exception) {
        android.util.Log.w("PDFCryptographicSignatureDialog", "No se pudo extraer ID de bitácora del nombre: $fileName")
        null
    }
}
