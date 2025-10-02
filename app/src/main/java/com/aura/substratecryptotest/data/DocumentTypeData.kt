package com.aura.substratecryptotest.data

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Clase de datos para representar tipos de documentos
 */
data class DocumentTypeData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val fields: List<String>
)



