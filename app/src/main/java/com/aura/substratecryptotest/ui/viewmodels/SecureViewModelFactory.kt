package com.aura.substratecryptotest.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.aura.substratecryptotest.data.SecureUserRepository
import com.aura.substratecryptotest.security.UserManager
import com.aura.substratecryptotest.data.UserDatabaseManager

/**
 * Factory para crear ViewModels que usan SecureUserRepository
 * Maneja la inyección de dependencias para el sistema seguro
 */
class SecureViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UserManagementViewModel::class.java)) {
            val userManager = UserManager(context)
            val secureUserRepository = SecureUserRepository.getInstance(context)
            val databaseManager = UserDatabaseManager(context, userManager)
            
            return UserManagementViewModel().apply {
                initialize(context)
            } as T
        }
        
        if (modelClass.isAssignableFrom(SecureWalletViewModel::class.java)) {
            val secureUserRepository = SecureUserRepository.getInstance(context)
            val userManager = UserManager(context)
            
            return SecureWalletViewModel(secureUserRepository, userManager) as T
        }
        
        if (modelClass.isAssignableFrom(SecureDocumentViewModel::class.java)) {
            val secureUserRepository = SecureUserRepository.getInstance(context)
            val userManager = UserManager(context)
            
            return SecureDocumentViewModel(secureUserRepository, userManager) as T
        }
        
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
