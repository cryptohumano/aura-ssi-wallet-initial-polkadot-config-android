package com.aura.substratecryptotest.data.wallet

import android.content.Context
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.wallet.WalletManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar el renombrado de wallets
 */
class WalletRenameService(private val context: Context) {
    
    private val walletManager = WalletManager(context)
    
    /**
     * Renombra una wallet por nombre
     */
    suspend fun renameWalletByName(oldName: String, newName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("WalletRenameService", "Renombrando wallet", "De: $oldName a: $newName")
                
                // Validar parámetros
                if (oldName.isBlank() || newName.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Los nombres no pueden estar vacíos"))
                }
                
                if (oldName == newName) {
                    return@withContext Result.failure(IllegalArgumentException("El nuevo nombre debe ser diferente al actual"))
                }
                
                // Buscar la wallet por nombre usando WalletManager directamente
                val allWallets = walletManager.wallets.value ?: emptyList()
                val walletToRename = allWallets.find { it.name == oldName }
                
                if (walletToRename == null) {
                    Logger.warning("WalletRenameService", "Wallet no encontrada", "Nombre: $oldName")
                    return@withContext Result.failure(IllegalArgumentException("Wallet '$oldName' no encontrada"))
                }
                
                // Verificar que el nuevo nombre no esté en uso
                val existingWallet = allWallets.find { it.name == newName }
                if (existingWallet != null) {
                    Logger.warning("WalletRenameService", "Nombre ya en uso", "Nombre: $newName")
                    return@withContext Result.failure(IllegalArgumentException("Ya existe una wallet con el nombre '$newName'"))
                }
                
                // Renombrar usando WalletManager
                walletManager.renameWallet(walletToRename.id, newName)
                
                Logger.success("WalletRenameService", "Wallet renombrada exitosamente", "De: $oldName a: $newName, ID: ${walletToRename.id}")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Logger.error("WalletRenameService", "Error renombrando wallet", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
}
