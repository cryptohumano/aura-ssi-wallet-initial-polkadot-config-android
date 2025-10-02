package com.aura.substratecryptotest.data.mountaineering

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.Flow

/**
 * Manager simple para la base de datos de montañismo
 * Sin Dagger Hilt - instanciación directa
 */
class MountaineeringDatabaseManager private constructor(context: Context) {
    
    private val database: MountaineeringDatabase = Room.databaseBuilder(
        context.applicationContext,
        MountaineeringDatabase::class.java,
        "mountaineering_database"
    )
    .fallbackToDestructiveMigration()
    .build()
    
    private val repository = MountaineeringRepository(
        database.logbookDao(),
        database.milestoneDao(),
        database.photoDao()
    )
    
    companion object {
        @Volatile
        private var INSTANCE: MountaineeringDatabaseManager? = null
        
        fun getInstance(context: Context): MountaineeringDatabaseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MountaineeringDatabaseManager(context).also { INSTANCE = it }
            }
        }
    }
    
    fun getRepository(): MountaineeringRepository = repository
}
