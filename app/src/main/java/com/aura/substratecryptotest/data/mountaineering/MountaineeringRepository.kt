package com.aura.substratecryptotest.data.mountaineering

import kotlinx.coroutines.flow.Flow

/**
 * Repository para gestionar las bitácoras de alpinismo
 */
class MountaineeringRepository(
    private val logbookDao: MountaineeringLogbookDao,
    private val milestoneDao: ExpeditionMilestoneDao,
    private val photoDao: ExpeditionPhotoDao
) {
    
    // ===== LOGBOOKS =====
    suspend fun createLogbook(logbook: MountaineeringLogbook): Long {
        android.util.Log.d("MountaineeringRepository", "Insertando bitácora: ${logbook.name}")
        val logbookId = logbookDao.insertLogbook(logbook)
        android.util.Log.d("MountaineeringRepository", "Bitácora insertada con ID: $logbookId")
        return logbookId
    }
    
    fun getAllLogbooks(): Flow<List<MountaineeringLogbook>> {
        return logbookDao.getAllLogbooks()
    }
    
    fun getActiveLogbooks(): Flow<List<MountaineeringLogbook>> {
        return logbookDao.getActiveLogbooks()
    }
    
    suspend fun getLogbookById(id: Long): MountaineeringLogbook? {
        return logbookDao.getLogbookById(id)
    }
    
    suspend fun completeLogbook(id: Long, pdfPath: String) {
        android.util.Log.d("MountaineeringRepository", "Completando bitácora ID: $id con PDF: $pdfPath")
        logbookDao.completeLogbook(id, pdfPath)
        android.util.Log.d("MountaineeringRepository", "Bitácora completada en base de datos")
    }
    
    suspend fun signLogbook(id: Long, did: String) {
        logbookDao.signLogbook(id, did)
    }
    
    suspend fun deleteLogbook(logbook: MountaineeringLogbook) {
        // Primero eliminar fotos relacionadas
        val milestones = milestoneDao.getMilestonesByLogbook(logbook.id)
        milestones.collect { milestoneList ->
            milestoneList.forEach { milestone ->
                photoDao.deletePhotosByMilestone(milestone.id)
            }
        }
        
        // Luego eliminar milestones
        milestoneDao.deleteMilestonesByLogbook(logbook.id)
        
        // Finalmente eliminar la bitácora
        logbookDao.deleteLogbook(logbook)
    }
    
    // ===== MILESTONES =====
    suspend fun addMilestone(milestone: ExpeditionMilestone): Long {
        android.util.Log.d("MountaineeringRepository", "Agregando milestone: ${milestone.title} para bitácora ${milestone.logbookId}")
        val milestoneId = milestoneDao.insertMilestone(milestone)
        android.util.Log.d("MountaineeringRepository", "Milestone insertado con ID: $milestoneId")
        return milestoneId
    }
    
    fun getMilestonesByLogbook(logbookId: Long): Flow<List<ExpeditionMilestone>> {
        return milestoneDao.getMilestonesByLogbook(logbookId)
    }
    
    fun getCompletedMilestonesByLogbook(logbookId: Long): Flow<List<ExpeditionMilestone>> {
        return milestoneDao.getCompletedMilestonesByLogbook(logbookId)
    }
    
    fun getDraftMilestonesByLogbook(logbookId: Long): Flow<List<ExpeditionMilestone>> {
        return milestoneDao.getDraftMilestonesByLogbook(logbookId)
    }
    
    suspend fun updateMilestone(milestone: ExpeditionMilestone) {
        milestoneDao.updateMilestone(milestone)
    }
    
    suspend fun deleteMilestone(milestone: ExpeditionMilestone) {
        milestoneDao.deleteMilestone(milestone)
    }
    
    suspend fun finalizeDraftMilestones(logbookId: Long) {
        milestoneDao.finalizeDraftMilestones(logbookId)
    }
    
    suspend fun deleteDraftMilestones(logbookId: Long) {
        milestoneDao.deleteDraftMilestones(logbookId)
    }
    
    // ===== PHOTOS =====
    suspend fun addPhoto(photo: ExpeditionPhoto): Long {
        return photoDao.insertPhoto(photo)
    }
    
    fun getPhotosByMilestone(milestoneId: Long): Flow<List<ExpeditionPhoto>> {
        return photoDao.getPhotosByMilestone(milestoneId)
    }
    
    fun getPhotosByLogbook(logbookId: Long): Flow<List<ExpeditionPhoto>> {
        return photoDao.getPhotosByLogbook(logbookId)
    }
    
    suspend fun deletePhoto(photo: ExpeditionPhoto) {
        photoDao.deletePhoto(photo)
    }
}
