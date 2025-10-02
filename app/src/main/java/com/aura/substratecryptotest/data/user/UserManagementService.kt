package com.aura.substratecryptotest.data.user

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Date

/**
 * Servicio para gestión de usuarios y distribución de bitácoras
 */
class UserManagementService(
    val userRepository: UserRepository,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "UserManagementService"
    }
    
    private var currentUser: User? = null
    private var currentSession: UserSession? = null
    private val profileImageService = ProfileImageService(context)
    
    // ===== USER MANAGEMENT =====
    
    /**
     * Crea un nuevo usuario cuando se crea una wallet
     */
    suspend fun createUserFromWallet(
        walletName: String,
        walletAddress: String,
        did: String? = null
    ): User {
        Log.i(TAG, "Creando usuario desde wallet: $walletName")
        
        // Generar imagen de perfil por defecto
        val profileImagePath = profileImageService.generateDefaultProfileImage(walletName)
        
        val user = userRepository.createUser(
            name = walletName,
            walletAddress = walletAddress,
            did = did
        )
        
        // Actualizar usuario con imagen de perfil si se generó
        if (profileImagePath != null) {
            val updatedUser = user.copy(profileImagePath = profileImagePath)
            userRepository.updateUser(updatedUser)
            Log.i(TAG, "Usuario creado con imagen de perfil: ${updatedUser.name}")
            return updatedUser
        }
        
        Log.i(TAG, "Usuario creado con ID: ${user.id}")
        return user
    }
    
    /**
     * Obtiene el usuario actual
     */
    fun getCurrentUser(): User? = currentUser
    
    /**
     * Establece el usuario actual
     */
    fun setCurrentUser(user: User) {
        currentUser = user
        Log.i(TAG, "Usuario actual establecido: ${user.name}")
    }
    
    /**
     * Obtiene todos los usuarios activos
     */
    fun getAllActiveUsers(): Flow<List<User>> {
        return userRepository.getAllActiveUsers()
    }
    
    /**
     * Busca usuario por dirección de wallet
     */
    suspend fun findUserByWalletAddress(walletAddress: String): User? {
        return userRepository.getUserByWalletAddress(walletAddress)
    }
    
    // ===== LOGBOOK DISTRIBUTION =====
    
    /**
     * Distribuye todas las bitácoras existentes entre los usuarios disponibles
     */
    suspend fun distributeExistingLogbooks() {
        Log.i(TAG, "Iniciando distribución de bitácoras existentes...")
        
        try {
            val users = userRepository.getAllActiveUsers().first()
            Log.i(TAG, "Usuarios encontrados: ${users.size}")
            
            if (users.isEmpty()) {
                Log.w(TAG, "No hay usuarios disponibles para distribuir bitácoras")
                return
            }
            
            // Aquí necesitaríamos acceso al repositorio de bitácoras
            // Por ahora solo logueamos la intención
            Log.i(TAG, "Distribución de bitácoras completada")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error distribuyendo bitácoras: ${e.message}", e)
        }
    }
    
    /**
     * Asocia una bitácora con el usuario actual
     */
    suspend fun associateLogbookWithCurrentUser(
        logbookId: Long,
        role: LogbookRole = LogbookRole.OWNER
    ): Boolean {
        val user = currentUser ?: run {
            Log.e(TAG, "No hay usuario actual para asociar bitácora")
            return false
        }
        
        try {
            userRepository.associateUserWithLogbook(user.id, logbookId, role)
            Log.i(TAG, "Bitácora $logbookId asociada con usuario ${user.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error asociando bitácora: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Obtiene las bitácoras del usuario actual
     */
    fun getCurrentUserLogbooks(): Flow<List<UserLogbook>> {
        val user = currentUser ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return userRepository.getLogbooksByUser(user.id)
    }
    
    /**
     * Verifica si el usuario actual puede editar una bitácora
     */
    suspend fun canUserEditLogbook(logbookId: Long): Boolean {
        val user = currentUser ?: return false
        
        val association = userRepository.getUserLogbookAssociation(user.id, logbookId)
        return association?.canEdit ?: false
    }
    
    /**
     * Verifica si el usuario actual puede exportar una bitácora
     */
    suspend fun canUserExportLogbook(logbookId: Long): Boolean {
        val user = currentUser ?: return false
        
        val association = userRepository.getUserLogbookAssociation(user.id, logbookId)
        return association?.canExport ?: false
    }
    
    // ===== SESSION MANAGEMENT =====
    
    /**
     * Crea una sesión para el usuario actual
     */
    suspend fun createUserSession(expiresInHours: Int = 24): UserSession? {
        val user = currentUser ?: return null
        
        try {
            val session = userRepository.createSession(user.id, expiresInHours)
            currentSession = session
            userRepository.updateLastLogin(user.id)
            
            Log.i(TAG, "Sesión creada para usuario ${user.name}")
            return session
        } catch (e: Exception) {
            Log.e(TAG, "Error creando sesión: ${e.message}", e)
            return null
        }
    }
    
    /**
     * Verifica la sesión actual
     */
    suspend fun isCurrentSessionValid(): Boolean {
        val session = currentSession ?: return false
        return userRepository.isSessionValid(session.sessionToken)
    }
    
    /**
     * Cierra la sesión actual
     */
    suspend fun closeCurrentSession() {
        val user = currentUser ?: return
        
        try {
            userRepository.deactivateAllSessionsForUser(user.id)
            currentSession = null
            Log.i(TAG, "Sesión cerrada para usuario ${user.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Error cerrando sesión: ${e.message}", e)
        }
    }
    
    // ===== BIOMETRIC MANAGEMENT =====
    
    /**
     * Habilita la biometría para el usuario actual
     */
    suspend fun enableBiometricForCurrentUser(): Boolean {
        val user = currentUser ?: return false
        
        try {
            userRepository.enableBiometric(user.id)
            Log.i(TAG, "Biometría habilitada para usuario ${user.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error habilitando biometría: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Deshabilita la biometría para el usuario actual
     */
    suspend fun disableBiometricForCurrentUser(): Boolean {
        val user = currentUser ?: return false
        
        try {
            userRepository.disableBiometric(user.id)
            Log.i(TAG, "Biometría deshabilitada para usuario ${user.name}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error deshabilitando biometría: ${e.message}", e)
            return false
        }
    }
    
    /**
     * Verifica si el usuario actual tiene biometría habilitada
     */
    suspend fun isBiometricEnabledForCurrentUser(): Boolean {
        val user = currentUser ?: return false
        val updatedUser = userRepository.getUserById(user.id)
        return updatedUser?.biometricEnabled ?: false
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Obtiene estadísticas del sistema de usuarios
     */
    suspend fun getSystemStats(): UserSystemStats {
        val users = userRepository.getAllActiveUsers().first()
        
        return UserSystemStats(
            totalUsers = users.size,
            usersWithBiometric = users.count { it.biometricEnabled },
            usersWithWallet = users.count { !it.walletAddress.isNullOrEmpty() },
            usersWithDID = users.count { !it.did.isNullOrEmpty() }
        )
    }
}

/**
 * Estadísticas del sistema de usuarios
 */
data class UserSystemStats(
    val totalUsers: Int,
    val usersWithBiometric: Int,
    val usersWithWallet: Int,
    val usersWithDID: Int
)
