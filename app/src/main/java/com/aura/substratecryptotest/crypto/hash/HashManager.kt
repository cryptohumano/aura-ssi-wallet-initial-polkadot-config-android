package com.aura.substratecryptotest.crypto.hash

import io.novasama.substrate_sdk_android.hash.Hasher
import io.novasama.substrate_sdk_android.hash.XXHash
import io.novasama.substrate_sdk_android.hash.Blake2b128
import io.novasama.substrate_sdk_android.hash.hashConcat
import org.bouncycastle.jcajce.provider.digest.Blake2b
import org.bouncycastle.jcajce.provider.digest.Keccak
import net.jpountz.xxhash.XXHashFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Gestor especializado para operaciones de hashing
 * Maneja diferentes algoritmos de hash utilizados en Substrate
 */
class HashManager {
    
    /**
     * Algoritmos de hash soportados
     */
    enum class HashAlgorithm {
        BLAKE2B_128,    // BLAKE2b con salida de 128 bits
        BLAKE2B_256,    // BLAKE2b con salida de 256 bits
        BLAKE2B_512,    // BLAKE2b con salida de 512 bits
        KECCAK_256,     // Keccak-256
        SHA_256,        // SHA-256
        SHA_512,        // SHA-512
        XXHASH_64,      // XXHash con salida de 64 bits
        XXHASH_128      // XXHash con salida de 128 bits
    }
    
    /**
     * Resultado de una operación de hash
     */
    data class HashResult(
        val algorithm: HashAlgorithm,
        val input: ByteArray,
        val hash: ByteArray,
        val hex: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as HashResult
            
            if (algorithm != other.algorithm) return false
            if (!input.contentEquals(other.input)) return false
            if (!hash.contentEquals(other.hash)) return false
            if (hex != other.hex) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = algorithm.hashCode()
            result = 31 * result + input.contentHashCode()
            result = 31 * result + hash.contentHashCode()
            result = 31 * result + hex.hashCode()
            return result
        }
    }
    
    /**
     * Calcula el hash BLAKE2b-128 de los datos de entrada
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun blake2b128(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val digest = Blake2b128()
                val hash = digest.digest(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.BLAKE2B_128,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando BLAKE2b-128: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calcula el hash BLAKE2b-256 de los datos de entrada
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun blake2b256(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val digest = Blake2b.Blake2b256()
                val hash = digest.digest(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.BLAKE2B_256,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando BLAKE2b-256: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calcula el hash BLAKE2b-512 de los datos de entrada
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun blake2b512(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val digest = Blake2b.Blake2b512()
                val hash = digest.digest(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.BLAKE2B_512,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando BLAKE2b-512: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calcula el hash Keccak-256 de los datos de entrada
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun keccak256(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val digest = Keccak.Digest256()
                val hash = digest.digest(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.KECCAK_256,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando Keccak-256: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calcula el hash SHA-256 de los datos de entrada
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun sha256(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                val hash = digest.digest(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.SHA_256,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando SHA-256: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calcula el hash SHA-512 de los datos de entrada
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun sha512(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val digest = MessageDigest.getInstance("SHA-512")
                val hash = digest.digest(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.SHA_512,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando SHA-512: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calcula el hash usando el algoritmo especificado
     * @param algorithm Algoritmo de hash a usar
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun xxHash64(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val xxHash = XXHashFactory.safeInstance().hash64()
                val hashLong = xxHash.hash(data, 0, data.size, 0)
                val hash = ByteArray(8) { i -> (hashLong shr (i * 8)).toByte() }
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.XXHASH_64,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando XXHash-64: ${e.message}", e)
            }
        }
    }
    
    suspend fun xxHash128(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val xxHash = XXHash(128, XXHashFactory.safeInstance().hash64())
                val hash = xxHash.hash(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.XXHASH_128,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando XXHash-128: ${e.message}", e)
            }
        }
    }
    
    /**
     * Calcula el hash BLAKE2b-128 con concatenación de datos
     * @param data Datos a hashear
     * @return HashResult con el resultado del hash concatenado
     */
    suspend fun blake2b128Concat(data: ByteArray): HashResult {
        return withContext(Dispatchers.IO) {
            try {
                val digest = Blake2b128()
                val hash = digest.hashConcat(data)
                val hex = hash.joinToString("") { "%02x".format(it) }
                
                HashResult(
                    algorithm = HashAlgorithm.BLAKE2B_128,
                    input = data,
                    hash = hash,
                    hex = hex
                )
            } catch (e: Exception) {
                throw HashException("Error calculando BLAKE2b-128 concat: ${e.message}", e)
            }
        }
    }

    suspend fun hash(algorithm: HashAlgorithm, data: ByteArray): HashResult {
        return when (algorithm) {
            HashAlgorithm.BLAKE2B_128 -> blake2b128(data)
            HashAlgorithm.BLAKE2B_256 -> blake2b256(data)
            HashAlgorithm.BLAKE2B_512 -> blake2b512(data)
            HashAlgorithm.KECCAK_256 -> keccak256(data)
            HashAlgorithm.SHA_256 -> sha256(data)
            HashAlgorithm.SHA_512 -> sha512(data)
            HashAlgorithm.XXHASH_64 -> xxHash64(data)
            HashAlgorithm.XXHASH_128 -> xxHash128(data)
        }
    }
    
    /**
     * Calcula el hash de un string usando UTF-8
     * @param algorithm Algoritmo de hash a usar
     * @param text Texto a hashear
     * @return HashResult con el resultado del hash
     */
    suspend fun hashString(algorithm: HashAlgorithm, text: String): HashResult {
        return hash(algorithm, text.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Calcula múltiples hashes del mismo dato
     * @param data Datos a hashear
     * @param algorithms Lista de algoritmos a usar
     * @return Lista de HashResult
     */
    suspend fun hashMultiple(data: ByteArray, algorithms: List<HashAlgorithm>): List<HashResult> {
        return withContext(Dispatchers.IO) {
            try {
                algorithms.map { algorithm ->
                    hash(algorithm, data)
                }
            } catch (e: Exception) {
                throw HashException("Error calculando múltiples hashes: ${e.message}", e)
            }
        }
    }
    
    /**
     * Verifica si un hash es válido para un dato dado
     * @param data Datos originales
     * @param expectedHash Hash esperado
     * @param algorithm Algoritmo usado para generar el hash
     * @return Boolean indicando si el hash es válido
     */
    suspend fun verifyHash(data: ByteArray, expectedHash: ByteArray, algorithm: HashAlgorithm): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = hash(algorithm, data)
                result.hash.contentEquals(expectedHash)
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Verifica si un hash en formato hex es válido para un dato dado
     * @param data Datos originales
     * @param expectedHex Hash esperado en formato hex
     * @param algorithm Algoritmo usado para generar el hash
     * @return Boolean indicando si el hash es válido
     */
    suspend fun verifyHashHex(data: ByteArray, expectedHex: String, algorithm: HashAlgorithm): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val result = hash(algorithm, data)
                result.hex.equals(expectedHex, ignoreCase = true)
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Obtiene información sobre un algoritmo de hash
     * @param algorithm Algoritmo de hash
     * @return String con información del algoritmo
     */
    fun getAlgorithmInfo(algorithm: HashAlgorithm): String {
        return when (algorithm) {
            HashAlgorithm.BLAKE2B_128 -> "BLAKE2b con salida de 128 bits (16 bytes)"
            HashAlgorithm.BLAKE2B_256 -> "BLAKE2b con salida de 256 bits (32 bytes)"
            HashAlgorithm.BLAKE2B_512 -> "BLAKE2b con salida de 512 bits (64 bytes)"
            HashAlgorithm.KECCAK_256 -> "Keccak-256 con salida de 256 bits (32 bytes)"
            HashAlgorithm.SHA_256 -> "SHA-256 con salida de 256 bits (32 bytes)"
            HashAlgorithm.SHA_512 -> "SHA-512 con salida de 512 bits (64 bytes)"
            HashAlgorithm.XXHASH_64 -> "XXHash con salida de 64 bits (8 bytes)"
            HashAlgorithm.XXHASH_128 -> "XXHash con salida de 128 bits (16 bytes)"
        }
    }
    
    /**
     * Obtiene el tamaño de salida en bytes para un algoritmo
     * @param algorithm Algoritmo de hash
     * @return Int con el tamaño en bytes
     */
    fun getOutputSize(algorithm: HashAlgorithm): Int {
        return when (algorithm) {
            HashAlgorithm.BLAKE2B_128 -> 16
            HashAlgorithm.BLAKE2B_256 -> 32
            HashAlgorithm.BLAKE2B_512 -> 64
            HashAlgorithm.KECCAK_256 -> 32
            HashAlgorithm.SHA_256 -> 32
            HashAlgorithm.SHA_512 -> 64
            HashAlgorithm.XXHASH_64 -> 8
            HashAlgorithm.XXHASH_128 -> 16
        }
    }
    
    /**
     * Calcula SHA-256 de un archivo (método de conveniencia)
     * @param fileBytes Bytes del archivo
     * @return String con el hash en hexadecimal
     */
    suspend fun calculateSHA256(fileBytes: ByteArray): String {
        val result = sha256(fileBytes)
        return result.hex
    }
}

/**
 * Excepción específica para errores de hashing
 */
class HashException(message: String, cause: Throwable? = null) : Exception(message, cause)
