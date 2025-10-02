package com.aura.substratecryptotest.ui.screens

import androidx.fragment.app.FragmentActivity
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
import com.aura.substratecryptotest.data.WalletState
import com.aura.substratecryptotest.ui.lifecycle.rememberCurrentActivity
import com.aura.substratecryptotest.ui.lifecycle.OnActivityChange
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.substratecryptotest.ui.viewmodels.DashboardViewModel
import com.aura.substratecryptotest.ui.components.AccountSwitchModal
import com.aura.substratecryptotest.ui.components.AuraBottomNavigationBar
import androidx.compose.ui.res.stringResource
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.ui.context.LanguageAware
import com.aura.substratecryptotest.data.wallet.WalletStateManager

/**
 * Dashboard principal de la wallet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToWalletInfo: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDID: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToRWA: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    
    // ✅ Monitorear wallet activa directamente en DashboardScreen
    val walletStateManager = remember { WalletStateManager.getInstance(activity) }
    var currentWalletState by remember { mutableStateOf<WalletState?>(null) }
    
    // ✅ Usar ActivityLifecycleComposables para monitorear activity activa
    // val currentActivity = rememberCurrentActivity() // TODO: Implementar cuando esté listo
    
    // Inicializar ViewModel
    LaunchedEffect(Unit) {
        android.util.Log.d("DashboardScreen", "=== INICIALIZANDO DASHBOARDSCREEN ===")
        viewModel.initialize(activity)
    }
    
    // ✅ Monitorear cambios de activity usando OnActivityChange
    // OnActivityChange { activity -> // TODO: Implementar cuando esté listo
    //     android.util.Log.d("DashboardScreen", "=== ACTIVITY CAMBIÓ VIA OnActivityChange ===")
    //     android.util.Log.d("DashboardScreen", "Nueva activity: ${activity?.javaClass?.simpleName}")
    //     
    //     if (activity != null) {
    //         android.util.Log.d("DashboardScreen", "✅ Activity detectada: ${activity.javaClass.simpleName}")
    //     }
    // }
    
    // ✅ Monitorear cambios en la wallet activa
    LaunchedEffect(Unit) {
        walletStateManager.currentWallet.observeForever { walletState ->
            android.util.Log.d("DashboardScreen", "=== WALLET STATE CAMBIÓ EN DASHBOARDSCREEN ===")
            android.util.Log.d("DashboardScreen", "Nuevo estado: $walletState")
            
            currentWalletState = walletState
            
            when (walletState) {
                is WalletState.Created -> {
                    val wallet = walletState.wallet
                    android.util.Log.d("DashboardScreen", "✅ Wallet activa detectada: ${wallet.name}")
                    android.util.Log.d("DashboardScreen", "Dirección: ${wallet.address}")
                    android.util.Log.d("DashboardScreen", "DID KILT: ${wallet.kiltDid}")
                    
                    // Refrescar ViewModel cuando cambie la wallet
                    viewModel.refreshWalletInfo()
                }
                is WalletState.None -> {
                    android.util.Log.d("DashboardScreen", "⚠️ No hay wallet activa")
                }
                is WalletState.Error -> {
                    android.util.Log.e("DashboardScreen", "❌ Error en wallet: ${walletState.message}")
                }
                else -> {
                    android.util.Log.d("DashboardScreen", "🔄 Estado de wallet: $walletState")
                }
            }
        }
    }
    
    // Log para verificar datos del usuario
    LaunchedEffect(uiState) {
        android.util.Log.d("DashboardScreen", "=== DATOS EN DASHBOARDSCREEN ===")
        android.util.Log.d("DashboardScreen", "Usuario actual: ${uiState.currentUser}")
        android.util.Log.d("DashboardScreen", "Wallet principal: ${uiState.walletInfo?.name}")
        android.util.Log.d("DashboardScreen", "Dirección Polkadot: ${uiState.polkadotAddress}")
        android.util.Log.d("DashboardScreen", "Cantidad de wallets disponibles: ${uiState.availableWallets.size}")
        android.util.Log.d("DashboardScreen", "Estado de wallet actual: $currentWalletState")
        // android.util.Log.d("DashboardScreen", "Activity actual detectada: ${currentActivity?.javaClass?.simpleName}") // TODO: Implementar cuando esté listo
    }
    LanguageAware {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.dashboard_title)) },
                    actions = {
                        // Botón de switch accounts
                        IconButton(onClick = { viewModel.showAccountSwitchModal() }) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = stringResource(R.string.dashboard_settings))
                        }
                        
                        // Botón de cerrar sesión
                        IconButton(onClick = {
                            android.util.Log.d("DashboardScreen", "=== BOTÓN LOGOUT PRESIONADO ===")
                            android.util.Log.d("DashboardScreen", "Timestamp: ${System.currentTimeMillis()}")
                            android.util.Log.d("DashboardScreen", "Stack trace: ${android.util.Log.getStackTraceString(Exception("Logout button pressed from:"))}")
                            
                            viewModel.logoutCurrentUser()
                            onLogout()
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                        }
                        
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                        }
                    }
                )
            },
        bottomBar = {
            AuraBottomNavigationBar(
                currentRoute = "dashboard",
                onNavigateToWallet = { /* Ya estamos aquí */ },
                onNavigateToIdentity = onNavigateToDID,
                onNavigateToCredentials = onNavigateToCredentials,
                onNavigateToDocuments = onNavigateToDocuments,
                onNavigateToRWA = onNavigateToRWA
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Saldo principal
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Saldo Total",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = if (uiState.walletInfo != null) "1,250.50 USDC" else "0.00 USDC",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = uiState.walletInfo?.name ?: "Crea una wallet para comenzar",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        // Mostrar dirección de Polkadot cuando no hay wallet
                        if (uiState.walletInfo == null) {
                            Text(
                                text = "Dirección Polkadot: ${uiState.polkadotAddress ?: "No disponible"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // Acciones rápidas del monedero
            item {
                Text(
                    text = "Acciones Rápidas",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickActionButton(
                        title = "Recibir",
                        icon = Icons.Default.CallReceived,
                        onClick = { /* TODO: Implementar recibir */ },
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionButton(
                        title = "Enviar",
                        icon = Icons.Default.Send,
                        onClick = { /* TODO: Implementar enviar */ },
                        modifier = Modifier.weight(1f)
                    )
                    
                    QuickActionButton(
                        title = "Intercambiar",
                        icon = Icons.Default.SwapHoriz,
                        onClick = { /* TODO: Implementar intercambiar */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Información de wallet
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Detalles de la Wallet",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Ver información completa",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        IconButton(onClick = onNavigateToWalletInfo) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Ver detalles"
                            )
                        }
                    }
                }
            }
            
            // Estado de desarrollo
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🚧 Modo Desarrollo",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        
                        Text(
                            text = "Esta es una versión de desarrollo.\n" +
                                    "Las funcionalidades están en construcción.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
    
    // Modal de switch de cuentas
    if (uiState.showAccountSwitchModal) {
        AccountSwitchModal(
            availableWallets = uiState.availableWallets,
            currentWalletName = uiState.walletInfo?.name,
            onWalletSelected = { walletName ->
                viewModel.switchToWallet(walletName)
            },
            onWalletDeleted = { walletName ->
                viewModel.deleteWallet(walletName)
            },
            onWalletRenamed = { oldName, newName ->
                viewModel.renameWallet(oldName, newName)
            },
            onDismiss = {
                viewModel.hideAccountSwitchModal()
            },
            onRefresh = {
                viewModel.refreshAvailableAccounts()
            }
        )
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
