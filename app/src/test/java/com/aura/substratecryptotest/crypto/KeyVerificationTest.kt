package com.aura.substratecryptotest.crypto

import com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm
import com.aura.substratecryptotest.crypto.keypair.KeyPairManager
import com.aura.substratecryptotest.crypto.verification.KeyVerificationManager
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.utils.Logger
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import kotlinx.coroutines.runBlocking

/**
 * Pruebas de verificación de claves criptográficas
 * Estas pruebas verifican que la generación de claves es correcta y compatible con el SDK de Substrate
 */
class KeyVerificationTest {
    
    private val keyPairManager = KeyPairManager()
    private val keyVerificationManager = KeyVerificationManager()
    private val ss58Encoder = SS58Encoder()
    
    /**
     * Prueba de compatibilidad con el SDK de referencia de Substrate
     * Usa el mnemonic de desarrollo estándar de Substrate
     */
    fun testSdkCompatibility() = runBlocking {
        val testMnemonic = "bottom drive obey lake curtain smoke basket hold race lonely fit walk"
        val expectedPublicKey = "46ebddef8cd9bb167dc30878d7113b7e168e6f0646beffd77d69d39bad76b47a"
        val expectedAddress = "5DfhGyQdFobKM8NsWvEeAKk5EQQgYe9AydgJ7rMB6E1EqRzV"
        
        Logger.debug("KeyVerificationTest", "🧪 Iniciando prueba de compatibilidad con SDK", "")
        Logger.debug("KeyVerificationTest", "Mnemónico de prueba", testMnemonic)
        Logger.debug("KeyVerificationTest", "Clave pública esperada", expectedPublicKey)
        Logger.debug("KeyVerificationTest", "Dirección esperada", expectedAddress)
        
        try {
            // Generar par de claves con nuestro KeyPairManager
            val keyPairInfo = keyPairManager.generateSr25519KeyPair(testMnemonic, null)
            
            if (keyPairInfo != null) {
                val publicKeyHex = keyPairInfo.publicKey.joinToString("") { "%02x".format(it) }
                val address = ss58Encoder.encode(keyPairInfo.publicKey, SS58Encoder.NetworkPrefix.SUBSTRATE)
                
                Logger.debug("KeyVerificationTest", "Clave pública generada", publicKeyHex)
                Logger.debug("KeyVerificationTest", "Dirección generada", address)
                
                // Verificar si coinciden
                val publicKeyMatch = publicKeyHex.equals(expectedPublicKey, ignoreCase = true)
                val addressMatch = address == expectedAddress
                
                Logger.success("KeyVerificationTest", "Resultado de la prueba", 
                    "Clave pública: ${if (publicKeyMatch) "✅ COINCIDE" else "❌ NO COINCIDE"}, " +
                    "Dirección: ${if (addressMatch) "✅ COINCIDE" else "❌ NO COINCIDE"}")
                
                if (publicKeyMatch && addressMatch) {
                    Logger.success("KeyVerificationTest", "🎉 ¡PRUEBA EXITOSA! Nuestros managers son compatibles con el SDK de referencia")
                    return@runBlocking true
                } else {
                    Logger.error("KeyVerificationTest", "❌ Prueba fallida", "Los resultados no coinciden con el SDK de referencia")
                    return@runBlocking false
                }
            } else {
                Logger.error("KeyVerificationTest", "❌ Error en la prueba", "No se pudo generar el par de claves")
                return@runBlocking false
            }
        } catch (e: Exception) {
            Logger.error("KeyVerificationTest", "❌ Excepción en la prueba", e.message ?: "Error desconocido", e)
            return@runBlocking false
        }
    }
    
    /**
     * Prueba de verificación con datos conocidos
     * Usa un mnemonic de prueba BIP39 estándar
     */
    fun testKeyGenerationWithKnownData() = runBlocking {
        Logger.debug("KeyVerificationTest", "Iniciando prueba", "con datos conocidos...")
        
        // Mnemonic de prueba conocido (12 palabras)
        val testMnemonic = "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"
        
        try {
            // Generar par de claves con este mnemonic
            val keyPairInfo = keyPairManager.generateSr25519KeyPair(testMnemonic, null)
            
            if (keyPairInfo != null) {
                val publicKeyHex = keyPairInfo.publicKey.joinToString("") { "%02x".format(it) }
                val address = ss58Encoder.encode(keyPairInfo.publicKey, SS58Encoder.NetworkPrefix.SUBSTRATE)
                
                Logger.debug("KeyVerificationTest", "Clave pública generada", publicKeyHex)
                Logger.debug("KeyVerificationTest", "Dirección generada", address)
                
                // Verificar con el verificador
                val verificationResult = keyVerificationManager.verifySr25519KeyPair(keyPairInfo.keyPair, testMnemonic)
                
                if (verificationResult.isValid) {
                    Logger.success("KeyVerificationTest", "🎉 PRUEBA EXITOSA", 
                        "Mnemonic conocido generó claves válidas y verificables")
                    return@runBlocking true
                } else {
                    Logger.error("KeyVerificationTest", "❌ PRUEBA FALLIDA", 
                        "Mnemonic conocido no generó claves válidas")
                    verificationResult.errors.forEach { error ->
                        Logger.error("KeyVerificationTest", "Error en prueba", error)
                    }
                    return@runBlocking false
                }
            } else {
                Logger.error("KeyVerificationTest", "❌ PRUEBA FALLIDA", "No se pudo generar keypair desde mnemonic conocido")
                return@runBlocking false
            }
        } catch (e: Exception) {
            Logger.error("KeyVerificationTest", "❌ Error en prueba", e.message ?: "Error desconocido", e)
            return@runBlocking false
        }
    }
    
    /**
     * Prueba de verificación con el mnemonic de desarrollo estándar de Substrate
     * Este mnemonic genera cuentas conocidas como Alice, Bob, Charlie, etc.
     */
    fun testSubstrateDevelopmentMnemonic() = runBlocking {
        Logger.debug("KeyVerificationTest", "Iniciando prueba", "con mnemonic de desarrollo de Substrate...")
        
        try {
            // Verificar con el verificador
            val verificationResult = keyVerificationManager.verifySubstrateDevelopmentMnemonic()
            
            if (verificationResult.isValid) {
                Logger.success("KeyVerificationTest", "🎉 PRUEBA SUBSTRATE EXITOSA", 
                    "Mnemonic de desarrollo de Substrate generó claves válidas y verificables")
                return@runBlocking true
            } else {
                Logger.error("KeyVerificationTest", "❌ PRUEBA SUBSTRATE FALLIDA", 
                    "Mnemonic de desarrollo de Substrate no generó claves válidas")
                verificationResult.errors.forEach { error ->
                    Logger.error("KeyVerificationTest", "Error en prueba Substrate", error)
                }
                return@runBlocking false
            }
        } catch (e: Exception) {
            Logger.error("KeyVerificationTest", "❌ Error en prueba Substrate", e.message ?: "Error desconocido", e)
            return@runBlocking false
        }
    }
}


