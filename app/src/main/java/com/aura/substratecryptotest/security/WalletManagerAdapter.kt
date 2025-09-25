package com.aura.substratecryptotest.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.wallet.WalletManager
import com.aura.substratecryptotest.wallet.WalletInfo
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adaptador que conecta WalletManager existente con SecureWalletFlowManager
 * Permite migración gradual y compatibilidad
 */
class WalletManagerAdapter(private val context: Context) {
    
    companion object {
        private const val TAG = "WalletManagerAdapter"
    }
    
    private val walletManager = WalletManager(context)
    private val secureFlowManager = SecureWalletFlowManager(context)
    private val secureWalletManager = SecureWalletManager(context)
    
    /**
     * Migra wallet existente a sistema seguro
     */
    suspend fun migrateToSecureWallet(
        activity: FragmentActivity,
        accountName: String
    ): MigrationResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Migrando wallet a sistema seguro", "Nombre: $accountName")
                
                // 1. Obtener wallet existente
                val existingWalletInfo = walletManager.getCurrentWalletInfo()
                if (existingWalletInfo == null) {
                    Logger.error(TAG, "No hay wallet existente", "No se puede migrar", null)
                    return@withContext MigrationResult.Error("No hay wallet existente")
                }
                
                // 2. Obtener mnemonic existente
                val existingMnemonic = walletManager.getCurrentWalletMnemonic()
                if (existingMnemonic == null) {
                    Logger.error(TAG, "No hay mnemonic existente", "No se puede migrar", null)
                    return@withContext MigrationResult.Error("No hay mnemonic existente")
                }
                
                // 3. Crear cuenta de fondos segura
                val fundsResult = secureFlowManager.createFundsAccount(
                    activity = activity,
                    accountName = accountName,
                    mnemonic = existingMnemonic
                )
                
                when (fundsResult) {
                    is SecureWalletFlowManager.FundsAccountResult.Success -> {
                        Logger.success(TAG, "Wallet migrada exitosamente", "Cuenta de fondos creada")
                        MigrationResult.Success(fundsResult.account)
                    }
                    is SecureWalletFlowManager.FundsAccountResult.Error -> {
                        Logger.error(TAG, "Error migrando wallet", fundsResult.message, null)
                        MigrationResult.Error(fundsResult.message)
                    }
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error en migración", e.message ?: "Error desconocido", e)
                MigrationResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Crea nueva wallet segura desde cero
     */
    suspend fun createNewSecureWallet(
        activity: FragmentActivity,
        accountName: String
    ): NewWalletResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Creando nueva wallet segura", "Nombre: $accountName")
                
                // 1. Generar nuevo mnemonic
                val mnemonic = generateNewMnemonic()
                if (mnemonic == null) {
                    Logger.error(TAG, "Error generando mnemonic", "No se puede crear wallet", null)
                    return@withContext NewWalletResult.Error("Error generando mnemonic")
                }
                
                // 2. Crear cuenta de fondos
                val fundsResult = secureFlowManager.createFundsAccount(
                    activity = activity,
                    accountName = accountName,
                    mnemonic = mnemonic
                )
                
                when (fundsResult) {
                    is SecureWalletFlowManager.FundsAccountResult.Success -> {
                        Logger.success(TAG, "Nueva wallet creada exitosamente", "Cuenta de fondos creada")
                        NewWalletResult.Success(fundsResult.account, mnemonic)
                    }
                    is SecureWalletFlowManager.FundsAccountResult.Error -> {
                        Logger.error(TAG, "Error creando nueva wallet", fundsResult.message, null)
                        NewWalletResult.Error(fundsResult.message)
                    }
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error creando nueva wallet", e.message ?: "Error desconocido", e)
                NewWalletResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Crea cuenta de identidad para wallet existente
     */
    suspend fun createIdentityForExistingWallet(
        activity: FragmentActivity,
        legalName: String
    ): IdentityCreationResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Creando identidad para wallet existente", "Nombre legal: $legalName")
                
                // 1. Obtener cuenta de fondos existente
                val existingWalletInfo = walletManager.getCurrentWalletInfo()
                if (existingWalletInfo == null) {
                    Logger.error(TAG, "No hay wallet existente", "No se puede crear identidad", null)
                    return@withContext IdentityCreationResult.Error("No hay wallet existente")
                }
                
                val existingMnemonic = walletManager.getCurrentWalletMnemonic()
                if (existingMnemonic == null) {
                    Logger.error(TAG, "No hay mnemonic existente", "No se puede crear identidad", null)
                    return@withContext IdentityCreationResult.Error("No hay mnemonic existente")
                }
                
                // 2. Crear cuenta de fondos temporal para el flujo
                val tempFundsAccount = SecureWalletFlowManager.FundsAccount(
                    name = existingWalletInfo.name,
                    mnemonic = existingMnemonic,
                    seed = ByteArray(64), // TODO: Obtener seed real
                    publicKey = ByteArray(32), // TODO: Obtener clave real
                    privateKey = ByteArray(32), // TODO: Obtener clave real
                    addresses = mapOf("substrate" to existingWalletInfo.address),
                    createdAt = System.currentTimeMillis()
                )
                
                // 3. Crear cuenta de identidad
                val identityResult = secureFlowManager.createIdentityAccount(
                    activity = activity,
                    legalName = legalName,
                    fundsAccount = tempFundsAccount
                )
                
                when (identityResult) {
                    is SecureWalletFlowManager.IdentityAccountResult.Success -> {
                        Logger.success(TAG, "Identidad creada exitosamente", "Nombre legal: $legalName")
                        IdentityCreationResult.Success(identityResult.wallet)
                    }
                    is SecureWalletFlowManager.IdentityAccountResult.Error -> {
                        Logger.error(TAG, "Error creando identidad", identityResult.message, null)
                        IdentityCreationResult.Error(identityResult.message)
                    }
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error creando identidad", e.message ?: "Error desconocido", e)
                IdentityCreationResult.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    /**
     * Genera nuevo mnemonic
     */
    private suspend fun generateNewMnemonic(): String? {
        return try {
            val mnemonicManager = com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager()
            mnemonicManager.generateMnemonic() // Este método devuelve String directamente
        } catch (e: Exception) {
            Logger.error(TAG, "Error generando mnemonic", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Verifica si hay wallet existente
     */
    fun hasExistingWallet(): Boolean {
        return walletManager.getCurrentWalletInfo() != null
    }
    
    /**
     * Verifica si hay wallet segura
     */
    fun hasSecureWallet(): Boolean {
        return secureWalletManager.hasStoredWallet()
    }
    
    /**
     * Obtiene información de wallet existente
     */
    fun getExistingWalletInfo(): WalletInfo? {
        return walletManager.getCurrentWalletInfo()
    }
    
    /**
     * Resultado de migración
     */
    sealed class MigrationResult {
        data class Success(val account: SecureWalletFlowManager.FundsAccount) : MigrationResult()
        data class Error(val message: String) : MigrationResult()
    }
    
    /**
     * Resultado de nueva wallet
     */
    sealed class NewWalletResult {
        data class Success(val account: SecureWalletFlowManager.FundsAccount, val mnemonic: String) : NewWalletResult()
        data class Error(val message: String) : NewWalletResult()
    }
    
    /**
     * Resultado de creación de identidad
     */
    sealed class IdentityCreationResult {
        data class Success(val wallet: SecureWalletFlowManager.CompleteWallet) : IdentityCreationResult()
        data class Error(val message: String) : IdentityCreationResult()
    }
}
