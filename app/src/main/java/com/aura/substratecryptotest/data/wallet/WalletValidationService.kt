package com.aura.substratecryptotest.data.wallet

import android.content.Context
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.wallet.WalletManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar operaciones DID y validaciones
 */
class WalletValidationService(private val context: Context) {
    
    private val walletManager = WalletManager(context)
    
    /**
     * Deriva DID desde la wallet actual
     */
    fun deriveDidFromCurrentWallet(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit,
        persistWalletCallback: (com.aura.substratecryptotest.wallet.Wallet) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.debug("WalletValidationService", "Iniciando derivación DID", "Desde wallet actual")
                
                // Validar que existe una wallet actual
                val currentWallet = walletManager.currentWallet.value
                if (currentWallet == null) {
                    Logger.error("WalletValidationService", "Error derivando DID", "No hay wallet actual", null)
                    onError("No hay wallet actual para derivar DID")
                    return@launch
                }
                
                val did = walletManager.deriveDidFromCurrentWallet()
                
                if (did != null && did.isNotBlank()) {
                    // Persistir wallet actualizada
                    persistWalletCallback(currentWallet)
                    
                    Logger.success("WalletValidationService", "DID derivado y persistido", "DID: $did")
                    onSuccess(did)
                } else {
                    Logger.error("WalletValidationService", "Error derivando DID", "Resultado null o vacío", null)
                    onError("No se pudo derivar el DID")
                }
            } catch (e: Exception) {
                Logger.error("WalletValidationService", "Error derivando DID", e.message ?: "Error desconocido", e)
                onError("Error derivando DID: ${e.message}")
            }
        }
    }
    
    /**
     * Valida parámetros comunes utilizados en el repositorio
     */
    fun validateCommonParameters(walletName: String? = null, mnemonic: String? = null): Result<Unit> {
        return try {
            Logger.debug("WalletValidationService", "Validando parámetros comunes", "WalletName: ${walletName?.take(10)}..., Mnemonic: ${mnemonic?.take(10)}...")
            
            // Validar nombre de wallet si se proporciona
            if (walletName != null) {
                if (walletName.isBlank()) {
                    Logger.error("WalletValidationService", "Error validando parámetros", "Nombre de wallet vacío", null)
                    return Result.failure(IllegalArgumentException("El nombre de wallet no puede estar vacío"))
                }
                
                if (walletName.length < 3) {
                    Logger.error("WalletValidationService", "Error validando parámetros", "Nombre de wallet muy corto", null)
                    return Result.failure(IllegalArgumentException("El nombre de wallet debe tener al menos 3 caracteres"))
                }
            }
            
            // Validar mnemonic si se proporciona
            if (mnemonic != null) {
                if (mnemonic.isBlank()) {
                    Logger.error("WalletValidationService", "Error validando parámetros", "Mnemonic vacío", null)
                    return Result.failure(IllegalArgumentException("El mnemonic no puede estar vacío"))
                }
                
                val words = mnemonic.trim().split("\\s+".toRegex())
                if (words.size != 12 && words.size != 24) {
                    Logger.error("WalletValidationService", "Error validando parámetros", "Mnemonic debe tener 12 o 24 palabras", null)
                    return Result.failure(IllegalArgumentException("El mnemonic debe tener 12 o 24 palabras"))
                }
            }
            
            Logger.success("WalletValidationService", "Parámetros validados correctamente", "")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Logger.error("WalletValidationService", "Error validando parámetros", e.message ?: "Error desconocido", e)
            Result.failure(e)
        }
    }
}
