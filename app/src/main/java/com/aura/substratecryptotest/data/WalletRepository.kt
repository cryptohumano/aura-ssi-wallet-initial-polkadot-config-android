package com.aura.substratecryptotest.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aura.substratecryptotest.wallet.WalletManager
import com.aura.substratecryptotest.wallet.Wallet
import com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm
import com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import com.aura.substratecryptotest.utils.Logger
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repositorio para gestionar el estado de las wallets
 * Implementa el flujo correcto: mnemonic → validación → derivación → confirmación → guardado
 */
class WalletRepository private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: WalletRepository? = null
        
        fun getInstance(context: Context): WalletRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WalletRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val walletManager = WalletManager(context)
    private val mnemonicManager = MnemonicManager()
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("wallet_storage", Context.MODE_PRIVATE)
    
    private val _currentWallet = MutableLiveData<WalletState>()
    val currentWallet: LiveData<WalletState> = _currentWallet
    
    private val _isWalletCreated = MutableLiveData<Boolean>()
    val isWalletCreated: LiveData<Boolean> = _isWalletCreated
    
    // Estado del flujo de creación
    private val _generatedMnemonic = MutableLiveData<String?>()
    val generatedMnemonic: LiveData<String?> = _generatedMnemonic
    
    private val _walletAddresses = MutableLiveData<Map<String, String>?>()
    val walletAddresses: LiveData<Map<String, String>?> = _walletAddresses
    
    init {
        checkExistingWallet()
    }
    
    /**
     * Verifica si ya existe una wallet
     */
    private fun checkExistingWallet() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // TODO: Implementar verificación de wallet existente
                // Por ahora asumimos que no hay wallet
                _isWalletCreated.postValue(false)
                _currentWallet.postValue(WalletState.None)
            } catch (e: Exception) {
                Logger.error("WalletRepository", "Error verificando wallet existente", e.message ?: "Error desconocido", e)
                _isWalletCreated.postValue(false)
                _currentWallet.postValue(WalletState.None)
            }
        }
    }
    
    /**
     * Paso 1: Genera un mnemonic para validación del usuario
     */
    suspend fun generateMnemonicForValidation(): String {
        val mnemonic = mnemonicManager.generateMnemonic(Mnemonic.Length.TWELVE)
        _generatedMnemonic.postValue(mnemonic)
        Logger.debug("WalletRepository", "Mnemonic generado para validación", "Longitud: ${mnemonic.split(" ").size} palabras")
        return mnemonic
    }

    /**
     * Paso 2: Valida el mnemonic ingresado por el usuario
     */
    fun validateMnemonic(userMnemonic: String, originalMnemonic: String): Boolean {
        val isValid = userMnemonic.trim().lowercase() == originalMnemonic.trim().lowercase()
        Logger.debug("WalletRepository", "Validación de mnemonic", "Resultado: $isValid")
        return isValid
    }

    /**
     * Paso 3: Deriva la cuenta de fondos (sin path) y muestra direcciones
     */
    fun deriveFundsAccount(mnemonic: String, onSuccess: (Map<String, String>) -> Unit, onError: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.debug("WalletRepository", "Generando direcciones de cuenta de fondos", "Mnemonic: ${mnemonic.length} palabras")
                
                // Generar solo las direcciones (sin crear wallet)
                val addresses = walletManager.generateAddressesOnly(mnemonic, null)
                
                if (addresses.isNotEmpty()) {
                    val addressMap = addresses.mapKeys { it.key.toString() }.mapValues { it.value.toString() }
                    
                    Logger.success("WalletRepository", "Direcciones generadas", "Cantidad: ${addressMap.size}")
                    Logger.debug("WalletRepository", "Dirección KILT", addressMap["KILT"] ?: "No encontrada")
                    
                    _walletAddresses.postValue(addressMap)
                    onSuccess(addressMap)
                } else {
                    Logger.error("WalletRepository", "No se pudieron generar direcciones", "Mapa vacío", null)
                    onError("No se pudieron generar las direcciones")
                }

            } catch (e: Exception) {
                Logger.error("WalletRepository", "Error generando direcciones", e.message ?: "Error desconocido", e)
                onError("Error generando direcciones: ${e.message}")
            }
        }
    }

    /**
     * Paso 4: Crea la wallet final después de confirmación del usuario
     */
    fun createFinalWallet(
        walletName: String,
        validatedMnemonic: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.debug("WalletRepository", "Creando wallet final", "Nombre: $walletName")

                // Usar WalletManager para crear la wallet final (solo cuenta de fondos)
                walletManager.createFundsAccountOnly(
                    name = walletName,
                    mnemonic = validatedMnemonic, // Usar el mnemonic validado por el usuario
                    password = null,
                    cryptoType = EncryptionAlgorithm.SR25519
                )

                // Observar el resultado en el hilo principal
                withContext(Dispatchers.Main) {
                    walletManager.currentWallet.observeForever { wallet ->
                        if (wallet != null) {
                            // Persistir la wallet
                            persistWallet(wallet)
                            
                            Logger.success("WalletRepository", "Wallet final creada y persistida", wallet.name)
                            _currentWallet.postValue(WalletState.Created(wallet))
                            _isWalletCreated.postValue(true)
                            onSuccess()
                        }
                    }

                    walletManager.error.observeForever { error ->
                        if (error != null) {
                            Logger.error("WalletRepository", "Error creando wallet final", error, null)
                            onError(error)
                        }
                    }
                }

            } catch (e: Exception) {
                Logger.error("WalletRepository", "Error creando wallet final", e.message ?: "Error desconocido", e)
                onError("Error creando wallet: ${e.message}")
            }
        }
    }
    
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
                    createdAt = wallet.createdAt
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
                createdAt = wallet.createdAt
            )
        } ?: emptyList()
    }
    
    /**
     * Cambia la wallet activa
     */
    fun switchToWallet(walletName: String) {
        val wallets = walletManager.wallets.value ?: return
        val targetWallet = wallets.find { it.name == walletName }
        
        if (targetWallet != null) {
            walletManager.selectWallet(targetWallet.id)
            Logger.debug("WalletRepository", "Wallet cambiada", "Nueva wallet activa: $walletName")
        } else {
            Logger.error("WalletRepository", "Wallet no encontrada", "No se encontró wallet con nombre: $walletName", null)
        }
    }

    /**
     * Persiste una wallet en SharedPreferences
     */
    private fun persistWallet(wallet: Wallet) {
        try {
            val walletJson = Gson().toJson(wallet)
            sharedPreferences.edit()
                .putString("wallet_${wallet.name}", walletJson)
                .putString("current_wallet_name", wallet.name)
                .apply()
            
            Logger.success("WalletRepository", "Wallet persistida", "Nombre: ${wallet.name}")
        } catch (e: Exception) {
            Logger.error("WalletRepository", "Error persistiendo wallet", e.message ?: "Error desconocido", e)
        }
    }

    /**
     * Carga todas las wallets desde SharedPreferences
     */
    private fun loadPersistedWallets(): List<Wallet> {
        val wallets = mutableListOf<Wallet>()
        val allKeys = sharedPreferences.all.keys.filter { it.startsWith("wallet_") }
        
        for (key in allKeys) {
            try {
                val walletJson = sharedPreferences.getString(key, null)
                if (walletJson != null) {
                    val wallet = Gson().fromJson(walletJson, Wallet::class.java)
                    wallets.add(wallet)
                }
            } catch (e: Exception) {
                Logger.error("WalletRepository", "Error cargando wallet", "Key: $key, Error: ${e.message}", e)
            }
        }
        
        Logger.success("WalletRepository", "Wallets cargadas desde persistencia", "Cantidad: ${wallets.size}")
        return wallets
    }

    /**
     * Carga la wallet activa desde SharedPreferences
     */
    private fun loadCurrentWallet(): String? {
        return sharedPreferences.getString("current_wallet_name", null)
    }

    /**
     * Deriva DID desde la wallet actual
     */
    fun deriveDidFromCurrentWallet(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.debug("WalletRepository", "Iniciando derivación DID", "Desde wallet actual")
                
                val did = walletManager.deriveDidFromCurrentWallet()
                
                if (did != null) {
                    // Persistir wallet actualizada
                    val currentWallet = walletManager.currentWallet.value
                    if (currentWallet != null) {
                        persistWallet(currentWallet)
                    }
                    
                    Logger.success("WalletRepository", "DID derivado y persistido", "DID: $did")
                    onSuccess(did)
                } else {
                    Logger.error("WalletRepository", "Error derivando DID", "Resultado null", null)
                    onError("No se pudo derivar el DID")
                }
            } catch (e: Exception) {
                Logger.error("WalletRepository", "Error derivando DID", e.message ?: "Error desconocido", e)
                onError("Error derivando DID: ${e.message}")
            }
        }
    }

    /**
     * Inicializa el repositorio cargando wallets persistidas
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
                            }
                            Logger.success("WalletRepository", "Wallet activa restaurada", "Nombre: $currentWalletName")
                        }
                    }
                } else {
                    Logger.debug("WalletRepository", "No hay wallets persistidas", "Iniciando con lista vacía")
                }
            } catch (e: Exception) {
                Logger.error("WalletRepository", "Error inicializando repositorio", e.message ?: "Error desconocido", e)
            }
        }
    }
}

/**
 * Estados posibles de la wallet
 */
sealed class WalletState {
    object None : WalletState()
    data class Created(val wallet: com.aura.substratecryptotest.wallet.Wallet) : WalletState()
    data class Error(val message: String) : WalletState()
}

/**
 * Información de la wallet para la UI
 */
data class WalletInfo(
    val name: String,
    val address: String,
    val kiltAddress: String?,
    val polkadotAddress: String?,
    val createdAt: Long
)