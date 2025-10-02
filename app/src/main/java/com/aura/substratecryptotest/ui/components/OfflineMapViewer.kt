package com.aura.substratecryptotest.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.substratecryptotest.data.models.MilestoneDetails
import kotlin.math.*

/**
 * Visor de mapa offline que muestra la ruta de milestones
 */
@Composable
fun OfflineMapViewer(
    milestoneDetails: List<MilestoneDetails>,
    modifier: Modifier = Modifier
) {
    if (milestoneDetails.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Sin datos GPS",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No hay datos GPS disponibles",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val gpsPoints = milestoneDetails.mapNotNull { it.gpsData }
    if (gpsPoints.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "Sin datos GPS",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No hay datos GPS disponibles",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "🗺️ Ruta de la Expedición",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Mapa offline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(
                        Color(0xFF2E7D32), // Verde oscuro para simular terreno
                        MaterialTheme.shapes.medium
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        MaterialTheme.shapes.medium
                    )
            ) {
                OfflineMapCanvas(
                    milestoneDetails = milestoneDetails,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Información de la ruta
            RouteInfo(milestoneDetails = milestoneDetails)
        }
    }
}

@Composable
private fun OfflineMapCanvas(
    milestoneDetails: List<MilestoneDetails>,
    modifier: Modifier = Modifier
) {
    val gpsPoints = milestoneDetails.mapNotNull { it.gpsData }
    
    Canvas(modifier = modifier) {
        if (gpsPoints.size < 2) return@Canvas
        
        // Validar que tenemos coordenadas válidas
        if (gpsPoints.any { it.latitude.isNaN() || it.longitude.isNaN() }) {
            return@Canvas
        }
        
        // Dibujar fondo blanco
        drawRect(color = Color.White)
        
        // Calcular límites del mapa
        val minLat = gpsPoints.minOf { it.latitude }
        val maxLat = gpsPoints.maxOf { it.latitude }
        val minLon = gpsPoints.minOf { it.longitude }
        val maxLon = gpsPoints.maxOf { it.longitude }
        
        // Agregar margen inteligente
        val latRange = maxLat - minLat
        val lonRange = maxLon - minLon
        val margin = 0.1 // 10% de margen
        
        // Usar un margen más pequeño para coordenadas cercanas
        val minRange = if (latRange < 0.0001 || lonRange < 0.0001) 0.0001 else 0.001
        
        val mapMinLat = minLat - maxOf(latRange * margin, minRange)
        val mapMaxLat = maxLat + maxOf(latRange * margin, minRange)
        val mapMinLon = minLon - maxOf(lonRange * margin, minRange)
        val mapMaxLon = maxLon + maxOf(lonRange * margin, minRange)
        
        // Función para convertir coordenadas GPS a píxeles
        fun gpsToPixel(lat: Double, lon: Double): Offset {
            val x = ((lon - mapMinLon) / (mapMaxLon - mapMinLon)) * size.width
            val y = ((mapMaxLat - lat) / (mapMaxLat - mapMinLat)) * size.height
            return Offset(x.toFloat(), y.toFloat())
        }
        
        // Dibujar líneas de conexión
        val pathPoints = gpsPoints.map { gpsToPixel(it.latitude, it.longitude) }
        
        // Dibujar ruta con líneas más visibles
        for (i in 1 until pathPoints.size) {
            // Línea principal más gruesa
            drawLine(
                color = Color(0xFF2196F3), // Azul
                start = pathPoints[i - 1],
                end = pathPoints[i],
                strokeWidth = 6.dp.toPx()
            )
            
            // Borde blanco para mejor visibilidad
            drawLine(
                color = Color.White,
                start = pathPoints[i - 1],
                end = pathPoints[i],
                strokeWidth = 8.dp.toPx()
            )
            
            // Línea principal encima del borde
            drawLine(
                color = Color(0xFF2196F3), // Azul
                start = pathPoints[i - 1],
                end = pathPoints[i],
                strokeWidth = 6.dp.toPx()
            )
        }
        
        // Dibujar puntos de milestones
        gpsPoints.forEachIndexed { index, gpsData ->
            val point = gpsToPixel(gpsData.latitude, gpsData.longitude)
            val color = when (index) {
                0 -> Color(0xFF4CAF50) // Verde para inicio
                gpsPoints.size - 1 -> Color(0xFFF44336) // Rojo para fin
                else -> Color(0xFF2196F3) // Azul para puntos intermedios
            }
            
            // Círculo exterior con borde
            drawCircle(
                color = Color.White,
                radius = 12.dp.toPx(),
                center = point
            )
            
            // Círculo interior
            drawCircle(
                color = color,
                radius = 8.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
private fun RouteInfo(milestoneDetails: List<MilestoneDetails>) {
    val gpsCalculator = remember { com.aura.substratecryptotest.data.services.GpsCalculatorService() }
    val stats = remember(milestoneDetails) {
        gpsCalculator.generateExpeditionStats(milestoneDetails)
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "📊 Estadísticas de Ruta",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem("Distancia", stats.getFormattedDistance())
            StatItem("Tiempo", stats.getFormattedTime())
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem("Velocidad", stats.getFormattedSpeed())
            StatItem("Desnivel +", stats.getFormattedElevationGain())
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem("Desnivel -", stats.getFormattedElevationLoss())
            StatItem("Altitud Max", stats.getFormattedMaxAltitude())
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
