package com.aura.substratecryptotest.crypto.encryption

import android.content.Context
import com.aura.substratecryptotest.utils.Logger
import kotlinx.coroutines.runBlocking

/**
 * Test simple para verificar el funcionamiento del EncryptionKeyManager
 * Este archivo se puede eliminar en producción
 */
class EncryptionTest(private val context: Context) {
    
    companion object {
        private const val TAG = "EncryptionTest"
    }
    
    /**
     * Ejecuta tests básicos de encriptación
     */
    fun runBasicTests(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Iniciando tests de encriptación", "Verificando funcionalidad básica")
            
            val encryptionKeyManager = EncryptionKeyManager(context)
            val biometricManager = BiometricManager(context)
            
            // Test 1: Generar salt
            val salt = encryptionKeyManager.generateSalt()
            Logger.success(TAG, "✅ Test 1: Salt generado", "Size: ${salt.size} bytes")
            
            // Test 2: Generar nonce
            val nonce = encryptionKeyManager.generateNonce()
            Logger.success(TAG, "✅ Test 2: Nonce generado", "Size: ${nonce.size} bytes")
            
            // Test 3: Generar encryption key
            runBlocking {
                val encryptionKey = encryptionKeyManager.generateEncryptionKey("test_password", salt)
                if (encryptionKey != null) {
                    Logger.success(TAG, "✅ Test 3: Encryption key generada", "Size: ${encryptionKey.size} bytes")
                    
                    // Test 4: Encriptar/desencriptar challenge
                    val challenge = "test_challenge_123"
                    Logger.debug(TAG, "🧪 Test 4: Iniciando encriptación", "Challenge original: '$challenge'")
                    
                    val encrypted = encryptionKeyManager.encryptChallenge(challenge, encryptionKey)
                    
                    if (encrypted != null) {
                        val (encryptedChallenge, challengeNonce) = encrypted
                        Logger.success(TAG, "✅ Test 4: Challenge encriptado", "Size: ${encryptedChallenge.size} bytes")
                        
                        // Logs detallados del test (formato mejorado)
                        Logger.debug(TAG, "🧪 Test 4: Datos de encriptación", 
                            "Original: '$challenge' → Encrypted: ${encryptedChallenge.take(8).joinToString("") { "%02x".format(it) }}... (${encryptedChallenge.size} bytes)")
                        Logger.debug(TAG, "🧪 Test 4: Nonce usado", 
                            "Nonce: ${challengeNonce.take(8).joinToString("") { "%02x".format(it) }}... (${challengeNonce.size} bytes)")

                        Logger.debug(TAG, "🧪 Test 5: Iniciando desencriptación", "Usando mismo nonce y key")
                        val decrypted = encryptionKeyManager.decryptChallenge(encryptedChallenge, challengeNonce, encryptionKey)
                        
                        if (decrypted == challenge) {
                            Logger.success(TAG, "✅ Test 5: Challenge desencriptado correctamente", "Match: '$decrypted'")
                            Logger.debug(TAG, "🧪 Test 5: Verificación exitosa", "Original: '$challenge' == Desencriptado: '$decrypted'")
                        } else {
                            Logger.error(TAG, "❌ Test 5: Challenge desencriptado incorrectamente", "Expected: '$challenge', Got: '$decrypted'", null)
                            return@runBlocking false
                        }
                    } else {
                        Logger.error(TAG, "❌ Test 4: Error encriptando challenge", "No se pudo encriptar")
                        return@runBlocking false
                    }
                } else {
                    Logger.error(TAG, "❌ Test 3: Error generando encryption key", "No se pudo generar")
                    return@runBlocking false
                }
            }
            
            // Test 6: Verificar disponibilidad biométrica
            val biometricAvailable = biometricManager.isBiometricAvailable()
            Logger.success(TAG, "✅ Test 6: Disponibilidad biométrica", "Available: $biometricAvailable")
            
            Logger.success(TAG, "🎉 Todos los tests pasaron", "EncryptionKeyManager funcionando correctamente")
            true
            
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Error en tests", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Test de integración con SessionManager
     */
    fun runSessionTests(): Boolean {
        return try {
            Logger.debug(TAG, "🧪 Iniciando tests de sesión", "Verificando SessionManager")
            
            val walletManager = com.aura.substratecryptotest.wallet.WalletManager(context)
            val sessionManager = SessionManager(context, walletManager)
            
            runBlocking {
                // Test de autenticación con challenge simulado
                val testChallenge = "server_challenge_${System.currentTimeMillis()}"
                val result = sessionManager.startAuthentication(testChallenge, useBiometric = false)
                
                if (result.success && result.sessionData != null && result.encryptedChallenge != null) {
                    Logger.success(TAG, "✅ Test de sesión exitoso", "SessionId: ${result.sessionData.sessionId.take(10)}...")
                    
                    // Test de verificación usando el nonce correcto
                    val challengeNonce = result.sessionData.challengeNonce
                    if (challengeNonce != null) {
                        val verifiedChallenge = sessionManager.verifyChallenge(
                            result.encryptedChallenge,
                            challengeNonce,
                            result.sessionData.encryptionKey
                        )
                    
                        if (verifiedChallenge == testChallenge) {
                            Logger.success(TAG, "✅ Test de verificación exitoso", "Challenge verificado correctamente")
                            true
                        } else {
                            Logger.error(TAG, "❌ Test de verificación fallido", "Expected: $testChallenge, Got: $verifiedChallenge", null)
                            false
                        }
                    } else {
                        Logger.error(TAG, "❌ No hay nonce disponible", "No se pudo verificar challenge", null)
                        false
                    }
                } else {
                    Logger.error(TAG, "❌ Test de sesión fallido", "Error: ${result.error}")
                    false
                }
            }
            
        } catch (e: Exception) {
            Logger.error(TAG, "❌ Error en tests de sesión", e.message ?: "Error desconocido", e)
            false
        }
    }
}
