package com.aura.substratecryptotest.data.wallet

import android.content.Context
import androidx.lifecycle.Observer
import com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.wallet.Wallet
import com.aura.substratecryptotest.wallet.WalletManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Servicio para gestionar la creación de wallets
 */
class WalletCreationService(private val context: Context) {
    
    private val walletManager = WalletManager(context)
    
    /**
     * Crea una wallet final usando WalletManager (versión con callbacks)
     */
    fun createFinalWallet(
        walletName: String,
        validatedMnemonic: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit,
        onWalletCreated: (Wallet) -> Unit,
        observers: MutableList<Observer<*>>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Logger.debug("WalletCreationService", "Creando wallet final", "Nombre: $walletName")

                // Usar WalletManager para crear la wallet final (solo cuenta de fondos)
                walletManager.createFundsAccountOnly(
                    name = walletName,
                    mnemonic = validatedMnemonic,
                    password = null,
                    cryptoType = EncryptionAlgorithm.SR25519
                )

                // Observar el resultado en el hilo principal
                withContext(Dispatchers.Main) {
                    val walletObserver = Observer<Wallet?> { wallet ->
                        if (wallet != null) {
                            Logger.success("WalletCreationService", "Wallet final creada", wallet.name)
                            onWalletCreated(wallet)
                            onSuccess()
                        }
                    }
                    
                    val errorObserver = Observer<String?> { error ->
                        if (error != null) {
                            Logger.error("WalletCreationService", "Error creando wallet final", error, null)
                            onError(error)
                        }
                    }
                    
                    // Agregar observadores a la lista para gestión
                    observers.add(walletObserver)
                    observers.add(errorObserver)
                    
                    walletManager.currentWallet.observeForever(walletObserver)
                    walletManager.error.observeForever(errorObserver)
                }

            } catch (e: Exception) {
                Logger.error("WalletCreationService", "Error creando wallet final", e.message ?: "Error desconocido", e)
                onError("Error creando wallet: ${e.message}")
            }
        }
    }
    
    /**
     * Crea una wallet final usando WalletManager (versión con Result)
     */
    suspend fun createFinalWalletWithResult(
        walletName: String, 
        validatedMnemonic: String,
        onWalletCreated: (Wallet) -> Unit,
        observers: MutableList<Observer<*>>
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                if (walletName.isBlank() || validatedMnemonic.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Nombre de wallet y mnemonic no pueden estar vacíos"))
                }
                
                Logger.debug("WalletCreationService", "Creando wallet final", "Nombre: $walletName")

                // Usar WalletManager para crear la wallet final (solo cuenta de fondos)
                walletManager.createFundsAccountOnly(
                    name = walletName,
                    mnemonic = validatedMnemonic,
                    password = null,
                    cryptoType = EncryptionAlgorithm.SR25519
                )

                // Esperar a que se complete la creación
                var walletCreated = false
                var errorOccurred: String? = null
                
                withContext(Dispatchers.Main) {
                    val walletObserver = Observer<Wallet?> { wallet ->
                        if (wallet != null && !walletCreated) {
                            walletCreated = true
                            Logger.success("WalletCreationService", "Wallet final creada", wallet.name)
                            onWalletCreated(wallet)
                        }
                    }
                    
                    val errorObserver = Observer<String?> { error ->
                        if (error != null && !walletCreated) {
                            errorOccurred = error
                        }
                    }
                    
                    observers.add(walletObserver)
                    observers.add(errorObserver)
                    
                    walletManager.currentWallet.observeForever(walletObserver)
                    walletManager.error.observeForever(errorObserver)
                }
                
                // Esperar un tiempo razonable para la creación
                delay(5000) // 5 segundos timeout
                
                if (walletCreated) {
                    Result.success(Unit)
                } else if (errorOccurred != null) {
                    Result.failure(Exception(errorOccurred))
                } else {
                    Result.failure(Exception("Timeout creando wallet"))
                }
            } catch (e: Exception) {
                Logger.error("WalletCreationService", "Error creando wallet final", e.message ?: "Error desconocido", e)
                Result.failure(e)
            }
        }
    }
}
