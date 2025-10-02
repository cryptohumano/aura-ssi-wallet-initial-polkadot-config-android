package com.aura.substratecryptotest.ui.screens

import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Fingerprint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.aura.substratecryptotest.security.BiometricManager
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.substratecryptotest.ui.viewmodels.CreateWalletViewModel
import com.aura.substratecryptotest.ui.viewmodels.CreateWalletStep
import androidx.compose.ui.res.stringResource
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.ui.context.LanguageAware

/**
 * Pantalla para crear una nueva wallet
 * Implementa el flujo correcto: mnemonic → validación → derivación → confirmación → guardado
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateWalletScreen(
    onNavigateBack: () -> Unit,
    onWalletCreated: () -> Unit,
    viewModel: CreateWalletViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as FragmentActivity
    
    // Inicializar ViewModel
    LaunchedEffect(Unit) {
        viewModel.initialize(activity)
        viewModel.generateMnemonic()
    }
    
    LanguageAware {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.create_wallet_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.settings_back))
                        }
                    }
                )
            }
        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Mostrar error si existe
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Mostrar estado actual
            uiState.statusMessage.takeIf { it.isNotEmpty() }?.let { status ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = status,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Contenido según el paso actual
            when (uiState.currentStep) {
                CreateWalletStep.MNEMONIC_DISPLAY -> {
                    MnemonicDisplayStep(
                        mnemonic = uiState.generatedMnemonic ?: "",
                        onContinue = { viewModel.validateMnemonic(it) }
                    )
                }
                
                CreateWalletStep.DERIVING_ACCOUNT -> {
                    DerivingAccountStep()
                }
                
                CreateWalletStep.ADDRESS_CONFIRMATION -> {
                    AddressConfirmationStep(
                        kiltAddress = uiState.kiltAddress ?: "",
                        onConfirm = { walletName ->
                            viewModel.confirmWalletCreation(walletName)
                        }
                    )
                }
                
                CreateWalletStep.CREATING_WALLET -> {
                    CreatingWalletStep()
                }
                
                CreateWalletStep.COMPLETED -> {
                    CompletedStep(
                        onContinue = {
                            // Pasar información del usuario creado
                            val createdUser = uiState.createdUser
                            val createdWallet = uiState.createdWallet
                            if (createdUser != null && createdWallet != null) {
                                android.util.Log.d("CreateWalletScreen", "Usuario creado: ${createdUser.name}")
                                android.util.Log.d("CreateWalletScreen", "Wallet creada: ${createdWallet.name}")
                            }
                            onWalletCreated()
                        }
                    )
                }
                
                else -> {
                    Text(stringResource(R.string.create_wallet_step_not_implemented, uiState.currentStep.toString()))
                }
            }
        }
        }
    }
}

@Composable
private fun MnemonicDisplayStep(
    mnemonic: String,
    onContinue: (String) -> Unit
) {
    var userMnemonic by remember { mutableStateOf("") }
    var showBiometricPrompt by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val biometricManager = remember { BiometricManager(context) }
    val activity = context as? FragmentActivity
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.create_wallet_step1_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = stringResource(R.string.create_wallet_step1_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        // Mostrar mnemonic generado con botón de copiar
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tu mnemonic:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Botón de copiar al portapapeles
                    Button(
                        onClick = {
                            val clip = ClipData.newPlainText("mnemonic", mnemonic)
                            clipboardManager.setPrimaryClip(clip)
                        },
                        modifier = Modifier.size(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copiar mnemonic",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mnemonic,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Botón de autocompletar con biometría
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🔐 Autenticación Biométrica",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Usa tu huella dactilar o reconocimiento facial para autocompletar el mnemonic",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { 
                        if (activity != null && biometricManager.isBiometricAvailable()) {
                            biometricManager.showBiometricPrompt(
                                activity = activity,
                                title = "Autenticación Biométrica",
                                subtitle = "Confirma tu identidad para autocompletar el mnemonic",
                                onSuccess = { 
                                    userMnemonic = mnemonic
                                    biometricError = null
                                },
                                onError = { error ->
                                    biometricError = error
                                },
                                onCancel = {
                                    biometricError = null
                                }
                            )
                        } else {
                            biometricError = "Biometría no disponible"
                        }
                    },
                    enabled = activity != null && biometricManager.isBiometricAvailable(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Autenticación biométrica",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Autocompletar con Biometría")
                }
            }
        }
        
        // Mostrar error de biometría si existe
        biometricError?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = "⚠️ $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        
        // Campo para que el usuario escriba el mnemonic
        OutlinedTextField(
            value = userMnemonic,
            onValueChange = { userMnemonic = it },
            label = { Text(stringResource(R.string.create_wallet_enter_mnemonic)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 3
        )
        
        Button(
            onClick = { onContinue(userMnemonic) },
            enabled = userMnemonic.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.create_wallet_validate_mnemonic))
        }
    }
}

@Composable
private fun DerivingAccountStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.create_wallet_step2_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        CircularProgressIndicator()
        
        Text(
            text = stringResource(R.string.create_wallet_step2_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddressConfirmationStep(
    kiltAddress: String,
    onConfirm: (String) -> Unit
) {
    var userName by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Crear Usuario Completo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Ingresa un nombre para tu usuario. Se creará un usuario completo con wallet asociada.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        // Mostrar dirección KILT que se generará
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dirección KILT que se generará:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = kiltAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Campo para nombre de usuario
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Nombre de Usuario") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Mi Usuario") }
        )
        
        Button(
            onClick = { onConfirm(userName) },
            enabled = userName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Usuario Completo")
        }
    }
}

@Composable
private fun CreatingWalletStep() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Creando Usuario Completo",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        CircularProgressIndicator()
        
        Text(
            text = "Creando usuario en ambos sistemas y generando wallet segura...",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompletedStep(
    onContinue: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.create_wallet_completed_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = stringResource(R.string.create_wallet_completed_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.create_wallet_continue_dashboard))
        }
    }
}