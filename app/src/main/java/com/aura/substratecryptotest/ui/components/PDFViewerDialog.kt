package com.aura.substratecryptotest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File

/**
 * Diálogo para visualizar PDF con capacidad de firma
 */
@Composable
fun PDFViewerDialog(
    pdfFile: File?,
    onDismiss: () -> Unit,
    onSignatureComplete: (android.graphics.Bitmap) -> Unit = {},
    onSavePDF: (File) -> Unit = {}
) {
    android.util.Log.d("PDFViewer", "=== PDFViewerDialog COMPOSABLE ===")
    android.util.Log.d("PDFViewer", "pdfFile: ${pdfFile?.name}")
    android.util.Log.d("PDFViewer", "pdfFile exists: ${pdfFile?.exists()}")
    
    if (pdfFile != null) {
        android.util.Log.d("PDFViewer", "Mostrando PDFViewerDialog para: ${pdfFile.name}")
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            android.util.Log.d("PDFViewer", "Dialog creado, llamando PDFViewerWithSignature")
            
            // Simplificar - llamar directamente PDFViewerWithSignature sin Card
            PDFViewerWithSignature(
                pdfFile = pdfFile,
                onSignatureComplete = onSignatureComplete,
                onSavePDF = onSavePDF,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
