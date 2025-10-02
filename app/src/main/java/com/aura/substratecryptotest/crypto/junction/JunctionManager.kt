package com.aura.substratecryptotest.crypto.junction

import io.novasama.substrate_sdk_android.extensions.fromHex
import io.novasama.substrate_sdk_android.hash.XXHash
import io.novasama.substrate_sdk_android.hash.Hasher
import org.bouncycastle.jcajce.provider.digest.Blake2b
import io.novasama.substrate_sdk_android.scale.dataType.string
import io.novasama.substrate_sdk_android.scale.dataType.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Gestor especializado para derivación de claves usando Junctions
 * Los Junctions son el sistema de derivación de claves de Substrate
 * Similar a BIP32 pero específico para el ecosistema Substrate
 */
class JunctionManager {
    
    /**
     * Tipos de Junction soportados en Substrate
     */
    enum class JunctionType {
        HARD,           // Derivación hard (//Alice, //Bob)
        SOFT,           // Derivación soft (/Alice, /Bob)
        PASSWORD,       // Con contraseña (///password)
        PARENT,         // Derivación desde padre
        PLACEHOLDER     // Placeholder para futuras extensiones
    }
    
    /**
     * Representa un Junction individual compatible con el SDK real
     */
    data class Junction(
        val type: JunctionType,
        val chainCode: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as Junction
            
            if (type != other.type) return false
            if (!chainCode.contentEquals(other.chainCode)) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + chainCode.contentHashCode()
            return result
        }
        
        /**
         * Obtiene el valor original del junction como string
         */
        fun getValueAsString(): String {
            return when (type) {
                JunctionType.HARD -> "//${chainCode.joinToString("") { "%02x".format(it) }}"
                JunctionType.SOFT -> "/${chainCode.joinToString("") { "%02x".format(it) }}"
                JunctionType.PASSWORD -> "///${chainCode.joinToString("") { "%02x".format(it) }}"
                JunctionType.PARENT -> "/.."
                JunctionType.PLACEHOLDER -> ""
            }
        }
    }
    
    /**
     * Representa una ruta de derivación completa
     */
    data class DerivationPath(
        val junctions: List<Junction>
    ) {
        /**
         * Convierte la ruta a string en formato Substrate
         */
        fun toSubstrateString(): String {
            return junctions.joinToString("") { junction ->
                when (junction.type) {
                    JunctionType.HARD -> "//${junction.chainCode.joinToString("") { "%02x".format(it) }}"
                    JunctionType.SOFT -> "/${junction.chainCode.joinToString("") { "%02x".format(it) }}"
                    JunctionType.PASSWORD -> "///${junction.chainCode.joinToString("") { "%02x".format(it) }}"
                    JunctionType.PARENT -> "/.."
                    JunctionType.PLACEHOLDER -> ""
                }
            }
        }
        
        /**
         * Convierte la ruta a string en formato BIP32
         */
        fun toBip32String(): String {
            return junctions.joinToString("/") { junction ->
                when (junction.type) {
                    JunctionType.HARD -> {
                        // Convertir chaincode a índice numérico para BIP32
                        val index = ByteBuffer.wrap(junction.chainCode.take(4).toByteArray())
                            .order(ByteOrder.LITTLE_ENDIAN).int
                        "$index'"
                    }
                    JunctionType.SOFT -> {
                        val index = ByteBuffer.wrap(junction.chainCode.take(4).toByteArray())
                            .order(ByteOrder.LITTLE_ENDIAN).int
                        "$index"
                    }
                    JunctionType.PASSWORD -> "0" // Password no tiene equivalente en BIP32
                    JunctionType.PARENT -> ".."
                    JunctionType.PLACEHOLDER -> "0"
                }
            }
        }
    }
    
    /**
     * Rutas de derivación predefinidas comunes en Substrate
     */
    object CommonPaths {
        val ALICE = DerivationPath(listOf(Junction(JunctionType.HARD, "Alice".toByteArray())))
        val BOB = DerivationPath(listOf(Junction(JunctionType.HARD, "Bob".toByteArray())))
        val CHARLIE = DerivationPath(listOf(Junction(JunctionType.HARD, "Charlie".toByteArray())))
        val DAVE = DerivationPath(listOf(Junction(JunctionType.HARD, "Dave".toByteArray())))
        val EVE = DerivationPath(listOf(Junction(JunctionType.HARD, "Eve".toByteArray())))
        val FERDIE = DerivationPath(listOf(Junction(JunctionType.HARD, "Ferdie".toByteArray())))
        
        // Rutas para diferentes propósitos
        val STASH = DerivationPath(listOf(Junction(JunctionType.HARD, "stash".toByteArray())))
        val CONTROLLER = DerivationPath(listOf(Junction(JunctionType.HARD, "controller".toByteArray())))
        val SESSION = DerivationPath(listOf(Junction(JunctionType.HARD, "session".toByteArray())))
        val GRANDPA = DerivationPath(listOf(Junction(JunctionType.HARD, "grandpa".toByteArray())))
        val BABE = DerivationPath(listOf(Junction(JunctionType.HARD, "babe".toByteArray())))
        val IMONLINE = DerivationPath(listOf(Junction(JunctionType.HARD, "imonline".toByteArray())))
        val PARA_VALIDATOR = DerivationPath(listOf(Junction(JunctionType.HARD, "para_validator".toByteArray())))
        val PARA_ASSIGNMENT = DerivationPath(listOf(Junction(JunctionType.HARD, "para_assignment".toByteArray())))
        val AUTHORITY_DISCOVERY = DerivationPath(listOf(Junction(JunctionType.HARD, "authority_discovery".toByteArray())))
        
        // Rutas específicas para DIDs KILT
        val DID_AUTHENTICATION = DerivationPath(listOf(Junction(JunctionType.HARD, "did".toByteArray()), Junction(JunctionType.HARD, "0".toByteArray())))
        val DID_ASSERTION = DerivationPath(listOf(Junction(JunctionType.HARD, "did".toByteArray()), Junction(JunctionType.HARD, "1".toByteArray())))
        val DID_DELEGATION = DerivationPath(listOf(Junction(JunctionType.HARD, "did".toByteArray()), Junction(JunctionType.HARD, "2".toByteArray())))
    }
    
    /**
     * Serializa un string a ByteArray usando el algoritmo del SDK real
     */
    private fun serialize(rawJunction: String): ByteArray {
        rawJunction.toLongOrNull()?.let {
            val bytes = ByteArray(8)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(it)
            return bytes
        }
        
        return runCatching {
            rawJunction.fromHex()
        }.getOrElse {
            string.toByteArray(rawJunction)
        }
    }
    
    /**
     * Normaliza bytes a 32 bytes usando el algoritmo del SDK real
     */
    private fun normalize(bytes: ByteArray): ByteArray = when {
        bytes.size < CHAINCODE_LENGTH -> ByteArray(CHAINCODE_LENGTH).apply {
            bytes.copyInto(this)
        }
        bytes.size > CHAINCODE_LENGTH -> {
            // Usar BLAKE2b-256 para normalizar el chaincode
            val digest = Blake2b.Blake2b256()
            val hash = digest.digest(bytes)
            hash
        }
        else -> bytes
    }
    
    companion object {
        private const val CHAINCODE_LENGTH = 32
    }
    
    /**
     * Parsea una ruta de derivación desde string usando el algoritmo del SDK real
     */
    suspend fun parseDerivationPath(path: String): DerivationPath? {
        return withContext(Dispatchers.IO) {
            try {
                val junctions = mutableListOf<Junction>()
                var remaining = path.trim()
                
                while (remaining.isNotEmpty()) {
                    when {
                        remaining.startsWith("///") -> {
                            // Password junction (///password)
                            val endIndex = remaining.indexOf('/', 3).let { if (it == -1) remaining.length else it }
                            val password = remaining.substring(3, endIndex)
                            junctions.add(createJunctionFromString(password, JunctionType.PASSWORD))
                            remaining = remaining.substring(endIndex)
                        }
                        remaining.startsWith("//") -> {
                            // Hard junction (//Alice)
                            val endIndex = remaining.indexOf('/', 2).let { if (it == -1) remaining.length else it }
                            val value = remaining.substring(2, endIndex)
                            junctions.add(createJunctionFromString(value, JunctionType.HARD))
                            remaining = remaining.substring(endIndex)
                        }
                        remaining.startsWith("/..") -> {
                            // Parent junction (/..)
                            junctions.add(Junction(JunctionType.PARENT, ByteArray(0)))
                            remaining = remaining.substring(3)
                        }
                        remaining.startsWith("/") -> {
                            // Soft junction (/Alice)
                            val endIndex = remaining.indexOf('/', 1).let { if (it == -1) remaining.length else it }
                            val value = remaining.substring(1, endIndex)
                            junctions.add(createJunctionFromString(value, JunctionType.SOFT))
                            remaining = remaining.substring(endIndex)
                        }
                        else -> break
                    }
                }
                
                DerivationPath(junctions)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Valida una ruta de derivación
     */
    suspend fun validateDerivationPath(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val parsed = parseDerivationPath(path)
                parsed != null && parsed.junctions.isNotEmpty()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Crea una ruta de derivación personalizada
     */
    fun createDerivationPath(vararg junctions: Junction): DerivationPath {
        return DerivationPath(junctions.toList())
    }
    
    /**
     * Crea un junction hard usando el algoritmo del SDK real
     */
    fun createHardJunction(value: String): Junction {
        return createJunctionFromString(value, JunctionType.HARD)
    }
    
    /**
     * Crea un junction soft usando el algoritmo del SDK real
     */
    fun createSoftJunction(value: String): Junction {
        return createJunctionFromString(value, JunctionType.SOFT)
    }
    
    /**
     * Crea un junction con contraseña usando el algoritmo del SDK real
     */
    fun createPasswordJunction(password: String): Junction {
        return createJunctionFromString(password, JunctionType.PASSWORD)
    }
    
    /**
     * Crea un junction con índice numérico
     */
    fun createIndexJunction(type: JunctionType, index: Int): Junction {
        val chainCode = normalize(serialize(index.toString()))
        return Junction(type, chainCode)
    }
    
    /**
     * Crea un junction desde string usando el algoritmo del SDK real
     */
    fun createJunctionFromString(value: String, type: JunctionType): Junction {
        val chainCode = normalize(serialize(value))
        return Junction(type, chainCode)
    }
    
    /**
     * Obtiene todas las rutas predefinidas
     */
    fun getCommonPaths(): Map<String, DerivationPath> {
        return mapOf(
            "Alice" to CommonPaths.ALICE,
            "Bob" to CommonPaths.BOB,
            "Charlie" to CommonPaths.CHARLIE,
            "Dave" to CommonPaths.DAVE,
            "Eve" to CommonPaths.EVE,
            "Ferdie" to CommonPaths.FERDIE,
            "stash" to CommonPaths.STASH,
            "controller" to CommonPaths.CONTROLLER,
            "session" to CommonPaths.SESSION,
            "grandpa" to CommonPaths.GRANDPA,
            "babe" to CommonPaths.BABE,
            "imonline" to CommonPaths.IMONLINE,
            "para_validator" to CommonPaths.PARA_VALIDATOR,
            "para_assignment" to CommonPaths.PARA_ASSIGNMENT,
            "authority_discovery" to CommonPaths.AUTHORITY_DISCOVERY,
            
            // DIDs KILT
            "did_auth" to CommonPaths.DID_AUTHENTICATION,
            "did_assertion" to CommonPaths.DID_ASSERTION,
            "did_delegation" to CommonPaths.DID_DELEGATION
        )
    }
    
    /**
     * Combina dos rutas de derivación
     */
    fun combinePaths(path1: DerivationPath, path2: DerivationPath): DerivationPath {
        return DerivationPath(path1.junctions + path2.junctions)
    }
    
    /**
     * Obtiene la profundidad de una ruta
     */
    fun getPathDepth(path: DerivationPath): Int {
        return path.junctions.size
    }
    
    /**
     * Verifica si una ruta es válida para Substrate
     */
    suspend fun isValidSubstratePath(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val parsed = parseDerivationPath(path)
                if (parsed == null) return@withContext false
                
                // Verificar que no tenga más de 5 niveles de profundidad
                if (parsed.junctions.size > 5) return@withContext false
                
                // Verificar que no tenga caracteres inválidos
                val validChars = Regex("^[a-zA-Z0-9_/]+$")
                parsed.junctions.all { junction ->
                    junction.getValueAsString().let { validChars.matches(it) }
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Obtiene el path de derivación para DID KILT de autenticación (//did//0)
     */
    fun getKiltDidAuthenticationPath(): DerivationPath {
        return CommonPaths.DID_AUTHENTICATION
    }
    
    /**
     * Obtiene el path de derivación para DID KILT de aserción (//did//1)
     */
    fun getKiltDidAssertionPath(): DerivationPath {
        return CommonPaths.DID_ASSERTION
    }
    
    /**
     * Obtiene el path de derivación para DID KILT de delegación (//did//2)
     */
    fun getKiltDidDelegationPath(): DerivationPath {
        return CommonPaths.DID_DELEGATION
    }
    
    /**
     * Obtiene todos los paths de DIDs KILT disponibles
     */
    fun getKiltDidPaths(): Map<String, DerivationPath> {
        return mapOf(
            "authentication" to CommonPaths.DID_AUTHENTICATION,
            "assertion" to CommonPaths.DID_ASSERTION,
            "delegation" to CommonPaths.DID_DELEGATION
        )
    }
}
