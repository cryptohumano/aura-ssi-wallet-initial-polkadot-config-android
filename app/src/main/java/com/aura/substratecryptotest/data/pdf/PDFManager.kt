package com.aura.substratecryptotest.data.pdf

import android.content.Context
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Image
import com.itextpdf.io.image.ImageDataFactory
import com.aura.substratecryptotest.data.mountaineering.MountaineeringLogbook
import com.aura.substratecryptotest.data.mountaineering.ExpeditionMilestone
import com.aura.substratecryptotest.data.mountaineering.ExpeditionPhoto
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager simple para generación de PDF sin Dagger Hilt
 */
class PDFManager(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    
    /**
     * Genera una vista previa temporal de un PDF de bitácora (no se guarda permanentemente)
     * Esta función es para mostrar una vista previa antes de guardar definitivamente
     */
    fun generateLogbookPreview(
        logbook: MountaineeringLogbook,
        milestones: List<ExpeditionMilestone>,
        photos: List<ExpeditionPhoto>
    ): File? {
        return try {
            // Crear directorio temporal para vistas previas
            val tempDir = File(context.cacheDir, "pdf_previews")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            
            // Generar nombre temporal único para la vista previa
            val timestamp = System.currentTimeMillis()
            val fileName = "preview_bitacora_${logbook.name.replace(" ", "_")}_${logbook.id}_${timestamp}.pdf"
            val outputFile = File(tempDir, fileName)
            
            android.util.Log.d("PDFManager", "📄 Generando vista previa temporal para bitácora ${logbook.id}: ${outputFile.name}")
            
            val pdfWriter = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Generar el contenido del PDF (mismo contenido que el PDF permanente)
            generatePDFContent(document, logbook, milestones, photos)
            
            document.close()
            pdfDocument.close()
            pdfWriter.close()
            
            android.util.Log.d("PDFManager", "📄 Vista previa generada: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            android.util.Log.e("PDFManager", "Error generando vista previa", e)
            null
        }
    }
    
    /**
     * Genera un PDF de bitácora de montañismo
     * Si ya existe un PDF exportado para esta bitácora, lo retorna sin generar uno nuevo
     */
    fun generateLogbookPDF(
        logbook: MountaineeringLogbook,
        milestones: List<ExpeditionMilestone>,
        photos: List<ExpeditionPhoto>
    ): File? {
        return try {
            val outputDir = File(context.getExternalFilesDir(null), "logbooks")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // Generar nombre estable basado en ID de bitácora para mantener vinculación con firmas
            val fileName = "bitacora_${logbook.name.replace(" ", "_")}_${logbook.id}.pdf"
            val outputFile = File(outputDir, fileName)
            
            // Si ya existe un PDF exportado para esta bitácora, retornarlo sin generar uno nuevo
            if (outputFile.exists()) {
                android.util.Log.d("PDFManager", "📄 PDF ya existe para bitácora ${logbook.id}: ${outputFile.name}")
                android.util.Log.d("PDFManager", "📄 Retornando PDF existente: ${outputFile.absolutePath}")
                return outputFile
            }
            
            android.util.Log.d("PDFManager", "📄 Generando nuevo PDF para bitácora ${logbook.id}: ${outputFile.name}")
            
            val pdfWriter = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Título
            document.add(
                Paragraph("BITÁCORA DE MONTAÑISMO")
                    .setFontSize(20f)
                    .setBold()
            )
            
            document.add(Paragraph("\n"))
            
            // Información de la bitácora
            document.add(Paragraph("INFORMACIÓN GENERAL").setBold())
            document.add(Paragraph("Nombre: ${logbook.name}"))
            document.add(Paragraph("Club: ${logbook.club}"))
            document.add(Paragraph("Asociación: ${logbook.association}"))
            document.add(Paragraph("Participantes: ${logbook.participantsCount}"))
            document.add(Paragraph("Licencia: ${logbook.licenseNumber}"))
            document.add(Paragraph("Ubicación: ${logbook.location}"))
            document.add(Paragraph("Fecha de inicio: ${dateFormat.format(logbook.startDate)}"))
            document.add(Paragraph("Fecha de término: ${dateFormat.format(logbook.endDate)}"))
            document.add(Paragraph("Observaciones: ${logbook.observations}"))
            
            document.add(Paragraph("\n"))
            
            // Milestones
            if (milestones.isNotEmpty()) {
                document.add(Paragraph("MILESTONES DE LA EXPEDICIÓN").setBold())
                
                val table = Table(4)
                table.addHeaderCell("Título")
                table.addHeaderCell("Descripción")
                table.addHeaderCell("Fecha/Hora")
                table.addHeaderCell("Ubicación")
                
                milestones.forEach { milestone ->
                    table.addCell(milestone.title)
                    table.addCell(milestone.description)
                    table.addCell(dateFormat.format(milestone.timestamp))
                    table.addCell(
                        if (milestone.latitude != null && milestone.longitude != null) {
                            "${String.format("%.6f", milestone.latitude)}, ${String.format("%.6f", milestone.longitude)}"
                        } else {
                            "No disponible"
                        }
                    )
                }
                
                document.add(table)
                
                // Agregar mapa de ubicaciones si hay coordenadas
                val locationsWithCoords = milestones.filter { it.latitude != null && it.longitude != null }
                if (locationsWithCoords.isNotEmpty()) {
                    document.add(Paragraph("\nMAPA DE UBICACIONES").setBold())
                    document.add(Paragraph("Coordenadas GPS de los milestones:"))
                    
                    locationsWithCoords.forEach { milestone ->
                        document.add(Paragraph("• ${milestone.title}"))
                        document.add(Paragraph("  Latitud: ${String.format("%.6f", milestone.latitude!!)}"))
                        document.add(Paragraph("  Longitud: ${String.format("%.6f", milestone.longitude!!)}"))
                        if (milestone.altitude != null) {
                            document.add(Paragraph("  Altitud: ${String.format("%.0f", milestone.altitude)} m"))
                        }
                        document.add(Paragraph(""))
                    }
                    
                    // Crear un mapa simple con caracteres ASCII
                    document.add(Paragraph("MAPA SIMPLIFICADO:"))
                    document.add(Paragraph("┌─────────────────────────────────────┐"))
                    document.add(Paragraph("│  📍 Ubicaciones de la expedición    │"))
                    document.add(Paragraph("├─────────────────────────────────────┤"))
                    
                    locationsWithCoords.forEachIndexed { index, milestone ->
                        val marker = when (index) {
                            0 -> "🏁" // Inicio
                            locationsWithCoords.size - 1 -> "🏆" // Final
                            else -> "📍" // Puntos intermedios
                        }
                        document.add(Paragraph("│ $marker ${milestone.title.padEnd(25)} │"))
                        document.add(Paragraph("│   ${String.format("%.4f", milestone.latitude!!)}, ${String.format("%.4f", milestone.longitude!!)} │"))
                    }
                    
                    document.add(Paragraph("└─────────────────────────────────────┘"))
                }
            }
            
            // Fotos
            if (photos.isNotEmpty()) {
                document.add(Paragraph("\nFOTOGRAFÍAS").setBold())
                document.add(Paragraph("Total de fotos: ${photos.size}"))
                
                photos.forEach { photo ->
                    document.add(Paragraph("\n• ${photo.photoType.name}"))
                    document.add(Paragraph("  Fecha: ${dateFormat.format(photo.timestamp)}"))
                    if (photo.latitude != null && photo.longitude != null) {
                        document.add(Paragraph("  Ubicación: ${String.format("%.6f", photo.latitude)}, ${String.format("%.6f", photo.longitude)}"))
                    }
                    
                    // Intentar incluir la imagen real
                    try {
                        val photoFile = File(photo.photoPath)
                        if (photoFile.exists() && photoFile.length() > 0) {
                            val imageData = ImageDataFactory.create(photoFile.absolutePath)
                            val image = Image(imageData)
                            
                            // Redimensionar la imagen para que quepa en la página
                            val pageWidth = pdfDocument.defaultPageSize.width - 72 // Margen de 36pt cada lado
                            val imageWidth = image.imageWidth
                            val imageHeight = image.imageHeight
                            
                            if (imageWidth > pageWidth) {
                                val scale = pageWidth / imageWidth
                                image.scaleToFit(pageWidth, imageHeight * scale)
                            }
                            
                            document.add(image)
                        } else {
                            document.add(Paragraph("  [Imagen no disponible: ${photo.photoPath}]"))
                        }
                    } catch (e: Exception) {
                        document.add(Paragraph("  [Error al cargar imagen: ${e.message}]"))
                    }
                }
            }
            
            document.close()
            outputFile
            
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Genera un PDF de bitácora de montañismo forzando la regeneración
     * Útil para casos donde se necesita actualizar el contenido del PDF
     */
    fun regenerateLogbookPDF(
        logbook: MountaineeringLogbook,
        milestones: List<ExpeditionMilestone>,
        photos: List<ExpeditionPhoto>
    ): File? {
        return try {
            val outputDir = File(context.getExternalFilesDir(null), "logbooks")
            if (!outputDir.exists()) {
                outputDir.mkdirs()
            }
            
            // Generar nombre estable basado en ID de bitácora
            val fileName = "bitacora_${logbook.name.replace(" ", "_")}_${logbook.id}.pdf"
            val outputFile = File(outputDir, fileName)
            
            // Eliminar PDF existente si existe
            if (outputFile.exists()) {
                android.util.Log.d("PDFManager", "🔄 Eliminando PDF existente para regenerar: ${outputFile.name}")
                outputFile.delete()
            }
            
            android.util.Log.d("PDFManager", "🔄 Regenerando PDF para bitácora ${logbook.id}: ${outputFile.name}")
            
            val pdfWriter = PdfWriter(outputFile)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)
            
            // Título
            document.add(
                Paragraph("BITÁCORA DE MONTAÑISMO")
                    .setFontSize(20f)
                    .setBold()
            )
            
            document.add(Paragraph("\n"))
            
            // Información de la bitácora
            document.add(Paragraph("INFORMACIÓN GENERAL").setBold())
            document.add(Paragraph("Nombre: ${logbook.name}"))
            document.add(Paragraph("Club: ${logbook.club}"))
            document.add(Paragraph("Asociación: ${logbook.association}"))
            document.add(Paragraph("Participantes: ${logbook.participantsCount}"))
            document.add(Paragraph("Licencia: ${logbook.licenseNumber}"))
            document.add(Paragraph("Ubicación: ${logbook.location}"))
            document.add(Paragraph("Fecha de inicio: ${dateFormat.format(logbook.startDate)}"))
            document.add(Paragraph("Fecha de término: ${dateFormat.format(logbook.endDate)}"))
            document.add(Paragraph("Observaciones: ${logbook.observations}"))
            
            document.add(Paragraph("\n"))
            
            // Milestones
            if (milestones.isNotEmpty()) {
                document.add(Paragraph("MILESTONES DE LA EXPEDICIÓN").setBold())
                
                val table = Table(4)
                table.addHeaderCell("Título")
                table.addHeaderCell("Descripción")
                table.addHeaderCell("Fecha/Hora")
                table.addHeaderCell("Ubicación")
                
                milestones.forEach { milestone ->
                    table.addCell(milestone.title)
                    table.addCell(milestone.description)
                    table.addCell(dateFormat.format(milestone.timestamp))
                    table.addCell(
                        if (milestone.latitude != null && milestone.longitude != null) {
                            "${String.format("%.6f", milestone.latitude)}, ${String.format("%.6f", milestone.longitude)}"
                        } else {
                            "No disponible"
                        }
                    )
                }
                
                document.add(table)
                
                // Agregar mapa de ubicaciones si hay coordenadas
                val locationsWithCoords = milestones.filter { it.latitude != null && it.longitude != null }
                if (locationsWithCoords.isNotEmpty()) {
                    document.add(Paragraph("\nMAPA DE UBICACIONES").setBold())
                    document.add(Paragraph("Coordenadas GPS de los milestones:"))
                    
                    locationsWithCoords.forEach { milestone ->
                        document.add(Paragraph("• ${milestone.title}"))
                        document.add(Paragraph("  Latitud: ${String.format("%.6f", milestone.latitude!!)}"))
                        document.add(Paragraph("  Longitud: ${String.format("%.6f", milestone.longitude!!)}"))
                        if (milestone.altitude != null) {
                            document.add(Paragraph("  Altitud: ${String.format("%.0f", milestone.altitude)} m"))
                        }
                        document.add(Paragraph(""))
                    }
                    
                    // Crear un mapa simple con caracteres ASCII
                    document.add(Paragraph("MAPA SIMPLIFICADO:"))
                    document.add(Paragraph("┌─────────────────────────────────────┐"))
                    document.add(Paragraph("│  📍 Ubicaciones de la expedición    │"))
                    document.add(Paragraph("├─────────────────────────────────────┤"))
                    
                    locationsWithCoords.forEachIndexed { index, milestone ->
                        val marker = when (index) {
                            0 -> "🏁" // Inicio
                            locationsWithCoords.size - 1 -> "🏆" // Final
                            else -> "📍" // Puntos intermedios
                        }
                        document.add(Paragraph("│ $marker ${milestone.title.padEnd(25)} │"))
                        document.add(Paragraph("│   ${String.format("%.4f", milestone.latitude!!)}, ${String.format("%.4f", milestone.longitude!!)} │"))
                    }
                    
                    document.add(Paragraph("└─────────────────────────────────────┘"))
                }
            }
            
            // Fotos
            if (photos.isNotEmpty()) {
                document.add(Paragraph("\nFOTOGRAFÍAS").setBold())
                document.add(Paragraph("Total de fotos: ${photos.size}"))
                
                photos.forEach { photo ->
                    document.add(Paragraph("\n• ${photo.photoType.name}"))
                    document.add(Paragraph("  Fecha: ${dateFormat.format(photo.timestamp)}"))
                    if (photo.latitude != null && photo.longitude != null) {
                        document.add(Paragraph("  Ubicación: ${String.format("%.6f", photo.latitude)}, ${String.format("%.6f", photo.longitude)}"))
                    }
                    
                    // Intentar incluir la imagen real
                    try {
                        val photoFile = File(photo.photoPath)
                        if (photoFile.exists() && photoFile.length() > 0) {
                            val imageData = ImageDataFactory.create(photoFile.absolutePath)
                            val image = Image(imageData)
                            
                            // Redimensionar la imagen para que quepa en la página
                            val pageWidth = pdfDocument.defaultPageSize.width - 72 // Margen de 36pt cada lado
                            val imageWidth = image.imageWidth
                            val imageHeight = image.imageHeight
                            
                            if (imageWidth > pageWidth) {
                                val scale = pageWidth / imageWidth
                                image.scaleToFit(pageWidth, imageHeight * scale)
                            }
                            
                            document.add(image)
                        } else {
                            document.add(Paragraph("  [Imagen no disponible: ${photo.photoPath}]"))
                        }
                    } catch (e: Exception) {
                        document.add(Paragraph("  [Error al cargar imagen: ${e.message}]"))
                    }
                }
            }
            
            document.close()
            android.util.Log.d("PDFManager", "✅ PDF regenerado exitosamente: ${outputFile.name}")
            outputFile
            
        } catch (e: Exception) {
            android.util.Log.e("PDFManager", "❌ Error regenerando PDF: ${e.message}", e)
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Verifica si ya existe un PDF exportado para una bitácora
     */
    fun hasExportedPDF(logbookId: Long, logbookName: String): File? {
        val outputDir = File(context.getExternalFilesDir(null), "logbooks")
        val fileName = "bitacora_${logbookName.replace(" ", "_")}_${logbookId}.pdf"
        val outputFile = File(outputDir, fileName)
        
        return if (outputFile.exists()) {
            android.util.Log.d("PDFManager", "📄 PDF existente encontrado: ${outputFile.name}")
            outputFile
        } else {
            android.util.Log.d("PDFManager", "📄 No existe PDF para bitácora $logbookId")
            null
        }
    }
    
    /**
     * Obtiene el directorio de salida para PDFs
     */
    fun getOutputDirectory(): File {
        val outputDir = File(context.getExternalFilesDir(null), "logbooks")
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        return outputDir
    }
    
    /**
     * Genera el contenido del PDF (común para vista previa y PDF permanente)
     */
    private fun generatePDFContent(
        document: Document,
        logbook: MountaineeringLogbook,
        milestones: List<ExpeditionMilestone>,
        photos: List<ExpeditionPhoto>
    ) {
        // Título
        document.add(
            Paragraph("BITÁCORA DE MONTAÑISMO")
                .setFontSize(20f)
                .setBold()
        )
        
        document.add(Paragraph("\n"))
        
        // Información de la bitácora
        document.add(Paragraph("INFORMACIÓN GENERAL").setBold())
        document.add(Paragraph("Nombre: ${logbook.name}"))
        document.add(Paragraph("Club: ${logbook.club}"))
        document.add(Paragraph("Asociación: ${logbook.association}"))
        document.add(Paragraph("Participantes: ${logbook.participantsCount}"))
        document.add(Paragraph("Licencia: ${logbook.licenseNumber}"))
        document.add(Paragraph("Ubicación: ${logbook.location}"))
        document.add(Paragraph("Fecha de inicio: ${dateFormat.format(logbook.startDate)}"))
        document.add(Paragraph("Fecha de término: ${dateFormat.format(logbook.endDate)}"))
        document.add(Paragraph("Observaciones: ${logbook.observations}"))
        
        document.add(Paragraph("\n"))
        
        // Milestones
        if (milestones.isNotEmpty()) {
            document.add(Paragraph("MILESTONES DE LA EXPEDICIÓN").setBold())
            
            val table = Table(4)
            table.addHeaderCell("Título")
            table.addHeaderCell("Descripción")
            table.addHeaderCell("Fecha/Hora")
            table.addHeaderCell("Ubicación")
            
            milestones.forEach { milestone ->
                table.addCell(milestone.title)
                table.addCell(milestone.description)
                table.addCell(dateFormat.format(milestone.timestamp))
                table.addCell(
                    if (milestone.latitude != null && milestone.longitude != null) {
                        "${String.format("%.6f", milestone.latitude)}, ${String.format("%.6f", milestone.longitude)}"
                    } else {
                        "No disponible"
                    }
                )
            }
            
            document.add(table)
            
            // Agregar mapa de ubicaciones si hay coordenadas
            val locationsWithCoords = milestones.filter { it.latitude != null && it.longitude != null }
            if (locationsWithCoords.isNotEmpty()) {
                document.add(Paragraph("\nMAPA DE UBICACIONES").setBold())
                
                // Crear un mapa simple con texto
                val minLat = locationsWithCoords.minOf { it.latitude!! }
                val maxLat = locationsWithCoords.maxOf { it.latitude!! }
                val minLon = locationsWithCoords.minOf { it.longitude!! }
                val maxLon = locationsWithCoords.maxOf { it.longitude!! }
                
                document.add(Paragraph("Rango de coordenadas:"))
                document.add(Paragraph("Latitud: ${String.format("%.6f", minLat)} a ${String.format("%.6f", maxLat)}"))
                document.add(Paragraph("Longitud: ${String.format("%.6f", minLon)} a ${String.format("%.6f", maxLon)}"))
                
                document.add(Paragraph("\nUbicaciones específicas:"))
                locationsWithCoords.forEach { milestone ->
                    document.add(Paragraph("• ${milestone.title}: ${String.format("%.6f", milestone.latitude)}, ${String.format("%.6f", milestone.longitude)}"))
                }
            }
        }
        
        document.add(Paragraph("\n"))
        
        // Fotos
        if (photos.isNotEmpty()) {
            document.add(Paragraph("FOTOGRAFÍAS DE LA EXPEDICIÓN").setBold())
            
            photos.forEach { photo ->
                document.add(Paragraph("• ${photo.photoType.name}"))
                document.add(Paragraph("  Capturada: ${dateFormat.format(photo.timestamp)}"))
                
                if (photo.latitude != null && photo.longitude != null) {
                    document.add(Paragraph("  Ubicación: ${String.format("%.6f", photo.latitude)}, ${String.format("%.6f", photo.longitude)}"))
                }
                
                // Intentar agregar la imagen si existe
                try {
                    val imageFile = File(photo.photoPath)
                    if (imageFile.exists()) {
                        val imageData = ImageDataFactory.create(imageFile.absolutePath)
                        val image = Image(imageData)
                        
                        // Redimensionar imagen si es muy grande
                        if (image.imageWidth > 400) {
                            image.scaleToFit(400f, 300f)
                        }
                        
                        document.add(image)
                    } else {
                        document.add(Paragraph("  [Imagen no disponible: ${photo.photoPath}]"))
                    }
                } catch (e: Exception) {
                    document.add(Paragraph("  [Error al cargar imagen: ${e.message}]"))
                }
            }
        }
    }
}