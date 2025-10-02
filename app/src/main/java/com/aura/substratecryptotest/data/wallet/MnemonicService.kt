package com.aura.substratecryptotest.data.wallet

import com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar operaciones relacionadas con mnemónicos
 */
class MnemonicService {
    
    private val mnemonicManager = MnemonicManager()
    
    /**
     * Genera un mnemonic para validación del usuario
     */
    suspend fun generateMnemonicForValidation(): String {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("MnemonicService", "Generando mnemonic para validación", "")
                
                val mnemonic = mnemonicManager.generateMnemonic()
                
                Logger.success("MnemonicService", "Mnemonic generado exitosamente", "Mnemonic completo: $mnemonic")
                mnemonic
                
            } catch (e: Exception) {
                Logger.error("MnemonicService", "Error generando mnemonic", e.message ?: "Error desconocido", e)
                throw e
            }
        }
    }
    
    /**
     * Valida un mnemonic comparándolo con el original
     */
    fun validateMnemonic(userMnemonic: String, originalMnemonic: String): Boolean {
        return try {
            Logger.debug("MnemonicService", "Validando mnemonic", "Usuario: $userMnemonic | Original: $originalMnemonic")
            
            val isValid = userMnemonic.trim().lowercase() == originalMnemonic.trim().lowercase()
            
            if (isValid) {
                Logger.success("MnemonicService", "Mnemonic validado correctamente")
            } else {
                Logger.warning("MnemonicService", "Mnemonic no coincide", "")
            }
            
            isValid
            
        } catch (e: Exception) {
            Logger.error("MnemonicService", "Error validando mnemonic", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Valida un mnemonic y retorna un Result
     */
    fun validateMnemonicWithResult(userMnemonic: String, originalMnemonic: String): Result<Boolean> {
        return try {
            Logger.debug("MnemonicService", "Validando mnemonic con Result", "Usuario: ${userMnemonic.take(20)}...")
            
            val isValid = userMnemonic.trim().lowercase() == originalMnemonic.trim().lowercase()
            
            if (isValid) {
                Logger.success("MnemonicService", "Mnemonic validado correctamente")
                Result.success(true)
            } else {
                Logger.warning("MnemonicService", "Mnemonic no coincide", "")
                Result.success(false)
            }
            
        } catch (e: Exception) {
            Logger.error("MnemonicService", "Error validando mnemonic", e.message ?: "Error desconocido", e)
            Result.failure(e)
        }
    }
}
