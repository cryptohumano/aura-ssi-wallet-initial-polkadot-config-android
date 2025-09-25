package com.aura.substratecryptotest.ui.viewmodels

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.WalletRepository
import com.aura.substratecryptotest.data.WalletInfo
import com.aura.substratecryptotest.data.WalletState
import com.aura.substratecryptotest.wallet.Wallet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el dashboard principal
 */
class DashboardViewModel : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private lateinit var walletRepository: WalletRepository
    
    fun initialize(activity: FragmentActivity) {
        walletRepository = WalletRepository.getInstance(activity)
        
        // Observar cambios en la wallet del WalletManager directamente
        viewModelScope.launch {
            walletRepository.walletManager.currentWallet.observeForever { wallet ->
                if (wallet != null) {
                    // Obtener dirección de Polkadot
                    val polkadotAddress = walletRepository.walletManager.getCurrentWalletPolkadotAddress()
                    
                    val walletInfo = WalletInfo(
                        name = wallet.name,
                        address = wallet.address,
                        kiltAddress = wallet.kiltAddress,
                        polkadotAddress = polkadotAddress,
                        createdAt = wallet.createdAt
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        walletInfo = walletInfo,
                        polkadotAddress = polkadotAddress,
                        isLoading = false,
                        error = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        walletInfo = null,
                        polkadotAddress = null,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }
    
    fun refreshWalletInfo() {
        if (::walletRepository.isInitialized) {
            val walletInfo = walletRepository.getCurrentWalletInfo()
            _uiState.value = _uiState.value.copy(walletInfo = walletInfo)
        }
    }
    
    /**
     * Muestra el modal de switch de cuentas
     */
    fun showAccountSwitchModal() {
        if (::walletRepository.isInitialized) {
            val availableWallets = walletRepository.getAllWallets()
            _uiState.value = _uiState.value.copy(
                showAccountSwitchModal = true,
                availableWallets = availableWallets
            )
        }
    }
    
    /**
     * Oculta el modal de switch de cuentas
     */
    fun hideAccountSwitchModal() {
        _uiState.value = _uiState.value.copy(showAccountSwitchModal = false)
    }
    
    /**
     * Cambia a una wallet específica
     */
    fun switchToWallet(walletName: String) {
        if (::walletRepository.isInitialized) {
            walletRepository.switchToWallet(walletName)
            hideAccountSwitchModal()
        }
    }
}

/**
 * Estado de la UI para el dashboard
 */
data class DashboardUiState(
    val walletInfo: WalletInfo? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAccountSwitchModal: Boolean = false,
    val availableWallets: List<WalletInfo> = emptyList(),
    val polkadotAddress: String? = null
)
