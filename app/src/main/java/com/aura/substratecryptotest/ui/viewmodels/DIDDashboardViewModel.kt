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
 * ViewModel para el Dashboard de Identidad Digital
 */
class DIDDashboardViewModel : ViewModel() {
    
    private lateinit var walletRepository: WalletRepository
    
    private val _uiState = MutableStateFlow(DIDDashboardUiState())
    val uiState: StateFlow<DIDDashboardUiState> = _uiState.asStateFlow()
    
    /**
     * Inicializa el ViewModel con el repositorio
     */
    fun initialize(activity: FragmentActivity) {
        walletRepository = WalletRepository.getInstance(activity)
        refreshDidInfo()
    }
    
    /**
     * Actualiza la información del DID desde la wallet actual
     */
    fun refreshDidInfo() {
        viewModelScope.launch {
            try {
                val currentWallet = walletRepository.currentWallet.value
                val walletManager = walletRepository.walletManager
                
                if (currentWallet != null && walletManager != null) {
                    val wallet = walletManager.currentWallet.value
                    
                    if (wallet != null && wallet.kiltDid != null) {
                        // Obtener dirección KILT base de la wallet
                        val kiltBaseAddress = walletManager.getCurrentWalletKiltBaseAddress()
                        
                        // Wallet tiene DID derivado
                        val didInfo = DIDInfo(
                            did = wallet.kiltDid,
                            kiltAddress = kiltBaseAddress ?: "No disponible", // Dirección KILT base
                            walletName = wallet.name,
                            walletAddress = wallet.address, // Dirección base de la wallet
                            derivationPath = wallet.derivationPath,
                            publicKey = wallet.publicKey.joinToString("") { "%02x".format(it) }
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            didInfo = didInfo,
                            isLoading = false,
                            error = null
                        )
                        
                        Logger.success("DIDDashboardViewModel", "Información DID cargada", "DID: ${wallet.kiltDid}")
                        Logger.debug("DIDDashboardViewModel", "Dirección KILT base", kiltBaseAddress ?: "No disponible")
                    } else {
                        // Wallet no tiene DID derivado
                        _uiState.value = _uiState.value.copy(
                            didInfo = null,
                            isLoading = false,
                            error = null
                        )
                        
                        Logger.debug("DIDDashboardViewModel", "Wallet sin DID", "Wallet: ${wallet?.name}")
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        didInfo = null,
                        isLoading = false,
                        error = "No hay wallet activa"
                    )
                    
                    Logger.error("DIDDashboardViewModel", "No hay wallet activa", "No se puede mostrar información DID", null)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error cargando información DID: ${e.message}"
                )
                
                Logger.error("DIDDashboardViewModel", "Error refrescando información DID", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    /**
     * Deriva DID desde la wallet actual
     */
    fun deriveDidFromWallet() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        walletRepository.deriveDidFromCurrentWallet(
            onSuccess = { did ->
                Logger.success("DIDDashboardViewModel", "DID derivado exitosamente", "DID: $did")
                
                // Refrescar información después de derivar
                refreshDidInfo()
            },
            onError = { error ->
                Logger.error("DIDDashboardViewModel", "Error derivando DID", error, null)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = error
                )
            }
        )
    }
}

/**
 * Estado de la UI del Dashboard DID
 */
data class DIDDashboardUiState(
    val didInfo: DIDInfo? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Información del DID
 */
data class DIDInfo(
    val did: String,
    val kiltAddress: String,
    val walletName: String,
    val walletAddress: String,
    val derivationPath: String,
    val publicKey: String
)
