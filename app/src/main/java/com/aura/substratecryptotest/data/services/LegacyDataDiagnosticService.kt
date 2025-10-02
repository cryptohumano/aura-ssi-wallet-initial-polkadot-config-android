package com.aura.substratecryptotest.data.services

import android.content.Context
import android.content.SharedPreferences
import com.aura.substratecryptotest.utils.Logger
import java.io.File

/**
 * Herramienta de diagnóstico para encontrar datos de usuario anterior
 */
class LegacyDataDiagnosticService(private val context: Context) {
    
    companion object {
        private const val TAG = "LegacyDataDiagnostic"
    }
    
    /**
     * Diagnostica todos los archivos de preferencias disponibles
     */
    fun diagnoseAllPreferences(): DiagnosticResult {
        try {
            Logger.debug(TAG, "Iniciando diagnóstico de preferencias", "")
            
            val allPrefsFiles = getAllPreferencesFiles()
            val diagnosticData = mutableListOf<PreferenceFileData>()
            
            for (prefsFile in allPrefsFiles) {
                val prefs = context.getSharedPreferences(prefsFile, Context.MODE_PRIVATE)
                val fileData = analyzePreferenceFile(prefsFile, prefs)
                diagnosticData.add(fileData)
            }
            
            val result = DiagnosticResult(
                totalFiles = allPrefsFiles.size,
                filesWithData = diagnosticData.count { it.hasUserData },
                filesWithWalletData = diagnosticData.count { it.hasWalletData },
                filesWithDIDData = diagnosticData.count { it.hasDIDData },
                filesWithMnemonicData = diagnosticData.count { it.hasMnemonicData },
                filesData = diagnosticData
            )
            
            Logger.success(TAG, "Diagnóstico completado", 
                "Archivos: ${result.totalFiles}, Con datos: ${result.filesWithData}")
            
            return result
            
        } catch (e: Exception) {
            Logger.error(TAG, "Error durante diagnóstico", e.message ?: "Error desconocido", e)
            return DiagnosticResult.error("Error en diagnóstico: ${e.message}")
        }
    }
    
    /**
     * Obtiene todos los archivos de preferencias disponibles
     */
    private fun getAllPreferencesFiles(): List<String> {
        try {
            Logger.debug(TAG, "Buscando archivos de preferencias", "")
            
            val prefsFiles = mutableListOf<String>()
            
            // 1. Buscar en el directorio real de SharedPreferences
            val prefsDir = File(context.filesDir.parent, "shared_prefs")
            if (prefsDir.exists() && prefsDir.isDirectory) {
                prefsDir.listFiles()?.forEach { file ->
                    if (file.name.endsWith(".xml")) {
                        val fileName = file.name.removeSuffix(".xml")
                        prefsFiles.add(fileName)
                        Logger.debug(TAG, "Archivo encontrado en shared_prefs", "Archivo: $fileName")
                    }
                }
            }
            
            // 2. Agregar archivos conocidos del sistema anterior
            val knownFiles = listOf(
                "substrate_crypto_prefs", // StorageManager
                "wallet_storage", // WalletRepository
                "secure_storage_default", // KeyStoreManager (usuario por defecto)
                "secure_user_data",
                "wallet_data", 
                "did_data",
                "user_preferences",
                "secure_preferences",
                "wallet_preferences",
                "user_data",
                "secure_data",
                "app_preferences",
                "default_preferences",
                "shared_prefs",
                "preferences",
                "legacy_user_prefs",
                "legacy_wallet_prefs", 
                "legacy_did_prefs",
                "kilt_preferences",
                "substrate_preferences",
                "aura_preferences"
            )
            
            knownFiles.forEach { fileName ->
                if (!prefsFiles.contains(fileName)) {
                    prefsFiles.add(fileName)
                }
            }
            
            // 3. Buscar archivos que podrían existir con patrones comunes
            val keywords = listOf("user", "wallet", "did", "secure", "key", "data", "mnemonic", "private", "public")
            for (keyword in keywords) {
                val patterns = listOf(
                    "${keyword}_preferences",
                    "${keyword}_data", 
                    "${keyword}_storage",
                    "${keyword}_prefs",
                    "secure_${keyword}",
                    "encrypted_${keyword}"
                )
                patterns.forEach { pattern ->
                    if (!prefsFiles.contains(pattern)) {
                        prefsFiles.add(pattern)
                    }
                }
            }
            
            Logger.debug(TAG, "Total archivos a analizar", "Encontrados: ${prefsFiles.size}")
            return prefsFiles.distinct()
            
        } catch (e: Exception) {
            Logger.error(TAG, "Error obteniendo archivos de preferencias", e.message ?: "Error desconocido", e)
            return emptyList()
        }
    }
    
    /**
     * Analiza un archivo de preferencias específico
     */
    private fun analyzePreferenceFile(fileName: String, prefs: SharedPreferences): PreferenceFileData {
        val allEntries = prefs.all
        
        if (allEntries.isEmpty()) {
            return PreferenceFileData(
                fileName = fileName,
                totalEntries = 0,
                hasUserData = false,
                hasWalletData = false,
                hasDIDData = false,
                hasMnemonicData = false,
                sampleKeys = emptyList(),
                sampleValues = emptyMap()
            )
        }
        
        val sampleKeys = allEntries.keys.take(10).toList()
        val sampleValues = allEntries.entries.take(5).associate { 
            it.key to it.value.toString().take(50) // Limitar longitud
        }
        
        return PreferenceFileData(
            fileName = fileName,
            totalEntries = allEntries.size,
            hasUserData = hasUserRelatedData(allEntries),
            hasWalletData = hasWalletRelatedData(allEntries),
            hasDIDData = hasDIDRelatedData(allEntries),
            hasMnemonicData = hasMnemonicRelatedData(allEntries),
            sampleKeys = sampleKeys,
            sampleValues = sampleValues
        )
    }
    
    /**
     * Verifica si hay datos relacionados con usuario
     */
    private fun hasUserRelatedData(entries: Map<String, *>): Boolean {
        val userKeywords = listOf("name", "user", "username", "id", "email")
        return entries.keys.any { key ->
            userKeywords.any { keyword -> 
                key.contains(keyword, ignoreCase = true) 
            }
        }
    }
    
    /**
     * Verifica si hay datos relacionados con wallet
     */
    private fun hasWalletRelatedData(entries: Map<String, *>): Boolean {
        val walletKeywords = listOf("wallet", "address", "public", "private", "key", "account")
        return entries.keys.any { key ->
            walletKeywords.any { keyword -> 
                key.contains(keyword, ignoreCase = true) 
            }
        }
    }
    
    /**
     * Verifica si hay datos relacionados con DID
     */
    private fun hasDIDRelatedData(entries: Map<String, *>): Boolean {
        val didKeywords = listOf("did", "identity", "credential", "verifiable")
        return entries.keys.any { key ->
            didKeywords.any { keyword -> 
                key.contains(keyword, ignoreCase = true) 
            }
        }
    }
    
    /**
     * Verifica si hay datos relacionados con mnemonic
     */
    private fun hasMnemonicRelatedData(entries: Map<String, *>): Boolean {
        val mnemonicKeywords = listOf("mnemonic", "seed", "phrase", "words", "bip39")
        return entries.keys.any { key ->
            mnemonicKeywords.any { keyword -> 
                key.contains(keyword, ignoreCase = true) 
            }
        }
    }
    
    /**
     * Datos de un archivo de preferencias
     */
    data class PreferenceFileData(
        val fileName: String,
        val totalEntries: Int,
        val hasUserData: Boolean,
        val hasWalletData: Boolean,
        val hasDIDData: Boolean,
        val hasMnemonicData: Boolean,
        val sampleKeys: List<String>,
        val sampleValues: Map<String, String>
    )
    
    /**
     * Resultado del diagnóstico
     */
    data class DiagnosticResult(
        val totalFiles: Int,
        val filesWithData: Int,
        val filesWithWalletData: Int,
        val filesWithDIDData: Int,
        val filesWithMnemonicData: Int,
        val filesData: List<PreferenceFileData>
    ) {
        companion object {
            fun error(message: String): DiagnosticResult {
                return DiagnosticResult(
                    totalFiles = 0,
                    filesWithData = 0,
                    filesWithWalletData = 0,
                    filesWithDIDData = 0,
                    filesWithMnemonicData = 0,
                    filesData = emptyList()
                )
            }
        }
    }
}
