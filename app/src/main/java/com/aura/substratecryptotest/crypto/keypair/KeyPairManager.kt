package com.aura.substratecryptotest.crypto.keypair

import android.util.Log
import com.aura.substratecryptotest.utils.Logger
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.Sr25519Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.Keypair
import io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory
// Las clases Sr25519SubstrateKeypairFactory y Ed25519SubstrateKeypairFactory son internas
import io.novasama.substrate_sdk_android.encrypt.EncryptionType
import io.novasama.substrate_sdk_android.encrypt.junction.Junction
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor especializado para operaciones con pares de claves
 * Maneja la generación, derivación y gestión de claves para diferentes algoritmos
 */
class KeyPairManager {
    
    // MnemonicCreator es un objeto singleton, no se instancia
    
    // EncryptionAlgorithm ahora está en su propio archivo
    
    /**
     * Información de un par de claves
     */
    data class KeyPairInfo(
        val keyPair: Keypair,
        val algorithm: EncryptionAlgorithm,
        val publicKey: ByteArray,
        val privateKey: ByteArray?,
        val address: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as KeyPairInfo
            
            if (algorithm != other.algorithm) return false
            if (!publicKey.contentEquals(other.publicKey)) return false
            if (privateKey != null) {
                if (other.privateKey == null) return false
                if (!privateKey.contentEquals(other.privateKey)) return false
            } else if (other.privateKey != null) return false
            if (address != other.address) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = algorithm.hashCode()
            result = 31 * result + publicKey.contentHashCode()
            result = 31 * result + (privateKey?.contentHashCode() ?: 0)
            result = 31 * result + (address?.hashCode() ?: 0)
            return result
        }
    }
    
    /**
     * Genera un par de claves SR25519 desde un mnemonic
     * @param mnemonic String con las palabras del mnemonic
     * @param password Contraseña opcional
     * @return KeyPairInfo con la información del par de claves
     */
    suspend fun generateSr25519KeyPair(mnemonic: String, password: String? = null): KeyPairInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val keyPair = generateKeyPair(EncryptionType.SR25519, mnemonic, password)
                
                if (keyPair != null) {
                    val keyPairInfo = createKeyPairInfo(keyPair, EncryptionAlgorithm.SR25519)
                    
                    // Verificar si las claves son válidas
                    if (keyPairInfo.privateKey == null || keyPairInfo.publicKey.isEmpty() || !isValidKeyPair(keyPair)) {
                        Logger.error("KeyPairManager", "Par de claves inválido", "Claves generadas no son válidas")
                        return@withContext null
                    }
                    
                    Logger.success("KeyPairManager", "Par de claves SR25519 generado exitosamente", 
                        "Algoritmo: ${keyPairInfo.algorithm}, Clave pública: ${keyPairInfo.publicKey.size} bytes")
                    
                    keyPairInfo
                } else {
                    Logger.error("KeyPairManager", "Error generando par de claves SR25519", "No se pudo generar el par de claves")
                    null
                }
            } catch (e: Exception) {
                Logger.error("KeyPairManager", "Excepción generando SR25519", e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Genera un par de claves ED25519 desde un mnemonic
     * @param mnemonic String con las palabras del mnemonic
     * @param password Contraseña opcional
     * @return KeyPairInfo con la información del par de claves
     */
    suspend fun generateEd25519KeyPair(mnemonic: String, password: String? = null): KeyPairInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val keyPair = generateKeyPair(EncryptionType.ED25519, mnemonic, password)
                keyPair?.let { createKeyPairInfo(it, EncryptionAlgorithm.ED25519) }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Genera un par de claves ECDSA desde un mnemonic
     * @param mnemonic String con las palabras del mnemonic
     * @param password Contraseña opcional
     * @return KeyPairInfo con la información del par de claves
     */
    suspend fun generateEcdsaKeyPair(mnemonic: String, password: String? = null): KeyPairInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val keyPair = generateKeyPair(EncryptionType.ECDSA, mnemonic, password)
                keyPair?.let { createKeyPairInfo(it, EncryptionAlgorithm.ECDSA) }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Genera un par de claves con Junctions (derivación)
     * @param algorithm Algoritmo de cifrado
     * @param mnemonic String con las palabras del mnemonic
     * @param junctions Lista de junctions para derivación
     * @param password Contraseña opcional
     * @return KeyPairInfo con la información del par de claves
     */
    suspend fun generateKeyPairWithJunctions(
        algorithm: EncryptionAlgorithm,
        mnemonic: String,
        junctions: List<Junction>,
        password: String? = null
    ): KeyPairInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val encryptionType = when (algorithm) {
                    EncryptionAlgorithm.SR25519 -> EncryptionType.SR25519
                    EncryptionAlgorithm.ED25519 -> EncryptionType.ED25519
                    EncryptionAlgorithm.ECDSA -> EncryptionType.ECDSA
                }
                
                val keyPair = generateKeyPairWithJunctions(encryptionType, mnemonic, junctions, password)
                keyPair?.let { createKeyPairInfo(it, algorithm) }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Genera un par de claves con ruta de derivación
     * @param algorithm Algoritmo de cifrado
     * @param mnemonic String con las palabras del mnemonic
     * @param derivationPath Ruta de derivación
     * @param password Contraseña opcional
     * @return KeyPairInfo con la información del par de claves
     */
    suspend fun generateKeyPairWithPath(
        algorithm: EncryptionAlgorithm,
        mnemonic: String,
        derivationPath: String?,
        password: String? = null
    ): KeyPairInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val encryptionType = when (algorithm) {
                    EncryptionAlgorithm.SR25519 -> EncryptionType.SR25519
                    EncryptionAlgorithm.ED25519 -> EncryptionType.ED25519
                    EncryptionAlgorithm.ECDSA -> EncryptionType.ECDSA
                }
                
                val keyPair = generateKeyPairWithPath(encryptionType, mnemonic, derivationPath, password)
                keyPair?.let { createKeyPairInfo(it, algorithm) }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Genera un par de claves desde un seed
     * @param algorithm Algoritmo de cifrado
     * @param seed ByteArray con el seed
     * @return KeyPairInfo con la información del par de claves
     */
    suspend fun generateKeyPairFromSeed(
        algorithm: EncryptionAlgorithm,
        seed: ByteArray
    ): KeyPairInfo? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("KeyPairManager", "🔍 Generando par de claves desde seed: ${seed.size} bytes")
                
                val encryptionType = when (algorithm) {
                    EncryptionAlgorithm.SR25519 -> EncryptionType.SR25519
                    EncryptionAlgorithm.ED25519 -> EncryptionType.ED25519
                    EncryptionAlgorithm.ECDSA -> EncryptionType.ECDSA
                }
                
                Log.d("KeyPairManager", "🔍 Usando EncryptionType: ${encryptionType.rawName}")
                
                val keyPair = when (algorithm) {
                    EncryptionAlgorithm.SR25519 -> SubstrateKeypairFactory.generate(EncryptionType.SR25519, seed, emptyList())
                    EncryptionAlgorithm.ED25519 -> SubstrateKeypairFactory.generate(EncryptionType.ED25519, seed, emptyList())
                    EncryptionAlgorithm.ECDSA -> null // ECDSA no está disponible en el SDK
                }
                
                if (keyPair != null) {
                    Log.d("KeyPairManager", "🔍 KeyPair generado exitosamente")
                } else {
                    Log.e("KeyPairManager", "❌ Error generando KeyPair")
                }
                keyPair?.let { createKeyPairInfo(it, algorithm) }
            } catch (e: Exception) {
                Log.e("KeyPairManager", "❌ Excepción en generateKeyPairFromSeed: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Obtiene la clave pública de un par de claves
     * @param keyPair Par de claves
     * @return ByteArray con la clave pública
     */
    fun extractPublicKey(keyPair: Keypair): ByteArray? {
        return try {
            keyPair.publicKey
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Obtiene la clave privada de un par de claves
     * @param keyPair Par de claves
     * @return ByteArray con la clave privada
     */
    fun extractPrivateKey(keyPair: Keypair): ByteArray? {
        return try {
            keyPair.privateKey
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Verifica si un par de claves es válido
     * @param keyPair Par de claves
     * @return Boolean indicando si es válido
     */
    fun isValidKeyPair(keyPair: Keypair): Boolean {
        return try {
            val publicKey = keyPair.publicKey
            val privateKey = keyPair.privateKey
            publicKey != null && privateKey != null && publicKey.isNotEmpty() && privateKey.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Genera un par de claves usando el SDK de Substrate
     * @param encryptionType Tipo de cifrado
     * @param mnemonic String con las palabras del mnemonic
     * @param password Contraseña opcional
     * @return Keypair generado
     */
    private suspend fun generateKeyPair(
        encryptionType: EncryptionType,
        mnemonic: String,
        password: String?
    ): Keypair? {
        return withContext(Dispatchers.IO) {
            try {
                val mnemonicObj = MnemonicCreator.fromWords(mnemonic)
                val entropy = mnemonicObj.entropy
                
                // Generar seed de 32 bytes usando nuestro MnemonicManager (PKCS5S2ParametersGenerator con SHA512)
                val mnemonicManager = com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager()
                val seed = mnemonicManager.generateSeed(mnemonic, password)
                
                val keyPair = SubstrateKeypairFactory.generate(encryptionType, seed, emptyList())
                keyPair
            } catch (e: Exception) {
                Logger.error("KeyPairManager", "Excepción en generateKeyPair", e.message ?: "Error desconocido", e)
                null
            }
        }
    }
    
    /**
     * Genera un par de claves con Junctions usando el SDK de Substrate
     * @param encryptionType Tipo de cifrado
     * @param mnemonic String con las palabras del mnemonic
     * @param junctions Lista de junctions
     * @param password Contraseña opcional
     * @return Keypair generado
     */
    private suspend fun generateKeyPairWithJunctions(
        encryptionType: EncryptionType,
        mnemonic: String,
        junctions: List<Junction>,
        password: String?
    ): Keypair? {
        return withContext(Dispatchers.IO) {
            try {
                val mnemonicObj = MnemonicCreator.fromWords(mnemonic)
                val entropy = mnemonicObj.entropy
                // Implementación temporal hasta que generateSeed esté disponible en el SDK
                val seed = entropy // Usar entropy como seed temporalmente
                
                SubstrateKeypairFactory.generate(encryptionType, seed, junctions)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Genera un par de claves con ruta de derivación usando el SDK de Substrate
     * @param encryptionType Tipo de cifrado
     * @param mnemonic String con las palabras del mnemonic
     * @param derivationPath Ruta de derivación
     * @param password Contraseña opcional
     * @return Keypair generado
     */
    private suspend fun generateKeyPairWithPath(
        encryptionType: EncryptionType,
        mnemonic: String,
        derivationPath: String?,
        password: String?
    ): Keypair? {
        return withContext(Dispatchers.IO) {
            try {
                val seed = com.aura.substratecryptotest.crypto.mnemonic.MnemonicManager().generateSeed(mnemonic, password)
                
                if (derivationPath != null) {
                    // Decodificar el path a junctions
                    val decoder = io.novasama.substrate_sdk_android.encrypt.junction.SubstrateJunctionDecoder
                    val decodeResult = decoder.decode(derivationPath)
                    SubstrateKeypairFactory.generate(encryptionType, seed, decodeResult.junctions)
                } else {
                    SubstrateKeypairFactory.generate(encryptionType, seed)
                }
            } catch (e: Exception) {
                println("🔍 KeyPairManager: Error en generateKeyPairWithPath: ${e.message}")
                println("🔍 KeyPairManager: Stack trace: ${e.stackTrace.joinToString("\n")}")
                null
            }
        }
    }
    
    /**
     * Crea información detallada de un par de claves
     * @param keyPair Par de claves
     * @param algorithm Algoritmo utilizado
     * @return KeyPairInfo con la información
     */
    private fun createKeyPairInfo(keyPair: Keypair, algorithm: EncryptionAlgorithm): KeyPairInfo {
        val publicKey = extractPublicKey(keyPair)
        val privateKey = extractPrivateKey(keyPair)
        
        return KeyPairInfo(
            keyPair = keyPair,
            algorithm = algorithm,
            publicKey = publicKey ?: ByteArray(0),
            privateKey = privateKey,
            address = null // Se puede calcular después con SS58Encoder
        )
    }
    
    /**
     * Genera múltiples pares de claves con diferentes algoritmos
     * @param mnemonic String con las palabras del mnemonic
     * @param password Contraseña opcional
     * @return Map con los pares de claves generados
     */
    suspend fun generateAllKeyPairs(mnemonic: String, password: String? = null): Map<EncryptionAlgorithm, KeyPairInfo?> {
        return withContext(Dispatchers.IO) {
            try {
                mapOf(
                    EncryptionAlgorithm.SR25519 to generateSr25519KeyPair(mnemonic, password),
                    EncryptionAlgorithm.ED25519 to generateEd25519KeyPair(mnemonic, password),
                    EncryptionAlgorithm.ECDSA to generateEcdsaKeyPair(mnemonic, password)
                )
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
}
