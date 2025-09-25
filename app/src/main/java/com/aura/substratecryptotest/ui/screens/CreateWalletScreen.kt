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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nueva Wallet") },
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
                        onContinue = onWalletCreated
                    )
                }
                
                else -> {
                    Text("Paso no implementado: ${uiState.currentStep}")
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
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Paso 1: Valida tu Mnemonic",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Escribe las siguientes palabras en el orden correcto:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        // Mostrar mnemonic generado
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Tu mnemonic:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mnemonic,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Campo para que el usuario escriba el mnemonic
        OutlinedTextField(
            value = userMnemonic,
            onValueChange = { userMnemonic = it },
            label = { Text("Escribe el mnemonic aquí") },
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
            Text("Validar Mnemonic")
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
            text = "Paso 2: Derivando Cuenta",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        CircularProgressIndicator()
        
        Text(
            text = "Generando tu dirección KILT...",
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
    var walletName by remember { mutableStateOf("") }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Paso 3: Confirma tu Dirección",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "Esta es tu dirección KILT:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        // Mostrar dirección KILT
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Dirección KILT:",
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
        
        // Campo para nombre de wallet
        OutlinedTextField(
            value = walletName,
            onValueChange = { walletName = it },
            label = { Text("Nombre de tu wallet") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Mi Wallet KILT") }
        )
        
        Button(
            onClick = { onConfirm(walletName) },
            enabled = walletName.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Wallet")
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
            text = "Paso 4: Creando Wallet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        CircularProgressIndicator()
        
        Text(
            text = "Guardando tu wallet de forma segura...",
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
            text = "¡Wallet Creada!",
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
            text = "Tu wallet ha sido creada exitosamente y guardada de forma segura.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continuar al Dashboard")
        }
    }
}