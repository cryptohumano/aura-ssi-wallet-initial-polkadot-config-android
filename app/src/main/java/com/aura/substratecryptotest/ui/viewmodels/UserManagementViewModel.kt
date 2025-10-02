package com.aura.substratecryptotest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.data.UserDatabaseManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para la gestión de usuarios
 * Maneja la creación, cambio y eliminación de usuarios con autenticación biométrica
 */
class UserManagementViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "UserManagementViewModel"
    }
    
    private val _uiState = MutableStateFlow(UserManagementUiState())
    val uiState: StateFlow<UserManagementUiState> = _uiState.asStateFlow()
    
    private lateinit var userManager: UserManager
    private lateinit var secureUserRepository: SecureUserRepository
    private lateinit var databaseManager: UserDatabaseManager
    
    fun initialize(context: android.content.Context) {
        userManager = UserManager(context)
        secureUserRepository = SecureUserRepository.getInstance(context)
        databaseManager = UserDatabaseManager(context, userManager)
    }
    
    /**
     * Carga los usuarios registrados
     */
    fun loadRegisteredUsers() {
        viewModelScope.launch {
            try {
                Logger.debug(TAG, "Cargando usuarios registrados", "")
                
                val registeredUsers = userManager.getRegisteredUsers()
                val currentUser = userManager.getCurrentUser()
                
                _uiState.value = _uiState.value.copy(
                    registeredUsers = registeredUsers,
                    currentUser = currentUser,
                    isLoading = false
                )
                
                Logger.success(TAG, "Usuarios cargados", "Cantidad: ${registeredUsers.size}")
            } catch (e: Exception) {
                Logger.error(TAG, "Error cargando usuarios", e.message ?: "Error desconocido", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error cargando usuarios: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Crea un nuevo usuario
     */
    fun createNewUser(userName: String) {
        viewModelScope.launch {
            try {
                Logger.debug(TAG, "Creando nuevo usuario", "Nombre: $userName")
                
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = userManager.registerNewUser(userName, requireBiometric = true)
                
                when (result) {
                    is UserManager.UserAuthResult.Success -> {
                        Logger.success(TAG, "Usuario creado exitosamente", "Nombre: ${result.user.name}")
                        
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            showCreateUserDialog = false,
                            currentUser = result.user,
                            message = "Usuario \"${result.user.name}\" creado exitosamente"
                        )
                        
                        // Recargar lista de usuarios
                        loadRegisteredUsers()
                    }
                    is UserManager.UserAuthResult.Error -> {
                        Logger.error(TAG, "Error creando usuario", result.message, null)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Error creando usuario: ${result.message}"
                        )
                    }
                    else -> {
                        Logger.error(TAG, "Error inesperado creando usuario", "Resultado: $result", null)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Error inesperado creando usuario"
                        )
                    }
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción creando usuario", e.message ?: "Error desconocido", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error creando usuario: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Cambia al usuario especificado
     */
    fun switchToUser(userId: String) {
        viewModelScope.launch {
            try {
                Logger.debug(TAG, "Cambiando usuario", "ID: ${userId.take(8)}...")
                
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val result = secureUserRepository.switchUser(userId)
                
                if (result.isSuccess) {
                    Logger.success(TAG, "Usuario cambiado exitosamente", "ID: ${userId.take(8)}...")
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentUser = userManager.getCurrentUser(),
                        message = "Usuario cambiado exitosamente"
                    )
                    
                    // Recargar lista de usuarios
                    loadRegisteredUsers()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Error desconocido"
                    Logger.error(TAG, "Error cambiando usuario", error, null)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Error cambiando usuario: $error"
                    )
                }
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción cambiando usuario", e.message ?: "Error desconocido", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error cambiando usuario: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Elimina un usuario
     */
    fun deleteUser(user: UserManager.User) {
        viewModelScope.launch {
            try {
                Logger.debug(TAG, "Eliminando usuario", "Nombre: ${user.name}")
                
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // 1. Eliminar datos de la base de datos
                val databaseDeleted = databaseManager.deleteUserDatabase(user.id)
                if (!databaseDeleted) {
                    Logger.warning(TAG, "Error eliminando base de datos", "Usuario: ${user.name}")
                }
                
                // 2. Eliminar datos del KeyStore
                // TODO: Implementar eliminación de datos del KeyStore por usuario
                
                // 3. Actualizar lista de usuarios (eliminar de la lista)
                val updatedUsers = _uiState.value.registeredUsers.filter { it.id != user.id }
                
                // 4. Si era el usuario actual, cerrar sesión
                if (_uiState.value.currentUser?.id == user.id) {
                    secureUserRepository.closeCurrentSession()
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showDeleteUserDialog = false,
                    userToDelete = null,
                    registeredUsers = updatedUsers,
                    currentUser = if (_uiState.value.currentUser?.id == user.id) null else _uiState.value.currentUser,
                    message = "Usuario \"${user.name}\" eliminado exitosamente"
                )
                
                Logger.success(TAG, "Usuario eliminado exitosamente", "Nombre: ${user.name}")
                
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción eliminando usuario", e.message ?: "Error desconocido", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "Error eliminando usuario: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Cierra la sesión del usuario actual
     */
    fun logoutCurrentUser() {
        viewModelScope.launch {
            try {
                Logger.debug(TAG, "Cerrando sesión de usuario", "")
                
                secureUserRepository.closeCurrentSession()
                
                _uiState.value = _uiState.value.copy(
                    currentUser = null,
                    message = "Sesión cerrada exitosamente"
                )
                
                Logger.success(TAG, "Sesión cerrada", "Usuario desconectado")
                
            } catch (e: Exception) {
                Logger.error(TAG, "Excepción cerrando sesión", e.message ?: "Error desconocido", e)
                _uiState.value = _uiState.value.copy(
                    message = "Error cerrando sesión: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Muestra el dialog para crear usuario
     */
    fun showCreateUserDialog() {
        _uiState.value = _uiState.value.copy(showCreateUserDialog = true)
    }
    
    /**
     * Oculta el dialog para crear usuario
     */
    fun hideCreateUserDialog() {
        _uiState.value = _uiState.value.copy(showCreateUserDialog = false)
    }
    
    /**
     * Muestra el dialog para eliminar usuario
     */
    fun showDeleteUserDialog(user: UserManager.User) {
        _uiState.value = _uiState.value.copy(userToDelete = user, showDeleteUserDialog = true)
    }
    
    /**
     * Oculta el dialog para eliminar usuario
     */
    fun hideDeleteUserDialog() {
        _uiState.value = _uiState.value.copy(userToDelete = null, showDeleteUserDialog = false)
    }
    
    /**
     * Limpia el mensaje actual
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

/**
 * Estado de la UI para la gestión de usuarios
 */
data class UserManagementUiState(
    val registeredUsers: List<UserManager.User> = emptyList(),
    val currentUser: UserManager.User? = null,
    val isLoading: Boolean = false,
    val showCreateUserDialog: Boolean = false,
    val showDeleteUserDialog: Boolean = false,
    val userToDelete: UserManager.User? = null,
    val message: String? = null
)


