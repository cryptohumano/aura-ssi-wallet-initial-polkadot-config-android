package com.aura.substratecryptotest.crypto.mnemonic

import android.util.Log
import io.novasama.substrate_sdk_android.encrypt.mnemonic.Mnemonic
import io.novasama.substrate_sdk_android.encrypt.mnemonic.MnemonicCreator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestor especializado para operaciones con mnemónicos BIP39
 * Maneja la creación, validación e importación de mnemónicos
 */
class MnemonicManager {
    
    // MnemonicCreator es un objeto singleton, no se instancia
    
    /**
     * Genera un mnemonic BIP39 usando el SDK de Substrate
     * @param length Longitud del mnemonic (12, 15, 18, 21, 24 palabras)
     * @return String con las palabras del mnemonic
     */
    suspend fun generateMnemonic(length: Mnemonic.Length = Mnemonic.Length.TWELVE): String {
        return withContext(Dispatchers.IO) {
            try {
                val mnemonic = MnemonicCreator.randomMnemonic(length)
                mnemonic.words
            } catch (e: Exception) {
                throw MnemonicException("Error generando mnemonic: ${e.message}", e)
            }
        }
    }
    
    /**
     * Valida un mnemonic BIP39 usando el SDK de Substrate
     * @param mnemonic String con las palabras del mnemonic
     * @return Boolean indicando si es válido
     */
    suspend fun validateMnemonic(mnemonic: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                MnemonicCreator.fromWords(mnemonic)
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Importa un mnemonic existente y lo valida
     * @param mnemonic String con las palabras del mnemonic
     * @return MnemonicInfo con información del mnemonic importado
     */
    suspend fun importMnemonic(mnemonic: String): MnemonicInfo {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedMnemonic = normalizeMnemonic(mnemonic)
                
                if (!isValidMnemonicFormat(normalizedMnemonic)) {
                    throw MnemonicException("Formato de mnemonic inválido")
                }
                
                val mnemonicObj = MnemonicCreator.fromWords(normalizedMnemonic)
                val length = getMnemonicLength(normalizedMnemonic)
                val entropy = mnemonicObj.entropy
                
                MnemonicInfo(
                    words = normalizedMnemonic,
                    length = length,
                    entropy = entropy,
                    isValid = true
                )
            } catch (e: Exception) {
                throw MnemonicException("Error importando mnemonic: ${e.message}", e)
            }
        }
    }
    
    /**
     * Genera un seed desde un mnemonic y contraseña opcional
     * Usa el mismo método que el SDK de Substrate (PKCS5S2ParametersGenerator con SHA512)
     * @param mnemonic String con las palabras del mnemonic
     * @param password Contraseña opcional para el seed
     * @return ByteArray con el seed generado (64 bytes)
     */
    suspend fun generateSeed(mnemonic: String, password: String? = null): ByteArray {
        return withContext(Dispatchers.IO) {
            try {
                val mnemonicObj = MnemonicCreator.fromWords(normalizeMnemonic(mnemonic))
                val entropy = mnemonicObj.entropy
                
                // Usar el mismo método que el SDK de Substrate
                val generator = org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator(
                    org.bouncycastle.crypto.digests.SHA512Digest()
                )
                
                val salt = if (password != null && password.isNotBlank()) {
                    "mnemonic$password".toByteArray()
                } else {
                    "mnemonic".toByteArray()
                }
                
                generator.init(entropy, salt, 2048)
                val key = generator.generateDerivedMacParameters(64 * 8) as org.bouncycastle.crypto.params.KeyParameter
                
                // Retornar solo los primeros 32 bytes para compatibilidad con SR25519
                key.key.copyOfRange(0, 32)
            } catch (e: Exception) {
                throw MnemonicException("Error generando seed: ${e.message}", e)
            }
        }
    }
    
    /**
     * Obtiene la longitud de un mnemonic
     * @param mnemonic String con las palabras del mnemonic
     * @return Int con el número de palabras
     */
    fun getMnemonicLength(mnemonic: String): Int {
        return normalizeMnemonic(mnemonic).split(" ").size
    }
    
    /**
     * Verifica si un mnemonic es válido según BIP39
     * @param mnemonic String con las palabras del mnemonic
     * @return Boolean indicando si es válido
     */
    suspend fun isBip39Valid(mnemonic: String): Boolean {
        return validateMnemonic(mnemonic)
    }
    
    /**
     * Normaliza un mnemonic (elimina espacios extra, convierte a minúsculas)
     * @param mnemonic String con las palabras del mnemonic
     * @return String normalizado
     */
    private fun normalizeMnemonic(mnemonic: String): String {
        return mnemonic.trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }
    
    /**
     * Verifica si el formato del mnemonic es válido
     * @param mnemonic String con las palabras del mnemonic
     * @return Boolean indicando si el formato es válido
     */
    private fun isValidMnemonicFormat(mnemonic: String): Boolean {
        val words = mnemonic.split(" ")
        val validLengths = listOf(12, 15, 18, 21, 24)
        
        return validLengths.contains(words.size) && 
               words.all { it.isNotBlank() && it.matches(Regex("[a-z]+")) }
    }
    
    /**
     * Obtiene información detallada de un mnemonic
     * @param mnemonic String con las palabras del mnemonic
     * @return MnemonicInfo con información detallada
     */
    suspend fun getMnemonicInfo(mnemonic: String): MnemonicInfo {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedMnemonic = normalizeMnemonic(mnemonic)
                val isValid = validateMnemonic(normalizedMnemonic)
                val length = getMnemonicLength(normalizedMnemonic)
                
                val entropy = if (isValid) {
                    try {
                        val mnemonicObj = MnemonicCreator.fromWords(normalizedMnemonic)
                        mnemonicObj.entropy
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                
                MnemonicInfo(
                    words = normalizedMnemonic,
                    length = length,
                    entropy = entropy,
                    isValid = isValid
                )
            } catch (e: Exception) {
                MnemonicInfo(
                    words = mnemonic,
                    length = 0,
                    entropy = null,
                    isValid = false
                )
            }
        }
    }
    
    /**
     * Genera múltiples mnemónicos
     * @param count Número de mnemónicos a generar
     * @param length Longitud de cada mnemonic
     * @return Lista de mnemónicos generados
     */
    suspend fun generateMultipleMnemonics(count: Int, length: Mnemonic.Length = Mnemonic.Length.TWELVE): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                (1..count).map { generateMnemonic(length) }
            } catch (e: Exception) {
                throw MnemonicException("Error generando múltiples mnemónicos: ${e.message}", e)
            }
        }
    }
    
    /**
     * Convierte un mnemonic a formato de backup (con checksum)
     * @param mnemonic String con las palabras del mnemonic
     * @return String con el mnemonic formateado para backup
     */
    suspend fun formatForBackup(mnemonic: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedMnemonic = normalizeMnemonic(mnemonic)
                val words = normalizedMnemonic.split(" ")
                
                // Formato: "1. palabra1\n2. palabra2\n..."
                words.mapIndexed { index, word ->
                    "${index + 1}. $word"
                }.joinToString("\n")
            } catch (e: Exception) {
                throw MnemonicException("Error formateando mnemonic para backup: ${e.message}", e)
            }
        }
    }
    
    /**
     * Restaura un mnemonic desde formato de backup
     * @param backupText String con el mnemonic en formato de backup
     * @return String con el mnemonic normalizado
     */
    suspend fun restoreFromBackup(backupText: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val lines = backupText.trim().split("\n")
                val words = lines.map { line ->
                    // Extraer la palabra después del número y punto
                    line.substringAfter(". ").trim()
                }.filter { it.isNotBlank() }
                
                val mnemonic = words.joinToString(" ")
                
                // Validar que el mnemonic restaurado sea válido
                if (!validateMnemonic(mnemonic)) {
                    throw MnemonicException("Mnemonic restaurado no es válido")
                }
                
                mnemonic
            } catch (e: Exception) {
                throw MnemonicException("Error restaurando mnemonic desde backup: ${e.message}", e)
            }
        }
    }
    
    /**
     * Verifica la fortaleza de un mnemonic
     * @param mnemonic String con las palabras del mnemonic
     * @return MnemonicStrength con información sobre la fortaleza
     */
    suspend fun checkMnemonicStrength(mnemonic: String): MnemonicStrength {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedMnemonic = normalizeMnemonic(mnemonic)
                val words = normalizedMnemonic.split(" ")
                val length = words.size
                
                val isValid = validateMnemonic(normalizedMnemonic)
                val entropyBits = when (length) {
                    12 -> 128
                    15 -> 160
                    18 -> 192
                    21 -> 224
                    24 -> 256
                    else -> 0
                }
                
                val strength = when {
                    !isValid -> MnemonicStrengthLevel.INVALID
                    length < 12 -> MnemonicStrengthLevel.WEAK
                    length == 12 -> MnemonicStrengthLevel.MEDIUM
                    length >= 18 -> MnemonicStrengthLevel.STRONG
                    else -> MnemonicStrengthLevel.MEDIUM
                }
                
                MnemonicStrength(
                    level = strength,
                    entropyBits = entropyBits,
                    wordCount = length,
                    isValid = isValid
                )
            } catch (e: Exception) {
                MnemonicStrength(
                    level = MnemonicStrengthLevel.INVALID,
                    entropyBits = 0,
                    wordCount = 0,
                    isValid = false
                )
            }
        }
    }
}

/**
 * Información detallada de un mnemonic
 */
data class MnemonicInfo(
    val words: String,
    val length: Int,
    val entropy: ByteArray?,
    val isValid: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as MnemonicInfo
        
        if (words != other.words) return false
        if (length != other.length) return false
        if (isValid != other.isValid) return false
        if (entropy != null) {
            if (other.entropy == null) return false
            if (!entropy.contentEquals(other.entropy)) return false
        } else if (other.entropy != null) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = words.hashCode()
        result = 31 * result + length
        result = 31 * result + (entropy?.contentHashCode() ?: 0)
        result = 31 * result + isValid.hashCode()
        return result
    }
}

/**
 * Información sobre la fortaleza de un mnemonic
 */
data class MnemonicStrength(
    val level: MnemonicStrengthLevel,
    val entropyBits: Int,
    val wordCount: Int,
    val isValid: Boolean
)

/**
 * Niveles de fortaleza de un mnemonic
 */
enum class MnemonicStrengthLevel {
    INVALID,
    WEAK,
    MEDIUM,
    STRONG
}

/**
 * Excepción específica para errores de mnemonic
 */
class MnemonicException(message: String, cause: Throwable? = null) : Exception(message, cause)
