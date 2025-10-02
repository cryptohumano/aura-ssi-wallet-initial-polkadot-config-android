package com.aura.substratecryptotest.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.aura.substratecryptotest.data.wallet.*
import com.aura.substratecryptotest.data.WalletState
import com.aura.substratecryptotest.data.WalletInfo
import com.aura.substratecryptotest.wallet.Wallet
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Repositorio principal para gestionar el estado de las wallets
 * Ahora utiliza servicios modulares para separar responsabilidades
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

    // Servicios modulares
    private val mnemonicService = MnemonicService()
    private val accountDerivationService = AccountDerivationService()
    private val walletCreationService = WalletCreationService(context)
    private val walletStateManager = WalletStateManager.getInstance(context)
    private val walletDeletionService = WalletDeletionService(context)
    private val walletRenameService = WalletRenameService(context) // ✅ Nuevo servicio
    private val userWalletService = UserWalletService(context)
    private val walletValidationService = WalletValidationService(context)
    
    // Gestión de observadores para evitar memory leaks
    private val observers = mutableListOf<Observer<*>>()
    
    // Estados expuestos
    val currentWallet: LiveData<WalletState> = walletStateManager.currentWallet
    val isWalletCreated: LiveData<Boolean> = walletStateManager.isWalletCreated
    
    // Estado del flujo de creación
    private val _generatedMnemonic = MutableLiveData<String?>()
    val generatedMnemonic: LiveData<String?> = _generatedMnemonic
    
    private val _walletAddresses = MutableLiveData<Map<String, String>?>()
    val walletAddresses: LiveData<Map<String, String>?> = _walletAddresses
    
    init {
        checkExistingWallet()
    }
    
    /**
     * Limpia todos los observadores para evitar memory leaks
     */
    fun cleanup() {
        observers.forEach { observer ->
            // Los observadores se remueven automáticamente
        }
        observers.clear()
        Logger.debug("WalletRepository", "Observadores limpiados", "Cantidad: ${observers.size}")
    }
    
    /**
     * Verifica si existe una wallet y la carga
     */
    private fun checkExistingWallet() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.debug("WalletRepository", "Verificando wallet existente", "")
                
                walletStateManager.initialize()
                
                // Verificar si hay wallet actual
                val currentWalletInfo = walletStateManager.getCurrentWalletInfo()
                if (currentWalletInfo != null) {
                    Logger.success("WalletRepository", "Wallet existente encontrada", "Nombre: ${currentWalletInfo.name}")
                } else {
                    Logger.debug("WalletRepository", "No hay wallet existente", "Iniciando estado vacío")
                }
            } catch (e: Exception) {
                Logger.error("WalletRepository", "Error verificando wallet existente", e.message ?: "Error desconocido", e)
            }
        }
    }
    
    // ==================== FUNCIONES DE MNEMÓNICOS ====================
    
    /**
     * Genera un mnemonic para validación del usuario
     */
    suspend fun generateMnemonicForValidation(): String {
        val mnemonic = mnemonicService.generateMnemonicForValidation()
        _generatedMnemonic.postValue(mnemonic)
        return mnemonic
    }
    
    /**
     * Valida un mnemonic comparándolo con el original
     */
    fun validateMnemonic(userMnemonic: String, originalMnemonic: String): Boolean {
        return mnemonicService.validateMnemonic(userMnemonic, originalMnemonic)
    }
    
    /**
     * Valida un mnemonic y retorna un Result
     */
    fun validateMnemonicWithResult(userMnemonic: String, originalMnemonic: String): Result<Boolean> {
        return mnemonicService.validateMnemonicWithResult(userMnemonic, originalMnemonic)
    }
    
    // ==================== FUNCIONES DE DERIVACIÓN ====================
    
    /**
     * Deriva una cuenta de fondos desde un mnemonic (versión con callbacks)
     */
    fun deriveFundsAccount(mnemonic: String, onSuccess: (Map<String, String>) -> Unit, onError: (String) -> Unit) {
        accountDerivationService.deriveFundsAccount(mnemonic, onSuccess, onError)
    }
    
    /**
     * Deriva una cuenta de fondos desde un mnemonic (versión con Result)
     */
    suspend fun deriveFundsAccountWithResult(mnemonic: String): Result<Map<String, String>> {
        val result = accountDerivationService.deriveFundsAccountWithResult(mnemonic)
        if (result.isSuccess) {
            _walletAddresses.postValue(result.getOrNull())
        }
        return result
    }
    
    // ==================== FUNCIONES DE CREACIÓN ====================
    
    /**
     * Crea una wallet final usando WalletManager (versión con callbacks)
     */
    fun createFinalWallet(
        walletName: String,
        validatedMnemonic: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        walletCreationService.createFinalWallet(
            walletName = walletName,
            validatedMnemonic = validatedMnemonic,
            onSuccess = onSuccess,
            onError = onError,
            onWalletCreated = { wallet ->
                walletStateManager.updateCurrentWalletState(wallet)
                walletStateManager.persistWallet(wallet)
            },
            observers = observers
        )
    }
    
    /**
     * Versión con Result<T> para creación de wallet final
     */
    suspend fun createFinalWalletWithResult(walletName: String, validatedMnemonic: String): Result<Unit> {
        return walletCreationService.createFinalWalletWithResult(
            walletName = walletName,
            validatedMnemonic = validatedMnemonic,
            onWalletCreated = { wallet ->
                walletStateManager.updateCurrentWalletState(wallet)
                walletStateManager.persistWallet(wallet)
            },
            observers = observers
        )
    }
    
    // ==================== FUNCIONES DE ESTADO ====================
    
    /**
     * Obtiene información de la wallet actual
     */
    fun getCurrentWalletInfo(): WalletInfo? {
        return walletStateManager.getCurrentWalletInfo()
    }
    
    /**
     * Obtiene todas las wallets disponibles
     */
    fun getAllWallets(): List<WalletInfo> {
        return walletStateManager.getAllWallets()
    }
    
    /**
     * Cambia la wallet activa
     */
    fun switchToWallet(walletName: String) {
        walletStateManager.switchToWallet(walletName)
    }
    
    // ==================== FUNCIONES DE DID ====================
    
    /**
     * Deriva DID desde la wallet actual
     */
    fun deriveDidFromCurrentWallet(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        walletValidationService.deriveDidFromCurrentWallet(
            onSuccess = onSuccess,
            onError = onError,
            persistWalletCallback = { wallet ->
                walletStateManager.persistWallet(wallet)
            }
        )
    }
    
    // ==================== FUNCIONES DE USUARIOS ====================
    
    /**
     * Crea un usuario completo con wallet
     */
    suspend fun createCompleteUserWithWallet(userName: String): Result<Unit> {
        return userWalletService.createCompleteUserWithWallet(userName) { walletName, mnemonic ->
            createFinalWallet(
                walletName = walletName,
                validatedMnemonic = mnemonic,
                onSuccess = {
                    Logger.success("WalletRepository", "Wallet creada exitosamente", "Nombre: $walletName")
                },
                onError = { error ->
                    Logger.error("WalletRepository", "Error creando wallet", error, null)
                }
            )
        }
    }
    
    /**
     * Cambia al usuario especificado
     */
    suspend fun switchToUser(userName: String): Result<Unit> {
        return userWalletService.switchToUser(userName)
    }
    
    /**
     * Obtiene usuarios sincronizados
     */
    suspend fun getSynchronizedUsers(): List<String> {
        return userWalletService.getSynchronizedUsers()
    }
    
    // ==================== FUNCIONES DE VALIDACIÓN ====================
    
    /**
     * Valida parámetros comunes utilizados en el repositorio
     */
    fun validateCommonParameters(walletName: String? = null, mnemonic: String? = null): Result<Unit> {
        return walletValidationService.validateCommonParameters(walletName, mnemonic)
    }
    
    // ==================== FUNCIONES DE ELIMINACIÓN ====================
    
    /**
     * Borra una wallet por nombre
     */
    suspend fun deleteWalletByName(walletName: String): Result<Unit> {
        return walletDeletionService.deleteWalletByName(walletName)
    }
    
    // ==================== FUNCIONES DE RENOMBRADO ====================
    
    /**
     * Renombra una wallet por nombre
     */
    suspend fun renameWalletByName(oldName: String, newName: String): Result<Unit> {
        return walletRenameService.renameWalletByName(oldName, newName)
    }
    
    /**
     * Inicializa el repositorio cargando wallets persistidas
     */
    fun initialize() {
        walletStateManager.initialize()
    }
}