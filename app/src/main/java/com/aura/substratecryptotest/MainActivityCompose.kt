package com.aura.substratecryptotest

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aura.substratecryptotest.data.WalletRepository
import com.aura.substratecryptotest.ui.navigation.WalletNavigation
import com.aura.substratecryptotest.ui.theme.AuraWalletTheme

/**
 * MainActivity principal usando Compose
 * Reemplaza la MainActivity original con una interfaz moderna
 */
class MainActivityCompose : FragmentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar repositorio y cargar wallets persistidas
        val walletRepository = WalletRepository.getInstance(this)
        walletRepository.initialize()
        
        setContent {
            AuraWalletTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WalletNavigation()
                }
            }
        }
    }
}
