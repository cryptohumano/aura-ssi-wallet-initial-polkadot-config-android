package com.aura.substratecryptotest.ui.viewmodels

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.WalletRepository
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la creación de wallet
 * Implementa el flujo correcto: mnemonic → validación → derivación → confirmación → guardado
 */
class CreateWalletViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(CreateWalletUiState())
    val uiState: StateFlow<CreateWalletUiState> = _uiState.asStateFlow()
    
    private lateinit var walletRepository: WalletRepository
    
    fun initialize(activity: FragmentActivity) {
        walletRepository = WalletRepository.getInstance(activity)
    }
    
    /**
     * Paso 1: Generar mnemonic para mostrar al usuario
     */
    fun generateMnemonic() {
        if (!::walletRepository.isInitialized) {
            Logger.error("CreateWalletViewModel", "WalletRepository no inicializado", "Llamar initialize() primero", null)
            return
        }
        
        viewModelScope.launch {
            try {
                val mnemonic = walletRepository.generateMnemonicForValidation()
                _uiState.value = _uiState.value.copy(
                    generatedMnemonic = mnemonic,
                    currentStep = CreateWalletStep.MNEMONIC_DISPLAY,
                    statusMessage = "Mnemonic generado. Escríbelo en orden para validar."
                )
                
                Logger.debug("CreateWalletViewModel", "Mnemonic generado", "Longitud: ${mnemonic.split(" ").size} palabras")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Error generando mnemonic: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Paso 2: Validar mnemonic ingresado por el usuario
     */
    fun validateMnemonic(userMnemonic: String) {
        val originalMnemonic = _uiState.value.generatedMnemonic
        if (originalMnemonic == null) {
            _uiState.value = _uiState.value.copy(
                error = "No hay mnemonic generado para validar"
            )
            return
        }
        
        val isValid = walletRepository.validateMnemonic(userMnemonic, originalMnemonic)
        
        if (isValid) {
            _uiState.value = _uiState.value.copy(
                currentStep = CreateWalletStep.DERIVING_ACCOUNT,
                statusMessage = "Mnemonic válido. Derivando cuenta de fondos..."
            )
            
            // Derivar cuenta automáticamente
            deriveFundsAccount()
        } else {
            _uiState.value = _uiState.value.copy(
                error = "Mnemonic incorrecto. Verifica el orden de las palabras.",
                statusMessage = ""
            )
        }
    }
    
    /**
     * Paso 3: Derivar cuenta de fondos y mostrar direcciones
     */
    private fun deriveFundsAccount() {
        val mnemonic = _uiState.value.generatedMnemonic ?: return
        
        walletRepository.deriveFundsAccount(
            mnemonic = mnemonic,
            onSuccess = { addresses ->
                val kiltAddress = addresses["KILT"] ?: "No disponible"
                _uiState.value = _uiState.value.copy(
                    currentStep = CreateWalletStep.ADDRESS_CONFIRMATION,
                    walletAddresses = addresses,
                    kiltAddress = kiltAddress,
                    statusMessage = "Dirección KILT: ${kiltAddress.take(20)}..."
                )
                
                Logger.success("CreateWalletViewModel", "Cuenta derivada", "Direcciones: ${addresses.size}")
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Error derivando cuenta: $error",
                    statusMessage = ""
                )
            }
        )
    }
    
    /**
     * Paso 4: Confirmar creación de wallet
     */
    fun confirmWalletCreation(walletName: String) {
        val mnemonic = _uiState.value.generatedMnemonic ?: return
        
        _uiState.value = _uiState.value.copy(
            currentStep = CreateWalletStep.CREATING_WALLET,
            walletName = walletName,
            statusMessage = "Creando wallet: $walletName..."
        )
        
        walletRepository.createFinalWallet(
            walletName = walletName,
            validatedMnemonic = mnemonic,
            onSuccess = {
                _uiState.value = _uiState.value.copy(
                    currentStep = CreateWalletStep.COMPLETED,
                    statusMessage = "¡Wallet creada exitosamente!"
                )
                
                Logger.success("CreateWalletViewModel", "Wallet creada", walletName)
            },
            onError = { error ->
                _uiState.value = _uiState.value.copy(
                    error = "Error creando wallet: $error",
                    statusMessage = ""
                )
            }
        )
    }
    
    /**
     * Limpiar error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Reiniciar flujo
     */
    fun resetFlow() {
        _uiState.value = CreateWalletUiState()
    }
}

/**
 * Estados del flujo de creación de wallet
 */
enum class CreateWalletStep {
    MNEMONIC_DISPLAY,      // Mostrar mnemonic al usuario
    MNEMONIC_VALIDATION,   // Usuario valida mnemonic
    DERIVING_ACCOUNT,      // Derivando cuenta de fondos
    ADDRESS_CONFIRMATION,  // Mostrar direcciones para confirmación
    CREATING_WALLET,       // Creando wallet final
    COMPLETED             // Wallet creada exitosamente
}

/**
 * Estado de la UI para la creación de wallet
 */
data class CreateWalletUiState(
    val currentStep: CreateWalletStep = CreateWalletStep.MNEMONIC_DISPLAY,
    val generatedMnemonic: String? = null,
    val walletAddresses: Map<String, String> = emptyMap(),
    val kiltAddress: String? = null,
    val walletName: String = "",
    val statusMessage: String = "",
    val error: String? = null
)