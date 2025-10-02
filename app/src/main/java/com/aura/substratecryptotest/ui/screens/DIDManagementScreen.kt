package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.data.wallet.WalletStateManager
import com.aura.substratecryptotest.data.WalletState

/**
 * Pantalla de gestión de identidad DID
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DIDManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDIDDashboard: () -> Unit,
    activity: FragmentActivity
) {
    // ✅ Monitorear wallet activa igual que DIDDashboardScreen
    val walletStateManager = remember { WalletStateManager.getInstance(activity) }
    var currentWalletState by remember { mutableStateOf<WalletState?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // ✅ Monitorear cambios en la wallet activa
    LaunchedEffect(Unit) {
        walletStateManager.currentWallet.observeForever { walletState ->
            android.util.Log.d("DIDManagementScreen", "=== WALLET STATE CAMBIÓ EN DIDMANAGEMENTSCREEN ===")
            android.util.Log.d("DIDManagementScreen", "Nuevo estado: $walletState")
            
            currentWalletState = walletState
            isLoading = false
            
            when (walletState) {
                is WalletState.Created -> {
                    val wallet = walletState.wallet
                    android.util.Log.d("DIDManagementScreen", "✅ Wallet activa detectada: ${wallet.name}")
                    android.util.Log.d("DIDManagementScreen", "Dirección: ${wallet.address}")
                    android.util.Log.d("DIDManagementScreen", "DID KILT: ${wallet.kiltDid}")
                }
                is WalletState.None -> {
                    android.util.Log.d("DIDManagementScreen", "⚠️ No hay wallet activa")
                }
                is WalletState.Error -> {
                    android.util.Log.e("DIDManagementScreen", "❌ Error en wallet: ${walletState.message}")
                }
                else -> {
                    android.util.Log.d("DIDManagementScreen", "🔄 Estado de wallet: $walletState")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión DID") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "🆔 Gestión DID",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Gestión de identidad digital con KILT Protocol",
                fontSize = 16.sp
            )
            
            // Estado de carga
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Información de wallet activa
            val walletState = currentWalletState
            when (walletState) {
                is WalletState.Created -> {
                    val wallet = walletState.wallet
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Wallet,
                                    contentDescription = "Wallet",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Wallet Activa",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Text(
                                text = "Nombre: ${wallet.name}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = "Dirección: ${wallet.address}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            
                            if (wallet.kiltDid != null && wallet.kiltDid.isNotBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = "DID Verificado",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "DID: ${wallet.kiltDid}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            } else {
                                Text(
                                    text = "⚠️ No hay DID derivado",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                is WalletState.None -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No hay wallet activa",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Crea o importa una wallet primero",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                is WalletState.Error -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Error en wallet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = walletState.message,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
                
                else -> {
                    // Estado desconocido
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
                            Text(
                                text = "Estado desconocido",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    onClick = onNavigateToDIDDashboard,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ver Dashboard DID")
                }
                
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Volver")
                }
            }
        }
    }
}
