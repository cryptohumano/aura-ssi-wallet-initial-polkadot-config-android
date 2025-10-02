package com.aura.substratecryptotest.ui.viewmodels

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.wallet.WalletStateManager
import com.aura.substratecryptotest.data.WalletInfo
import com.aura.substratecryptotest.data.WalletState
import com.aura.substratecryptotest.wallet.Wallet
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import com.aura.substratecryptotest.data.WalletRepository
import com.aura.substratecryptotest.data.user.User
import com.aura.substratecryptotest.ui.lifecycle.ActivityLifecycleManager
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
    
    private lateinit var walletStateManager: WalletStateManager
    private lateinit var userManager: UserManager
    private lateinit var appDatabaseManager: AppDatabaseManager
    private lateinit var walletRepository: WalletRepository
    private lateinit var activity: FragmentActivity
    
    fun initialize(activity: FragmentActivity) {
        android.util.Log.d("DashboardViewModel", "=== INICIALIZANDO DASHBOARD VIEWMODEL ===")
        android.util.Log.i("DashboardViewModel", "=== LOG DE PRUEBA - INICIALIZANDO ===")
        this.activity = activity
        walletStateManager = WalletStateManager.getInstance(activity)
        userManager = UserManager(activity)
        appDatabaseManager = AppDatabaseManager(activity)
        walletRepository = WalletRepository.getInstance(activity)
        
        android.util.Log.d("DashboardViewModel", "Componentes inicializados correctamente")
        
        // ✅ Observar cambios en la wallet usando WalletStateManager (patrón modular)
        viewModelScope.launch {
            walletStateManager.currentWallet.observeForever { walletState ->
                android.util.Log.d("DashboardViewModel", "=== WALLET STATE CAMBIÓ ===")
                android.util.Log.d("DashboardViewModel", "Nuevo estado: $walletState")
                
                when (walletState) {
                    is WalletState.Created -> {
                        val wallet = walletState.wallet
                        android.util.Log.d("DashboardViewModel", "=== INFORMACIÓN DE WALLET ===")
                        android.util.Log.d("DashboardViewModel", "Wallet: ${wallet.name}")
                        android.util.Log.d("DashboardViewModel", "Address base: ${wallet.address}")
                        
                        val addresses = wallet.metadata["addresses"] as? Map<*, *>
                        android.util.Log.d("DashboardViewModel", "Direcciones en metadata: $addresses")
                        
                        val polkadotAddress = addresses?.get("POLKADOT")?.toString()
                        val kiltAddress = addresses?.get("KILT")?.toString()
                        val kusamaAddress = addresses?.get("KUSAMA")?.toString()
                        
                        android.util.Log.d("DashboardViewModel", "Polkadot Address: $polkadotAddress")
                        android.util.Log.d("DashboardViewModel", "KILT Address: $kiltAddress")
                        android.util.Log.d("DashboardViewModel", "Kusama Address: $kusamaAddress")
                        android.util.Log.d("DashboardViewModel", "KILT DID: ${wallet.kiltDid}")
                        
                        val walletInfo = WalletInfo(
                            name = wallet.name,
                            address = wallet.address,
                            kiltAddress = kiltAddress,
                            polkadotAddress = polkadotAddress,
                            kusamaAddress = kusamaAddress,
                            createdAt = wallet.createdAt
                        )
                        
                        // ✅ También obtener el usuario actual
                        val currentUser = if (::appDatabaseManager.isInitialized) {
                            appDatabaseManager.userManagementService.getCurrentUser()
                        } else {
                            null
                        }
                        android.util.Log.d("DashboardViewModel", "Usuario actual obtenido en initialize: $currentUser")
                        
                        _uiState.value = _uiState.value.copy(
                            walletInfo = walletInfo,
                            polkadotAddress = polkadotAddress,
                            kusamaAddress = kusamaAddress,
                            currentUser = currentUser,
                            isLoading = false,
                            error = null
                        )
                        
                        // Refrescar lista de cuentas cuando cambie la wallet
                        if (::appDatabaseManager.isInitialized) {
                            refreshAvailableAccounts()
                        }
                    }
                    is WalletState.None -> {
                        _uiState.value = _uiState.value.copy(
                            walletInfo = null,
                            isLoading = false,
                            error = "No hay wallet activa"
                        )
                    }
                    is WalletState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            walletInfo = null,
                            isLoading = false,
                            error = walletState.message
                        )
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            walletInfo = null,
                            isLoading = true,
                            error = null
                        )
                    }
                }
            }
        }
    }
    
    fun refreshWalletInfo() {
        if (::walletStateManager.isInitialized) {
            android.util.Log.d("DashboardViewModel", "=== REFRESHING WALLET INFO ===")
            
            val walletInfo = walletStateManager.getCurrentWalletInfo()
            android.util.Log.d("DashboardViewModel", "WalletInfo obtenido: $walletInfo")
            
            // ✅ También actualizar el usuario actual si UserManagementService está disponible
            val currentUser = if (::appDatabaseManager.isInitialized) {
                appDatabaseManager.userManagementService.getCurrentUser()
            } else {
                null
            }
            android.util.Log.d("DashboardViewModel", "Usuario actual obtenido: $currentUser")
            
            _uiState.value = _uiState.value.copy(
                walletInfo = walletInfo,
                currentUser = currentUser
            )
            android.util.Log.d("DashboardViewModel", "UI State actualizado con walletInfo: ${_uiState.value.walletInfo}")
            android.util.Log.d("DashboardViewModel", "UI State actualizado con currentUser: ${_uiState.value.currentUser}")
        } else {
            android.util.Log.e("DashboardViewModel", "WalletStateManager no está inicializado")
        }
    }
    
    /**
     * Muestra el modal de switch de cuentas
     */
    fun showAccountSwitchModal() {
        if (::appDatabaseManager.isInitialized) {
            viewModelScope.launch {
                try {
                    android.util.Log.d("DashboardViewModel", "=== INICIANDO CARGA DE USUARIOS PARA SWITCHER ===")
                    
                    // Obtener wallets disponibles desde WalletRepository
                    val allWallets = walletStateManager.getAllWallets()
                    android.util.Log.d("DashboardViewModel", "Wallets totales encontradas: ${allWallets.size}")
                    
                    // ✅ Usar WalletStateManager para obtener WalletInfo correctamente mapeado
                    val availableWallets = allWallets
                    
                    android.util.Log.d("DashboardViewModel", "Wallets disponibles para switcher: ${availableWallets.size}")
                    
                    _uiState.value = _uiState.value.copy(
                        showAccountSwitchModal = true,
                        availableWallets = availableWallets,
                        currentUser = appDatabaseManager.userManagementService.getCurrentUser() // ✅ Usar UserManagementService
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DashboardViewModel", "❌ Error cargando usuarios para switcher", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Error cargando usuarios: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Oculta el modal de switch de cuentas
     */
    fun hideAccountSwitchModal() {
        _uiState.value = _uiState.value.copy(showAccountSwitchModal = false)
    }
    
    /**
     * Cambia a un usuario específico usando el servicio unificado
     */
    fun switchToWallet(userName: String) {
        if (::appDatabaseManager.isInitialized) {
            viewModelScope.launch {
                try {
                    android.util.Log.d("DashboardViewModel", "=== CAMBIANDO A USUARIO: $userName ===")
                    
                    // ✅ Usar WalletStateManager para cambiar wallet
                    walletStateManager.switchToWallet(userName)
                    val switchResult = Result.success(Unit)
                    
                    if (switchResult.isSuccess) {
                        android.util.Log.d("DashboardViewModel", "✅ Usuario cambiado exitosamente: $userName")
                        
                        // Actualizar estado y refrescar información de wallet
                        refreshWalletInfo()
                        _uiState.value = _uiState.value.copy(
                            currentUser = appDatabaseManager.userManagementService.getCurrentUser(), // ✅ Usar UserManagementService
                            error = null
                        )
                    } else {
                        android.util.Log.e("DashboardViewModel", "❌ Error cambiando usuario: $userName")
                        _uiState.value = _uiState.value.copy(
                            error = "Error cambiando usuario: $userName"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DashboardViewModel", "❌ Error cambiando usuario: $userName", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Error cambiando usuario: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Refresca la lista de cuentas disponibles
     */
    fun refreshAvailableAccounts() {
        if (::appDatabaseManager.isInitialized) {
            viewModelScope.launch {
                try {
                    android.util.Log.d("DashboardViewModel", "=== REFRESCANDO LISTA DE CUENTAS ===")
                    
                    // ✅ Sincronizar WalletStateManager primero
                    walletStateManager.syncCurrentWalletState()
                    
                    // Obtener wallets disponibles desde WalletStateManager
                    val allWallets = walletStateManager.getAllWallets()
                    android.util.Log.d("DashboardViewModel", "Wallets totales encontradas: ${allWallets.size}")
                    
                    // Log detallado de cada wallet
                    allWallets.forEachIndexed { index, wallet ->
                        android.util.Log.d("DashboardViewModel", "Wallet $index: ${wallet.name}")
                    }
                    
                    android.util.Log.d("DashboardViewModel", "Wallets disponibles actualizadas: ${allWallets.size}")
                    
                    _uiState.value = _uiState.value.copy(
                        availableWallets = allWallets,
                        currentUser = appDatabaseManager.userManagementService.getCurrentUser() // ✅ Usar UserManagementService
                    )
                } catch (e: Exception) {
                    android.util.Log.e("DashboardViewModel", "❌ Error refrescando cuentas", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Error refrescando cuentas: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Cierra la sesión del usuario actual
     */
    fun logoutCurrentUser() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DashboardViewModel", "=== INICIANDO LOGOUT ===")
                android.util.Log.d("DashboardViewModel", "Stack trace: ${android.util.Log.getStackTraceString(Exception("Logout called from:"))}")
                
                val currentUser = userManager.getCurrentUser()
                android.util.Log.d("DashboardViewModel", "Usuario actual antes del logout: ${currentUser?.name}")
                android.util.Log.d("DashboardViewModel", "Timestamp: ${System.currentTimeMillis()}")
                
                userManager.closeCurrentSession()
                android.util.Log.d("DashboardViewModel", "✅ Sesión cerrada exitosamente")
                
                _uiState.value = _uiState.value.copy(
                    currentUser = null,
                    walletInfo = null,
                    availableWallets = emptyList(),
                    error = null
                )
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "❌ Error cerrando sesión", e)
                _uiState.value = _uiState.value.copy(
                    error = "Error cerrando sesión: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Renombra una wallet
     */
    fun renameWallet(oldName: String, newName: String) {
        if (::walletRepository.isInitialized) {
            viewModelScope.launch {
                try {
                    android.util.Log.d("DashboardViewModel", "=== RENOMBRANDO WALLET: $oldName -> $newName ===")
                    
                    val result = walletRepository.renameWalletByName(oldName, newName)
                    
                    if (result.isSuccess) {
                        android.util.Log.d("DashboardViewModel", "✅ Wallet renombrada exitosamente")
                        
                        // Refrescar información de wallet y lista de cuentas
                        refreshWalletInfo()
                        refreshAvailableAccounts()
                        
                        _uiState.value = _uiState.value.copy(
                            error = null
                        )
                    } else {
                        android.util.Log.e("DashboardViewModel", "❌ Error renombrando wallet: ${result.exceptionOrNull()?.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Error renombrando wallet: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DashboardViewModel", "❌ Error renombrando wallet", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Error renombrando wallet: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Elimina una wallet
     */
    fun deleteWallet(walletName: String) {
        if (::walletRepository.isInitialized) {
            viewModelScope.launch {
                try {
                    android.util.Log.d("DashboardViewModel", "=== ELIMINANDO WALLET: $walletName ===")
                    
                    val result = walletRepository.deleteWalletByName(walletName)
                    
                    if (result.isSuccess) {
                        android.util.Log.d("DashboardViewModel", "✅ Wallet eliminada exitosamente")
                        
                        // Refrescar información de wallet y lista de cuentas
                        refreshWalletInfo()
                        refreshAvailableAccounts()
                        
                        _uiState.value = _uiState.value.copy(
                            error = null
                        )
                    } else {
                        android.util.Log.e("DashboardViewModel", "❌ Error eliminando wallet: ${result.exceptionOrNull()?.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Error eliminando wallet: ${result.exceptionOrNull()?.message}"
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DashboardViewModel", "❌ Error eliminando wallet", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Error eliminando wallet: ${e.message}"
                    )
                }
            }
        }
    }
}

/**
 * Estado de la UI del Dashboard
 */
data class DashboardUiState(
    val walletInfo: WalletInfo? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val showAccountSwitchModal: Boolean = false,
    val availableWallets: List<WalletInfo> = emptyList(),
    val polkadotAddress: String? = null,
    val kusamaAddress: String? = null,
    val currentUser: User? = null
)