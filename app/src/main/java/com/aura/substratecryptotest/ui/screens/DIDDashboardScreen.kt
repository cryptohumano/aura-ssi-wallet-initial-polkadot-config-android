package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.aura.substratecryptotest.data.wallet.WalletStateManager
import com.aura.substratecryptotest.data.WalletState
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.ui.context.LanguageAware
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.UserDatabaseManager
import com.aura.substratecryptotest.data.UserKiltIdentity
import com.aura.substratecryptotest.ui.components.ProfileImageSelector
import com.aura.substratecryptotest.ui.components.AuraBottomNavigationBar
import com.aura.substratecryptotest.ui.viewmodels.DIDDashboardViewModel
import com.aura.substratecryptotest.ui.viewmodels.DIDInfo

data class InteractionData(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: String,
    val icon: ImageVector
)

@Composable
fun DIDInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(text)
    }
}

@Composable
fun ScoreStat(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun CompactStatisticItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DIDInfoModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    didInfo: DIDInfo?,
    walletState: com.aura.substratecryptotest.data.WalletState?
) {
    if (isOpen && didInfo != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Información DID Completa",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Direcciones sin path (base)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "📍 Direcciones Base (sin path)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Obtener direcciones base desde metadata de la wallet
                                val addresses = if (walletState is com.aura.substratecryptotest.data.WalletState.Created) {
                                    walletState.wallet.metadata["addresses"] as? Map<*, *>
                                } else null
                                
                                DIDInfoRow("KILT Base", addresses?.get("KILT")?.toString() ?: "No disponible")
                                DIDInfoRow("Polkadot Base", addresses?.get("POLKADOT")?.toString() ?: "No disponible")
                                DIDInfoRow("Kusama Base", addresses?.get("KUSAMA")?.toString() ?: "No disponible")
                                DIDInfoRow("Substrate Base", didInfo.walletAddress)
                            }
                        }
                    }
                    
                    // Direcciones con path //did//0
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "🔐 Direcciones DID (//did//0)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Formatear DIDs con prefijos correctos
                                DIDInfoRow("did:kilt", didInfo.did)
                                
                                // Obtener direcciones DID derivadas si están disponibles
                                val didAddresses = if (walletState is com.aura.substratecryptotest.data.WalletState.Created) {
                                    walletState.wallet.metadata["didAddresses"] as? Map<*, *>
                                } else null
                                
                                // Formatear DIDs con prefijos correctos
                                val kusamaDid = didAddresses?.get("KUSAMA")?.toString()
                                val polkadotDid = didAddresses?.get("POLKADOT")?.toString()
                                
                                DIDInfoRow("did:ksm", kusamaDid?.let { "did:ksm:$it" } ?: "No derivado")
                                DIDInfoRow("did:dot", polkadotDid?.let { "did:dot:$it" } ?: "No derivado")
                            }
                        }
                    }
                    
                    // Información técnica
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "⚙️ Información Técnica",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                DIDInfoRow("Wallet", didInfo.walletName)
                                DIDInfoRow("Path de derivación", didInfo.derivationPath)
                                DIDInfoRow("Clave pública", didInfo.publicKey.take(32) + "...")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun CompactUserProfile(
    userName: String,
    didAddress: String,
    profileImage: String?,
    onEditProfile: () -> Unit,
    onShowDIDInfo: () -> Unit,
    onNameChanged: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selector de imagen de perfil persistente
            ProfileImageSelector(
                didAddress = didAddress,
                onImageSelected = { /* La imagen se guarda automáticamente */ }
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Información del usuario
            Column(modifier = Modifier.weight(1f)) {
                // ✅ Mostrar solo el nombre de la wallet (no editable)
                Text(
                    text = userName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                // ✅ Mostrar información adicional de la wallet
                Text(
                    text = "Wallet: $userName",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                Text(
                    text = "DID: ${didAddress.take(20)}...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "Completo: $didAddress",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Botones de acción
            Row {
                IconButton(onClick = onEditProfile) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.did_dashboard_edit_profile),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onShowDIDInfo) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = stringResource(R.string.did_dashboard_did_info),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CompactAuraScore(
    score: Int,
    interactions: Int,
    verifiedCredentials: Int,
    trustLevel: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Score circular compacto
            Box(
                modifier = Modifier.size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f)
                )
                Text(
                    text = "$score",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Estadísticas en fila compacta
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactStatisticItem(
                    value = "$interactions",
                    label = "Interacciones",
                    icon = Icons.Default.TouchApp
                )
                
                CompactStatisticItem(
                    value = "$verifiedCredentials",
                    label = "Credenciales",
                    icon = Icons.Default.Verified
                )
                
                CompactStatisticItem(
                    value = "$trustLevel%",
                    label = "Confianza",
                    icon = Icons.Default.Security
                )
            }
        }
    }
}

@Composable
fun EmptyDIDCard(
    title: String,
    message: String,
    onDeriveDid: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(onClick = onDeriveDid) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(stringResource(R.string.did_dashboard_derive_did))
            }
        }
    }
}

@Composable
fun InformationSheet(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun InteractionCard(
    interaction: InteractionData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = interaction.icon,
                contentDescription = interaction.type,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = interaction.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = interaction.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(interaction.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Text(
                        text = interaction.type,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecentInteractionsSheet() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(R.string.did_dashboard_interactions),
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Interacciones Recientes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Lista scrolleable de interacciones
            LazyColumn(
                modifier = Modifier.height(180.dp), // Altura fija para scroll independiente
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val interactions = listOf(
                    InteractionData(
                        id = "1",
                        title = "Firma de mensaje para dApp",
                        description = "Conectado a DeFi Protocol",
                        timestamp = System.currentTimeMillis() - (2 * 60 * 60 * 1000), // Hace 2 horas
                        icon = Icons.Default.Edit,
                        type = "Firma"
                    ),
                    InteractionData(
                        id = "2",
                        title = "Presentación de credencial",
                        description = "Licencia de Piloto a FEACH",
                        timestamp = System.currentTimeMillis() - (24 * 60 * 60 * 1000), // Hace 1 día
                        icon = Icons.Default.Verified,
                        type = "Credencial"
                    ),
                    InteractionData(
                        id = "3",
                        title = "Firma de documento",
                        description = "Bitácora de alpinismo",
                        timestamp = System.currentTimeMillis() - (3 * 24 * 60 * 60 * 1000), // Hace 3 días
                        icon = Icons.Default.Description,
                        type = "Documento"
                    ),
                    InteractionData(
                        id = "4",
                        title = "Registro de activo RWA",
                        description = "Aeronave Cessna 172",
                        timestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000), // Hace 1 semana
                        icon = Icons.Default.Flight,
                        type = "RWA"
                    ),
                    InteractionData(
                        id = "5",
                        title = "Verificación de identidad",
                        description = "KYC completado",
                        timestamp = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000), // Hace 2 semanas
                        icon = Icons.Default.Security,
                        type = "Verificación"
                    )
                )
                
                items(interactions) { interaction ->
                    InteractionCard(
                        interaction = interaction,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ScrollableInformationSheets(
    didInfo: DIDInfo
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sheet de información básica
        item {
            InformationSheet(
                title = "Información Básica",
                icon = Icons.Default.Person,
                content = {
                    Column {
                        DIDInfoRow("DID", didInfo.did)
                        DIDInfoRow("Dirección KILT", didInfo.kiltAddress)
                        DIDInfoRow("Estado", "Activo")
                        DIDInfoRow("Wallet", didInfo.walletName)
                    }
                }
            )
        }
        
        // Sheet de credenciales
        item {
            InformationSheet(
                title = "Credenciales",
                icon = Icons.Default.Verified,
                content = {
                    Column {
                        DIDInfoRow("Piloto Privado", "FlyAdvanced")
                        DIDInfoRow("Licencia Deportiva", "FEACH")
                        DIDInfoRow("KYC Internet", "Peranto")
                        DIDInfoRow("Total Activas", "3")
                    }
                }
            )
        }
        
        // Sheet de actividad
        item {
            InformationSheet(
                title = "Actividad Reciente",
                icon = Icons.Default.History,
                content = {
                    Column {
                        DIDInfoRow("Última interacción", "Hace 2 horas")
                        DIDInfoRow("Total de firmas", "47")
                        DIDInfoRow("Documentos firmados", "12")
                        DIDInfoRow("dApps conectadas", "5")
                    }
                }
            )
        }
        
        // Sheet de interacciones recientes
        item {
            RecentInteractionsSheet()
        }
        
        // Sheet de estadísticas avanzadas
        item {
            InformationSheet(
                title = "Estadísticas Avanzadas",
                icon = Icons.Default.Analytics,
                content = {
                    Column {
                        DIDInfoRow("Tiempo promedio de respuesta", "2.3s")
                        DIDInfoRow("Tasa de éxito", "98.5%")
                        DIDInfoRow("Última verificación", "Hace 1 día")
                        DIDInfoRow("Próxima renovación", "En 30 días")
                    }
                }
            )
        }
    }
}

/**
 * Dashboard de Identidad Digital
 * Muestra información completa del DID derivado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DIDDashboardScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToRWA: () -> Unit,
    onNavigateToWallet: () -> Unit,
    activity: FragmentActivity
) {
    val context = LocalContext.current
    val userManager = remember { UserManager(context) }
    val userDatabaseManager = remember { UserDatabaseManager(context, userManager) }
    
    // ✅ Monitorear wallet activa directamente en DIDDashboardScreen
    val walletStateManager = remember { WalletStateManager.getInstance(activity) }
    var currentWalletState by remember { mutableStateOf<WalletState?>(null) }
    
    // Estado del usuario actual
    var currentUser by remember { mutableStateOf<String?>(null) }
    var userKiltIdentities by remember { mutableStateOf<List<UserKiltIdentity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // ✅ Monitorear cambios en la wallet activa
    LaunchedEffect(Unit) {
        walletStateManager.currentWallet.observeForever { walletState ->
            android.util.Log.d("DIDDashboardScreen", "=== WALLET STATE CAMBIÓ EN DIDDASHBOARDSCREEN ===")
            android.util.Log.d("DIDDashboardScreen", "Nuevo estado: $walletState")
            
            currentWalletState = walletState
            
            when (walletState) {
                is WalletState.Created -> {
                    val wallet = walletState.wallet
                    android.util.Log.d("DIDDashboardScreen", "✅ Wallet activa detectada: ${wallet.name}")
                    android.util.Log.d("DIDDashboardScreen", "Dirección: ${wallet.address}")
                    android.util.Log.d("DIDDashboardScreen", "DID KILT: ${wallet.kiltDid}")
                    
                    // Refrescar datos cuando cambie la wallet
                    // TODO: Implementar refreshDidInfo() cuando esté listo
                }
                is WalletState.None -> {
                    android.util.Log.d("DIDDashboardScreen", "⚠️ No hay wallet activa")
                }
                is WalletState.Error -> {
                    android.util.Log.e("DIDDashboardScreen", "❌ Error en wallet: ${walletState.message}")
                }
                else -> {
                    android.util.Log.d("DIDDashboardScreen", "🔄 Estado de wallet: $walletState")
                }
            }
        }
    }
    
    // ✅ Usar método seguro para obtener usuario sin cerrar sesión automáticamente
    // Cargar datos del usuario actual (solo para identidades KILT adicionales)
    LaunchedEffect(context) {
        try {
            // ✅ Usar getCurrentUserSafe() para evitar cierre automático de sesión
            val user = userManager.getCurrentUserSafe()
            val sessionStatus = userManager.checkSessionStatus()
            
            android.util.Log.d("DIDDashboardScreen", "=== VERIFICANDO SESIÓN DE USUARIO ===")
            android.util.Log.d("DIDDashboardScreen", "Usuario: $user")
            android.util.Log.d("DIDDashboardScreen", "Estado de sesión: $sessionStatus")
            
            when (sessionStatus) {
                UserManager.SessionStatus.Active -> {
                    if (user != null) {
                        currentUser = user.id
                        android.util.Log.d("DIDDashboardScreen", "✅ Usuario activo: ${user.name} (${user.id})")
                        
                        // Cargar identidades KILT del usuario
                        val userDb = userDatabaseManager.getUserDatabase(user.id)
                        val identities = userDb.userKiltIdentityDao().getKiltIdentitiesByUser(user.id)
                        userKiltIdentities = identities
                        
                        android.util.Log.d("DIDDashboardScreen", "Identidades KILT encontradas: ${identities.size}")
                        identities.forEach { identity: UserKiltIdentity ->
                            android.util.Log.d("DIDDashboardScreen", "DID: ${identity.did} - Activa: ${identity.isActive}")
                        }
                        
                        error = null
                    }
                }
                UserManager.SessionStatus.Expired -> {
                    android.util.Log.w("DIDDashboardScreen", "⚠️ Sesión expirada - Requiere re-autenticación")
                    currentUser = null
                    error = "Sesión expirada. Por favor, inicia sesión nuevamente."
                }
                UserManager.SessionStatus.NoUser -> {
                    android.util.Log.w("DIDDashboardScreen", "⚠️ No hay usuario activo")
                    currentUser = null
                    error = null  // Permitir que ViewModel maneje esto
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DIDDashboardScreen", "Error cargando datos del usuario", e)
            error = "Error cargando datos: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    val viewModel = remember { DIDDashboardViewModel() }
    val uiState by viewModel.uiState.collectAsState()
    
    // Estado para el modal de información DID
    var showDIDInfoModal by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("Usuario Aura") }
    
    // ✅ Log para verificar cambios en uiState
    LaunchedEffect(uiState) {
        android.util.Log.d("DIDDashboardScreen", "=== UI STATE CAMBIÓ ===")
        android.util.Log.d("DIDDashboardScreen", "uiState: $uiState")
        android.util.Log.d("DIDDashboardScreen", "uiState.didInfo: ${uiState.didInfo}")
        android.util.Log.d("DIDDashboardScreen", "uiState.isLoading: ${uiState.isLoading}")
        android.util.Log.d("DIDDashboardScreen", "uiState.error: ${uiState.error}")
        val didInfo = uiState.didInfo
        if (didInfo != null) {
            android.util.Log.d("DIDDashboardScreen", "🎉 DID INFO DISPONIBLE EN UI STATE!")
            android.util.Log.d("DIDDashboardScreen", "🎉 didInfo.did: ${didInfo.did}")
        } else {
            android.util.Log.d("DIDDashboardScreen", "❌ didInfo es NULL en uiState")
        }
    }
    
    // ✅ Inicializar ViewModel con información del usuario
    LaunchedEffect(activity, currentUser) {
        android.util.Log.d("DIDDashboardScreen", "=== INICIALIZANDO VIEWMODEL CON USUARIO ===")
        android.util.Log.d("DIDDashboardScreen", "currentUser: $currentUser")
        viewModel.initialize(activity)
        
        // ✅ Pasar información del usuario al ViewModel
        viewModel.setCurrentUser(currentUser)
        
        if (currentUser != null) {
            android.util.Log.d("DIDDashboardScreen", "✅ Usuario activo detectado - ViewModel puede proceder")
        } else {
            android.util.Log.w("DIDDashboardScreen", "⚠️ No hay usuario activo - ViewModel mostrará EmptyDIDCard")
        }
    }

    LanguageAware {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.did_dashboard_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.did_dashboard_back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refreshDidInfo() }) {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.did_dashboard_refresh))
                        }
                    }
                )
            },
        bottomBar = {
            AuraBottomNavigationBar(
                currentRoute = "did_dashboard",
                onNavigateToWallet = onNavigateToWallet,
                onNavigateToIdentity = { /* Ya estamos aquí */ },
                onNavigateToCredentials = onNavigateToCredentials,
                onNavigateToDocuments = onNavigateToDocuments,
                onNavigateToRWA = onNavigateToRWA
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Estado de carga
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Error
            if (error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = error ?: "Error desconocido",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // ✅ Contenido principal - siempre mostrar, el ViewModel maneja la lógica
            if (!uiState.isLoading) {
                if (currentUser != null) {
                    // Información del usuario actual
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
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
                                text = "Identidades KILT: ${userKiltIdentities.size}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Lista de identidades KILT del usuario
                    if (userKiltIdentities.isNotEmpty()) {
                        Text(
                            text = "Mis Identidades KILT",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        userKiltIdentities.forEach { identity ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
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
                                            text = "DID KILT",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = "DID Verificado",
                                            tint = if (identity.isActive) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text(
                                        text = "DID:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    
                                    Text(
                                        text = identity.did,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    
                                    Text(
                                        text = "Dirección KILT:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    
                                    Text(
                                        text = identity.kiltAddress,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                    
                                    Text(
                                        text = "Estado: ${if (identity.isActive) "Activa" else "Inactiva"}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                    
                                    Text(
                                        text = "Creada: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(identity.createdAt))}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    } else {
                        // No hay identidades KILT
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "No hay identidades KILT",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = "Crea una wallet para derivar tu identidad DID",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    
                    // ✅ LÓGICA DEL DID MOVIDA FUERA DE LA CONDICIÓN DE USUARIO
                }
                
                // ✅ LÓGICA DEL DID - INDEPENDIENTE DEL USUARIO
                // ✅ Usar SOLO el ViewModel para determinar si hay DID disponible
                val didInfo = uiState.didInfo
                android.util.Log.d("DIDDashboardScreen", "=== VERIFICANDO DID INFO ===")
                android.util.Log.d("DIDDashboardScreen", "uiState: $uiState")
                android.util.Log.d("DIDDashboardScreen", "uiState.didInfo: ${uiState.didInfo}")
                android.util.Log.d("DIDDashboardScreen", "didInfo: $didInfo")
                android.util.Log.d("DIDDashboardScreen", "didInfo != null: ${didInfo != null}")
                if (didInfo != null) {
                    android.util.Log.d("DIDDashboardScreen", "🔍 didInfo.did: ${didInfo.did}")
                    android.util.Log.d("DIDDashboardScreen", "🔍 didInfo.kiltAddress: ${didInfo.kiltAddress}")
                }
                
                if (didInfo != null) {
                    android.util.Log.d("DIDDashboardScreen", "🎉 === RENDERIZANDO PERFIL DID ===")
                    android.util.Log.d("DIDDashboardScreen", "🎉 didInfo.did: ${didInfo.did}")
                    android.util.Log.d("DIDDashboardScreen", "🎉 didInfo.kiltAddress: ${didInfo.kiltAddress}")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Header compacto del usuario
                    CompactUserProfile(
                        userName = currentUserName,
                        didAddress = didInfo.did,
                        profileImage = null,
                        onEditProfile = { /* TODO: Implementar edición de perfil */ },
                        onShowDIDInfo = { showDIDInfoModal = true },
                        onNameChanged = { newName -> currentUserName = newName }
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Aura Score compacto con más elementos
                    CompactAuraScore(
                        score = 85,
                        interactions = 12,
                        verifiedCredentials = 3,
                        trustLevel = 98
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Sheets scrolleables que ocupan el resto del espacio
                    ScrollableInformationSheets(
                        didInfo = didInfo
                    )
                } else {
                    android.util.Log.d("DIDDashboardScreen", "❌ === RENDERIZANDO EMPTY DID CARD ===")
                    android.util.Log.d("DIDDashboardScreen", "❌ didInfo es null, mostrando EmptyDIDCard")
                    // ✅ Mostrar EmptyDIDCard cuando no hay DID disponible
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    EmptyDIDCard(
                        title = "No hay DID disponible",
                        message = "Deriva tu identidad DID desde tu wallet para comenzar a usar credenciales y firmas digitales.",
                        onDeriveDid = { 
                            android.util.Log.d("DIDDashboardScreen", "=== BOTÓN DERIVAR DID PRESIONADO ===")
                            android.util.Log.d("DIDDashboardScreen", "Llamando a viewModel.deriveDidFromWallet()")
                            viewModel.deriveDidFromWallet()
                        }
                    )
                }
            }
        }
        
        // Modal de información DID
        DIDInfoModal(
            isOpen = showDIDInfoModal,
            onDismiss = { showDIDInfoModal = false },
            didInfo = uiState.didInfo,
            walletState = currentWalletState
        )
    }
}

@Composable
fun DIDInfoCard(
    title: String,
    did: String,
    address: String,
    icon: ImageVector
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Text(
                text = "DID:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = did,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Dirección KILT Base:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = address,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun WalletInfoCard(
    title: String,
    walletName: String,
    walletAddress: String,
    icon: ImageVector
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Text(
                text = "Nombre: $walletName",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "Dirección: $walletAddress",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun TechnicalInfoCard(
    title: String,
    derivationPath: String,
    algorithm: String,
    network: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            
            DIDInfoRow("Path de derivación:", derivationPath)
            DIDInfoRow("Algoritmo:", algorithm)
            DIDInfoRow("Red:", network)
        }
    }
}

@Composable
fun ActionsCard(
    title: String,
    onGenerateCredentials: () -> Unit,
    onSignDocument: () -> Unit,
    onManageKeys: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            ActionButton(
                text = "Generar Credenciales",
                icon = Icons.Default.Verified,
                onClick = onGenerateCredentials
            )
            
            ActionButton(
                text = "Firmar Documento",
                icon = Icons.Default.Edit,
                onClick = onSignDocument
            )
            
            ActionButton(
                text = "Gestionar Claves",
                icon = Icons.Default.Key,
                onClick = onManageKeys
            )
        }
    }
}

@Composable
fun DIDDerivedAddressCard(
    title: String,
    didAddress: String,
    icon: ImageVector
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Text(
                text = "Dirección derivada con path //did//0:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = didAddress,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun AuraScoreCard(
    score: Int,
    interactions: Int,
    verifiedCredentials: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Aura Score",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Score circular
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = score / 100f,
                    modifier = Modifier.size(80.dp),
                    strokeWidth = 6.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$score",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreStat(
                    label = "Interacciones",
                    value = interactions.toString(),
                    icon = Icons.Default.TouchApp
                )
                ScoreStat(
                    label = "Credenciales",
                    value = verifiedCredentials.toString(),
                    icon = Icons.Default.Verified
                )
            }
        }
    }
}

@Composable
fun StatisticItem(
    value: String,
    label: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
    }
}
}
