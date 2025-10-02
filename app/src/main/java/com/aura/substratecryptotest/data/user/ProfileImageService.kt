package com.aura.substratecryptotest.data.user

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Servicio para manejar imágenes de perfil de usuarios
 */
class ProfileImageService(private val context: Context) {
    
    companion object {
        private const val TAG = "ProfileImageService"
        private const val PROFILE_IMAGES_DIR = "profile_images"
        private const val DEFAULT_IMAGE_SIZE = 200
    }
    
    /**
     * Genera una imagen de perfil por defecto basada en las iniciales del usuario
     */
    fun generateDefaultProfileImage(userName: String): String? {
        return try {
            val initials = getUserInitials(userName)
            val bitmap = createInitialsBitmap(initials)
            val fileName = "profile_${System.currentTimeMillis()}.png"
            val file = saveBitmapToFile(bitmap, fileName)
            file?.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error generando imagen de perfil por defecto: ${e.message}", e)
            null
        }
    }
    
    /**
     * Obtiene las iniciales del nombre del usuario
     */
    private fun getUserInitials(userName: String): String {
        val words = userName.trim().split("\\s+".toRegex())
        return when {
            words.isEmpty() -> "U"
            words.size == 1 -> words[0].take(1).uppercase()
            else -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
        }
    }
    
    /**
     * Crea un bitmap con las iniciales del usuario
     */
    private fun createInitialsBitmap(initials: String): Bitmap {
        val bitmap = Bitmap.createBitmap(DEFAULT_IMAGE_SIZE, DEFAULT_IMAGE_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Fondo con color basado en las iniciales
        val backgroundColor = getColorFromInitials(initials)
        canvas.drawColor(backgroundColor)
        
        // Texto de las iniciales
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = DEFAULT_IMAGE_SIZE * 0.4f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            isFakeBoldText = true
        }
        
        val textBounds = Rect()
        paint.getTextBounds(initials, 0, initials.length, textBounds)
        
        val x = DEFAULT_IMAGE_SIZE / 2f
        val y = DEFAULT_IMAGE_SIZE / 2f + textBounds.height() / 2f
        
        canvas.drawText(initials, x, y, paint)
        
        return bitmap
    }
    
    /**
     * Genera un color basado en las iniciales para consistencia
     */
    private fun getColorFromInitials(initials: String): Int {
        val colors = listOf(
            android.graphics.Color.parseColor("#FF6B6B"), // Rojo
            android.graphics.Color.parseColor("#4ECDC4"), // Turquesa
            android.graphics.Color.parseColor("#45B7D1"), // Azul
            android.graphics.Color.parseColor("#96CEB4"), // Verde
            android.graphics.Color.parseColor("#FFEAA7"), // Amarillo
            android.graphics.Color.parseColor("#DDA0DD"), // Ciruela
            android.graphics.Color.parseColor("#98D8C8"), // Verde agua
            android.graphics.Color.parseColor("#F7DC6F")  // Dorado
        )
        
        val hash = initials.hashCode()
        val index = kotlin.math.abs(hash) % colors.size
        return colors[index]
    }
    
    /**
     * Guarda un bitmap en un archivo
     */
    private fun saveBitmapToFile(bitmap: Bitmap, fileName: String): File? {
        return try {
            val imagesDir = File(context.getExternalFilesDir(null), PROFILE_IMAGES_DIR)
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val file = File(imagesDir, fileName)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "Imagen de perfil guardada: ${file.absolutePath}")
            file
        } catch (e: IOException) {
            Log.e(TAG, "Error guardando imagen de perfil: ${e.message}", e)
            null
        }
    }
    
    /**
     * Carga una imagen de perfil desde un archivo
     */
    fun loadProfileImage(imagePath: String?): Bitmap? {
        if (imagePath.isNullOrEmpty()) return null
        
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                Log.w(TAG, "Archivo de imagen no existe: $imagePath")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando imagen de perfil: ${e.message}", e)
            null
        }
    }
    
    /**
     * Elimina una imagen de perfil
     */
    fun deleteProfileImage(imagePath: String?): Boolean {
        if (imagePath.isNullOrEmpty()) return true
        
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Imagen de perfil eliminada: $imagePath, éxito: $deleted")
                deleted
            } else {
                Log.w(TAG, "Archivo de imagen no existe para eliminar: $imagePath")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando imagen de perfil: ${e.message}", e)
            false
        }
    }
    
    /**
     * Actualiza la imagen de perfil de un usuario
     */
    suspend fun updateUserProfileImage(userId: Long, userName: String): String? {
        return try {
            val imagePath = generateDefaultProfileImage(userName)
            if (imagePath != null) {
                Log.d(TAG, "Imagen de perfil generada para usuario $userId: $imagePath")
            }
            imagePath
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando imagen de perfil: ${e.message}", e)
            null
        }
    }
}
