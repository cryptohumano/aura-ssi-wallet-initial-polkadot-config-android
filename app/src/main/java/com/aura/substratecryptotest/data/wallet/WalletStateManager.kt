package com.aura.substratecryptotest.data.wallet

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aura.substratecryptotest.data.WalletInfo
import com.aura.substratecryptotest.data.WalletState
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.wallet.Wallet
import com.aura.substratecryptotest.wallet.WalletManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar el estado de las wallets
 * Implementa patrón Singleton para evitar múltiples instancias
 */
class WalletStateManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WalletStateManager? = null
        
        fun getInstance(context: Context): WalletStateManager {
            val threadId = Thread.currentThread().id
            val processId = android.os.Process.myPid()
            android.util.Log.d("WalletStateManager", "🔍 getInstance llamado - Thread: $threadId, Process: $processId, INSTANCE: ${INSTANCE != null}")
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WalletStateManager(context.applicationContext).also { 
                    android.util.Log.d("WalletStateManager", "🔍 NUEVA INSTANCIA CREADA - Thread: $threadId, Process: $processId")
                    INSTANCE = it 
                }
            }
        }
    }
    
    private val walletManager = WalletManager(context.applicationContext)
    private val sharedPreferences: SharedPreferences = context.applicationContext.getSharedPreferences("wallet_storage", Context.MODE_PRIVATE)
    
    private val _currentWallet = MutableLiveData<WalletState>()
    val currentWallet: LiveData<WalletState> = _currentWallet
    
    private val _isWalletCreated = MutableLiveData<Boolean>()
    val isWalletCreated: LiveData<Boolean> = _isWalletCreated
    
    /**
     * Obtiene información de la wallet actual
     */
    fun getCurrentWalletInfo(): WalletInfo? {
        return when (val walletState = _currentWallet.value) {
            is WalletState.Created -> {
                val wallet = walletState.wallet
                val addresses = wallet.metadata["addresses"] as? Map<*, *>
                val addressMap = addresses?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
                
                WalletInfo(
                    name = wallet.name,
                    address = wallet.address,
                    kiltAddress = addressMap["KILT"],
                    polkadotAddress = addressMap["POLKADOT"],
                    kusamaAddress = addressMap["KUSAMA"],
                    createdAt = wallet.createdAt,
                    kiltDid = wallet.kiltDid,
                    kiltDids = wallet.kiltDids,
                    derivationPath = wallet.derivationPath,
                    publicKey = wallet.publicKey.joinToString("") { "%02x".format(it) },
                    metadata = wallet.metadata
                )
            }
            else -> null
        }
    }
    
    /**
     * Obtiene todas las wallets disponibles
     */
    fun getAllWallets(): List<WalletInfo> {
        return walletManager.wallets.value?.map { wallet ->
            val addresses = wallet.metadata["addresses"] as? Map<*, *>
            val addressMap = addresses?.mapKeys { it.key.toString() }?.mapValues { it.value.toString() } ?: emptyMap()
            
            WalletInfo(
                name = wallet.name,
                address = wallet.address,
                kiltAddress = addressMap["KILT"],
                polkadotAddress = addressMap["POLKADOT"],
                kusamaAddress = addressMap["KUSAMA"],
                createdAt = wallet.createdAt,
                kiltDid = wallet.kiltDid,
                kiltDids = wallet.kiltDids,
                derivationPath = wallet.derivationPath,
                publicKey = wallet.publicKey.joinToString("") { "%02x".format(it) },
                metadata = wallet.metadata
            )
        } ?: emptyList()
    }
    
    /**
     * Sincroniza el estado actual con WalletManager
     */
    fun syncCurrentWalletState() {
        val currentWallet = walletManager.getCurrentWalletInfo()
        if (currentWallet != null) {
            // Convertir WalletInfo a Wallet para updateCurrentWalletState
            val wallet = Wallet(
                id = currentWallet.id,
                name = currentWallet.name,
                mnemonic = currentWallet.mnemonic,
                publicKey = currentWallet.publicKey.toByteArray(),
                privateKey = null, // ✅ WalletInfo no tiene privateKey, se establece como null
                address = currentWallet.address,
                cryptoType = currentWallet.cryptoType,
                derivationPath = currentWallet.derivationPath,
                createdAt = currentWallet.createdAt,
                kiltDid = currentWallet.kiltDid,
                kiltAddress = currentWallet.kiltAddress,
                kiltDids = currentWallet.kiltDids,
                metadata = mapOf("addresses" to mapOf(
                    "KILT" to currentWallet.kiltAddress
                ))
            )
            updateCurrentWalletState(wallet)
            Logger.debug("WalletStateManager", "Estado sincronizado", "Wallet: ${wallet.name}")
        } else {
            Logger.warning("WalletStateManager", "No hay wallet actual para sincronizar", "")
        }
    }
    
    /**
     * Cambia la wallet activa
     */
    fun switchToWallet(walletName: String) {
        if (walletName.isBlank()) {
            Logger.error("WalletStateManager", "Error cambiando wallet", "Nombre de wallet vacío", null)
            return
        }
        
        val wallets = walletManager.wallets.value ?: run {
            Logger.error("WalletStateManager", "Error cambiando wallet", "Lista de wallets vacía", null)
            return
        }
        
        val targetWallet = wallets.find { it.name == walletName }
        
        if (targetWallet != null) {
            walletManager.selectWallet(targetWallet.id)
            // ✅ Sincronizar estado después de cambiar wallet
            syncCurrentWalletState()
            Logger.debug("WalletStateManager", "Wallet cambiada", "Nueva wallet activa: $walletName")
        } else {
            Logger.error("WalletStateManager", "Wallet no encontrada", "No se encontró wallet con nombre: $walletName", null)
        }
    }
    
    /**
     * Actualiza el estado de la wallet actual
     */
    fun updateCurrentWalletState(wallet: Wallet) {
        android.util.Log.d("WalletStateManager", "=== ACTUALIZANDO CURRENT WALLET STATE ===")
        android.util.Log.d("WalletStateManager", "Wallet: ${wallet.name}")
        android.util.Log.d("WalletStateManager", "DID: ${wallet.kiltDid}")
        android.util.Log.d("WalletStateManager", "KiltDids: ${wallet.kiltDids}")
        _currentWallet.postValue(WalletState.Created(wallet))
        _isWalletCreated.postValue(true)
        android.util.Log.d("WalletStateManager", "✅ LiveData actualizado con postValue")
    }
    
    /**
     * Persiste una wallet en SharedPreferences
     */
    fun persistWallet(wallet: Wallet) {
        try {
            // Validar datos antes de persistir
            if (wallet.name.isBlank()) {
                Logger.error("WalletStateManager", "Error persistiendo wallet", "Nombre de wallet vacío", null)
                return
            }
            
            val walletJson = Gson().toJson(wallet)
            
            // Verificar que el JSON no esté vacío
            if (walletJson.isBlank()) {
                Logger.error("WalletStateManager", "Error persistiendo wallet", "JSON vacío", null)
                return
            }
            
            sharedPreferences.edit()
                .putString("wallet_${wallet.name}", walletJson)
                .putString("current_wallet_name", wallet.name)
                .apply()
            
            Logger.success("WalletStateManager", "Wallet persistida", "Nombre: ${wallet.name}")
        } catch (e: Exception) {
            Logger.error("WalletStateManager", "Error persistiendo wallet", e.message ?: "Error desconocido", e)
        }
    }
    
    /**
     * Carga todas las wallets desde SharedPreferences
     */
    fun loadPersistedWallets(): List<Wallet> {
        val wallets = mutableListOf<Wallet>()
        val allKeys = sharedPreferences.all.keys.filter { it.startsWith("wallet_") }
        
        Logger.debug("WalletStateManager", "Cargando wallets persistidas", "Claves encontradas: ${allKeys.size}")
        
        for (key in allKeys) {
            try {
                val walletJson = sharedPreferences.getString(key, null)
                if (walletJson != null && walletJson.isNotBlank()) {
                    val wallet = Gson().fromJson(walletJson, Wallet::class.java)
                    
                    // Validar wallet cargada
                    if (wallet.name.isNotBlank() && wallet.id.isNotBlank()) {
                        wallets.add(wallet)
                        Logger.debug("WalletStateManager", "Wallet cargada", "Nombre: ${wallet.name}")
                    } else {
                        Logger.error("WalletStateManager", "Wallet inválida ignorada", "Key: $key, Nombre: ${wallet.name}", null)
                    }
                } else {
                    Logger.error("WalletStateManager", "JSON vacío ignorado", "Key: $key", null)
                }
            } catch (e: Exception) {
                Logger.error("WalletStateManager", "Error cargando wallet", "Key: $key, Error: ${e.message}", e)
            }
        }
        
        Logger.success("WalletStateManager", "Wallets cargadas desde persistencia", "Cantidad: ${wallets.size}")
        return wallets
    }
    
    /**
     * Carga la wallet activa desde SharedPreferences
     */
    fun loadCurrentWallet(): String? {
        return sharedPreferences.getString("current_wallet_name", null)
    }
    
    /**
     * Deriva DID desde la wallet actual usando WalletManager
     */
    suspend fun deriveDidFromCurrentWallet(): String? {
        return try {
            Logger.debug("WalletStateManager", "=== DERIVANDO DID DESDE WALLET ACTUAL ===", "")
            
            val currentWalletState = _currentWallet.value
            if (currentWalletState is WalletState.Created) {
                val wallet = currentWalletState.wallet
                Logger.debug("WalletStateManager", "Wallet encontrada", "Nombre: ${wallet.name}")
                
                // ✅ Asegurar que WalletManager tenga la wallet cargada
                val currentWalletInManager = walletManager.currentWallet.value
                if (currentWalletInManager == null) {
                    Logger.warning("WalletStateManager", "WalletManager no tiene wallet activa, sincronizando...", "")
                    syncCurrentWalletState()
                }
                
                // Usar WalletManager para derivar DID
                val did = walletManager.deriveDidFromCurrentWallet()
                
                if (did != null) {
                    Logger.success("WalletStateManager", "DID derivado exitosamente", "DID: $did")
                    
                    // Sincronizar estado después de derivar DID
                    syncCurrentWalletState()
                    
                    did
                } else {
                    Logger.error("WalletStateManager", "Error derivando DID", "Resultado null")
                    null
                }
            } else {
                Logger.warning("WalletStateManager", "No hay wallet activa para derivar DID", "Estado: $currentWalletState")
                null
            }
        } catch (e: Exception) {
            Logger.error("WalletStateManager", "Error derivando DID", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Inicializa el estado de las wallets
     */
    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val persistedWallets = loadPersistedWallets()
                val currentWalletName = loadCurrentWallet()
                
                if (persistedWallets.isNotEmpty()) {
                    // Cargar wallets en WalletManager (debe ejecutarse en Main thread)
                    withContext(Dispatchers.Main) {
                        walletManager.loadWallets(persistedWallets)
                    }
                    
                    // Seleccionar wallet activa (debe ejecutarse en Main thread)
                    if (currentWalletName != null) {
                        val currentWallet = persistedWallets.find { it.name == currentWalletName }
                        if (currentWallet != null) {
                            withContext(Dispatchers.Main) {
                                walletManager.selectWallet(currentWallet.id)
                                // ✅ SINCRONIZAR ESTADO DESPUÉS DE SELECCIONAR
                                updateCurrentWalletState(currentWallet)
                            }
                            Logger.success("WalletStateManager", "Wallet activa restaurada", "Nombre: $currentWalletName")
                            
                            // ✅ SINCRONIZAR ESTADO DESPUÉS DE RESTAURAR
                            syncCurrentWalletState()
                        }
                    }
                } else {
                    Logger.debug("WalletStateManager", "No hay wallets persistidas", "Iniciando con lista vacía")
                }
            } catch (e: Exception) {
                Logger.error("WalletStateManager", "Error inicializando estado", e.message ?: "Error desconocido", e)
            }
        }
    }
}
