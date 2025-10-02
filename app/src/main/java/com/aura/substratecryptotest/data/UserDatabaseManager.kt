package com.aura.substratecryptotest.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.utils.Logger

/**
 * Gestor de base de datos aislada por usuario
 * Cada usuario tiene su propia instancia de base de datos
 */
class UserDatabaseManager(private val context: Context, private val userManager: UserManager) {
    
    companion object {
        private const val TAG = "UserDatabaseManager"
        private const val DATABASE_NAME_PREFIX = "user_db_"
        
        // Migraciones de base de datos
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_wallets ADD COLUMN biometric_protected INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla de bitácoras de montañismo por usuario
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_mountaineering_logbooks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        club TEXT NOT NULL,
                        association TEXT NOT NULL,
                        participantsCount INTEGER NOT NULL,
                        licenseNumber TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER NOT NULL,
                        location TEXT NOT NULL,
                        observations TEXT NOT NULL,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        pdfPath TEXT,
                        signedByDID TEXT,
                        walletId TEXT
                    )
                """)
                
                // Crear tabla de milestones de expedición por usuario
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_expedition_milestones (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        logbookId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        altitude REAL,
                        photos TEXT NOT NULL DEFAULT '[]',
                        gpxPath TEXT,
                        kmzPath TEXT,
                        duration INTEGER,
                        isDraft INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Crear tabla de fotos de expedición por usuario
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_expedition_photos (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userId TEXT NOT NULL,
                        milestoneId INTEGER NOT NULL,
                        photoPath TEXT NOT NULL,
                        photoType TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        latitude REAL,
                        longitude REAL,
                        altitude REAL
                    )
                """)
            }
        }
    }
    private var currentDatabase: AppDatabase? = null
    
    /**
     * Obtiene la base de datos del usuario actual
     */
    suspend fun getCurrentUserDatabase(): AppDatabase? {
        val currentUser = userManager.getCurrentUser()
        if (currentUser == null) {
            Logger.warning(TAG, "No hay usuario activo", "No se puede acceder a la base de datos")
            return null
        }
        
        return getUserDatabase(currentUser.id)
    }
    
    /**
     * Obtiene la base de datos de un usuario específico
     */
    fun getUserDatabase(userId: String): AppDatabase {
        val databaseName = "$DATABASE_NAME_PREFIX$userId"
        
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            databaseName
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .fallbackToDestructiveMigration()
        .build()
    }
    
    /**
     * Cambia a la base de datos de otro usuario
     */
    suspend fun switchToUserDatabase(userId: String): AppDatabase? {
        return try {
            Logger.debug(TAG, "Cambiando a base de datos de usuario", "ID: ${userId.take(8)}...")
            
            // Cerrar base de datos actual
            currentDatabase?.close()
            
            // Abrir nueva base de datos
            val newDatabase = getUserDatabase(userId)
            currentDatabase = newDatabase
            
            Logger.success(TAG, "Base de datos cambiada exitosamente", "Usuario: $userId")
            newDatabase
        } catch (e: Exception) {
            Logger.error(TAG, "Error cambiando base de datos", e.message ?: "Error desconocido", e)
            null
        }
    }
    
    /**
     * Cierra la base de datos actual
     */
    fun closeCurrentDatabase() {
        currentDatabase?.close()
        currentDatabase = null
        Logger.debug(TAG, "Base de datos cerrada", "Conexión terminada")
    }
    
    /**
     * Elimina la base de datos de un usuario (cuando se elimina el usuario)
     */
    fun deleteUserDatabase(userId: String): Boolean {
        return try {
            val databaseName = "$DATABASE_NAME_PREFIX$userId"
            val databaseFile = context.getDatabasePath(databaseName)
            
            if (databaseFile.exists()) {
                val deleted = databaseFile.delete()
                if (deleted) {
                    Logger.success(TAG, "Base de datos de usuario eliminada", "ID: ${userId.take(8)}...")
                } else {
                    Logger.error(TAG, "Error eliminando base de datos", "No se pudo eliminar archivo", null)
                }
                deleted
            } else {
                Logger.warning(TAG, "Base de datos no existe", "ID: ${userId.take(8)}...")
                true
            }
        } catch (e: Exception) {
            Logger.error(TAG, "Error eliminando base de datos", e.message ?: "Error desconocido", e)
            false
        }
    }
}

/**
 * Entidad para wallets de usuario
 */
@Entity(tableName = "user_wallets")
data class UserWallet(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val mnemonic: String, // Encriptado
    val publicKey: String, // Base64
    val privateKey: String, // Encriptado
    val address: String,
    val cryptoType: String,
    val derivationPath: String,
    val createdAt: Long,
    val biometricProtected: Boolean = true,
    val metadata: String = "{}" // JSON
)

/**
 * Entidad para documentos de usuario
 */
@Entity(tableName = "user_documents")
data class UserDocument(
    @PrimaryKey val id: String,
    val userId: String,
    val walletId: String,
    val documentHash: String,
    val documentType: String,
    val timestamp: Long,
    val blockchainTimestamp: String? = null,
    val metadata: String = "{}" // JSON
)

/**
 * Entidad para identidades KILT de usuario
 */
@Entity(tableName = "user_kilt_identities")
data class UserKiltIdentity(
    @PrimaryKey val id: String,
    val userId: String,
    val walletId: String,
    val did: String,
    val kiltAddress: String,
    val createdAt: Long,
    val isActive: Boolean = true
)

/**
 * Entidad para bitácoras de montañismo por usuario
 */
@Entity(tableName = "user_mountaineering_logbooks")
data class UserMountaineeringLogbook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID del usuario propietario
    val name: String,
    val club: String,
    val association: String,
    val participantsCount: Int,
    val licenseNumber: String,
    val startDate: Long, // Timestamp
    val endDate: Long, // Timestamp
    val location: String,
    val observations: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val pdfPath: String? = null,
    val signedByDID: String? = null,
    val walletId: String? = null // Wallet usada para firmar
)

/**
 * Entidad para milestones de expedición por usuario
 */
@Entity(tableName = "user_expedition_milestones")
data class UserExpeditionMilestone(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID del usuario propietario
    val logbookId: Long,
    val title: String,
    val description: String,
    val timestamp: Long, // Timestamp
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val photos: String = "[]", // JSON array de rutas de fotos
    val gpxPath: String? = null,
    val kmzPath: String? = null,
    val duration: Long? = null, // Duración en milisegundos
    val isDraft: Boolean = false
)

/**
 * Entidad para fotos de expedición por usuario
 */
@Entity(tableName = "user_expedition_photos")
data class UserExpeditionPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String, // ID del usuario propietario
    val milestoneId: Long,
    val photoPath: String,
    val photoType: String, // "RECORRIDO", "CUMBRE", "GENERAL"
    val timestamp: Long, // Timestamp
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null
)

/**
 * DAO para wallets de usuario
 */
@Dao
interface UserWalletDao {
    @Query("SELECT * FROM user_wallets WHERE userId = :userId")
    suspend fun getWalletsByUser(userId: String): List<UserWallet>
    
    @Query("SELECT * FROM user_wallets WHERE id = :walletId AND userId = :userId")
    suspend fun getWalletById(walletId: String, userId: String): UserWallet?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWallet(wallet: UserWallet)
    
    @Update
    suspend fun updateWallet(wallet: UserWallet)
    
    @Delete
    suspend fun deleteWallet(wallet: UserWallet)
    
    @Query("DELETE FROM user_wallets WHERE userId = :userId")
    suspend fun deleteAllWalletsForUser(userId: String)
}

/**
 * DAO para documentos de usuario
 */
@Dao
interface UserDocumentDao {
    @Query("SELECT * FROM user_documents WHERE userId = :userId")
    suspend fun getDocumentsByUser(userId: String): List<UserDocument>
    
    @Query("SELECT * FROM user_documents WHERE walletId = :walletId AND userId = :userId")
    suspend fun getDocumentsByWallet(walletId: String, userId: String): List<UserDocument>
    
    @Query("SELECT * FROM user_documents WHERE id = :documentId AND userId = :userId")
    suspend fun getDocumentById(documentId: String, userId: String): UserDocument?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: UserDocument)
    
    @Update
    suspend fun updateDocument(document: UserDocument)
    
    @Delete
    suspend fun deleteDocument(document: UserDocument)
    
    @Query("DELETE FROM user_documents WHERE userId = :userId")
    suspend fun deleteAllDocumentsForUser(userId: String)
}

/**
 * DAO para bitácoras de montañismo por usuario
 */
@Dao
interface UserMountaineeringLogbookDao {
    
    @Query("SELECT * FROM user_mountaineering_logbooks WHERE userId = :userId ORDER BY createdAt DESC")
    suspend fun getLogbooksByUser(userId: String): List<UserMountaineeringLogbook>
    
    @Query("SELECT * FROM user_mountaineering_logbooks WHERE id = :id AND userId = :userId")
    suspend fun getLogbookById(id: Long, userId: String): UserMountaineeringLogbook?
    
    @Query("SELECT * FROM user_mountaineering_logbooks WHERE userId = :userId AND isCompleted = 0 ORDER BY createdAt DESC")
    suspend fun getActiveLogbooksByUser(userId: String): List<UserMountaineeringLogbook>
    
    @Insert
    suspend fun insertLogbook(logbook: UserMountaineeringLogbook): Long
    
    @Update
    suspend fun updateLogbook(logbook: UserMountaineeringLogbook)
    
    @Delete
    suspend fun deleteLogbook(logbook: UserMountaineeringLogbook)
    
    @Query("UPDATE user_mountaineering_logbooks SET isCompleted = 1, pdfPath = :pdfPath WHERE id = :id AND userId = :userId")
    suspend fun completeLogbook(id: Long, pdfPath: String, userId: String)
    
    @Query("UPDATE user_mountaineering_logbooks SET signedByDID = :did WHERE id = :id AND userId = :userId")
    suspend fun signLogbook(id: Long, did: String, userId: String)
    
    @Query("DELETE FROM user_mountaineering_logbooks WHERE userId = :userId")
    suspend fun deleteAllLogbooksForUser(userId: String)
}

/**
 * DAO para milestones de expedición por usuario
 */
@Dao
interface UserExpeditionMilestoneDao {
    
    @Query("SELECT * FROM user_expedition_milestones WHERE userId = :userId AND logbookId = :logbookId ORDER BY timestamp ASC")
    suspend fun getMilestonesByLogbook(logbookId: Long, userId: String): List<UserExpeditionMilestone>
    
    @Query("SELECT * FROM user_expedition_milestones WHERE userId = :userId AND logbookId = :logbookId AND isDraft = 0 ORDER BY timestamp ASC")
    suspend fun getCompletedMilestonesByLogbook(logbookId: Long, userId: String): List<UserExpeditionMilestone>
    
    @Query("SELECT * FROM user_expedition_milestones WHERE userId = :userId AND logbookId = :logbookId AND isDraft = 1 ORDER BY timestamp ASC")
    suspend fun getDraftMilestonesByLogbook(logbookId: Long, userId: String): List<UserExpeditionMilestone>
    
    @Insert
    suspend fun insertMilestone(milestone: UserExpeditionMilestone): Long
    
    @Update
    suspend fun updateMilestone(milestone: UserExpeditionMilestone)
    
    @Delete
    suspend fun deleteMilestone(milestone: UserExpeditionMilestone)
    
    @Query("UPDATE user_expedition_milestones SET isDraft = 0 WHERE logbookId = :logbookId AND userId = :userId")
    suspend fun finalizeDraftMilestones(logbookId: Long, userId: String)
    
    @Query("DELETE FROM user_expedition_milestones WHERE logbookId = :logbookId AND userId = :userId AND isDraft = 1")
    suspend fun deleteDraftMilestones(logbookId: Long, userId: String)
    
    @Query("DELETE FROM user_expedition_milestones WHERE logbookId = :logbookId AND userId = :userId")
    suspend fun deleteMilestonesByLogbook(logbookId: Long, userId: String)
    
    @Query("DELETE FROM user_expedition_milestones WHERE userId = :userId")
    suspend fun deleteAllMilestonesForUser(userId: String)
}

/**
 * DAO para fotos de expedición por usuario
 */
@Dao
interface UserExpeditionPhotoDao {
    
    @Query("SELECT * FROM user_expedition_photos WHERE userId = :userId AND milestoneId = :milestoneId ORDER BY timestamp ASC")
    suspend fun getPhotosByMilestone(milestoneId: Long, userId: String): List<UserExpeditionPhoto>
    
    @Insert
    suspend fun insertPhoto(photo: UserExpeditionPhoto): Long
    
    @Update
    suspend fun updatePhoto(photo: UserExpeditionPhoto)
    
    @Delete
    suspend fun deletePhoto(photo: UserExpeditionPhoto)
    
    @Query("DELETE FROM user_expedition_photos WHERE milestoneId = :milestoneId AND userId = :userId")
    suspend fun deletePhotosByMilestone(milestoneId: Long, userId: String)
    
    @Query("DELETE FROM user_expedition_photos WHERE userId = :userId")
    suspend fun deleteAllPhotosForUser(userId: String)
}

/**
 * DAO para identidades KILT de usuario
 */
@Dao
interface UserKiltIdentityDao {
    @Query("SELECT * FROM user_kilt_identities WHERE userId = :userId")
    suspend fun getKiltIdentitiesByUser(userId: String): List<UserKiltIdentity>
    
    @Query("SELECT * FROM user_kilt_identities WHERE walletId = :walletId AND userId = :userId")
    suspend fun getKiltIdentityByWallet(walletId: String, userId: String): UserKiltIdentity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKiltIdentity(identity: UserKiltIdentity)
    
    @Update
    suspend fun updateKiltIdentity(identity: UserKiltIdentity)
    
    @Delete
    suspend fun deleteKiltIdentity(identity: UserKiltIdentity)
    
    @Query("DELETE FROM user_kilt_identities WHERE userId = :userId")
    suspend fun deleteAllKiltIdentitiesForUser(userId: String)
}

/**
 * Base de datos de la aplicación con aislamiento por usuario
 */
@Database(
    entities = [
        UserWallet::class,
        UserDocument::class,
        UserKiltIdentity::class,
        UserMountaineeringLogbook::class,
        UserExpeditionMilestone::class,
        UserExpeditionPhoto::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userWalletDao(): UserWalletDao
    abstract fun userDocumentDao(): UserDocumentDao
    abstract fun userKiltIdentityDao(): UserKiltIdentityDao
    abstract fun userMountaineeringLogbookDao(): UserMountaineeringLogbookDao
    abstract fun userExpeditionMilestoneDao(): UserExpeditionMilestoneDao
    abstract fun userExpeditionPhotoDao(): UserExpeditionPhotoDao
}


