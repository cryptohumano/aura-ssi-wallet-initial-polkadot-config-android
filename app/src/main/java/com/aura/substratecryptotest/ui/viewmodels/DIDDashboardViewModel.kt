package com.aura.substratecryptotest.ui.viewmodels

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.data.wallet.WalletStateManager
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.ui.lifecycle.ActivityLifecycleManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el Dashboard de Identidad Digital
 */
class DIDDashboardViewModel : ViewModel() {
    
    private lateinit var walletStateManager: WalletStateManager
    
    private val _uiState = MutableStateFlow(DIDDashboardUiState())
    val uiState: StateFlow<DIDDashboardUiState> = _uiState.asStateFlow()
    
    /**
     * Inicializa el ViewModel con el repositorio
     */
    fun initialize(activity: FragmentActivity) {
        val threadId = Thread.currentThread().id
        val processId = android.os.Process.myPid()
        Logger.debug("DIDDashboardViewModel", "=== INICIALIZANDO DID DASHBOARD VIEWMODEL ===", "Thread: $threadId, Process: $processId")
        walletStateManager = WalletStateManager.getInstance(activity)
        
        // ✅ 1. Primero monitorear cambios de activity
        monitorActivityChanges()
        
        // ✅ 2. Luego monitorear cambios en la wallet activa
        monitorWalletStateChanges()
        
        // ✅ 3. NO refrescar información aquí - se hará después de establecer el usuario
        
        Logger.debug("DIDDashboardViewModel", "Inicialización completada", "Thread: $threadId, Process: $processId")
    }
    
    /**
     * ✅ NUEVO: Establece el usuario activo para el ViewModel
     */
    fun setCurrentUser(userId: String?) {
        Logger.debug("DIDDashboardViewModel", "Usuario establecido", "ID: ${userId?.take(8)}...")
        
        if (userId != null) {
            // Usuario activo - proceder con la lógica normal
            Logger.debug("DIDDashboardViewModel", "✅ Usuario activo - Refrescando información DID", "")
            refreshDidInfo()
        } else {
            // No hay usuario activo - pero verificar si hay wallet para permitir derivación
            Logger.debug("DIDDashboardViewModel", "⚠️ No hay usuario activo - Verificando wallet para derivación", "")
            val currentWalletState = walletStateManager.currentWallet.value
            if (currentWalletState is com.aura.substratecryptotest.data.WalletState.Created) {
                Logger.debug("DIDDashboardViewModel", "✅ Wallet activa encontrada - Permitiendo derivación DID", "")
                refreshDidInfo() // Refrescar para mostrar el estado actual y permitir derivación
            } else {
                Logger.debug("DIDDashboardViewModel", "⚠️ No hay wallet activa - Mostrando EmptyDIDCard", "")
                _uiState.value = _uiState.value.copy(
                    didInfo = null,
                    isLoading = false,
                    error = null
                )
            }
        }
    }
    
    /**
     * Monitorea cambios en la activity activa
     */
    private fun monitorActivityChanges() {
        viewModelScope.launch {
            ActivityLifecycleManager.getInstance().currentActivity.collect { currentActivity ->
                Logger.debug("DIDDashboardViewModel", "Activity cambió", "Activity: ${currentActivity?.javaClass?.simpleName}")
                
                if (currentActivity != null) {
                    // Verificar si es DIDDashboardScreen
                    if (currentActivity.javaClass.simpleName.contains("DID")) {
                        Logger.debug("DIDDashboardViewModel", "DIDDashboardScreen activa - Refrescando datos", "")
                        refreshDidInfo()
                    }
                }
            }
        }
    }
    
    /**
     * Monitorea cambios en el estado de la wallet activa
     */
    private fun monitorWalletStateChanges() {
        viewModelScope.launch {
            walletStateManager.currentWallet.observeForever { walletState ->
                Logger.debug("DIDDashboardViewModel", "=== WALLET STATE CAMBIÓ EN DIDDASHBOARDVIEWMODEL ===", "")
                Logger.debug("DIDDashboardViewModel", "Nuevo estado: $walletState", "")
                android.util.Log.d("DIDDashboardViewModel", "=== WALLET STATE CAMBIÓ EN DIDDASHBOARDVIEWMODEL ===")
                android.util.Log.d("DIDDashboardViewModel", "Nuevo estado: $walletState")
                
                when (walletState) {
                    is com.aura.substratecryptotest.data.WalletState.Created -> {
                        val wallet = walletState.wallet
                        Logger.debug("DIDDashboardViewModel", "✅ Wallet activa detectada: ${wallet.name}", "")
                        Logger.debug("DIDDashboardViewModel", "Dirección: ${wallet.address}", "")
                        Logger.debug("DIDDashboardViewModel", "DID KILT: ${wallet.kiltDid}", "")
                        
                        // Refrescar información cuando cambie la wallet
                        refreshDidInfo()
                    }
                    is com.aura.substratecryptotest.data.WalletState.None -> {
                        Logger.debug("DIDDashboardViewModel", "⚠️ No hay wallet activa", "")
                        refreshDidInfo()
                    }
                    is com.aura.substratecryptotest.data.WalletState.Error -> {
                        Logger.error("DIDDashboardViewModel", "❌ Error en wallet: ${walletState.message}", "", null)
                        refreshDidInfo()
                    }
                    else -> {
                        Logger.debug("DIDDashboardViewModel", "🔄 Estado de wallet: $walletState", "")
                        refreshDidInfo()
                    }
                }
            }
        }
    }
    
    /**
     * Actualiza la información del DID desde la wallet actual
     */
    fun refreshDidInfo() {
        Logger.debug("DIDDashboardViewModel", "=== REFRESHING DID INFO ===", "")
        viewModelScope.launch {
            try {
                val walletState = walletStateManager.currentWallet.value
                Logger.debug("DIDDashboardViewModel", "WalletState obtenido: $walletState", "")
                
                when (walletState) {
                    is com.aura.substratecryptotest.data.WalletState.Created -> {
                        val wallet = walletState.wallet
                        
                        // Obtener direcciones desde metadata
                        val addresses = wallet.metadata["addresses"] as? Map<*, *>
                        val kiltAddress = addresses?.get("KILT")?.toString()
                        
                        // Log detallado para debugging
                        Logger.debug("DIDDashboardViewModel", "=== INFORMACIÓN DE WALLET PARA DID ===", "")
                        Logger.debug("DIDDashboardViewModel", "Wallet: ${wallet.name}", "")
                        Logger.debug("DIDDashboardViewModel", "Address base: ${wallet.address}", "")
                        Logger.debug("DIDDashboardViewModel", "Direcciones en metadata: $addresses", "")
                        Logger.debug("DIDDashboardViewModel", "KILT Address: $kiltAddress", "")
                        Logger.debug("DIDDashboardViewModel", "KILT DID: ${wallet.kiltDid}", "")
                        
                        android.util.Log.d("DIDDashboardViewModel", "=== VERIFICANDO KILT DID ===")
                        android.util.Log.d("DIDDashboardViewModel", "wallet.kiltDid: ${wallet.kiltDid}")
                        android.util.Log.d("DIDDashboardViewModel", "wallet.kiltDid != null: ${wallet.kiltDid != null}")
                        android.util.Log.d("DIDDashboardViewModel", "wallet.kiltDid.isNotBlank(): ${wallet.kiltDid?.isNotBlank()}")
                        
                        // Verificar si hay DID disponible (tanto en kiltDid como en kiltDids)
                        val availableDid = wallet.kiltDid ?: wallet.kiltDids?.get("authentication")
                        if (availableDid != null && availableDid.isNotBlank()) {
                            // Wallet tiene DID derivado
                            android.util.Log.d("DIDDashboardViewModel", "✅ Wallet TIENE DID - Mostrando información")
                            android.util.Log.d("DIDDashboardViewModel", "DID encontrado: $availableDid")
                            val didInfo = DIDInfo(
                                did = availableDid,
                                kiltAddress = kiltAddress ?: "No disponible",
                                walletName = wallet.name,
                                walletAddress = wallet.address,
                                derivationPath = wallet.derivationPath ?: "",
                                publicKey = wallet.publicKey.joinToString("") { "%02x".format(it) }
                            )
                            
                            android.util.Log.d("DIDDashboardViewModel", "🔍 === ACTUALIZANDO UI STATE CON DID INFO ===")
                            android.util.Log.d("DIDDashboardViewModel", "🔍 didInfo creado: $didInfo")
                            android.util.Log.d("DIDDashboardViewModel", "🔍 didInfo.did: ${didInfo.did}")
                            android.util.Log.d("DIDDashboardViewModel", "🔍 didInfo.kiltAddress: ${didInfo.kiltAddress}")
                            
                            _uiState.value = _uiState.value.copy(
                                didInfo = didInfo,
                                isLoading = false,
                                error = null
                            )
                            
                            android.util.Log.d("DIDDashboardViewModel", "🔍 === UI STATE ACTUALIZADO ===")
                            android.util.Log.d("DIDDashboardViewModel", "🔍 _uiState.value.didInfo: ${_uiState.value.didInfo}")
                            android.util.Log.d("DIDDashboardViewModel", "🔍 _uiState.value.didInfo != null: ${_uiState.value.didInfo != null}")
                            
                            Logger.success("DIDDashboardViewModel", "✅ Información DID cargada", "DID: ${wallet.kiltDid}")
                        } else {
                            // Wallet no tiene DID derivado
                            android.util.Log.d("DIDDashboardViewModel", "⚠️ Wallet SIN DID - Mostrando EmptyDIDCard")
                            _uiState.value = _uiState.value.copy(
                                didInfo = null,
                                isLoading = false,
                                error = null
                            )
                            
                            Logger.debug("DIDDashboardViewModel", "⚠️ Wallet sin DID - Mostrando EmptyDIDCard", "Wallet: ${wallet.name}, kiltDid: ${wallet.kiltDid}")
                        }
                    }
                    is com.aura.substratecryptotest.data.WalletState.None -> {
                        _uiState.value = _uiState.value.copy(
                            didInfo = null,
                            isLoading = false,
                            error = "No hay wallet activa"
                        )
                        
                        Logger.error("DIDDashboardViewModel", "No hay wallet activa", "No se puede mostrar información DID", null)
                    }
                    is com.aura.substratecryptotest.data.WalletState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            didInfo = null,
                            isLoading = false,
                            error = walletState.message
                        )
                        
                        Logger.error("DIDDashboardViewModel", "Error en wallet", walletState.message, null)
                    }
                    else -> {
                        _uiState.value = _uiState.value.copy(
                            didInfo = null,
                            isLoading = false,
                            error = "Estado de wallet desconocido"
                        )
                    }
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
        
        viewModelScope.launch {
            try {
                Logger.debug("DIDDashboardViewModel", "=== INICIANDO DERIVACIÓN DID ===", "")
                android.util.Log.d("DIDDashboardViewModel", "=== INICIANDO DERIVACIÓN DID ===")
                
                // ✅ Verificar wallet actual antes de derivar
                val currentWalletState = walletStateManager.currentWallet.value
                android.util.Log.d("DIDDashboardViewModel", "Estado de wallet actual: $currentWalletState")
                
                if (currentWalletState is com.aura.substratecryptotest.data.WalletState.Created) {
                    val wallet = currentWalletState.wallet
                    android.util.Log.d("DIDDashboardViewModel", "Wallet encontrada: ${wallet.name}")
                    android.util.Log.d("DIDDashboardViewModel", "Mnemonic disponible: ${wallet.mnemonic.isNotEmpty()}")
                    android.util.Log.d("DIDDashboardViewModel", "Mnemonic palabras: ${wallet.mnemonic.split(" ").size}")
                } else {
                    android.util.Log.e("DIDDashboardViewModel", "❌ No hay wallet activa para derivar DID")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No hay wallet activa para derivar DID"
                    )
                    return@launch
                }
                
                // ✅ Usar WalletStateManager para derivar DID
                android.util.Log.d("DIDDashboardViewModel", "Llamando a walletStateManager.deriveDidFromCurrentWallet()")
                val did = walletStateManager.deriveDidFromCurrentWallet()
                android.util.Log.d("DIDDashboardViewModel", "Resultado de derivación: $did")
                
                if (did != null) {
                    Logger.success("DIDDashboardViewModel", "DID derivado exitosamente", "DID: $did")
                    android.util.Log.d("DIDDashboardViewModel", "✅ DID derivado exitosamente: $did")
                    
                    // ✅ Forzar actualización inmediata después de derivar DID
                    android.util.Log.d("DIDDashboardViewModel", "✅ DID derivado - Forzando actualización inmediata")
                    refreshDidInfo()
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                } else {
                    Logger.error("DIDDashboardViewModel", "Error derivando DID", "Resultado null")
                    android.util.Log.e("DIDDashboardViewModel", "❌ Error derivando DID - Resultado null")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "No se pudo derivar el DID"
                    )
                }
            } catch (e: Exception) {
                Logger.error("DIDDashboardViewModel", "Error derivando DID", e.message ?: "Error desconocido", e)
                android.util.Log.e("DIDDashboardViewModel", "❌ Error derivando DID: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error derivando DID: ${e.message}"
                )
            }
        }
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
