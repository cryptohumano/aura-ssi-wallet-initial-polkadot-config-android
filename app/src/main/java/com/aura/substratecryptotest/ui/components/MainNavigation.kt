package com.aura.substratecryptotest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Componente de navegación principal con las 5 rutas principales
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation(
    currentRoute: String,
    onNavigateToWallet: () -> Unit,
    onNavigateToIdentity: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToRWA: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Navegación Principal",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Primera fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavigationItem(
                    title = "Monedero",
                    icon = Icons.Default.AccountBalanceWallet,
                    isSelected = currentRoute == "dashboard",
                    onClick = onNavigateToWallet,
                    modifier = Modifier.weight(1f)
                )
                
                NavigationItem(
                    title = "Identidad",
                    icon = Icons.Default.Person,
                    isSelected = currentRoute == "did_dashboard",
                    onClick = onNavigateToIdentity,
                    modifier = Modifier.weight(1f)
                )
                
                NavigationItem(
                    title = "Credenciales",
                    icon = Icons.Default.Verified,
                    isSelected = currentRoute == "credentials",
                    onClick = onNavigateToCredentials,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Segunda fila
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavigationItem(
                    title = "Documentos",
                    icon = Icons.Default.Description,
                    isSelected = currentRoute == "documents",
                    onClick = onNavigateToDocuments,
                    modifier = Modifier.weight(1f)
                )
                
                NavigationItem(
                    title = "RWA",
                    icon = Icons.Default.Business,
                    isSelected = currentRoute == "rwa",
                    onClick = onNavigateToRWA,
                    modifier = Modifier.weight(1f)
                )
                
                // Espacio vacío para mantener la alineación
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
