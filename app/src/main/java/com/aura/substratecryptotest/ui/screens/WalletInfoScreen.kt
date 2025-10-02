package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.substratecryptotest.ui.viewmodels.DashboardViewModel
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.UserDatabaseManager
import com.aura.substratecryptotest.data.UserWallet
import androidx.compose.runtime.collectAsState
import com.aura.substratecryptotest.ui.context.LanguageAware

/**
 * Pantalla de información detallada de la wallet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletInfoScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }
    val userDatabaseManager = remember { UserDatabaseManager(context, userManager) }
    
    // Estado del usuario actual
    var currentUser by remember { mutableStateOf<String?>(null) }
    var userWallets by remember { mutableStateOf<List<UserWallet>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Cargar datos del usuario actual
    LaunchedEffect(context) {
        try {
            val user = userManager.getCurrentUser()
            if (user != null) {
                currentUser = user.id
                android.util.Log.d("WalletInfoScreen", "Usuario actual: ${user.name} (${user.id})")
                
                // Cargar wallets del usuario
                val userDb = userDatabaseManager.getUserDatabase(user.id)
                val wallets = userDb.userWalletDao().getWalletsByUser(user.id)
                userWallets = wallets
                
                android.util.Log.d("WalletInfoScreen", "Wallets encontradas: ${wallets.size}")
                wallets.forEach { wallet: UserWallet ->
                    android.util.Log.d("WalletInfoScreen", "Wallet: ${wallet.name} - ${wallet.address}")
                }
            } else {
                android.util.Log.w("WalletInfoScreen", "No hay usuario actual")
            }
        } catch (e: Exception) {
            android.util.Log.e("WalletInfoScreen", "Error cargando datos del usuario", e)
        } finally {
            isLoading = false
        }
    }
    
    LanguageAware {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Información de Wallet") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (currentUser == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⚠️ No hay usuario activo",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Debes iniciar sesión para ver la información de tu wallet",
                            fontSize = 16.sp
                        )
                        
                        Button(onClick = onNavigateBack) {
                            Text("Volver")
                        }
                    }
                }
            } else if (userWallets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "💳 No hay wallets",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "No tienes wallets creadas para este usuario",
                            fontSize = 16.sp
                        )
                        
                        Button(onClick = onNavigateBack) {
                            Text("Volver")
                        }
                    }
                }
            } else {
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
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "👤 Usuario Actual",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                
                                Text(
                                    text = "ID: $currentUser",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                
                                Text(
                                    text = "Wallets: ${userWallets.size}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    
                    // Lista de wallets
                    item {
                        Text(
                            text = "Mis Wallets",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(userWallets) { wallet ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = wallet.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Icon(
                                        Icons.Default.AccountBalanceWallet,
                                        contentDescription = "Wallet",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Dirección:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                
                                Text(
                                    text = wallet.address,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                                
                                Text(
                                    text = "Tipo: ${wallet.cryptoType}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                
                                Text(
                                    text = "Ruta: ${wallet.derivationPath}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                
                                Text(
                                    text = "Creada: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(wallet.createdAt))}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}




