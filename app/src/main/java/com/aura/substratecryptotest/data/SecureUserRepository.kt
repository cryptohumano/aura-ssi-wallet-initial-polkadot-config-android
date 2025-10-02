package com.aura.substratecryptotest.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.security.KeyStoreManager
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repositorio seguro por usuario
 * Maneja wallets, documentos y datos de forma aislada por usuario
 * Requiere autenticación biométrica para operaciones críticas
 */
class SecureUserRepository(private val context: Context, private val userManager: UserManager) {
    
    companion object {
        private const val TAG = "SecureUserRepository"
        
        @Volatile
        private var INSTANCE: SecureUserRepository? = null
        
        fun getInstance(context: Context): SecureUserRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureUserRepository(context.applicationContext, UserManager(context.applicationContext)).also { INSTANCE = it }
            }
        }
        
        fun getInstance(context: Context, userManager: com.aura.substratecryptotest.security.UserManager): SecureUserRepository {
            return synchronized(this) {
                INSTANCE = SecureUserRepository(context.applicationContext, userManager)
                INSTANCE!!
            }
        }
    }
    private val keyStoreManager = KeyStoreManager(context)
    private val databaseManager = UserDatabaseManager(context, userManager)
    
    // Estado actual
    private val _currentUserWallets = MutableLiveData<List<UserWallet>>()
    val currentUserWallets: LiveData<List<UserWallet>> = _currentUserWallets
    
    private val _currentUserDocuments = MutableLiveData<List<UserDocument>>()
    val currentUserDocuments: LiveData<List<UserDocument>> = _currentUserDocuments
    
    private val _currentUserKiltIdentities = MutableLiveData<List<UserKiltIdentity>>()
    val currentUserKiltIdentities: LiveData<List<UserKiltIdentity>> = _currentUserKiltIdentities
    
    /**
     * Crea una nueva wallet para el usuario actual
     * Requiere autenticación biométrica
     */
    suspend fun createUserWallet(
        walletName: String,
        mnemonic: String,
        publicKey: ByteArray,
        privateKey: ByteArray,
        address: String,
        cryptoType: String,
        derivationPath: String,
        requireBiometric: Boolean = true
    ): Result<UserWallet> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Creando wallet de usuario", "Nombre: $walletName")
                
                val currentUser = userManager.getCurrentUser()
                android.util.Log.d("SecureUserRepository", "=== VERIFICANDO USUARIO EN SECUREUSERREPOSITORY ===")
                android.util.Log.d("SecureUserRepository", "Usuario actual: ${currentUser?.name}")
                android.util.Log.d("SecureUserRepository", "UserManager instance: ${userManager.hashCode()}")
                if (currentUser == null) {
                    Logger.error(TAG, "No hay usuario activo", "No se puede crear wallet", null)
                    android.util.Log.e("SecureUserRepository", "❌ No hay usuario activo en SecureUserRepository")
                    return@withContext Result.failure(Exception("No hay usuario activo"))
                }
                android.util.Log.d("SecureUserRepository", "✅ Usuario encontrado en SecureUserRepository: ${currentUser.name}")
                
                // Requerir autenticación biométrica para operaciones críticas
                if (requireBiometric) {
                    val biometricAuth = userManager.requireBiometricAuth("creación de wallet")
                    if (!biometricAuth) {
                        Logger.error(TAG, "Autenticación biométrica fallida", "No se puede crear wallet", null)
                        return@withContext Result.failure(Exception("Autenticación biométrica requerida"))
                    }
                }
                
                // Generar ID único para la wallet
                val walletId = UUID.randomUUID().toString()
                
                // Encriptar datos sensibles usando KeyStore del usuario
                val encryptedMnemonic = encryptUserData(mnemonic, currentUser.id, "mnemonic")
                val encryptedPrivateKey = encryptUserData(privateKey, currentUser.id, "private_key")
                
                if (encryptedMnemonic == null || encryptedPrivateKey == null) {
                    Logger.error(TAG, "Error encriptando datos sensibles", "No se puede crear wallet", null)
                    return@withContext Result.failure(Exception("Error encriptando datos sensibles"))
                }
                
                // Crear wallet
                val userWallet = UserWallet(
                    id = walletId,
                    userId = currentUser.id,
                    name = walletName,
                    mnemonic = encryptedMnemonic,
                    publicKey = android.util.Base64.encodeToString(publicKey, android.util.Base64.DEFAULT),
                    privateKey = encryptedPrivateKey,
                    address = address,
                    cryptoType = cryptoType,
                    derivationPath = derivationPath,
                    createdAt = System.currentTimeMillis(),
                    biometricProtected = requireBiometric,
                    metadata = "{}"
                )
                
                // Guardar en base de datos del usuario
                val database = databaseManager.getCurrentUserDatabase()
                if (database == null) {
                    Logger.error(TAG, "No se puede acceder a la base de datos", "Usuario: ${currentUser.name}", null)
                    return@withContext Result.failure(Exception("No se puede acceder a la base de datos"))
                }
                
                database.userWalletDao().insertWallet(userWallet)
                
                // Actualizar estado
                loadUserWallets()
                
                Logger.success(TAG, "Wallet creada exitosamente", "ID: ${walletId.take(8)}...")
                
                Result.success(userWallet)
            } catch (e: Exception) {
                Logger.error(TAG, "Error creando wallet de usuario", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Obtiene las wallets del usuario actual
     */
    suspend fun getUserWallets(): List<UserWallet> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = userManager.getCurrentUser()
                android.util.Log.d("SecureUserRepository", "=== VERIFICANDO USUARIO EN getUserWallets ===")
                android.util.Log.d("SecureUserRepository", "Usuario actual: ${currentUser?.name}")
                android.util.Log.d("SecureUserRepository", "UserManager instance: ${userManager.hashCode()}")
                if (currentUser == null) {
                    Logger.warning(TAG, "No hay usuario activo", "No se pueden cargar wallets")
                    android.util.Log.w("SecureUserRepository", "❌ No hay usuario activo en getUserWallets")
                    return@withContext emptyList()
                }
                android.util.Log.d("SecureUserRepository", "✅ Usuario encontrado en getUserWallets: ${currentUser.name}")
                
                // Actualizar actividad del usuario para evitar timeout
                userManager.updateUserActivity()
                
                val database = databaseManager.getCurrentUserDatabase()
                if (database == null) {
                    Logger.error(TAG, "No se puede acceder a la base de datos", "Usuario: ${currentUser.name}", null)
                    android.util.Log.e("SecureUserRepository", "❌ No se puede acceder a la base de datos para usuario: ${currentUser.name}")
                    return@withContext emptyList()
                }
                
                val wallets = database.userWalletDao().getWalletsByUser(currentUser.id)
                Logger.debug(TAG, "🔍 Wallets cargadas: Cantidad: ${wallets.size}", "")
                android.util.Log.d("SecureUserRepository", "✅ Wallets cargadas: ${wallets.size} para usuario: ${currentUser.name}")
                
                wallets
            } catch (e: Exception) {
                Logger.error(TAG, "Error cargando wallets de usuario", e.message ?: "Error desconocido", e)
                android.util.Log.e("SecureUserRepository", "❌ Error cargando wallets: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Obtiene el mnemonic de una wallet (requiere autenticación biométrica)
     */
    suspend fun getWalletMnemonic(
        walletId: String,
        requireBiometric: Boolean = true
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Obteniendo mnemonic de wallet", "ID: ${walletId.take(8)}...")
                
                val currentUser = userManager.getCurrentUser()
                if (currentUser == null) {
                    Logger.error(TAG, "No hay usuario activo", "No se puede obtener mnemonic", null)
                    return@withContext Result.failure(Exception("No hay usuario activo"))
                }
                
                // Requerir autenticación biométrica para datos sensibles
                if (requireBiometric) {
                    val biometricAuth = userManager.requireBiometricAuth("acceso a mnemonic")
                    if (!biometricAuth) {
                        Logger.error(TAG, "Autenticación biométrica fallida", "No se puede obtener mnemonic", null)
                        return@withContext Result.failure(Exception("Autenticación biométrica requerida"))
                    }
                }
                
                val database = databaseManager.getCurrentUserDatabase()
                if (database == null) {
                    Logger.error(TAG, "No se puede acceder a la base de datos", "Usuario: ${currentUser.name}", null)
                    return@withContext Result.failure(Exception("No se puede acceder a la base de datos"))
                }
                
                val wallet = database.userWalletDao().getWalletById(walletId, currentUser.id)
                if (wallet == null) {
                    Logger.error(TAG, "Wallet no encontrada", "ID: $walletId", null)
                    return@withContext Result.failure(Exception("Wallet no encontrada"))
                }
                
                // Desencriptar mnemonic
                val decryptedMnemonic = decryptUserData(wallet.mnemonic, currentUser.id, "mnemonic")
                if (decryptedMnemonic == null) {
                    Logger.error(TAG, "Error desencriptando mnemonic", "No se puede obtener mnemonic", null)
                    return@withContext Result.failure(Exception("Error desencriptando mnemonic"))
                }
                
                Logger.success(TAG, "Mnemonic obtenido exitosamente", "Wallet: ${wallet.name}")
                
                Result.success(decryptedMnemonic)
            } catch (e: Exception) {
                Logger.error(TAG, "Error obteniendo mnemonic de wallet", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Crea un documento para el usuario actual
     */
    suspend fun createUserDocument(
        walletId: String,
        documentHash: String,
        documentType: String,
        blockchainTimestamp: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ): Result<UserDocument> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Creando documento de usuario", "Hash: ${documentHash.take(20)}...")
                
                val currentUser = userManager.getCurrentUser()
                if (currentUser == null) {
                    Logger.error(TAG, "No hay usuario activo", "No se puede crear documento", null)
                    return@withContext Result.failure(Exception("No hay usuario activo"))
                }
                
                val documentId = UUID.randomUUID().toString()
                
                val userDocument = UserDocument(
                    id = documentId,
                    userId = currentUser.id,
                    walletId = walletId,
                    documentHash = documentHash,
                    documentType = documentType,
                    timestamp = System.currentTimeMillis(),
                    blockchainTimestamp = blockchainTimestamp,
                    metadata = metadata.toString()
                )
                
                val database = databaseManager.getCurrentUserDatabase()
                if (database == null) {
                    Logger.error(TAG, "No se puede acceder a la base de datos", "Usuario: ${currentUser.name}", null)
                    return@withContext Result.failure(Exception("No se puede acceder a la base de datos"))
                }
                
                database.userDocumentDao().insertDocument(userDocument)
                
                // Actualizar estado
                loadUserDocuments()
                
                Logger.success(TAG, "Documento creado exitosamente", "ID: ${documentId.take(8)}...")
                
                Result.success(userDocument)
            } catch (e: Exception) {
                Logger.error(TAG, "Error creando documento de usuario", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cambia al usuario especificado
     */
    suspend fun switchUser(userId: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                Logger.debug(TAG, "Cambiando usuario", "ID: ${userId.take(8)}...")
                
                // Cambiar usuario en UserManager
                val authResult = userManager.switchUser(userId, requireBiometric = true)
                if (authResult !is UserManager.UserAuthResult.Success) {
                    Logger.error(TAG, "Error autenticando usuario", "No se puede cambiar usuario", null)
                    return@withContext Result.failure(Exception("Error autenticando usuario"))
                }
                
                // Cambiar base de datos
                val database = databaseManager.switchToUserDatabase(userId)
                if (database == null) {
                    Logger.error(TAG, "Error cambiando base de datos", "Usuario: ${userId.take(8)}...", null)
                    return@withContext Result.failure(Exception("Error cambiando base de datos"))
                }
                
                // Cargar datos del nuevo usuario
                loadUserWallets()
                loadUserDocuments()
                loadUserKiltIdentities()
                
                Logger.success(TAG, "Usuario cambiado exitosamente", "Usuario: ${authResult.user.name}")
                
                Result.success(true)
            } catch (e: Exception) {
                Logger.error(TAG, "Error cambiando usuario", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Cierra la sesión actual
     */
    fun closeCurrentSession() {
        Logger.debug(TAG, "Cerrando sesión de usuario", "Limpiando datos")
        
        userManager.closeCurrentSession()
        databaseManager.closeCurrentDatabase()
        
        _currentUserWallets.postValue(emptyList())
        _currentUserDocuments.postValue(emptyList())
        _currentUserKiltIdentities.postValue(emptyList())
        
        Logger.success(TAG, "Sesión cerrada", "Datos limpiados")
    }
    
    // ===== MÉTODOS PRIVADOS =====
    
    /**
     * Carga las wallets del usuario actual
     */
    private suspend fun loadUserWallets() {
        val wallets = getUserWallets()
        _currentUserWallets.postValue(wallets)
    }
    
    /**
     * Carga los documentos del usuario actual
     */
    private suspend fun loadUserDocuments() {
        val currentUser = userManager.getCurrentUser()
        if (currentUser != null) {
            val database = databaseManager.getCurrentUserDatabase()
            if (database != null) {
                val documents = database.userDocumentDao().getDocumentsByUser(currentUser.id)
                _currentUserDocuments.postValue(documents)
            }
        }
    }
    
    /**
     * Carga las identidades KILT del usuario actual
     */
    private suspend fun loadUserKiltIdentities() {
        val currentUser = userManager.getCurrentUser()
        if (currentUser != null) {
            val database = databaseManager.getCurrentUserDatabase()
            if (database != null) {
                val identities = database.userKiltIdentityDao().getKiltIdentitiesByUser(currentUser.id)
                _currentUserKiltIdentities.postValue(identities)
            }
        }
    }
    
    /**
     * Encripta datos del usuario usando KeyStore
     */
    private fun encryptUserData(data: String, userId: String, dataType: String): String? {
        val alias = "${userId}_${dataType}"
        val encryptedData = keyStoreManager.encryptData(data.toByteArray(), alias)
        return encryptedData?.encryptedData
    }
    
    /**
     * Encripta datos del usuario usando KeyStore (ByteArray)
     */
    private fun encryptUserData(data: ByteArray, userId: String, dataType: String): String? {
        val alias = "${userId}_${dataType}"
        val encryptedData = keyStoreManager.encryptData(data, alias)
        return encryptedData?.encryptedData
    }
    
    /**
     * Desencripta datos del usuario usando KeyStore
     */
    private fun decryptUserData(encryptedData: String, userId: String, dataType: String): String? {
        val alias = "${userId}_${dataType}"
        val encryptedDataObj = KeyStoreManager.EncryptedData(
            encryptedData = encryptedData,
            iv = "", // Se maneja internamente
            alias = alias
        )
        val decryptedBytes = keyStoreManager.decryptData(encryptedDataObj)
        return decryptedBytes?.toString(Charsets.UTF_8)
    }
}


