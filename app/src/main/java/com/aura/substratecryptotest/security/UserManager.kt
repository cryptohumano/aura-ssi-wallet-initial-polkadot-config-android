package com.aura.substratecryptotest.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.security.MessageDigest
import java.util.UUID

/**
 * Gestor de múltiples usuarios con autenticación biométrica
 * Cada usuario tiene su propia base de datos y documentos aislados
 */
class UserManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UserManager"
        private const val USERS_PREFS = "users_management"
        private const val CURRENT_USER_KEY = "current_user_id"
        private const val SESSION_TIMEOUT = 5 * 60 * 1000L // ✅ 5 minutos (más apropiado para móvil)
    }
    
    private val biometricManager = BiometricManager.from(context)
    private val keyStoreManager = KeyStoreManager(context)
    private val prefs = context.getSharedPreferences(USERS_PREFS, Context.MODE_PRIVATE)
    
    // Estado actual
    private var currentUser: User? = null
    private var lastActivityTime: Long = 0L
    
    /**
     * Datos de usuario
     */
    data class User(
        val id: String,
        val name: String,
        val biometricId: String, // Identificador único basado en biometría
        val createdAt: Long,
        val lastLogin: Long,
        val isActive: Boolean = true
    )
    
    /**
     * Resultado de autenticación de usuario
     */
    sealed class UserAuthResult {
        data class Success(val user: User) : UserAuthResult()
        data class Error(val message: String) : UserAuthResult()
        object BiometricRequired : UserAuthResult()
        object SessionExpired : UserAuthResult()
    }
    
    /**
     * Verifica si hay usuarios registrados
     */
    fun hasRegisteredUsers(): Boolean {
        val usersJson = prefs.getString("registered_users", null)
        return !usersJson.isNullOrEmpty()
    }
    
    /**
     * Obtiene todos los usuarios registrados
     */
    fun getRegisteredUsers(): List<User> {
        val usersJson = prefs.getString("registered_users", null)
        return if (usersJson != null) {
            try {
                // Parsear JSON simple (en producción usar Gson)
                usersJson.split("|").mapNotNull { userStr ->
                    if (userStr.isNotEmpty()) {
                        val parts = userStr.split(",")
                        if (parts.size >= 6) {
                            User(
                                id = parts[0],
                                name = parts[1],
                                biometricId = parts[2],
                                createdAt = parts[3].toLongOrNull() ?: 0L,
                                lastLogin = parts[4].toLongOrNull() ?: 0L,
                                isActive = parts[5].toBoolean()
                            )
                        } else null
                    } else null
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Error parseando usuarios", e.message ?: "Error desconocido", e)
                emptyList()
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Registra un nuevo usuario con autenticación biométrica
     */
    suspend fun registerNewUser(
        userName: String,
        requireBiometric: Boolean = true
    ): UserAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Registrando nuevo usuario", "Nombre: $userName")
                
                // 1. Verificar disponibilidad biométrica
                if (requireBiometric && !isBiometricAvailable()) {
                    Logger.error(TAG, "Biometría no disponible", "No se puede registrar usuario", null)
                    return@withContext UserAuthResult.Error("Autenticación biométrica no disponible")
                }
                
                // 2. Generar ID único del usuario
                val userId = UUID.randomUUID().toString()
                val biometricId = generateBiometricId()
                
                // 3. Crear usuario
                val newUser = User(
                    id = userId,
                    name = userName,
                    biometricId = biometricId,
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis(),
                    isActive = true
                )
                
                // 4. Guardar usuario
                saveUser(newUser)
                
                // 5. Establecer como usuario actual
                currentUser = newUser
                lastActivityTime = System.currentTimeMillis()
                prefs.edit().putString(CURRENT_USER_KEY, userId).apply()
                
                Logger.success(TAG, "Usuario registrado exitosamente", "ID: ${userId.take(8)}...")
                
                UserAuthResult.Success(newUser)
            } catch (e: Exception) {
                Logger.error(TAG, "Error registrando usuario", e.message ?: "Error desconocido", e)
                UserAuthResult.Error("Error registrando usuario: ${e.message}")
            }
        }
    }
    
    /**
     * Autentica usuario existente con TouchID/FaceID
     */
    suspend fun authenticateUser(
        userId: String,
        requireBiometric: Boolean = true
    ): UserAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Autenticando usuario", "ID: ${userId.take(8)}...")
                
                // 1. Buscar usuario
                val user = getRegisteredUsers().find { it.id == userId }
                if (user == null) {
                    Logger.error(TAG, "Usuario no encontrado", "ID: $userId", null)
                    return@withContext UserAuthResult.Error("Usuario no encontrado")
                }
                
                // 2. Verificar sesión activa
                if (isSessionActive()) {
                    Logger.debug(TAG, "Sesión activa encontrada", "Usuario: ${user.name}")
                    currentUser = user
                    lastActivityTime = System.currentTimeMillis()
                    return@withContext UserAuthResult.Success(user)
                }
                
                // 3. Requerir autenticación biométrica para operaciones críticas
                if (requireBiometric) {
                    val biometricResult = authenticateWithBiometric(
                        title = "Autenticación ${user.name}",
                        subtitle = "Usa tu huella para acceder a tus datos"
                    )
                    
                    if (biometricResult == null) {
                        Logger.error(TAG, "Autenticación biométrica fallida", "Usuario: ${user.name}", null)
                        return@withContext UserAuthResult.Error("Autenticación biométrica fallida")
                    }
                }
                
                // 4. Establecer sesión activa
                currentUser = user
                lastActivityTime = System.currentTimeMillis()
                prefs.edit().putString(CURRENT_USER_KEY, userId).apply()
                
                // 5. Actualizar último login
                updateUserLastLogin(userId)
                
                Logger.success(TAG, "Usuario autenticado exitosamente", "Usuario: ${user.name}")
                
                UserAuthResult.Success(user)
            } catch (e: Exception) {
                Logger.error(TAG, "Error autenticando usuario", e.message ?: "Error desconocido", e)
                UserAuthResult.Error("Error autenticando usuario: ${e.message}")
            }
        }
    }
    
    /**
     * Cambia a otro usuario (requiere autenticación biométrica)
     */
    suspend fun switchUser(
        targetUserId: String,
        requireBiometric: Boolean = true
    ): UserAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Cambiando usuario", "Target: ${targetUserId.take(8)}...")
                
                // 1. Cerrar sesión actual
                closeCurrentSession()
                
                // 2. Autenticar nuevo usuario
                authenticateUser(targetUserId, requireBiometric)
            } catch (e: Exception) {
                Logger.error(TAG, "Error cambiando usuario", e.message ?: "Error desconocido", e)
                UserAuthResult.Error("Error cambiando usuario: ${e.message}")
            }
        }
    }
    
    /**
     * Cierra la sesión actual
     */
    fun closeCurrentSession() {
        Logger.debug(TAG, "Cerrando sesión actual", "Usuario: ${currentUser?.name}")
        
        // Log detallado para rastrear el cierre de sesión
        android.util.Log.d("UserManager", "=== CERRANDO SESIÓN EN USERMANAGER ===")
        android.util.Log.d("UserManager", "Usuario actual: ${currentUser?.name}")
        android.util.Log.d("UserManager", "Timestamp: ${System.currentTimeMillis()}")
        android.util.Log.d("UserManager", "Stack trace: ${android.util.Log.getStackTraceString(Exception("CloseCurrentSession called from:"))}")
        
        currentUser = null
        lastActivityTime = 0L
        prefs.edit().remove(CURRENT_USER_KEY).apply()
        
        Logger.success(TAG, "Sesión cerrada", "Usuario desconectado")
        android.util.Log.d("UserManager", "✅ Sesión cerrada en UserManager")
    }
    
    /**
     * Obtiene el usuario actual (con verificación automática de sesión)
     */
    fun getCurrentUser(): User? {
        // Verificar si la sesión sigue activa
        if (currentUser != null && !isSessionActive()) {
            android.util.Log.w("UserManager", "⚠️ SESIÓN EXPIRADA AUTOMÁTICAMENTE")
            android.util.Log.w("UserManager", "Usuario: ${currentUser?.name}")
            android.util.Log.w("UserManager", "Tiempo desde última actividad: ${System.currentTimeMillis() - lastActivityTime}ms")
            android.util.Log.w("UserManager", "Timeout configurado: ${SESSION_TIMEOUT}ms")
            android.util.Log.w("UserManager", "Stack trace: ${android.util.Log.getStackTraceString(Exception("Session expired from:"))}")
            
            Logger.warning(TAG, "Sesión expirada", "Cerrando sesión automáticamente")
            closeCurrentSession()
        }
        return currentUser
    }
    
    /**
     * ✅ NUEVO: Obtiene el usuario actual SIN cerrar la sesión automáticamente
     * Útil para verificaciones que no deben interrumpir el flujo
     */
    fun getCurrentUserSafe(): User? {
        return currentUser  // Solo devuelve el usuario sin verificar timeout
    }
    
    /**
     * ✅ NUEVO: Verifica el estado de la sesión sin cerrarla
     */
    fun checkSessionStatus(): SessionStatus {
        return when {
            currentUser == null -> SessionStatus.NoUser
            isSessionActive() -> SessionStatus.Active
            else -> SessionStatus.Expired
        }
    }
    
    /**
     * ✅ NUEVO: Estados de sesión
     */
    enum class SessionStatus {
        NoUser,     // No hay usuario
        Active,     // Sesión activa
        Expired     // Sesión expirada
    }
    
    /**
     * ✅ NUEVO: Carga automáticamente el usuario persistido al iniciar la aplicación
     */
    suspend fun loadPersistedUser(): User? {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Cargando usuario persistido...", "")
                
                // Obtener ID del usuario persistido desde SharedPreferences
                val persistedUserId = prefs.getString(CURRENT_USER_KEY, null)
                
                if (persistedUserId != null) {
                    Logger.debug(TAG, "ID de usuario persistido encontrado", "ID: ${persistedUserId.take(8)}...")
                    
                    // ✅ Cargar usuario desde usuarios registrados
                    val user = getRegisteredUsers().find { it.id == persistedUserId }
                    
                    if (user != null) {
                        // Establecer como usuario actual
                        currentUser = user
                        lastActivityTime = System.currentTimeMillis()
                        
                        Logger.success(TAG, "Usuario persistido cargado", "Nombre: ${user.name}")
                        android.util.Log.d("UserManager", "✅ Usuario persistido cargado: ${user.name}")
                        
                        user
                    } else {
                        Logger.warning(TAG, "Usuario persistido no encontrado en almacenamiento", "ID: ${persistedUserId.take(8)}...")
                        android.util.Log.w("UserManager", "⚠️ Usuario persistido no encontrado en almacenamiento")
                        null
                    }
                } else {
                    Logger.debug(TAG, "No hay usuario persistido", "Primera ejecución de la aplicación")
                    android.util.Log.d("UserManager", "ℹ️ No hay usuario persistido - Primera ejecución")
                    null
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Error cargando usuario persistido", e.message ?: "Error desconocido", e)
                android.util.Log.e("UserManager", "❌ Error cargando usuario persistido: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * Verifica si la sesión está activa
     */
    fun isSessionActive(): Boolean {
        if (currentUser == null) return false
        
        val timeSinceLastActivity = System.currentTimeMillis() - lastActivityTime
        return timeSinceLastActivity < SESSION_TIMEOUT
    }
    
    /**
     * Actualiza la actividad del usuario (llamar en cada operación)
     */
    fun updateUserActivity() {
        if (currentUser != null) {
            lastActivityTime = System.currentTimeMillis()
        }
    }
    
    /**
     * ✅ NUEVO: Cierra la sesión cuando la app va al background
     */
    fun onAppBackgrounded() {
        Logger.debug(TAG, "App en background - Cerrando sesión por seguridad", "")
        android.util.Log.d("UserManager", "📱 App en background - Cerrando sesión")
        closeCurrentSession()
    }
    
    /**
     * ✅ NUEVO: Maneja el ciclo de vida de la app
     */
    fun onAppForegrounded() {
        Logger.debug(TAG, "App en foreground - Sesión cerrada, requiere re-autenticación", "")
        android.util.Log.d("UserManager", "📱 App en foreground - Sesión cerrada")
        // La sesión ya está cerrada, el usuario debe re-autenticarse
    }
    
    /**
     * Requiere autenticación biométrica para operaciones críticas
     */
    suspend fun requireBiometricAuth(
        operation: String = "operación crítica"
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Solicitando autenticación biométrica", "Operación: $operation")
                
                val result = authenticateWithBiometric(
                    title = "Confirmar $operation",
                    subtitle = "Usa tu huella para confirmar esta acción"
                )
                
                result != null
            } catch (e: Exception) {
                Logger.error(TAG, "Error en autenticación biométrica", e.message ?: "Error desconocido", e)
                false
            }
        }
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Verifica si la autenticación biométrica está disponible
     */
    private fun isBiometricAvailable(): Boolean {
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Genera un ID único basado en características biométricas del dispositivo
     */
    private fun generateBiometricId(): String {
        val deviceId = android.provider.Settings.Secure.getString(
            context.contentResolver, 
            android.provider.Settings.Secure.ANDROID_ID
        )
        val timestamp = System.currentTimeMillis()
        val biometricData = "${deviceId}_${timestamp}_biometric"
        
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(biometricData.toByteArray())
        
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Autentica con TouchID/FaceID
     */
    private suspend fun authenticateWithBiometric(
        title: String,
        subtitle: String
    ): String? {
        return suspendCancellableCoroutine { continuation ->
            try {
                val executor = ContextCompat.getMainExecutor(context)
                val biometricPrompt = BiometricPrompt(
                    context as FragmentActivity,
                    executor,
                    object : BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            Logger.success(TAG, "Autenticación biométrica exitosa", "TouchID/FaceID verificado")
                            continuation.resume("biometric_success")
                        }
                        
                        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                            Logger.error(TAG, "Error en autenticación biométrica", "Code: $errorCode, Message: $errString")
                            continuation.resume(null)
                        }
                        
                        override fun onAuthenticationFailed() {
                            Logger.warning(TAG, "Autenticación biométrica fallida", "Huella no reconocida")
                            continuation.resume(null)
                        }
                    }
                )
                
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setNegativeButtonText("Cancelar")
                    .setConfirmationRequired(true)
                    .build()
                
                biometricPrompt.authenticate(promptInfo)
                
                continuation.invokeOnCancellation {
                    biometricPrompt.cancelAuthentication()
                }
                
            } catch (e: Exception) {
                Logger.error(TAG, "Error iniciando autenticación biométrica", e.message ?: "Error desconocido", e)
                continuation.resume(null)
            }
        }
    }
    
    /**
     * Guarda un usuario en las preferencias
     */
    private fun saveUser(user: User) {
        val users = getRegisteredUsers().toMutableList()
        val existingIndex = users.indexOfFirst { it.id == user.id }
        
        if (existingIndex >= 0) {
            users[existingIndex] = user
        } else {
            users.add(user)
        }
        
        val usersJson = users.joinToString("|") { user ->
            "${user.id},${user.name},${user.biometricId},${user.createdAt},${user.lastLogin},${user.isActive}"
        }
        
        prefs.edit().putString("registered_users", usersJson).apply()
    }
    
    /**
     * Actualiza el último login de un usuario
     */
    private fun updateUserLastLogin(userId: String) {
        val users = getRegisteredUsers().toMutableList()
        val userIndex = users.indexOfFirst { it.id == userId }
        
        if (userIndex >= 0) {
            val user = users[userIndex]
            users[userIndex] = user.copy(lastLogin = System.currentTimeMillis())
            saveUser(users[userIndex])
        }
    }
    
    /**
     * Elimina un usuario del sistema
     */
    fun deleteUser(userId: String): Boolean {
        return try {
            Logger.debug(TAG, "Eliminando usuario", "ID: ${userId.take(8)}...")
            
            val users = getRegisteredUsers().toMutableList()
            val userIndex = users.indexOfFirst { it.id == userId }
            
            if (userIndex >= 0) {
                val userToDelete = users[userIndex]
                
                // Si es el usuario actual, cerrar sesión
                if (currentUser?.id == userId) {
                    closeCurrentSession()
                }
                
                // Eliminar de la lista
                users.removeAt(userIndex)
                
                // Guardar lista actualizada
                val usersJson = users.joinToString("|") { user ->
                    "${user.id},${user.name},${user.biometricId},${user.createdAt},${user.lastLogin},${user.isActive}"
                }
                
                prefs.edit().putString("registered_users", usersJson).apply()
                
                Logger.success(TAG, "Usuario eliminado exitosamente", "Nombre: ${userToDelete.name}")
                true
            } else {
                Logger.warning(TAG, "Usuario no encontrado para eliminar", "ID: ${userId.take(8)}...")
                false
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error eliminando usuario", e.message ?: "Error desconocido", e)
            false
        }
    }
}


