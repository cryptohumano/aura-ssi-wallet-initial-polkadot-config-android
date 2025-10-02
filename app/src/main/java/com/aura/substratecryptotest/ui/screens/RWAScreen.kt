package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.ui.context.LanguageAware

data class AssetTypeData(
    val title: String,
    val description: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val example: String,
    val documents: List<String>
)

/**
 * Pantalla RWA (Real World Assets)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RWAScreen(
    onNavigateBack: () -> Unit
) {
    LanguageAware {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.rwa_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Implementar creación de activo */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Activo")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Información sobre RWA
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "🏢 Activos del Mundo Real",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "Crea activos digitales vinculados a propiedades físicas. Sube documentos de propiedad, manténlos firmados on-chain y asócialos a tu identidad digital.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Tipos de activos disponibles
            item {
                Text(
                    text = "Tipos de Activos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            val assetTypes = listOf(
                AssetTypeData(
                    title = "Aeronaves",
                    description = "Documentos de propiedad y mantenimiento de aeronaves",
                    icon = Icons.Default.Flight,
                    example = "Aeroplano Cessna 172",
                    documents = listOf(
                        "Certificado de Registro Aeronáutico",
                        "Manual de Mantenimiento",
                        "Certificados de Inspección",
                        "Póliza de Seguro"
                    )
                ),
                AssetTypeData(
                    title = "Vehículos",
                    description = "Documentos de propiedad y mantenimiento de vehículos",
                    icon = Icons.Default.DirectionsCar,
                    example = "Automóvil Toyota Camry 2020",
                    documents = listOf(
                        "Título de Propiedad",
                        "Registro de Mantenimiento",
                        "Póliza de Seguro",
                        "Certificado de Inspección Técnica"
                    )
                ),
                AssetTypeData(
                    title = "Propiedades",
                    description = "Documentos de propiedad inmobiliaria",
                    icon = Icons.Default.Home,
                    example = "Casa en Santiago Centro",
                    documents = listOf(
                        "Escritura de Propiedad",
                        "Certificado de Dominio Vigente",
                        "Pago de Contribuciones",
                        "Certificado de No Deuda"
                    )
                ),
                AssetTypeData(
                    title = "Equipos Industriales",
                    description = "Maquinaria y equipos industriales",
                    icon = Icons.Default.Build,
                    example = "Máquina CNC Industrial",
                    documents = listOf(
                        "Factura de Compra",
                        "Manual de Operación",
                        "Certificados de Calibración",
                        "Póliza de Seguro"
                    )
                )
            )
            
            items(assetTypes) { assetType ->
                AssetTypeCard(
                    assetType = assetType,
                    onCreate = { /* TODO: Implementar creación */ }
                )
            }
            
            // Activos existentes
            item {
                Text(
                    text = "Mis Activos",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            // Placeholder para activos existentes
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Business,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "No hay activos registrados",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Text(
                            text = "Usa el botón + para registrar tu primer activo",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
    }
}

@Composable
fun AssetTypeCard(
    assetType: AssetTypeData,
    onCreate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = assetType.icon,
                    contentDescription = assetType.title,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = assetType.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = assetType.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Ejemplo: ${assetType.example}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Text(
                text = "Documentos requeridos:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            assetType.documents.forEach { document ->
                Text(
                    text = "• $document",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onCreate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Crear ${assetType.title}")
            }
        }
    }
}
