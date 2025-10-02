package com.aura.substratecryptotest.data.wallet

import android.content.Context
import com.aura.substratecryptotest.data.user.UserManagementService
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar la integración entre usuarios y wallets
 */
class UserWalletService(private val context: Context) {
    
    private val userManager = UserManager(context)
    private val userManagementService = UserManagementService(
        userRepository = AppDatabaseManager(context).userRepository,
        context = context
    )
    private val mnemonicManager = MnemonicManager()
    
    /**
     * Crea un usuario completo con wallet
     */
    suspend fun createCompleteUserWithWallet(
        userName: String,
        createWalletCallback: (String, String) -> Unit
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("UserWalletService", "=== CREANDO USUARIO COMPLETO CON WALLET ===", "")
                Logger.debug("UserWalletService", "Nombre: $userName", "")
                
                // 1. Crear usuario en Sistema A (UserManager)
                Logger.debug("UserWalletService", "1. Creando usuario en Sistema A (UserManager)...", "")
                val userResult = userManager.registerNewUser(userName)
                if (userResult is UserManager.UserAuthResult.Error) {
                    Logger.error("UserWalletService", "Error creando usuario en Sistema A", userResult.message, null)
                    return@withContext Result.failure(Exception(userResult.message))
                }
                
                val createdUser = (userResult as UserManager.UserAuthResult.Success).user
                Logger.success("UserWalletService", "Usuario creado en Sistema A", "Nombre: ${createdUser.name}")
                
                // Actualizar actividad del usuario
                userManager.updateUserActivity()
                Logger.success("UserWalletService", "Actividad de usuario actualizada", "")
                
                // 2. Generar mnemonic y crear wallet
                Logger.debug("UserWalletService", "2. Generando mnemonic y creando wallet...", "")
                val mnemonic = mnemonicManager.generateMnemonic()
                Logger.success("UserWalletService", "Mnemonic generado", "Mnemonic completo: $mnemonic")
                
                // 3. Llamar callback para crear wallet
                Logger.debug("UserWalletService", "3. Creando wallet...", "")
                createWalletCallback(userName, mnemonic)
                
                // 4. Crear usuario en Sistema B (UserManagementService)
                Logger.debug("UserWalletService", "4. Creando usuario en Sistema B (UserManagementService)...", "")
                val systemBUser = userManagementService.createUserFromWallet(
                    walletName = userName,
                    walletAddress = "temp_address", // Se actualizará cuando se cree la wallet
                    did = null
                )
                Logger.success("UserWalletService", "Usuario creado en Sistema B", "Nombre: $userName")
                
                // 5. Establecer como usuario actual en ambos sistemas
                Logger.debug("UserWalletService", "5. Estableciendo como usuario actual en ambos sistemas...", "")
                userManagementService.setCurrentUser(systemBUser)
                Logger.success("UserWalletService", "Usuario actual establecido", "Nombre: $userName")
                
                // Verificar usuario final
                val finalUser = userManager.getCurrentUser()
                Logger.success("UserWalletService", "Usuario final en UserManager", "Nombre: ${finalUser?.name}")
                Logger.success("UserWalletService", "Sesión activa en UserManager", "Estado: ${userManager.isSessionActive()}")
                
                Logger.success("UserWalletService", "=== USUARIO COMPLETO CREADO EXITOSAMENTE ===", "")
                Logger.success("UserWalletService", "Sistema A - Usuario", "Nombre: ${finalUser?.name}")
                Logger.success("UserWalletService", "Sistema B - Usuario", "Nombre: $userName")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Logger.error("UserWalletService", "Error creando usuario completo", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cambia al usuario especificado
     */
    suspend fun switchToUser(userName: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("UserWalletService", "=== CAMBIANDO A USUARIO: $userName ===", "")
                
                // Cambiar usuario en UserManager
                val switchResult = userManager.switchUser(userName)
                if (switchResult is UserManager.UserAuthResult.Error) {
                    Logger.error("UserWalletService", "Error cambiando usuario en UserManager", switchResult.message, null)
                    return@withContext Result.failure(Exception(switchResult.message))
                }
                
                Logger.success("UserWalletService", "Usuario cambiado exitosamente", "Nombre: $userName")
                Result.success(Unit)
            } catch (e: Exception) {
                Logger.error("UserWalletService", "Error cambiando usuario", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene usuarios sincronizados
     */
    suspend fun getSynchronizedUsers(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug("UserWalletService", "=== OBTENIENDO USUARIOS SINCRONIZADOS ===", "")
                
                // Obtener usuarios del Sistema A (UserManager)
                val systemAUsers = userManager.getRegisteredUsers().map { it.name }
                Logger.debug("UserWalletService", "Usuarios Sistema A", "Cantidad: ${systemAUsers.size}")
                
                // Obtener usuarios del Sistema B (UserManagementService)
                val systemBUsers = userManagementService.getAllActiveUsers().first().map { it.name }
                Logger.debug("UserWalletService", "Usuarios Sistema B", "Cantidad: ${systemBUsers.size}")
                
                // Retornar usuarios del Sistema A (más completo)
                val synchronizedUsers = systemAUsers
                Logger.success("UserWalletService", "Usuarios sincronizados", "Cantidad: ${synchronizedUsers.size}")
                
                synchronizedUsers
            } catch (e: Exception) {
                Logger.error("UserWalletService", "Error obteniendo usuarios sincronizados", e.message ?: "Error desconocido", e)
                emptyList()
            }
        }
    }
}
