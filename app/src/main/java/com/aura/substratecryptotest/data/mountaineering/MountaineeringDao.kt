package com.aura.substratecryptotest.data.mountaineering

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la bitácora de alpinismo
 */
@Dao
interface MountaineeringLogbookDao {
    
    @Query("SELECT * FROM mountaineering_logbooks ORDER BY createdAt DESC")
    fun getAllLogbooks(): Flow<List<MountaineeringLogbook>>
    
    @Query("SELECT * FROM mountaineering_logbooks WHERE id = :id")
    suspend fun getLogbookById(id: Long): MountaineeringLogbook?
    
    @Query("SELECT * FROM mountaineering_logbooks WHERE isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveLogbooks(): Flow<List<MountaineeringLogbook>>
    
    @Insert
    suspend fun insertLogbook(logbook: MountaineeringLogbook): Long
    
    @Update
    suspend fun updateLogbook(logbook: MountaineeringLogbook)
    
    @Delete
    suspend fun deleteLogbook(logbook: MountaineeringLogbook)
    
    @Query("UPDATE mountaineering_logbooks SET isCompleted = 1, pdfPath = :pdfPath WHERE id = :id")
    suspend fun completeLogbook(id: Long, pdfPath: String)
    
    @Query("UPDATE mountaineering_logbooks SET signedByDID = :did WHERE id = :id")
    suspend fun signLogbook(id: Long, did: String)
}

/**
 * DAO para los milestones de expedición
 */
@Dao
interface ExpeditionMilestoneDao {
    
    @Query("SELECT * FROM expedition_milestones WHERE logbookId = :logbookId ORDER BY timestamp ASC")
    fun getMilestonesByLogbook(logbookId: Long): Flow<List<ExpeditionMilestone>>
    
    @Query("SELECT * FROM expedition_milestones WHERE logbookId = :logbookId AND isDraft = 0 ORDER BY timestamp ASC")
    fun getCompletedMilestonesByLogbook(logbookId: Long): Flow<List<ExpeditionMilestone>>
    
    @Query("SELECT * FROM expedition_milestones WHERE logbookId = :logbookId AND isDraft = 1 ORDER BY timestamp ASC")
    fun getDraftMilestonesByLogbook(logbookId: Long): Flow<List<ExpeditionMilestone>>
    
    @Query("SELECT * FROM expedition_milestones WHERE id = :id")
    suspend fun getMilestoneById(id: Long): ExpeditionMilestone?
    
    @Insert
    suspend fun insertMilestone(milestone: ExpeditionMilestone): Long
    
    @Update
    suspend fun updateMilestone(milestone: ExpeditionMilestone)
    
    @Delete
    suspend fun deleteMilestone(milestone: ExpeditionMilestone)
    
    @Query("DELETE FROM expedition_milestones WHERE logbookId = :logbookId")
    suspend fun deleteMilestonesByLogbook(logbookId: Long)
    
    @Query("UPDATE expedition_milestones SET isDraft = 0 WHERE logbookId = :logbookId AND isDraft = 1")
    suspend fun finalizeDraftMilestones(logbookId: Long)
    
    @Query("DELETE FROM expedition_milestones WHERE logbookId = :logbookId AND isDraft = 1")
    suspend fun deleteDraftMilestones(logbookId: Long)
}

/**
 * DAO para las fotos de expedición
 */
@Dao
interface ExpeditionPhotoDao {
    
    @Query("SELECT * FROM expedition_photos WHERE milestoneId = :milestoneId ORDER BY timestamp ASC")
    fun getPhotosByMilestone(milestoneId: Long): Flow<List<ExpeditionPhoto>>
    
    @Query("SELECT * FROM expedition_photos WHERE milestoneId IN (SELECT id FROM expedition_milestones WHERE logbookId = :logbookId) ORDER BY timestamp ASC")
    fun getPhotosByLogbook(logbookId: Long): Flow<List<ExpeditionPhoto>>
    
    @Insert
    suspend fun insertPhoto(photo: ExpeditionPhoto): Long
    
    @Delete
    suspend fun deletePhoto(photo: ExpeditionPhoto)
    
    @Query("DELETE FROM expedition_photos WHERE milestoneId = :milestoneId")
    suspend fun deletePhotosByMilestone(milestoneId: Long)
}
