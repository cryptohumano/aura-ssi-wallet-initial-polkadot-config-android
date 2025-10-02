package com.aura.substratecryptotest.ui.viewmodels

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.data.UserWallet
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel seguro para gestión de wallets
 * Usa SecureUserRepository con autenticación biométrica
 */
class SecureWalletViewModel(
    private val secureUserRepository: SecureUserRepository,
    private val userManager: UserManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "SecureWalletViewModel"
    }
    
    private val _uiState = MutableStateFlow(SecureWalletUiState())
    val uiState: StateFlow<SecureWalletUiState> = _uiState.asStateFlow()
    
    /**
     * Crea una nueva wallet de forma segura
     * Requiere autenticación biométrica
     */
    fun createSecureWallet(
        activity: FragmentActivity,
        walletName: String,
        mnemonic: String,
        publicKey: ByteArray,
        privateKey: ByteArray,
        address: String,
        cryptoType: String,
        derivationPath: String
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                Logger.debug(TAG, "Creando wallet segura", "Nombre: $walletName")
                
                val result = secureUserRepository.createUserWallet(
                    walletName = walletName,
                    mnemonic = mnemonic,
                    publicKey = publicKey,
                    privateKey = privateKey,
                    address = address,
                    cryptoType = cryptoType,
                    derivationPath = derivationPath,
                    requireBiometric = true
                )
                
                if (result.isSuccess) {
                    val wallet = result.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentWallet = wallet,
                        wallets = _uiState.value.wallets + wallet!!
                    )
                    Logger.success(TAG, "Wallet segura creada", "ID: ${wallet.id.take(8)}...")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                    Logger.error(TAG, "Error creando wallet segura", error, null)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Error desconocido"
                _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                Logger.error(TAG, "Excepción creando wallet segura", error, e)
            }
        }
    }
    
    /**
     * Obtiene el mnemonic de una wallet de forma segura
     * Requiere autenticación biométrica
     */
    fun getWalletMnemonic(walletId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                Logger.debug(TAG, "Obteniendo mnemonic seguro", "Wallet: ${walletId.take(8)}...")
                
                val result = secureUserRepository.getWalletMnemonic(
                    walletId = walletId,
                    requireBiometric = true
                )
                
                if (result.isSuccess) {
                    val mnemonic = result.getOrNull()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentMnemonic = mnemonic
                    )
                    Logger.success(TAG, "Mnemonic obtenido", "Wallet: ${walletId.take(8)}...")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                    Logger.error(TAG, "Error obteniendo mnemonic", error, null)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Error desconocido"
                _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                Logger.error(TAG, "Excepción obteniendo mnemonic", error, e)
            }
        }
    }
    
    /**
     * Carga las wallets del usuario actual
     */
    fun loadUserWallets() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val wallets = secureUserRepository.getUserWallets()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    wallets = wallets
                )
                
                Logger.success(TAG, "Wallets cargadas", "Cantidad: ${wallets.size}")
            } catch (e: Exception) {
                val error = e.message ?: "Error desconocido"
                _uiState.value = _uiState.value.copy(isLoading = false, error = error)
                Logger.error(TAG, "Error cargando wallets", error, e)
            }
        }
    }
}

/**
 * Estado de la UI para SecureWalletViewModel
 */
data class SecureWalletUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val wallets: List<UserWallet> = emptyList(),
    val currentWallet: UserWallet? = null,
    val currentMnemonic: String? = null
)
