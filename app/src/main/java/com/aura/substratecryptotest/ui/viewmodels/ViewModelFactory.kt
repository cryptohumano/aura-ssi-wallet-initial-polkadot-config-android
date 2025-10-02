package com.aura.substratecryptotest.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aura.substratecryptotest.data.mountaineering.MountaineeringDatabase
import com.aura.substratecryptotest.data.mountaineering.MountaineeringRepository
import com.aura.substratecryptotest.data.location.GPSManager
import com.aura.substratecryptotest.data.services.MilestoneDetailsService
import com.aura.substratecryptotest.data.pdf.PDFManager
import com.aura.substratecryptotest.data.database.AppDatabaseManager
import com.aura.substratecryptotest.data.user.UserManagementService

/**
 * Factory para crear ViewModels con sus dependencias
 */
class MountaineeringViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MountaineeringViewModel::class.java)) {
            // Crear base de datos principal
            val appDatabaseManager = AppDatabaseManager(context)
            
            // Crear repositorio usando la nueva base de datos
            val repository = appDatabaseManager.mountaineeringRepository
            
            // Crear GPS Manager
            val gpsManager = GPSManager(context)
            
            // Crear PDF Manager
            val pdfManager = PDFManager(context)
            
            // Crear User Management Service
            val userManagementService = appDatabaseManager.userManagementService
            
            // Crear ViewModel
            return MountaineeringViewModel(repository, gpsManager, pdfManager, userManagementService) as T
        }
        
        if (modelClass.isAssignableFrom(MilestoneDetailsViewModel::class.java)) {
            // Crear base de datos
            val database = MountaineeringDatabase.getDatabase(context)
            
            // Crear repositorio
            val repository = MountaineeringRepository(
                logbookDao = database.logbookDao(),
                milestoneDao = database.milestoneDao(),
                photoDao = database.photoDao()
            )
            
            // Crear servicio de detalles
            val milestoneDetailsService = MilestoneDetailsService(repository)
            
            // Crear ViewModel
            return MilestoneDetailsViewModel(milestoneDetailsService) as T
        }
        
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
