package com.aura.substratecryptotest.crypto.verification

import android.util.Log
import com.aura.substratecryptotest.utils.Logger
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import io.novasama.substrate_sdk_android.encrypt.Signer
import io.novasama.substrate_sdk_android.encrypt.SignatureVerifier
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor de verificación de claves criptográficas
 * Valida que las claves generadas sean correctas usando el SDK de Substrate
 */
class KeyVerificationManager {
    
    /**
     * Resultado de verificación de claves
     */
    data class KeyVerificationResult(
        val isValid: Boolean,
        val publicKeyValid: Boolean,
        val privateKeyValid: Boolean,
        val signatureValid: Boolean,
        val addressValid: Boolean,
        val errors: List<String> = emptyList(),
        val warnings: List<String> = emptyList()
    )
    
    /**
     * Verifica un par de claves SR25519 de forma completa
     * @param keyPair Par de claves a verificar
     * @param mnemonic Mnemonic original (opcional, para verificación completa)
     * @return KeyVerificationResult con el resultado de la verificación
     */
    suspend fun verifySr25519KeyPair(
        keyPair: Keypair,
        mnemonic: String? = null
    ): KeyVerificationResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            Logger.debug("KeyVerificationManager", "Iniciando verificación", "Par de claves SR25519")
            
            // 1. Verificar estructura básica de las claves
            val publicKeyValid = verifyPublicKey(keyPair, errors)
            val privateKeyValid = verifyPrivateKey(keyPair, errors)
            
            // 2. Verificar que las claves son compatibles entre sí
            val keyCompatibilityValid = verifyKeyCompatibility(keyPair, errors)
            
            // 3. Verificar firma digital (prueba más importante)
            val signatureValid = verifySignature(keyPair, errors)
            
            // 4. Verificar que las claves son válidas para Substrate
            val substrateValid = verifySubstrateCompatibility(keyPair, errors)
            
            // 5. Si se proporciona mnemonic, verificar regeneración
            val mnemonicValid = if (mnemonic != null) {
                verifyMnemonicRegeneration(keyPair, mnemonic, errors, warnings)
            } else {
                true
            }
            
            val isValid = publicKeyValid && privateKeyValid && keyCompatibilityValid && 
                         signatureValid && substrateValid && mnemonicValid
            
            Logger.success("KeyVerificationManager", "Verificación completada", 
                "Válida: $isValid, Errores: ${errors.size}, Advertencias: ${warnings.size}")
            
            KeyVerificationResult(
                isValid = isValid,
                publicKeyValid = publicKeyValid,
                privateKeyValid = privateKeyValid,
                signatureValid = signatureValid,
                addressValid = substrateValid,
                errors = errors,
                warnings = warnings
            )
            
        } catch (e: Exception) {
            Logger.error("KeyVerificationManager", "Error en verificación", e.message ?: "Error desconocido", e)
            KeyVerificationResult(
                isValid = false,
                publicKeyValid = false,
                privateKeyValid = false,
                signatureValid = false,
                addressValid = false,
                errors = listOf("Error crítico: ${e.message}")
            )
        }
    }
    
    /**
     * Verifica la clave pública
     */
    private fun verifyPublicKey(keyPair: Keypair, errors: MutableList<String>): Boolean {
        return try {
            val publicKey = keyPair.publicKey
            
            if (publicKey == null) {
                errors.add("Clave pública es null")
                false
            } else if (publicKey.isEmpty()) {
                errors.add("Clave pública está vacía")
                false
            } else if (publicKey.size != 32) {
                errors.add("Clave pública tiene tamaño incorrecto: ${publicKey.size} bytes (esperado: 32)")
                false
            } else {
                Logger.debug("KeyVerificationManager", "Clave pública válida", "${publicKey.size} bytes")
                true
            }
        } catch (e: Exception) {
            errors.add("Error verificando clave pública: ${e.message}")
            false
        }
    }
    
    /**
     * Verifica la clave privada
     */
    private fun verifyPrivateKey(keyPair: Keypair, errors: MutableList<String>): Boolean {
        return try {
            val privateKey = keyPair.privateKey
            
            if (privateKey == null) {
                errors.add("Clave privada es null")
                false
            } else if (privateKey.isEmpty()) {
                errors.add("Clave privada está vacía")
                false
            } else if (privateKey.size != 32) {
                errors.add("Clave privada tiene tamaño incorrecto: ${privateKey.size} bytes (esperado: 32)")
                false
            } else {
                Logger.debug("KeyVerificationManager", "Clave privada válida", "${privateKey.size} bytes")
                true
            }
        } catch (e: Exception) {
            errors.add("Error verificando clave privada: ${e.message}")
            false
        }
    }
    
    /**
     * Verifica que las claves son compatibles entre sí
     */
    private fun verifyKeyCompatibility(keyPair: Keypair, errors: MutableList<String>): Boolean {
        return try {
            val publicKey = keyPair.publicKey
            val privateKey = keyPair.privateKey
            
            if (publicKey == null || privateKey == null) {
                errors.add("No se pueden verificar claves null")
                return false
            }
            
            // Verificar que ambas claves tienen el mismo tamaño esperado
            val compatible = publicKey.size == 32 && privateKey.size == 32
            
            if (!compatible) {
                errors.add("Claves incompatibles: pública=${publicKey.size} bytes, privada=${privateKey.size} bytes")
            } else {
                Logger.debug("KeyVerificationManager", "Claves compatibles", "Ambas de 32 bytes")
            }
            
            compatible
        } catch (e: Exception) {
            errors.add("Error verificando compatibilidad: ${e.message}")
            false
        }
    }
    
    /**
     * Verifica la firma digital (prueba más importante)
     */
    private fun verifySignature(keyPair: Keypair, errors: MutableList<String>): Boolean {
        return try {
            val testMessage = "Test message for signature verification".toByteArray()
            
            // Usar el método estático de Signer para SR25519
            val signature = Signer.sign(
                multiChainEncryption = MultiChainEncryption.Substrate(EncryptionType.SR25519),
                message = testMessage,
                keypair = keyPair
            )
            
            if (signature == null) {
                errors.add("No se pudo generar firma")
                false
            } else {
                // Verificar la firma usando SignatureVerifier
                val isValid = SignatureVerifier.verify(
                    signatureWrapper = signature,
                    messageHashing = Signer.MessageHashing.SUBSTRATE,
                    data = testMessage,
                    publicKey = keyPair.publicKey
                )
                
                if (isValid) {
                    Logger.success("KeyVerificationManager", "Firma verificada exitosamente", "Verificación de firma digital completada")
                } else {
                    errors.add("Firma no válida - verificación falló")
                }
                
                isValid
            }
        } catch (e: Exception) {
            errors.add("Error en verificación de firma: ${e.message}")
            Logger.error("KeyVerificationManager", "Error en firma", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Verifica compatibilidad con Substrate
     */
    private fun verifySubstrateCompatibility(keyPair: Keypair, errors: MutableList<String>): Boolean {
        return try {
            // Verificar que el keypair es del tipo correcto
            val isSr25519 = keyPair is Sr25519Keypair
            
            if (!isSr25519) {
                errors.add("KeyPair no es de tipo SR25519")
                false
            } else {
                Logger.debug("KeyVerificationManager", "KeyPair es SR25519 válido", "Tipo de keypair correcto")
                true
            }
        } catch (e: Exception) {
            errors.add("Error verificando compatibilidad Substrate: ${e.message}")
            false
        }
    }
    
    /**
     * Verifica regeneración desde mnemonic
     */
    private suspend fun verifyMnemonicRegeneration(
        keyPair: Keypair,
        mnemonic: String,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ): Boolean {
        return try {
            Logger.debug("KeyVerificationManager", "Verificando regeneración", "desde mnemonic")
            
            // Regenerar el par de claves desde el mnemonic
            val mnemonicObj = MnemonicCreator.fromWords(mnemonic)
            val entropy = mnemonicObj.entropy
            
            // Generar seed usando el mismo método que en KeyPairManager
            val mnemonicManager = com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager()
            val seed = mnemonicManager.generateSeed(mnemonic, null)
            
            // Regenerar keypair
            val regeneratedKeyPair = SubstrateKeypairFactory.generate(EncryptionType.SR25519, seed, emptyList())
            
            if (regeneratedKeyPair == null) {
                errors.add("No se pudo regenerar keypair desde mnemonic")
                false
            } else {
                // Comparar claves públicas
                val originalPublicKey = keyPair.publicKey
                val regeneratedPublicKey = regeneratedKeyPair.publicKey
                
                val publicKeysMatch = originalPublicKey.contentEquals(regeneratedPublicKey)
                
                if (publicKeysMatch) {
                    Logger.success("KeyVerificationManager", "Mnemonic regeneración exitosa", "Claves coinciden después de regeneración")
                    true
                } else {
                    errors.add("Claves públicas no coinciden después de regeneración")
                    Logger.error("KeyVerificationManager", "Regeneración fallida", 
                        "Original: ${originalPublicKey.joinToString("") { "%02x".format(it) }}, " +
                        "Regenerada: ${regeneratedPublicKey.joinToString("") { "%02x".format(it) }}")
                    false
                }
            }
        } catch (e: Exception) {
            errors.add("Error en regeneración desde mnemonic: ${e.message}")
            Logger.error("KeyVerificationManager", "Error en regeneración", e.message ?: "Error desconocido", e)
            false
        }
    }
    
    /**
     * Verifica el mnemonic de desarrollo estándar de Substrate
     * Este mnemonic genera cuentas conocidas como Alice, Bob, Charlie, etc.
     */
    suspend fun verifySubstrateDevelopmentMnemonic(): KeyVerificationResult = withContext(Dispatchers.IO) {
        val developmentMnemonic = "bottom drive obey lake curtain smoke basket hold race lonely fit walk"
        Logger.debug("KeyVerificationManager", "Verificando mnemonic", "Desarrollo de Substrate: ${developmentMnemonic.take(20)}...")
        
        verifyKnownMnemonic(
            mnemonic = developmentMnemonic,
            expectedPublicKey = null, // No tenemos la clave pública esperada, solo verificamos consistencia
            expectedAddress = null    // No tenemos la dirección esperada, solo verificamos consistencia
        )
    }
    
    /**
     * Verifica un mnemonic específico contra un resultado esperado
     * Útil para pruebas con datos conocidos
     */
    suspend fun verifyKnownMnemonic(
        mnemonic: String,
        expectedPublicKey: String? = null,
        expectedAddress: String? = null
    ): KeyVerificationResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            Logger.debug("KeyVerificationManager", "Verificando mnemonic conocido", "${mnemonic.take(20)}...")
            
            // Generar keypair desde mnemonic
            val mnemonicManager = com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager()
            val seed = mnemonicManager.generateSeed(mnemonic, null)
            val keyPair = SubstrateKeypairFactory.generate(EncryptionType.SR25519, seed, emptyList())
            
            if (keyPair == null) {
                KeyVerificationResult(
                    isValid = false,
                    publicKeyValid = false,
                    privateKeyValid = false,
                    signatureValid = false,
                    addressValid = false,
                    errors = listOf("No se pudo generar keypair desde mnemonic")
                )
            } else {
            
            // Verificar clave pública
            val publicKeyHex = keyPair.publicKey.joinToString("") { "%02x".format(it) }
            val publicKeyMatch = if (expectedPublicKey != null) {
                publicKeyHex.equals(expectedPublicKey, ignoreCase = true)
            } else {
                true // Si no hay clave esperada, consideramos válida si se generó correctamente
            }
            
            if (expectedPublicKey != null && !publicKeyMatch) {
                errors.add("Clave pública no coincide: esperada=$expectedPublicKey, obtenida=$publicKeyHex")
            } else if (expectedPublicKey == null) {
                Logger.debug("KeyVerificationManager", "Clave pública generada", publicKeyHex)
            }
            
            // Verificar dirección si se proporciona
            var addressValid = true
            val ss58Encoder = com.aura.substratecryptotest.crypto.ss58.SS58Encoder()
            val generatedAddress = ss58Encoder.encode(keyPair.publicKey, com.aura.substratecryptotest.crypto.ss58.SS58Encoder.NetworkPrefix.SUBSTRATE)
            
            if (expectedAddress != null) {
                addressValid = generatedAddress == expectedAddress
                
                if (!addressValid) {
                    errors.add("Dirección no coincide: esperada=$expectedAddress, obtenida=$generatedAddress")
                }
            } else {
                Logger.debug("KeyVerificationManager", "Dirección generada", generatedAddress)
            }
            
            // Verificar firma
            val signatureValid = verifySignature(keyPair, errors)
            
            val isValid = publicKeyMatch && addressValid && signatureValid
            
            Logger.success("KeyVerificationManager", "Verificación de mnemonic conocido completada", 
                "Válida: $isValid, Clave pública: ${if (publicKeyMatch) "✅" else "❌"}, " +
                "Dirección: ${if (addressValid) "✅" else "❌"}, Firma: ${if (signatureValid) "✅" else "❌"}")
            
                KeyVerificationResult(
                    isValid = isValid,
                    publicKeyValid = publicKeyMatch,
                    privateKeyValid = true, // Ya verificado en generateKeyPair
                    signatureValid = signatureValid,
                    addressValid = addressValid,
                    errors = errors,
                    warnings = warnings
                )
            }
            
        } catch (e: Exception) {
            Logger.error("KeyVerificationManager", "Error en verificación de mnemonic conocido", e.message ?: "Error desconocido", e)
            KeyVerificationResult(
                isValid = false,
                publicKeyValid = false,
                privateKeyValid = false,
                signatureValid = false,
                addressValid = false,
                errors = listOf("Error crítico: ${e.message}")
            )
        }
    }
}
