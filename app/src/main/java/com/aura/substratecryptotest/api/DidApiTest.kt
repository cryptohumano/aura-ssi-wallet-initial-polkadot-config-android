package com.aura.substratecryptotest.api

import android.content.Context
import com.aura.substratecryptotest.utils.Logger
import com.aura.substratecryptotest.wallet.WalletManager
import kotlinx.coroutines.runBlocking

/**
 * Tests básicos para el API Client
 * Verifica la conectividad y funcionalidad básica
 */
class DidApiTest(private val context: Context) {
    
    companion object {
        private const val TAG = "DidApiTest"
    }
    
    private val walletManager = WalletManager(context)
    private val apiManager = DidApiManager(context, walletManager)
    
    /**
     * Ejecuta todos los tests básicos
     */
    fun runBasicTests(): Boolean {
        return runBlocking {
            try {
                Logger.debug(TAG, "🧪 Iniciando tests básicos del API Client", "Verificando conectividad")
                
                // Test 1: Verificar estado del servidor
                val healthTest = testServerHealth()
                if (!healthTest) {
                    Logger.error(TAG, "❌ Test 1: Servidor no disponible", "No se puede conectar al servidor", null)
                    return@runBlocking false
                }
                
                // Test 2: Iniciar autenticación DID
                val authTest = testDidAuthentication()
                if (!authTest) {
                    Logger.error(TAG, "❌ Test 2: Autenticación DID fallida", "No se pudo iniciar autenticación", null)
                    return@runBlocking false
                }
                
                // Test 3: Verificar challenge
                val challengeTest = testChallengeVerification()
                if (!challengeTest) {
                    Logger.error(TAG, "❌ Test 3: Verificación de challenge fallida", "No se pudo verificar challenge", null)
                    return@runBlocking false
                }
                
                // Test 4: Obtener estadísticas
                val statsTest = testGetStats()
                if (!statsTest) {
                    Logger.error(TAG, "❌ Test 4: Obtención de estadísticas fallida", "No se pudieron obtener estadísticas", null)
                    return@runBlocking false
                }
                
                Logger.success(TAG, "🎉 Todos los tests básicos pasaron", "API Client funcionando correctamente")
                true
                
            } catch (e: Exception) {
                Logger.error(TAG, "❌ Error en tests básicos del API Client", e.message ?: "Error desconocido", e)
                false
            }
        }
    }
    
    /**
     * Test 1: Verificar estado del servidor
     */
    private suspend fun testServerHealth(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test 1: Verificando estado del servidor", "GET /health")
            
            val result = apiManager.initialize()
            if (result.isSuccess) {
                val health = result.getOrNull()
                Logger.success(TAG, "✅ Test 1: Servidor disponible", "Status: ${health?.status}, Routes: ${health?.routes?.size}")
                true
            } else {
                Logger.error(TAG, "❌ Test 1: Servidor no disponible", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                false
            }
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test 1: Excepción verificando servidor", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Test 2: Iniciar autenticación DID
     */
    private suspend fun testDidAuthentication(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test 2: Iniciando autenticación DID", "Challenge: test_challenge_api")
            
            // Primero derivar el DID KILT si no existe
            val currentWalletInfo = walletManager.getCurrentWalletInfo()
            if (currentWalletInfo != null && currentWalletInfo.kiltAddress == null) {
                Logger.debug(TAG, "🔧 Derivando DID KILT para autenticación", "Generando DID desde wallet actual")
                val derivationResult = walletManager.deriveKiltDidFromCurrentWallet()
                if (derivationResult == null) {
                    Logger.error(TAG, "❌ No se pudo derivar DID KILT", "Derivación fallida", null)
                    return false
                }
                Logger.success(TAG, "✅ DID KILT derivado exitosamente", "DID: ${derivationResult.did}")
                
                // Verificar que el DID se guardó correctamente
                val updatedWalletInfo = walletManager.getCurrentWalletInfo()
                if (updatedWalletInfo?.kiltAddress != null) {
                    Logger.success(TAG, "✅ DID guardado en wallet", "Address: ${updatedWalletInfo.kiltAddress}")
                } else {
                    Logger.error(TAG, "❌ DID no se guardó en wallet", "Persistencia fallida", null)
                    return false
                }
            } else if (currentWalletInfo?.kiltAddress != null) {
                Logger.success(TAG, "✅ DID ya existe en wallet", "Address: ${currentWalletInfo.kiltAddress}")
            }
            
            val testChallenge = "test_challenge_api_${System.currentTimeMillis()}"
            val result = apiManager.startDidAuthentication(testChallenge, useBiometric = false)
            
            if (result.isSuccess) {
                val authResult = result.getOrNull()
                Logger.success(TAG, "✅ Test 2: Autenticación DID iniciada", "SessionId: ${authResult?.sessionData?.sessionId?.take(10)}..., Token: ${authResult?.authToken?.take(20)}...")
                true
            } else {
                Logger.error(TAG, "❌ Test 2: Error iniciando autenticación DID", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                false
            }
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test 2: Excepción iniciando autenticación DID", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Test 3: Verificar challenge
     */
    private suspend fun testChallengeVerification(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test 3: Verificando challenge", "Usando datos de sesión activa")
            
            if (!apiManager.hasActiveSession()) {
                Logger.error(TAG, "❌ Test 3: No hay sesión activa", "Iniciar autenticación primero", null)
                return false
            }
            
            // Crear un challenge de prueba
            val testChallenge = "test_verification_${System.currentTimeMillis()}"
            val encryptionKey = apiManager.currentSessionData?.encryptionKey
            
            if (encryptionKey == null) {
                Logger.error(TAG, "❌ Test 3: No hay encryption key disponible", "Sesión incompleta", null)
                return false
            }
            
            // Encriptar challenge
            val encryptionKeyManager = com.aura.substratecryptotest.crypto.encryption.EncryptionKeyManager(context)
            val encrypted = encryptionKeyManager.encryptChallenge(testChallenge, encryptionKey)
            
            if (encrypted != null) {
                val (encryptedChallenge, nonce) = encrypted
                
                // Verificar challenge
                val result = apiManager.verifyChallenge(encryptedChallenge, nonce, encryptionKey)
                
                if (result.isSuccess) {
                    val decryptedChallenge = result.getOrNull()
                    if (decryptedChallenge == testChallenge) {
                        Logger.success(TAG, "✅ Test 3: Challenge verificado correctamente", "Original: '$testChallenge' == Desencriptado: '$decryptedChallenge'")
                        true
                    } else {
                        Logger.error(TAG, "❌ Test 3: Challenge desencriptado incorrectamente", "Expected: '$testChallenge', Got: '$decryptedChallenge'", null)
                        false
                    }
                } else {
                    Logger.error(TAG, "❌ Test 3: Error verificando challenge", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                    false
                }
            } else {
                Logger.error(TAG, "❌ Test 3: Error encriptando challenge", "No se pudo encriptar", null)
                false
            }
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test 3: Excepción verificando challenge", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Test 4: Obtener estadísticas
     */
    private suspend fun testGetStats(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Test 4: Obteniendo estadísticas de firmas", "GET /stats")
            
            val result = apiManager.getUserSignatureStats()
            if (result.isSuccess) {
                val stats = result.getOrNull()
                Logger.success(TAG, "✅ Test 4: Estadísticas obtenidas", "Contracts: ${stats?.contractsWithDidSignatures}, Signatures: ${stats?.userDidSignatures}")
                true
            } else {
                Logger.error(TAG, "❌ Test 4: Error obteniendo estadísticas", result.exceptionOrNull()?.message ?: "Error desconocido", null)
                false
            }
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Test 4: Excepción obteniendo estadísticas", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Limpia la sesión después de los tests
     */
    fun cleanup() {
        Logger.debug(TAG, "🧹 Limpiando sesión después de tests", "Cerrando sesión activa")
        apiManager.closeSession()
        Logger.success(TAG, "✅ Sesión limpiada", "Tests completados")
    }
}
