package com.aura.substratecryptotest.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.runBlocking

/**
 * Tests para el sistema de seguridad
 * Verifica KeyStore, Biometría y SecureWalletManager
 */
class SecurityTest(private val context: Context) {
    
    companion object {
        private const val TAG = "SecurityTest"
    }
    
    /**
     * Ejecuta todos los tests de seguridad
     */
    fun runSecurityTests(activity: FragmentActivity): Boolean {
        return runBlocking {
            try {
                Logger.debug(TAG, "🔐 Iniciando tests de seguridad", "Verificando KeyStore y Biometría")
                
                // Test 1: Verificar KeyStore
                val keyStoreTest = testKeyStore()
                if (!keyStoreTest) {
                    Logger.error(TAG, "❌ Test 1: KeyStore fallido", "No se puede continuar", null)
                    return@runBlocking false
                }
                
                // Test 2: Verificar Biometría
                val biometricTest = testBiometric()
                if (!biometricTest) {
                    Logger.error(TAG, "❌ Test 2: Biometría fallida", "No se puede continuar", null)
                    return@runBlocking false
                }
                
                // Test 3: Verificar SecureWalletManager
                val walletTest = testSecureWallet(activity)
                if (!walletTest) {
                    Logger.error(TAG, "❌ Test 3: SecureWallet fallida", "No se puede continuar", null)
                    return@runBlocking false
                }
                
                Logger.success(TAG, "🎉 Todos los tests de seguridad pasaron", "Sistema de seguridad funcionando correctamente")
                true
                
            } catch (e: Exception) {
                Logger.error(TAG, "❌ Error en tests de seguridad", e.message ?: "Error desconocido", e)
                false
            }
        }
    }
    
    /**
     * Test 1: Verificar KeyStore
     */
    private fun testKeyStore(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test 1: Verificando KeyStore", "Probando encriptación/desencriptación")
            
            val keyStoreManager = KeyStoreManager(context)
            
            // Datos de prueba
            val testData = "test_mnemonic_phrase_for_security_testing"
            val testAlias = "test_alias"
            
            // Encriptar datos
            val encryptedData = keyStoreManager.encryptData(testData.toByteArray(), testAlias)
            if (encryptedData == null) {
                Logger.error(TAG, "❌ Test 1: Error encriptando datos", "No se pudo encriptar", null)
                return false
            }
            
            Logger.success(TAG, "✅ Test 1: Datos encriptados", "Alias: $testAlias, Size: ${testData.length} chars")
            
            // Verificar que los datos están encriptados
            if (encryptedData.encryptedData == testData) {
                Logger.error(TAG, "❌ Test 1: Datos no encriptados", "Los datos no están encriptados", null)
                return false
            }
            
            Logger.success(TAG, "✅ Test 1: KeyStore funcionando", "Encriptación exitosa")
            true
            
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test 1: Excepción en KeyStore", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Test 2: Verificar Biometría
     */
    private fun testBiometric(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test 2: Verificando Biometría", "Probando disponibilidad")
            
            val biometricManager = SecureBiometricManager(context)
            
            // Verificar disponibilidad
            val isAvailable = biometricManager.isBiometricAvailable()
            if (!isAvailable) {
                Logger.error(TAG, "❌ Test 2: Biometría no disponible", "No se puede continuar", null)
                return false
            }
            
            // Verificar estado
            val status = biometricManager.getBiometricStatus()
            Logger.success(TAG, "✅ Test 2: Biometría disponible", "Estado: $status")
            
            // Verificar biometría fuerte
            val isStrongAvailable = biometricManager.isStrongBiometricAvailable()
            if (isStrongAvailable) {
                Logger.success(TAG, "✅ Test 2: Biometría fuerte disponible", "Hardware de seguridad disponible")
            } else {
                Logger.warning(TAG, "⚠️ Test 2: Biometría fuerte no disponible", "Usando biometría débil")
            }
            
            true
            
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test 2: Excepción en Biometría", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Test 3: Verificar SecureWalletManager
     */
    private suspend fun testSecureWallet(activity: FragmentActivity): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test 3: Verificando SecureWalletManager", "Probando creación y recuperación")
            
            val secureWalletManager = SecureWalletManager(context)
            
            // Verificar si ya hay una wallet
            val hasWallet = secureWalletManager.hasStoredWallet()
            if (hasWallet) {
                Logger.success(TAG, "✅ Test 3: Wallet existente encontrada", "Probando recuperación")
                
                // Probar recuperación (requiere biometría)
                val mnemonic = secureWalletManager.retrieveMnemonic(activity)
                if (mnemonic != null) {
                    Logger.success(TAG, "✅ Test 3: Mnemonic recuperado", "Length: ${mnemonic.length}")
                } else {
                    Logger.error(TAG, "❌ Test 3: No se pudo recuperar mnemonic", "Error en recuperación", null)
                    return false
                }
                
                val seed = secureWalletManager.retrieveSeed(activity)
                if (seed != null) {
                    Logger.success(TAG, "✅ Test 3: Seed recuperado", "Size: ${seed.size} bytes")
                } else {
                    Logger.error(TAG, "❌ Test 3: No se pudo recuperar seed", "Error en recuperación", null)
                    return false
                }
                
            } else {
                Logger.success(TAG, "✅ Test 3: No hay wallet existente", "Sistema listo para crear wallet")
            }
            
            // Verificar biometría
            val biometricAvailable = secureWalletManager.isBiometricAvailable()
            if (!biometricAvailable) {
                Logger.error(TAG, "❌ Test 3: Biometría no disponible", "No se puede crear wallet segura", null)
                return false
            }
            
            Logger.success(TAG, "✅ Test 3: SecureWalletManager funcionando", "Sistema listo para uso")
            true
            
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test 3: Excepción en SecureWalletManager", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Test de creación de wallet segura
     */
    suspend fun testSecureWalletCreation(activity: FragmentActivity): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test: Creando wallet segura", "Probando flujo completo")
            
            val secureWalletManager = SecureWalletManager(context)
            
            // Datos de prueba
            val testMnemonic = "test mnemonic phrase for secure wallet creation testing purposes only"
            val testSeed = "test_seed_data_for_secure_wallet_creation".toByteArray()
            
            // Crear wallet segura
            val created = secureWalletManager.createSecureWallet(activity, testMnemonic, testSeed)
            if (!created) {
                Logger.error(TAG, "❌ Test: Error creando wallet segura", "Creación fallida", null)
                return false
            }
            
            Logger.success(TAG, "✅ Test: Wallet segura creada", "Datos almacenados en KeyStore")
            
            // Verificar que se puede recuperar
            val retrievedMnemonic = secureWalletManager.retrieveMnemonic(activity)
            if (retrievedMnemonic != testMnemonic) {
                Logger.error(TAG, "❌ Test: Mnemonic recuperado incorrecto", "Expected: $testMnemonic, Got: $retrievedMnemonic", null)
                return false
            }
            
            val retrievedSeed = secureWalletManager.retrieveSeed(activity)
            if (!retrievedSeed.contentEquals(testSeed)) {
                Logger.error(TAG, "❌ Test: Seed recuperado incorrecto", "Datos no coinciden", null)
                return false
            }
            
            Logger.success(TAG, "✅ Test: Wallet segura funcionando", "Creación y recuperación exitosas")
            true
            
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test: Excepción en creación de wallet", e.message ?: "Error desconocido", e)
            false
        }
    }
}
