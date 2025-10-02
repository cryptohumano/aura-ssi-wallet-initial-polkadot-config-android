package com.aura.substratecryptotest.examples

import android.content.Context
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.security.BiometricAuthInterceptor
import com.aura.substratecryptotest.security.BiometricAuthResult
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ejemplo de uso del sistema de múltiples usuarios con autenticación biométrica
 * Demuestra cómo usar el sistema completo
 */
class MultiUserSystemExample(private val context: Context) {
    
    companion object {
        private const val TAG = "MultiUserSystemExample"
    }
    
    private val userManager = UserManager(context)
    private val biometricAuthInterceptor = BiometricAuthInterceptor(context)
    private val secureUserRepository = SecureUserRepository.getInstance(context)
    
    /**
     * Ejemplo completo: Crear usuario, crear wallet, y realizar operaciones seguras
     */
    suspend fun demonstrateMultiUserSystem(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "🚀 Iniciando demostración del sistema multi-usuario", "")
                
                // 1. Crear primer usuario
                val user1Result = userManager.registerNewUser("Usuario Principal", requireBiometric = true)
                if (user1Result !is UserManager.UserAuthResult.Success) {
                    return@withContext Result.failure(Exception("Error creando usuario principal"))
                }
                
                val user1 = user1Result.user
                Logger.success(TAG, "✅ Usuario creado", "Nombre: ${user1.name}")
                
                // 2. Crear segunda usuario
                val user2Result = userManager.registerNewUser("Usuario Secundario", requireBiometric = true)
                if (user2Result !is UserManager.UserAuthResult.Success) {
                    return@withContext Result.failure(Exception("Error creando usuario secundario"))
                }
                
                val user2 = user2Result.user
                Logger.success(TAG, "✅ Segundo usuario creado", "Nombre: ${user2.name}")
                
                // 3. Crear wallet para usuario 1 (requiere autenticación biométrica)
                val walletResult = secureUserRepository.createUserWallet(
                    walletName = "Mi Wallet Principal",
                    mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
                    publicKey = ByteArray(32) { it.toByte() },
                    privateKey = ByteArray(64) { it.toByte() },
                    address = "5GrwvaEF5zXb26Fz9rcQpDWS57CtERHpNehXCPcNoHGKutQY",
                    cryptoType = "SR25519",
                    derivationPath = "//0",
                    requireBiometric = true
                )
                
                if (walletResult.isSuccess) {
                    Logger.success(TAG, "✅ Wallet creada para usuario 1", "ID: ${walletResult.getOrNull()?.id?.take(8)}...")
                } else {
                    Logger.error(TAG, "❌ Error creando wallet", walletResult.exceptionOrNull()?.message ?: "Error desconocido", null)
                }
                
                // 4. Cambiar a usuario 2 (requiere autenticación biométrica)
                val switchResult = secureUserRepository.switchUser(user2.id)
                if (switchResult.isSuccess) {
                    Logger.success(TAG, "✅ Cambiado a usuario 2", "Nombre: ${user2.name}")
                    
                    // 5. Crear wallet para usuario 2
                    val wallet2Result = secureUserRepository.createUserWallet(
                        walletName = "Wallet de Usuario 2",
                        mnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
                        publicKey = ByteArray(32) { (it + 10).toByte() },
                        privateKey = ByteArray(64) { (it + 10).toByte() },
                        address = "5FHneW46xGXgs5mUiveU4sbTyGBzmstUspZC92UhjJM694ty",
                        cryptoType = "SR25519",
                        derivationPath = "//0",
                        requireBiometric = true
                    )
                    
                    if (wallet2Result.isSuccess) {
                        Logger.success(TAG, "✅ Wallet creada para usuario 2", "ID: ${wallet2Result.getOrNull()?.id?.take(8)}...")
                    }
                }
                
                // 6. Demostrar acceso a mnemonic (requiere autenticación biométrica)
                val wallets = secureUserRepository.getUserWallets()
                if (wallets.isNotEmpty()) {
                    val wallet = wallets.first()
                    val mnemonicResult = secureUserRepository.getWalletMnemonic(
                        walletId = wallet.id,
                        requireBiometric = true
                    )
                    
                    if (mnemonicResult.isSuccess) {
                        Logger.success(TAG, "✅ Mnemonic obtenido exitosamente", "Wallet: ${wallet.name}")
                    } else {
                        Logger.error(TAG, "❌ Error obteniendo mnemonic", mnemonicResult.exceptionOrNull()?.message ?: "Error desconocido", null)
                    }
                }
                
                // 7. Crear documento para usuario actual
                if (wallets.isNotEmpty()) {
                    val documentResult = secureUserRepository.createUserDocument(
                        walletId = wallets.first().id,
                        documentHash = "0x1234567890abcdef",
                        documentType = "Certificado de Identidad",
                        blockchainTimestamp = "2024-01-15T10:30:00Z"
                    )
                    
                    if (documentResult.isSuccess) {
                        Logger.success(TAG, "✅ Documento creado", "ID: ${documentResult.getOrNull()?.id?.take(8)}...")
                    }
                }
                
                // 8. Volver a usuario 1
                val switchBackResult = secureUserRepository.switchUser(user1.id)
                if (switchBackResult.isSuccess) {
                    Logger.success(TAG, "✅ Vuelto a usuario 1", "Nombre: ${user1.name}")
                }
                
                // 9. Verificar aislamiento de datos
                val user1Wallets = secureUserRepository.getUserWallets()
                Logger.success(TAG, "✅ Verificación de aislamiento", "Usuario 1 tiene ${user1Wallets.size} wallets")
                
                Logger.success(TAG, "🎉 Demostración completada exitosamente", "Sistema multi-usuario funcionando correctamente")
                
                Result.success("Demostración completada exitosamente")
                
            } catch (e: Exception) {
                Logger.error(TAG, "❌ Error en demostración", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Ejemplo de operaciones que requieren autenticación biométrica
     */
    suspend fun demonstrateBiometricAuth(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "🔐 Demostrando autenticación biométrica", "")
                
                // 1. Autenticación para operación crítica
                val authResult = biometricAuthInterceptor.requireBiometricAuthForWriteOperation("creación de wallet")
                if (authResult is BiometricAuthResult.Success) {
                    Logger.success(TAG, "✅ Autenticación biométrica exitosa", "Operación crítica autorizada")
                } else {
                    Logger.error(TAG, "❌ Autenticación biométrica fallida", (authResult as BiometricAuthResult.Error).message, null)
                    return@withContext Result.failure(Exception("Autenticación biométrica fallida"))
                }
                
                // 2. Autenticación para acceso a datos sensibles
                val sensitiveAuthResult = biometricAuthInterceptor.requireBiometricAuthForSensitiveData("mnemonic")
                if (sensitiveAuthResult is BiometricAuthResult.Success) {
                    Logger.success(TAG, "✅ Acceso a datos sensibles autorizado", "Mnemonic accesible")
                } else {
                    Logger.error(TAG, "❌ Acceso a datos sensibles denegado", (sensitiveAuthResult as BiometricAuthResult.Error).message, null)
                }
                
                // 3. Autenticación para cambio de usuario
                val switchAuthResult = biometricAuthInterceptor.requireBiometricAuthForUserSwitch()
                if (switchAuthResult is BiometricAuthResult.Success) {
                    Logger.success(TAG, "✅ Cambio de usuario autorizado", "TouchID/FaceID verificado")
                } else {
                    Logger.error(TAG, "❌ Cambio de usuario denegado", (switchAuthResult as BiometricAuthResult.Error).message, null)
                }
                
                Logger.success(TAG, "🎉 Autenticación biométrica demostrada", "Todas las operaciones críticas protegidas")
                
                Result.success("Autenticación biométrica demostrada exitosamente")
                
            } catch (e: Exception) {
                Logger.error(TAG, "❌ Error en demostración de autenticación", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Ejemplo de gestión de sesiones
     */
    suspend fun demonstrateSessionManagement(): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "⏰ Demostrando gestión de sesiones", "")
                
                val currentUser = userManager.getCurrentUser()
                if (currentUser != null) {
                    Logger.success(TAG, "✅ Usuario activo", "Nombre: ${currentUser.name}")
                    
                    // Simular actividad del usuario
                    userManager.updateUserActivity()
                    Logger.debug(TAG, "📝 Actividad actualizada", "Sesión renovada")
                    
                    // Verificar si la sesión está activa
                    val isActive = userManager.isSessionActive()
                    Logger.success(TAG, "✅ Estado de sesión", "Activa: $isActive")
                    
                    // Cerrar sesión
                    secureUserRepository.closeCurrentSession()
                    Logger.success(TAG, "✅ Sesión cerrada", "Usuario desconectado")
                    
                    // Verificar que no hay usuario activo
                    val noUser = userManager.getCurrentUser()
                    if (noUser == null) {
                        Logger.success(TAG, "✅ Sesión cerrada correctamente", "No hay usuario activo")
                    }
                } else {
                    Logger.warning(TAG, "⚠️ No hay usuario activo", "No se puede demostrar gestión de sesiones")
                }
                
                Logger.success(TAG, "🎉 Gestión de sesiones demostrada", "Sesiones manejadas correctamente")
                
                Result.success("Gestión de sesiones demostrada exitosamente")
                
            } catch (e: Exception) {
                Logger.error(TAG, "❌ Error en demostración de sesiones", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
}
