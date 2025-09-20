package com.aura.substratecryptotest.crypto

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestor especializado para almacenamiento seguro de datos
 */
class StorageManager(private val context: Context) {
    
    // Almacenamiento seguro usando EncryptedSharedPreferences
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "substrate_crypto_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Almacena de forma segura un par de claves
     */
    fun storeKeyPairSecurely(keyPairId: String, publicKey: ByteArray) {
        val publicKeyString = publicKey.joinToString(",") { it.toString() }
        encryptedPrefs.edit()
            .putString("keypair_${keyPairId}_public", publicKeyString)
            .apply()
    }
    
    /**
     * Recupera un par de claves almacenado
     */
    fun getStoredKeyPair(keyPairId: String): ByteArray? {
        val publicKeyString = encryptedPrefs.getString("keypair_${keyPairId}_public", null)
        return publicKeyString?.split(",")?.map { it.toByte() }?.toByteArray()
    }
    
    /**
     * Elimina un par de claves almacenado
     */
    fun removeStoredKeyPair(keyPairId: String) {
        encryptedPrefs.edit()
            .remove("keypair_${keyPairId}_public")
            .apply()
    }
    
    /**
     * Almacena metadata de una cuenta
     */
    fun storeAccountMetadata(accountId: String, metadata: Map<String, Any>) {
        val editor = encryptedPrefs.edit()
        metadata.forEach { (key, value) ->
            when (value) {
                is String -> editor.putString("account_${accountId}_$key", value)
                is Long -> editor.putLong("account_${accountId}_$key", value)
                is Int -> editor.putInt("account_${accountId}_$key", value)
                is Boolean -> editor.putBoolean("account_${accountId}_$key", value)
            }
        }
        editor.apply()
    }
    
    /**
     * Recupera metadata de una cuenta
     */
    fun getAccountMetadata(accountId: String, key: String): String? {
        return encryptedPrefs.getString("account_${accountId}_$key", null)
    }
    
    /**
     * Elimina metadata de una cuenta
     */
    fun removeAccountMetadata(accountId: String) {
        val editor = encryptedPrefs.edit()
        val allKeys = encryptedPrefs.all.keys
        allKeys.filter { it.startsWith("account_${accountId}_") }
            .forEach { editor.remove(it) }
        editor.apply()
    }
    
    /**
     * Almacena un mnemonic de forma segura
     */
    fun storeMnemonicSecurely(mnemonicId: String, mnemonic: String) {
        encryptedPrefs.edit()
            .putString("mnemonic_$mnemonicId", mnemonic)
            .apply()
    }
    
    /**
     * Recupera un mnemonic almacenado
     */
    fun getStoredMnemonic(mnemonicId: String): String? {
        return encryptedPrefs.getString("mnemonic_$mnemonicId", null)
    }
    
    /**
     * Elimina un mnemonic almacenado
     */
    fun removeStoredMnemonic(mnemonicId: String) {
        encryptedPrefs.edit()
            .remove("mnemonic_$mnemonicId")
            .apply()
    }
    
    /**
     * Lista todos los IDs de cuentas almacenadas
     */
    fun getStoredAccountIds(): List<String> {
        val allKeys = encryptedPrefs.all.keys
        return allKeys.filter { it.startsWith("keypair_") && it.endsWith("_public") }
            .map { it.removePrefix("keypair_").removeSuffix("_public") }
    }
    
    /**
     * Limpia todo el almacenamiento
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }
}
