package com.aura.substratecryptotest.data.wallet

import android.content.Context
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.wallet.WalletManager
import com.aura.substratecryptotest.security.KeyStoreManager
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.wallet.WalletStateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar la eliminación de wallets
 */
class WalletDeletionService(private val context: Context) {
    
    private val walletManager = WalletManager(context)
    private val keyStoreManager = KeyStoreManager(context)
    private val userManager = UserManager(context)
    private val walletStateManager = WalletStateManager.getInstance(context)
    
    /**
     * Borra una wallet por nombre y todos sus datos relacionados
     */
    suspend fun deleteWalletByName(walletName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("WalletDeletionService", "🔍 === ELIMINANDO WALLET COMPLETA ===", "Nombre: $walletName")
                
                // 1. Buscar la wallet por nombre usando WalletStateManager (igual que DashboardViewModel)
                val allWallets = walletStateManager.getAllWallets()
                Logger.debug("WalletDeletionService", "📋 Total wallets en manager", "${allWallets.size}")
                
                // Log de todas las wallets para debug
                allWallets.forEachIndexed { index, wallet ->
                    Logger.debug("WalletDeletionService", "📦 Wallet $index", "Nombre: '${wallet.name}', Address: ${wallet.address}")
                }
                
                Logger.debug("WalletDeletionService", "🔎 Buscando wallet", "Nombre exacto a buscar: '$walletName'")
                val walletToDelete = allWallets.find { it.name == walletName }
                
                if (walletToDelete == null) {
                    Logger.warning("WalletDeletionService", "⚠️ Wallet no encontrada", "Nombre: $walletName")
                    Logger.warning("WalletDeletionService", "📋 Wallets disponibles", allWallets.map { "'${it.name}'" }.joinToString(", "))
                    return@withContext Result.failure(IllegalArgumentException("Wallet '$walletName' no encontrada"))
                }
                
                Logger.debug("WalletDeletionService", "Wallet encontrada", "Nombre: ${walletToDelete.name}, Address: ${walletToDelete.address}")
                
                // 2. Buscar usuario asociado a esta wallet
                val registeredUsers = userManager.getRegisteredUsers()
                val associatedUser = registeredUsers.find { it.name == walletName }
                
                if (associatedUser != null) {
                    Logger.debug("WalletDeletionService", "Usuario asociado encontrado", "ID: ${associatedUser.id}")
                    
                    // 3. Eliminar datos del KeyStore del usuario
                    val keyStoreCleared = keyStoreManager.clearUserData(associatedUser.id)
                    if (keyStoreCleared) {
                        Logger.success("WalletDeletionService", "Datos del KeyStore eliminados", "Usuario: ${associatedUser.id}")
                    } else {
                        Logger.warning("WalletDeletionService", "Error eliminando datos del KeyStore", "Usuario: ${associatedUser.id}")
                    }
                    
                    // 4. Eliminar usuario del UserManager
                    try {
                        val userDeleted = userManager.deleteUser(associatedUser.id)
                        if (userDeleted) {
                            Logger.success("WalletDeletionService", "Usuario eliminado del UserManager", "ID: ${associatedUser.id}")
                        } else {
                            Logger.warning("WalletDeletionService", "Error eliminando usuario del UserManager", "ID: ${associatedUser.id}")
                        }
                    } catch (e: Exception) {
                        Logger.warning("WalletDeletionService", "Error eliminando usuario del UserManager", e.message ?: "Error desconocido")
                    }
                } else {
                    Logger.debug("WalletDeletionService", "No se encontró usuario asociado", "Wallet: $walletName")
                }
                
                // 6. Borrar wallet del WalletManager usando el nombre para encontrar el ID
                val walletManagerWallets = walletManager.wallets.value ?: emptyList()
                val walletToDeleteFromManager = walletManagerWallets.find { it.name == walletName }
                
                if (walletToDeleteFromManager != null) {
                    try {
                        walletManager.deleteWallet(walletToDeleteFromManager.id)
                        Logger.success("WalletDeletionService", "Wallet eliminada del WalletManager", "ID: ${walletToDeleteFromManager.id}")
                    } catch (e: Exception) {
                        Logger.warning("WalletDeletionService", "Error eliminando wallet del WalletManager", "ID: ${walletToDeleteFromManager.id}, Error: ${e.message}")
                    }
                } else {
                    Logger.warning("WalletDeletionService", "Wallet no encontrada en WalletManager para eliminar", "Nombre: $walletName")
                }
                
                // 7. Eliminar wallet de SharedPreferences
                removeWalletFromSharedPreferences(walletName)
                Logger.success("WalletDeletionService", "Wallet eliminada de SharedPreferences", "Nombre: $walletName")
                
                // 8. Sincronizar WalletStateManager
                walletStateManager.syncCurrentWalletState()
                Logger.success("WalletDeletionService", "WalletStateManager sincronizado", "Nombre: $walletName")
                
                // 9. Recargar wallets en WalletManager para sincronizar (en Main thread)
                val updatedWallets = walletStateManager.loadPersistedWallets()
                withContext(Dispatchers.Main) {
                    walletManager.loadWallets(updatedWallets)
                }
                Logger.success("WalletDeletionService", "WalletManager recargado", "Wallets actualizadas: ${updatedWallets.size}")
                
                Logger.success("WalletDeletionService", "=== WALLET ELIMINADA COMPLETAMENTE ===", "Nombre: $walletName")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Logger.error("WalletDeletionService", "Error eliminando wallet", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Elimina una wallet de SharedPreferences
     */
    private fun removeWalletFromSharedPreferences(walletName: String) {
        try {
            val sharedPreferences = context.getSharedPreferences("wallet_storage", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            
            // Eliminar la entrada de la wallet
            editor.remove("wallet_$walletName")
            
            // Si era la wallet actual, limpiar también la referencia
            val currentWalletName = sharedPreferences.getString("current_wallet_name", null)
            if (currentWalletName == walletName) {
                editor.remove("current_wallet_name")
                Logger.debug("WalletDeletionService", "Wallet actual eliminada", "Limpiando referencia")
            }
            
            editor.apply()
            Logger.success("WalletDeletionService", "Wallet eliminada de SharedPreferences", "Nombre: $walletName")
        } catch (e: Exception) {
            Logger.error("WalletDeletionService", "Error eliminando wallet de SharedPreferences", e.message ?: "Error desconocido", e)
        }
    }
}
