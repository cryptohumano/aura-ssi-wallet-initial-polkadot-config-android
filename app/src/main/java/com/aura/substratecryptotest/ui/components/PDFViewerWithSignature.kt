package com.aura.substratecryptotest.ui.components

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import java.io.FileOutputStream
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.io.image.ImageDataFactory

/**
 * Visor de PDF real usando PdfRenderer nativo de Android
 */
@Composable
fun PDFViewerWithSignature(
    pdfFile: File,
    onSignatureComplete: (android.graphics.Bitmap) -> Unit = {},
    onSavePDF: (File) -> Unit = {},
    modifier: Modifier = Modifier
) {
    android.util.Log.d("PDFViewer", "=== PDFViewerWithSignature INICIADO ===")
    android.util.Log.d("PDFViewer", "PDF File: ${pdfFile.name}")
    android.util.Log.d("PDFViewer", "PDF exists: ${pdfFile.exists()}")
    android.util.Log.d("PDFViewer", "PDF path: ${pdfFile.absolutePath}")
    
    var isSigning by remember { mutableStateOf(false) }
    var signaturePath by remember { mutableStateOf<android.graphics.Path?>(null) }
    var signatureBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var savedSignatureBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var pageSignatures by remember { mutableStateOf<Map<Int, android.graphics.Bitmap>>(emptyMap()) }
    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var parcelFileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var totalPages by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    
    android.util.Log.d("PDFViewer", "Variables de estado inicializadas")
    
    // Cargar PDF usando PdfRenderer
    LaunchedEffect(pdfFile) {
        android.util.Log.d("PDFViewer", "=== LaunchedEffect INICIADO ===")
        try {
            isLoading = true
            error = null
            
            // Abrir el archivo PDF
            android.util.Log.d("PDFViewer", "Abriendo ParcelFileDescriptor...")
            parcelFileDescriptor = ParcelFileDescriptor.open(
                pdfFile, 
                ParcelFileDescriptor.MODE_READ_ONLY
            )
            android.util.Log.d("PDFViewer", "ParcelFileDescriptor creado")
            
            android.util.Log.d("PDFViewer", "Creando PdfRenderer...")
            pdfRenderer = PdfRenderer(parcelFileDescriptor!!)
            android.util.Log.d("PDFViewer", "PdfRenderer creado")
            
            totalPages = pdfRenderer!!.pageCount
            android.util.Log.d("PDFViewer", "Total páginas: $totalPages")
            
            isLoading = false
            android.util.Log.d("PDFViewer", "PDF cargado exitosamente")
            
        } catch (e: Exception) {
            android.util.Log.e("PDFViewer", "Error al cargar PDF: ${e.message}")
            android.util.Log.e("PDFViewer", "Stack trace: ${e.stackTraceToString()}")
            error = "Error al cargar PDF: ${e.message}"
            isLoading = false
        }
    }
    
    // Limpiar recursos cuando se desmonte el composable
    DisposableEffect(Unit) {
        onDispose {
            pdfRenderer?.close()
            parcelFileDescriptor?.close()
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Header compacto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Visor de PDF",
                style = MaterialTheme.typography.titleMedium
            )
            
                   if (totalPages > 0) {
                       Row(
                           verticalAlignment = Alignment.CenterVertically,
                           horizontalArrangement = Arrangement.spacedBy(8.dp)
                       ) {
                           Text(
                               text = "${currentPage + 1} / ${totalPages}",
                               style = MaterialTheme.typography.bodySmall,
                               color = MaterialTheme.colorScheme.onSurfaceVariant
                           )
                           
                           // Indicador de firma en página actual
                           if (pageSignatures.containsKey(currentPage)) {
                               Icon(
                                   imageVector = Icons.Default.Edit,
                                   contentDescription = "Página firmada",
                                   modifier = Modifier.size(16.dp),
                                   tint = MaterialTheme.colorScheme.primary
                               )
                           }
                       }
                   }
        }
        
        // Área de visualización del PDF
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when {
                isLoading -> {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Cargando PDF...")
                            Text(
                                text = pdfFile.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                error != null -> {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "❌",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error al cargar PDF",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Archivo: ${pdfFile.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                
                pdfRenderer != null && totalPages > 0 -> {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Mostrar página actual usando PdfRenderer
                            AndroidView(
                                factory = { context ->
                                    PDFPageView(context).apply {
                                        setPdfRenderer(pdfRenderer!!, currentPage)
                                    }
                                },
                                update = { view ->
                                    view.setPdfRenderer(pdfRenderer!!, currentPage)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                            
                            // Overlay para firma - solo sobre el área del PDF
                            if (isSigning || pageSignatures.containsKey(currentPage)) {
                                SignatureCanvas(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp), // Delimitar área de firma
                                    onPathChanged = { path ->
                                        signaturePath = path
                                    },
                                    savedSignature = pageSignatures[currentPage],
                                    currentPage = currentPage
                                )
                            }
                            
                            // Controles de navegación compactos
                            if (totalPages > 1) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { 
                                            if (currentPage > 0) currentPage-- 
                                        },
                                        enabled = currentPage > 0,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowLeft,
                                            contentDescription = "Página anterior",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    
                                    IconButton(
                                        onClick = { 
                                            if (currentPage < totalPages - 1) currentPage++ 
                                        },
                                        enabled = currentPage < totalPages - 1,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowRight,
                                            contentDescription = "Página siguiente",
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            
                            // FAB para firma compacto
                            FloatingActionButton(
                                onClick = { isSigning = !isSigning },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp),
                                containerColor = if (isSigning) MaterialTheme.colorScheme.error 
                                else MaterialTheme.colorScheme.primary
                            ) {
                                Icon(
                                    imageVector = if (isSigning) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = if (isSigning) "Cancelar firma" else "Firmar"
                                )
                            }
                            
                       // Botones para firma cuando está firmando
                       if (isSigning) {
                           // Botón para borrar firma
                           FloatingActionButton(
                                       onClick = {
                                           signaturePath = null
                                           signatureBitmap = null
                                           // Borrar firma solo de la página actual
                                           pageSignatures = pageSignatures - currentPage
                                           android.util.Log.d("PDFViewer", "Firma borrada de página $currentPage")
                                       },
                               modifier = Modifier
                                   .align(Alignment.BottomEnd)
                                   .padding(16.dp, 80.dp, 16.dp, 16.dp),
                               containerColor = MaterialTheme.colorScheme.error
                           ) {
                               Icon(
                                   imageVector = Icons.Default.Close,
                                   contentDescription = "Borrar firma"
                               )
                           }
                           
                           // Botón para guardar firma cuando hay firma
                           if (signaturePath != null) {
                               FloatingActionButton(
                                   onClick = {
                                       signaturePath?.let { path ->
                                           try {
                                               val bitmap = android.graphics.Bitmap.createBitmap(800, 400, android.graphics.Bitmap.Config.ARGB_8888)
                                               val canvas = Canvas(bitmap)
                                               canvas.drawColor(android.graphics.Color.WHITE)
                                               
                                               val paint = Paint().apply {
                                                   color = android.graphics.Color.BLACK
                                                   strokeWidth = 6f
                                                   style = Paint.Style.STROKE
                                                   strokeJoin = Paint.Join.ROUND
                                                   strokeCap = Paint.Cap.ROUND
                                                   isAntiAlias = true
                                               }
                                               
                                               canvas.drawPath(path, paint)
                                               signatureBitmap = bitmap
                                               // Guardar firma por página
                                               pageSignatures = pageSignatures + (currentPage to bitmap)
                                               
                                               android.util.Log.d("PDFViewer", "=== FIRMA AUTOGRÁFICA COMPLETADA ===")
                                               android.util.Log.d("PDFViewer", "Página: $currentPage")
                                               android.util.Log.d("PDFViewer", "Tamaño firma: ${bitmap.width}x${bitmap.height}")
                                               android.util.Log.d("PDFViewer", "Total firmas: ${pageSignatures.size + 1}")
                                               android.util.Log.d("PDFViewer", "Llamando onSignatureComplete...")
                                               
                                               onSignatureComplete(bitmap)
                                               isSigning = false
                                               signaturePath = null
                                               android.util.Log.d("PDFViewer", "✅ Firma guardada exitosamente en página $currentPage")
                                           } catch (e: Exception) {
                                               android.util.Log.e("PDFViewer", "Error al guardar firma: ${e.message}")
                                           }
                                       }
                                   },
                                   modifier = Modifier
                                       .align(Alignment.BottomEnd)
                                       .padding(16.dp, 140.dp, 16.dp, 16.dp),
                                   containerColor = MaterialTheme.colorScheme.secondary
                               ) {
                                   Icon(
                                       imageVector = Icons.Default.Save,
                                       contentDescription = "Guardar firma"
                                   )
                               }
                           }
                       }
                            
                            // Botón para guardar PDF
                            FloatingActionButton(
                               onClick = { 
                                   android.util.Log.d("PDFViewer", "=== BOTÓN GUARDAR PRESIONADO ===")
                                   android.util.Log.d("PDFViewer", "PDF original: ${pdfFile.name}")
                                   android.util.Log.d("PDFViewer", "Total firmas: ${pageSignatures.size}")
                                   android.util.Log.d("PDFViewer", "Páginas con firma: ${pageSignatures.keys}")
                                   
                                   try {
                                       // Si hay firmas, guardar PDF con todas las firmas
                                       if (pageSignatures.isNotEmpty()) {
                                           android.util.Log.d("PDFViewer", "Guardando PDF con ${pageSignatures.size} firmas...")
                                           val signedPdfFile = savePDFWithSignatures(pdfFile, pageSignatures)
                                           if (signedPdfFile != null) {
                                               android.util.Log.d("PDFViewer", "✅ PDF con firmas creado exitosamente")
                                               android.util.Log.d("PDFViewer", "Archivo: ${signedPdfFile.name}")
                                               android.util.Log.d("PDFViewer", "Path: ${signedPdfFile.absolutePath}")
                                               android.util.Log.d("PDFViewer", "Tamaño: ${signedPdfFile.length()} bytes")
                                               android.util.Log.d("PDFViewer", "Llamando onSavePDF...")
                                               onSavePDF(signedPdfFile)
                                           } else {
                                               android.util.Log.e("PDFViewer", "❌ Error al crear PDF con firmas")
                                               android.util.Log.d("PDFViewer", "Guardando PDF original sin firmas...")
                                               onSavePDF(pdfFile)
                                           }
                                       } else {
                                           android.util.Log.d("PDFViewer", "No hay firmas, guardando PDF original...")
                                           onSavePDF(pdfFile)
                                       }
                                   } catch (e: Exception) {
                                       android.util.Log.e("PDFViewer", "❌ ERROR FATAL al guardar PDF", e)
                                       android.util.Log.e("PDFViewer", "Mensaje: ${e.message}")
                                       android.util.Log.e("PDFViewer", "Stack trace:", e)
                                       android.util.Log.d("PDFViewer", "Guardando PDF original como fallback...")
                                       onSavePDF(pdfFile)
                                   }
                               },
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp),
                                containerColor = MaterialTheme.colorScheme.secondary
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Guardar PDF"
                                )
                            }
                        }
                    }
                }
                
                else -> {
                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "📄",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay contenido",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                text = "El PDF no contiene páginas",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * View personalizado para mostrar una página del PDF renderizada
 */
class PDFPageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private var pdfRenderer: PdfRenderer? = null
    private var currentPageIndex = 0
    private var bitmap: android.graphics.Bitmap? = null
    
    fun setPdfRenderer(pdfRenderer: PdfRenderer, pageIndex: Int) {
        this.pdfRenderer = pdfRenderer
        this.currentPageIndex = pageIndex
        renderCurrentPage()
    }
    
    private fun renderCurrentPage() {
        pdfRenderer?.let { renderer ->
            try {
                val page = renderer.openPage(currentPageIndex)
                
                // Crear bitmap con mayor resolución para mejor calidad
                val scale = 2.0f // Aumentar resolución
                val scaledWidth = (page.width * scale).toInt()
                val scaledHeight = (page.height * scale).toInt()
                
                val bitmap = android.graphics.Bitmap.createBitmap(
                    scaledWidth,
                    scaledHeight,
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                
                // Renderizar la página en el bitmap con mejor calidad
                page.render(
                    bitmap, 
                    null, 
                    null, 
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )
                
                this.bitmap = bitmap
                page.close()
                invalidate()
                
            } catch (e: Exception) {
                android.util.Log.e("PDFPageView", "Error rendering page: ${e.message}")
            }
        }
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        bitmap?.let { bmp ->
            // Dibujar fondo blanco
            canvas.drawColor(android.graphics.Color.WHITE)
            
            // Calcular escala para ajustar la imagen al view manteniendo aspecto
            val scaleX = width.toFloat() / bmp.width
            val scaleY = height.toFloat() / bmp.height
            val scale = minOf(scaleX, scaleY) * 0.95f // 95% para dejar margen
            
            val scaledWidth = (bmp.width * scale).toInt()
            val scaledHeight = (bmp.height * scale).toInt()
            
            // Centrar la imagen
            val left = (width - scaledWidth) / 2
            val top = (height - scaledHeight) / 2
            
            // Dibujar el bitmap escalado con mejor calidad
            val paint = Paint().apply {
                isFilterBitmap = true
                isAntiAlias = true
                isDither = true
            }
            
            canvas.drawBitmap(
                bmp,
                Rect(0, 0, bmp.width, bmp.height),
                Rect(left, top, left + scaledWidth, top + scaledHeight),
                paint
            )
            
            // Dibujar borde sutil
            val borderPaint = Paint().apply {
                color = android.graphics.Color.LTGRAY
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            
            canvas.drawRect(
                left.toFloat(), 
                top.toFloat(), 
                (left + scaledWidth).toFloat(), 
                (top + scaledHeight).toFloat(),
                borderPaint
            )
        }
    }
}

@Composable
private fun SignatureCanvas(
    modifier: Modifier = Modifier,
    onPathChanged: (android.graphics.Path) -> Unit,
    savedSignature: android.graphics.Bitmap? = null,
    currentPage: Int = 0
) {
    android.util.Log.d("SignatureCanvas", "=== SignatureCanvas INICIADO ===")
    android.util.Log.d("SignatureCanvas", "Página actual: $currentPage")
    android.util.Log.d("SignatureCanvas", "Firma guardada para esta página: ${savedSignature != null}")
    
    var path by remember(currentPage) { mutableStateOf(android.graphics.Path()) }
    var pathPoints by remember(currentPage) { mutableStateOf<List<androidx.compose.ui.geometry.Offset>>(emptyList()) }
    
    android.util.Log.d("SignatureCanvas", "Path inicializado")
    
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                android.util.Log.d("SignatureCanvas", "pointerInput configurado")
                detectDragGestures(
                    onDragStart = { offset ->
                        android.util.Log.d("SignatureCanvas", "onDragStart: $offset")
                        path.moveTo(offset.x, offset.y)
                        pathPoints = listOf(offset)
                    },
                    onDrag = { change, _ ->
                        path.lineTo(change.position.x, change.position.y)
                        pathPoints = pathPoints + change.position
                        onPathChanged(path)
                    },
                    onDragEnd = {
                        android.util.Log.d("SignatureCanvas", "onDragEnd - Firma completada")
                    }
                )
            }
    ) {
        // Dibujar usando Compose Path para mejor integración
        android.util.Log.d("SignatureCanvas", "Canvas dibujando - PathPoints: ${pathPoints.size}, SavedSignature: ${savedSignature != null}")
        
        // Dibujar firma guardada si existe
        savedSignature?.let { bitmap ->
            val imageBitmap = bitmap.asImageBitmap()
            drawImage(
                image = imageBitmap,
                topLeft = androidx.compose.ui.geometry.Offset.Zero,
                alpha = 0.8f
            )
        }
        
        // Dibujar firma actual si hay puntos
        if (pathPoints.isNotEmpty()) {
            val composePath = androidx.compose.ui.graphics.Path().apply {
                moveTo(pathPoints.first().x, pathPoints.first().y)
                pathPoints.drop(1).forEach { point ->
                    lineTo(point.x, point.y)
                }
            }
            
            drawPath(
                path = composePath,
                color = androidx.compose.ui.graphics.Color.Black,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8.dp.toPx())
            )
        }
    }
}

/**
 * Función para guardar PDF con todas las firmas por página
 */
private fun savePDFWithSignatures(originalPdfFile: File, pageSignatures: Map<Int, android.graphics.Bitmap>): File? {
    return try {
        android.util.Log.d("PDFViewer", "=== INICIANDO savePDFWithSignatures ===")
        android.util.Log.d("PDFViewer", "PDF original: ${originalPdfFile.name}")
        android.util.Log.d("PDFViewer", "PDF original path: ${originalPdfFile.absolutePath}")
        android.util.Log.d("PDFViewer", "PDF original existe: ${originalPdfFile.exists()}")
        android.util.Log.d("PDFViewer", "PDF original tamaño: ${originalPdfFile.length()} bytes")
        android.util.Log.d("PDFViewer", "Total firmas: ${pageSignatures.size}")
        android.util.Log.d("PDFViewer", "Páginas con firma: ${pageSignatures.keys}")
        
        // Crear nuevo PDF con firmas
        val timestamp = System.currentTimeMillis()
        val signedPdfFile = File(originalPdfFile.parent, "signed_${timestamp}_${originalPdfFile.name}")
        android.util.Log.d("PDFViewer", "PDF firmado: ${signedPdfFile.name}")
        android.util.Log.d("PDFViewer", "PDF firmado path: ${signedPdfFile.absolutePath}")
        
        val writer = PdfWriter(signedPdfFile)
        val pdfDoc = PdfDocument(writer)
        val document = Document(pdfDoc)
        
        // Leer el PDF original página por página
        val originalPdf = PdfDocument(PdfReader(originalPdfFile))
        
        for (pageNum in 0 until originalPdf.numberOfPages) {
            // Copiar página original
            val page = originalPdf.getPage(pageNum + 1)
            pdfDoc.addPage(page.copyTo(pdfDoc))
            
            // Agregar firma si existe para esta página
            pageSignatures[pageNum]?.let { signatureBitmap ->
                android.util.Log.d("PDFViewer", "Agregando firma a página $pageNum")
                
                // Crear archivo temporal para la firma
                val signatureFile = File.createTempFile("signature_$pageNum", ".png")
                val signatureStream = FileOutputStream(signatureFile)
                signatureBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, signatureStream)
                signatureStream.close()
                
                // Agregar la firma
                val signatureImageData = ImageDataFactory.create(signatureFile.absolutePath)
                val signatureImage = Image(signatureImageData)
                signatureImage.scaleToFit(200f, 100f)
                signatureImage.setFixedPosition(pageNum + 1, 50f, 50f) // Posición fija en la página
                
                document.add(signatureImage)
                
                // Limpiar archivo temporal
                signatureFile.delete()
            }
        }
        
        document.close()
        originalPdf.close()
        
        android.util.Log.d("PDFViewer", "=== PDF CON FIRMAS GUARDADO EXITOSAMENTE ===")
        android.util.Log.d("PDFViewer", "Archivo: ${signedPdfFile.name}")
        android.util.Log.d("PDFViewer", "Path: ${signedPdfFile.absolutePath}")
        android.util.Log.d("PDFViewer", "Tamaño: ${signedPdfFile.length()} bytes")
        android.util.Log.d("PDFViewer", "Existe: ${signedPdfFile.exists()}")
        android.util.Log.d("PDFViewer", "Total firmas agregadas: ${pageSignatures.size}")
        
        signedPdfFile
        
    } catch (e: Exception) {
        android.util.Log.e("PDFViewer", "❌ ERROR FATAL en savePDFWithSignatures", e)
        android.util.Log.e("PDFViewer", "Mensaje: ${e.message}")
        android.util.Log.e("PDFViewer", "Stack trace:", e)
        android.util.Log.e("PDFViewer", "PDF original: ${originalPdfFile.name}")
        android.util.Log.e("PDFViewer", "Total firmas: ${pageSignatures.size}")
        null
    }
}