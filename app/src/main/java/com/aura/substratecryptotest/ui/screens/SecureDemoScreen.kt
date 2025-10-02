package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.ui.viewmodels.SecureWalletViewModel
import com.aura.substratecryptotest.ui.viewmodels.SecureDocumentViewModel
import com.aura.substratecryptotest.ui.viewmodels.UserManagementViewModel

/**
 * Pantalla de demostración del sistema seguro
 * Muestra las características de SecureUserRepository
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureDemoScreen(
    onNavigateBack: () -> Unit,
    secureWalletViewModel: SecureWalletViewModel,
    secureDocumentViewModel: SecureDocumentViewModel,
    userManagementViewModel: UserManagementViewModel
) {
    val context = LocalContext.current
    val walletUiState by secureWalletViewModel.uiState.collectAsState()
    val documentUiState by secureDocumentViewModel.uiState.collectAsState()
    
    // Inicializar ViewModels
    LaunchedEffect(Unit) {
        userManagementViewModel.initialize(context)
        secureWalletViewModel.loadUserWallets()
        secureDocumentViewModel.loadUserDocuments()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sistema Seguro Demo") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("← Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "🔐 Sistema Seguro Activado",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Android KeyStore + Autenticación Biométrica",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "👤 Gestión de Usuarios",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                // Crear usuario de prueba
                                userManagementViewModel.createNewUser("Usuario Demo")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Crear Usuario Demo")
                        }
                        
                        Button(
                            onClick = {
                                userManagementViewModel.loadRegisteredUsers()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cargar Usuarios")
                        }
                    }
                }
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💼 Wallets Seguras",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (walletUiState.isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text("Wallets: ${walletUiState.wallets.size}")
                            
                            walletUiState.wallets.forEach { wallet ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = wallet.name,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "ID: ${wallet.id.take(8)}...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Tipo: ${wallet.cryptoType}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Protegida: ${if (wallet.biometricProtected) "✅" else "❌"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (walletUiState.error != null) {
                            Text(
                                text = "Error: ${walletUiState.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📄 Documentos Seguros",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (documentUiState.isLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text("Documentos: ${documentUiState.documents.size}")
                            
                            documentUiState.documents.forEach { document ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = document.documentType,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Hash: ${document.documentHash.take(20)}...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "Usuario: ${document.userId.take(8)}...",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (documentUiState.error != null) {
                            Text(
                                text = "Error: ${documentUiState.error}",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🔒 Características de Seguridad",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val features = listOf(
                            "✅ Android KeyStore (Hardware)",
                            "✅ Autenticación Biométrica",
                            "✅ Encriptación AES-256-GCM",
                            "✅ Aislamiento por Usuario",
                            "✅ Base de Datos Separada",
                            "✅ Limpieza Automática"
                        )
                        
                        features.forEach { feature ->
                            Text(
                                text = feature,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
