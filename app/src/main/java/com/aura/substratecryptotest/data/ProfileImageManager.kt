package com.aura.substratecryptotest.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Manager para la persistencia de imágenes de perfil
 * Almacena las imágenes en el directorio interno de la app
 */
class ProfileImageManager(private val context: Context) {
    
    private val internalDir = File(context.filesDir, "profile_images")
    
    init {
        // Crear directorio si no existe
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
    }
    
    /**
     * Guarda una imagen de perfil para un DID específico
     */
    suspend fun saveProfileImage(didAddress: String, imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                
                if (bitmap != null) {
                    val fileName = "profile_${didAddress.hashCode()}.jpg"
                    val file = File(internalDir, fileName)
                    
                    val outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()
                    
                    Result.success(file.absolutePath)
                } else {
                    Result.failure(Exception("No se pudo decodificar la imagen"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Carga la imagen de perfil para un DID específico
     */
    suspend fun loadProfileImage(didAddress: String): Result<Bitmap?> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "profile_${didAddress.hashCode()}.jpg"
                val file = File(internalDir, fileName)
                
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    Result.success(bitmap)
                } else {
                    Result.success(null)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Elimina la imagen de perfil para un DID específico
     */
    suspend fun deleteProfileImage(didAddress: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val fileName = "profile_${didAddress.hashCode()}.jpg"
                val file = File(internalDir, fileName)
                
                if (file.exists()) {
                    Result.success(file.delete())
                } else {
                    Result.success(true)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Verifica si existe una imagen de perfil para un DID
     */
    fun hasProfileImage(didAddress: String): Boolean {
        val fileName = "profile_${didAddress.hashCode()}.jpg"
        val file = File(internalDir, fileName)
        return file.exists()
    }
    
    /**
     * Obtiene la ruta del archivo de imagen
     */
    fun getProfileImagePath(didAddress: String): String? {
        val fileName = "profile_${didAddress.hashCode()}.jpg"
        val file = File(internalDir, fileName)
        return if (file.exists()) file.absolutePath else null
    }
}
