package com.aura.substratecryptotest.data.user

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO para bitácoras de montañismo por usuario
 */
@Dao
interface UserMountaineeringLogbookDao {
    
    @Query("SELECT * FROM user_mountaineering_logbooks WHERE userId = :userId ORDER BY createdAt DESC")
    fun getLogbooksByUser(userId: String): Flow<List<UserMountaineeringLogbook>>
    
    @Query("SELECT * FROM user_mountaineering_logbooks WHERE id = :id AND userId = :userId")
    suspend fun getLogbookById(id: Long, userId: String): UserMountaineeringLogbook?
    
    @Query("SELECT * FROM user_mountaineering_logbooks WHERE userId = :userId AND isCompleted = 0 ORDER BY createdAt DESC")
    fun getActiveLogbooksByUser(userId: String): Flow<List<UserMountaineeringLogbook>>
    
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
    fun getMilestonesByLogbook(logbookId: Long, userId: String): Flow<List<UserExpeditionMilestone>>
    
    @Query("SELECT * FROM user_expedition_milestones WHERE userId = :userId AND logbookId = :logbookId AND isDraft = 0 ORDER BY timestamp ASC")
    fun getCompletedMilestonesByLogbook(logbookId: Long, userId: String): Flow<List<UserExpeditionMilestone>>
    
    @Query("SELECT * FROM user_expedition_milestones WHERE userId = :userId AND logbookId = :logbookId AND isDraft = 1 ORDER BY timestamp ASC")
    fun getDraftMilestonesByLogbook(logbookId: Long, userId: String): Flow<List<UserExpeditionMilestone>>
    
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
    fun getPhotosByMilestone(milestoneId: Long, userId: String): Flow<List<UserExpeditionPhoto>>
    
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

