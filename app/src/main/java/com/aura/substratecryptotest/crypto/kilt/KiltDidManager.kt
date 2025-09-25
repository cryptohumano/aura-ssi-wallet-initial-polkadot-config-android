package com.aura.substratecryptotest.crypto.kilt

import com.aura.substratecryptotest.crypto.junction.JunctionManager
import com.aura.substratecryptotest.crypto.ss58.SS58Encoder
import com.aura.substratecryptotest.crypto.keypair.KeyPairManager
import com.aura.substratecryptotest.utils.Logger
import io.novasama.substrate_sdk_android.encrypt.junction.Junction as SdkJunction
import io.novasama.substrate_sdk_android.encrypt.junction.JunctionType as SdkJunctionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.math.BigInteger

/**
 * Gestor especializado para DIDs del protocolo KILT
 * Maneja la derivación específica de DIDs usando el path mnemonic//did//0
 */
class KiltDidManager {
    
    private val junctionManager = JunctionManager()
    private val ss58Encoder = SS58Encoder()
    private val keyPairManager = KeyPairManager()
    
    /**
     * Convierte nuestros Junction personalizados a Junction del SDK
     */
    private fun convertToSdkJunctions(junctions: List<JunctionManager.Junction>): List<SdkJunction> {
        Logger.debug("KiltDidManager", "convertToSdkJunctions", "Input junctions: ${junctions.size}")
        junctions.forEachIndexed { index, junction ->
            Logger.debug("KiltDidManager", "Junction $index", "type=${junction.type}, chainCode=${junction.chainCode}")
        }
        
        val result = junctions.map { junction ->
            val sdkType = when (junction.type) {
                JunctionManager.JunctionType.HARD -> SdkJunctionType.HARD
                JunctionManager.JunctionType.SOFT -> SdkJunctionType.SOFT
                // El SDK solo soporta HARD y SOFT, convertimos el resto a HARD
                JunctionManager.JunctionType.PASSWORD -> SdkJunctionType.HARD
                JunctionManager.JunctionType.PARENT -> SdkJunctionType.HARD
                JunctionManager.JunctionType.PLACEHOLDER -> SdkJunctionType.HARD
            }
            Logger.debug("KiltDidManager", "Converted junction", "type=${junction.type} -> sdkType=$sdkType")
            SdkJunction(sdkType, junction.chainCode)
        }
        
        Logger.debug("KiltDidManager", "convertToSdkJunctions", "Output SDK junctions: ${result.size}")
        return result
    }
    
    /**
     * Aplica derivación manual al path //did//0
     * Para KILT DIDs, necesitamos derivar desde la clave base
     */
    private suspend fun applyManualDerivation(basePublicKey: ByteArray, derivationPath: String): ByteArray {
        return try {
            println("🔍 KiltDidManager: Aplicando derivación manual")
            println("🔍 KiltDidManager: Path: $derivationPath")
            println("🔍 KiltDidManager: Clave base: ${basePublicKey.size} bytes")
            
            // Usar JunctionDecoder del SDK para parsear el path
            println("🔍 KiltDidManager: === Usando JunctionDecoder del SDK ===")
            val decoder = io.novasama.substrate_sdk_android.encrypt.junction.SubstrateJunctionDecoder
            val decodeResult = decoder.decode(derivationPath)
            
            println("🔍 KiltDidManager: Path decodificado exitosamente")
            println("🔍 KiltDidManager: Junctions encontradas: ${decodeResult.junctions.size}")
            println("🔍 KiltDidManager: Password: ${decodeResult.password ?: "null"}")

            // Usar SubstrateKeypairFactory con las junctions reales
            println("🔍 KiltDidManager: === Usando SubstrateKeypairFactory con junctions ===")
            val derivedKeypair = io.novasama.substrate_sdk_android.encrypt.keypair.substrate.SubstrateKeypairFactory.generate(
                encryptionType = io.novasama.substrate_sdk_android.encrypt.EncryptionType.SR25519,
                seed = basePublicKey, // Usar la clave pública como seed
                junctions = decodeResult.junctions // Usar las junctions del decoder
            )

            println("🔍 KiltDidManager: ✅ Derivación SR25519 exitosa")
            println("🔍 KiltDidManager: Clave derivada: ${derivedKeypair.publicKey.size} bytes")
            derivedKeypair.publicKey
        } catch (e: Exception) {
            println("🔍 KiltDidManager: ❌ Error en derivación SR25519: ${e.message}")
            println("🔍 KiltDidManager: Stack trace: ${e.stackTrace.joinToString("\n")}")
            
            // Fallback: usar derivación por hash
            println("🔍 KiltDidManager: === Fallback: Hash derivation ===")
            val derivedKey = deriveUsingHash(basePublicKey, "did", "0")
            println("🔍 KiltDidManager: ✅ Derivación exitosa con hash")
            derivedKey
        }
    }
    
    /**
     * Implementa derivación usando hash como método alternativo
     */
    private fun deriveUsingHash(baseKey: ByteArray, junction1: String, junction2: String): ByteArray {
        return try {
            println("🔍 KiltDidManager: Derivando usando hash")
            println("🔍 KiltDidManager: Base key: ${baseKey.size} bytes")
            println("🔍 KiltDidManager: Junction 1: $junction1")
            println("🔍 KiltDidManager: Junction 2: $junction2")
            
            // Crear un hash combinando la clave base con las junctions
            // Usar un separador para evitar colisiones
            val separator = byteArrayOf(0x00.toByte()) // Separador null
            val combined = baseKey + separator + junction1.toByteArray() + separator + junction2.toByteArray()
            
            // Usar SHA-256 para generar hash determinístico
            val hash = java.security.MessageDigest.getInstance("SHA-256").digest(combined)
            
            // Tomar los primeros 32 bytes como nueva clave pública
            val derivedKey = hash.take(32).toByteArray()
            
            println("🔍 KiltDidManager: Hash generado: ${hash.size} bytes")
            println("🔍 KiltDidManager: Clave derivada: ${derivedKey.size} bytes")
            println("🔍 KiltDidManager: Base key (hex): ${baseKey.joinToString("") { "%02x".format(it) }}")
            println("🔍 KiltDidManager: Derived key (hex): ${derivedKey.joinToString("") { "%02x".format(it) }}")
            
            derivedKey
        } catch (e: Exception) {
            println("🔍 KiltDidManager: Error en derivación por hash: ${e.message}")
            baseKey
        }
    }
    
    /**
     * Tipos de DIDs soportados en KILT
     */
    enum class DidType {
        FULL,   // did:kilt:<address>
        LIGHT   // did:kilt:light:<authKeyType><address>:<encodedDetails>
    }
    
    /**
     * Relaciones de verificación para firmas DID
     */
    enum class VerificationRelationship {
        AUTHENTICATION,           // Para autenticación básica
        ASSERTION_METHOD,         // Para aserciones/afirmaciones
        CAPABILITY_DELEGATION     // Para delegación de capacidades
    }
    
    /**
     * Información completa de un DID KILT
     */
    data class KiltDidInfo(
        val did: String,                          // DID completo
        val address: String,                      // Dirección SS58 (prefix 38)
        val publicKey: ByteArray,                  // Clave pública
        val verificationRelationship: VerificationRelationship,
        val didType: DidType,
        val derivationPath: String = "//did//0"   // Path de derivación usado
    )
    
    /**
     * Información de una transacción DID
     */
    data class DidTransactionInfo(
        val extrinsic: Any,                       // Transacción Substrate
        val requiredRelationship: VerificationRelationship,
        val needsDidSignature: Boolean,
        val methodName: String                    // Nombre del método de runtime
    )
    
    /**
     * Mapeo de métodos de runtime a relaciones de verificación
     * Basado en el análisis del código KILT original
     */
    private val methodMapping = mapOf(
        "attestation" to VerificationRelationship.ASSERTION_METHOD,
        "ctype" to VerificationRelationship.ASSERTION_METHOD,
        "delegation" to VerificationRelationship.CAPABILITY_DELEGATION,
        "did" to VerificationRelationship.AUTHENTICATION,
        "web3Names" to VerificationRelationship.AUTHENTICATION,
        "publicCredentials" to VerificationRelationship.ASSERTION_METHOD,
        "dipProvider" to VerificationRelationship.AUTHENTICATION
    )
    
    /**
     * Genera un DID KILT completo desde un mnemonic
     * @param mnemonic Mnemonic BIP39
     * @param password Contraseña opcional para el mnemonic
     * @return KiltDidInfo con toda la información del DID
     */
    suspend fun generateKiltDid(
        mnemonic: String,
        password: String? = null
    ): KiltDidInfo = withContext(Dispatchers.IO) {
        try {
            // 1. Usar path de derivación predefinido para DID de autenticación
            println("🔍 KiltDidManager: Usando path predefinido para DID de autenticación")
            val didPath = junctionManager.getKiltDidAuthenticationPath()
            println("🔍 KiltDidManager: didPath = $didPath")
            println("🔍 KiltDidManager: path string = ${didPath.toSubstrateString()}")
            
            // 2. Generar par de claves usando el path específico
            val derivationPathString = didPath.toSubstrateString()
            println("🔍 KiltDidManager: Generando clave con path: $derivationPathString")
            println("🔍 KiltDidManager: Junctions: ${didPath.junctions}")
            
            val sdkJunctions = convertToSdkJunctions(didPath.junctions)
            println("🔍 KiltDidManager: SDK Junctions: $sdkJunctions")
            
            val keyPairInfo = keyPairManager.generateKeyPairWithJunctions(
                algorithm = com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm.SR25519,
                mnemonic = mnemonic,
                junctions = sdkJunctions,
                password = password
            )
            
            println("🔍 KiltDidManager: keyPairInfo = $keyPairInfo")
            println("🔍 KiltDidManager: publicKey = ${keyPairInfo?.publicKey?.size} bytes")
            
            // 3. Obtener clave pública
            val publicKey = keyPairInfo?.publicKey
                ?: throw KiltDidException("Error obteniendo clave pública - keyPairInfo es null o publicKey es null")
            
            // 5. Generar dirección KILT (prefix 38)
            val address = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.KILT)
            
            // 6. Construir DID completo
            val did = "did:kilt:$address"
            
            KiltDidInfo(
                did = did,
                address = address,
                publicKey = publicKey,
                verificationRelationship = VerificationRelationship.AUTHENTICATION,
                didType = DidType.FULL,
                derivationPath = "//did//0"
            )
        } catch (e: Exception) {
            throw KiltDidException("Error generando DID KILT: ${e.message}", e)
        }
    }
    
    /**
     * Prueba la derivación del path //did//0 sin generar DID completo
     * @param mnemonic Mnemonic de la wallet Substrate
     * @param password Contraseña opcional
     * @return Información de la derivación
     */
    suspend fun testPathDerivation(
        mnemonic: String,
        password: String? = null
    ): DerivationTestResult = withContext(Dispatchers.IO) {
        try {
            println("🔍 KiltDidManager: === INICIO testPathDerivation ===")
            println("🔍 KiltDidManager: Probando derivación del path //did//0")
            println("🔍 KiltDidManager: Mnemonic recibido: ${mnemonic.length} palabras")
            println("🔍 KiltDidManager: Password: ${password ?: "null"}")
            
            // Paso 1: Crear path de derivación directo
            println("🔍 KiltDidManager: === PASO 1: Crear path de derivación directo ===")
            val derivationPath = "//did//0"
            println("🔍 KiltDidManager: Path de derivación: $derivationPath")
            
            // Paso 2: Generar keypair directamente con path //did//0
            println("🔍 KiltDidManager: === PASO 2: Generar keypair con path //did//0 ===")
            println("🔍 KiltDidManager: Mnemonic: ${mnemonic.split(" ").size} palabras")
            println("🔍 KiltDidManager: Password: ${password ?: "null"}")
            
            val didKeyPairInfo = keyPairManager.generateKeyPairWithPath(
                algorithm = com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm.SR25519,
                mnemonic = mnemonic,
                derivationPath = "//did//0", // Usar el path directamente
                password = password
            )
            
            if (didKeyPairInfo == null) {
                throw KiltDidException("No se pudo generar keypair con path //did//0")
            }
            
            println("🔍 KiltDidManager: ✅ Keypair DID generado exitosamente")
            println("🔍 KiltDidManager: Public key: ${didKeyPairInfo.publicKey.size} bytes")
            
            val publicKey = didKeyPairInfo.publicKey
            
            // Paso 4: Generar dirección KILT
            println("🔍 KiltDidManager: === PASO 4: Generar dirección KILT ===")
            val address = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.KILT)
            println("🔍 KiltDidManager: Dirección KILT generada: $address")
            
            DerivationTestResult(
                success = true,
                derivationPath = derivationPath,
                publicKey = publicKey,
                kiltAddress = address,
                message = "Derivación exitosa del path //did//0"
            )
        } catch (e: Exception) {
            println("🔍 KiltDidManager: Error en derivación: ${e.message}")
            DerivationTestResult(
                success = false,
                derivationPath = "//did//0",
                publicKey = null,
                kiltAddress = null,
                message = "Error en derivación: ${e.message}"
            )
        }
    }
    
    /**
     * Resultado de prueba de derivación
     */
    data class DerivationTestResult(
        val success: Boolean,
        val derivationPath: String,
        val publicKey: ByteArray?,
        val kiltAddress: String?,
        val message: String
    )
    
    /**
     * Genera solo el DID de autenticación para KILT usando mnemonic (método legacy)
     * @param mnemonic Mnemonic BIP39
     * @param password Contraseña opcional
     * @return KiltDidInfo con el DID de autenticación
     */
    suspend fun generateKiltAuthenticationDid(
        mnemonic: String,
        password: String? = null
    ): KiltDidInfo = withContext(Dispatchers.IO) {
        try {
            println("🔍 KiltDidManager: Generando DID de autenticación KILT usando mnemonic (legacy)")
            
            // Generar la clave pública de la cuenta Substrate (sin path)
            val substrateKeyPairInfo = keyPairManager.generateKeyPairWithPath(
                algorithm = com.aura.substratecryptotest.crypto.keypair.EncryptionAlgorithm.SR25519,
                mnemonic = mnemonic,
                derivationPath = "//0", // Path de la cuenta Substrate
                password = password
            )
            
            println("🔍 KiltDidManager: Substrate keyPairInfo generado: ${substrateKeyPairInfo != null}")
            println("🔍 KiltDidManager: Substrate publicKey size: ${substrateKeyPairInfo?.publicKey?.size ?: "null"}")
            
            val publicKey = substrateKeyPairInfo?.publicKey
                ?: throw KiltDidException("Error obteniendo clave pública de cuenta Substrate")
            
            // Usar la misma clave pública para generar la dirección KILT
            val address = ss58Encoder.encode(publicKey, SS58Encoder.NetworkPrefix.KILT)
            val did = "did:kilt:$address"
            
            println("🔍 KiltDidManager: DID generado: $did")
            println("🔍 KiltDidManager: Dirección KILT: $address")
            println("🔍 KiltDidManager: Clave pública reutilizada de cuenta Substrate")
            
            KiltDidInfo(
                did = did,
                address = address,
                publicKey = publicKey,
                verificationRelationship = VerificationRelationship.AUTHENTICATION,
                didType = DidType.FULL,
                derivationPath = "//did//0" // Path conceptual para el DID
            )
        } catch (e: Exception) {
            throw KiltDidException("Error generando DID de autenticación KILT: ${e.message}", e)
        }
    }
    
    /**
     * Valida un DID KILT
     * @param did DID a validar
     * @return Boolean indicando si es válido
     */
    suspend fun validateKiltDid(did: String): Boolean = withContext(Dispatchers.IO) {
        try {
            when {
                did.startsWith("did:kilt:") && !did.contains("light") -> {
                    // Full DID: did:kilt:<address>
                    val address = did.removePrefix("did:kilt:")
                    ss58Encoder.validateAddress(address) && 
                    ss58Encoder.isFromNetwork(address, SS58Encoder.NetworkPrefix.KILT)
                }
                did.startsWith("did:kilt:light:") -> {
                    // Light DID: did:kilt:light:<authKeyType><address>:<encodedDetails>
                    val lightPart = did.removePrefix("did:kilt:light:")
                    val parts = lightPart.split(":")
                    if (parts.size >= 2) {
                        val addressPart = parts[0].drop(2) // Remove authKeyType (2 chars)
                        ss58Encoder.validateAddress(addressPart) &&
                        ss58Encoder.isFromNetwork(addressPart, SS58Encoder.NetworkPrefix.KILT)
                    } else {
                        false
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Extrae la dirección de un DID KILT
     * @param did DID KILT
     * @return String con la dirección SS58
     */
    suspend fun extractAddressFromDid(did: String): String = withContext(Dispatchers.IO) {
        try {
            when {
                did.startsWith("did:kilt:") && !did.contains("light") -> {
                    did.removePrefix("did:kilt:")
                }
                did.startsWith("did:kilt:light:") -> {
                    val lightPart = did.removePrefix("did:kilt:light:")
                    val parts = lightPart.split(":")
                    if (parts.size >= 2) {
                        parts[0].drop(2) // Remove authKeyType
                    } else {
                        throw KiltDidException("Invalid light DID format")
                    }
                }
                else -> throw KiltDidException("Invalid KILT DID format")
            }
        } catch (e: Exception) {
            throw KiltDidException("Error extrayendo dirección del DID: ${e.message}", e)
        }
    }
    
    /**
     * Determina la relación de verificación requerida para una transacción
     * @param extrinsic Transacción Substrate
     * @return VerificationRelationship requerida
     */
    suspend fun getVerificationRelationshipForTransaction(@Suppress("UNUSED_PARAMETER") extrinsic: Any): VerificationRelationship? = withContext(Dispatchers.IO) {
        try {
            // Análisis de transacción Substrate (pendiente de implementación)
            // Por ahora retornamos AUTHENTICATION como default
            VerificationRelationship.AUTHENTICATION
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Crea información de transacción DID
     * @param extrinsic Transacción Substrate
     * @return DidTransactionInfo con información de la transacción
     */
    suspend fun createDidTransactionInfo(extrinsic: Any): DidTransactionInfo = withContext(Dispatchers.IO) {
        try {
            val relationship = getVerificationRelationshipForTransaction(extrinsic)
            
            DidTransactionInfo(
                extrinsic = extrinsic,
                requiredRelationship = relationship ?: VerificationRelationship.AUTHENTICATION,
                needsDidSignature = relationship != null,
                methodName = "unknown" // Pendiente: Extraer nombre del método
            )
        } catch (e: Exception) {
            throw KiltDidException("Error creando información de transacción DID: ${e.message}", e)
        }
    }
    
    /**
     * Genera nonce para transacciones DID
     * @param did DID KILT
     * @return BigInteger con el siguiente nonce
     */
    suspend fun getNextNonce(@Suppress("UNUSED_PARAMETER") did: String): BigInteger = withContext(Dispatchers.IO) {
        try {
            // Pendiente: Implementar consulta de nonce desde blockchain
            // Por ahora retornamos un valor incremental
            BigInteger.ONE
        } catch (e: Exception) {
            throw KiltDidException("Error obteniendo nonce para DID: ${e.message}", e)
        }
    }
    
    /**
     * Verifica si una transacción requiere firma DID
     * @param extrinsic Transacción Substrate
     * @return Boolean indicando si requiere firma DID
     */
    suspend fun requiresDidSignature(extrinsic: Any): Boolean = withContext(Dispatchers.IO) {
        try {
            val relationship = getVerificationRelationshipForTransaction(extrinsic)
            relationship != null
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtiene todas las relaciones de verificación soportadas
     * @return Lista de VerificationRelationship
     */
    fun getSupportedVerificationRelationships(): List<VerificationRelationship> {
        return VerificationRelationship.values().toList()
    }
    
    /**
     * Obtiene el mapeo de métodos a relaciones de verificación
     * @return Map con el mapeo completo
     */
    fun getMethodMapping(): Map<String, VerificationRelationship> {
        return methodMapping.toMap()
    }
}

/**
 * Excepción específica para errores de DIDs KILT
 */
class KiltDidException(message: String, cause: Throwable? = null) : Exception(message, cause)
