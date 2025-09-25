package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.substratecryptotest.R

/**
 * Pantalla de bienvenida/onboarding de la wallet
 */
@Composable
fun WelcomeScreen(
    onNavigateToCreateWallet: () -> Unit,
    onNavigateToImportWallet: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    // TODO: Verificar si ya existe una wallet para mostrar dashboard directamente
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo/Icono de la app
        Text(
            text = "🔐",
            fontSize = 80.sp,
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Título principal
        Text(
            text = "Aura Wallet",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Subtítulo
        Text(
            text = "Tu wallet segura para el ecosistema Substrate",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Características principales
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                FeatureItem(
                    icon = "🔐",
                    title = "Seguridad Biométrica",
                    description = "Protección con huella dactilar y PIN"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FeatureItem(
                    icon = "🌐",
                    title = "Multi-Red",
                    description = "Soporte para Polkadot, KILT y más"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                FeatureItem(
                    icon = "🆔",
                    title = "Identidad Digital",
                    description = "Gestión de DIDs y credenciales"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Botones de acción
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Crear nueva wallet
            Button(
                onClick = onNavigateToCreateWallet,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Crear Nueva Wallet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            // Importar wallet existente
            OutlinedButton(
                onClick = onNavigateToImportWallet,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Importar Wallet Existente",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            // Continuar (si ya existe wallet)
            TextButton(
                onClick = onNavigateToDashboard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continuar con Wallet Existente",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Información adicional
        Text(
            text = "Desarrollado para el ecosistema Substrate\nVersión de desarrollo",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 16.dp)
        )
        
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
