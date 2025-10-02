package com.aura.substratecryptotest.ui.components

import androidx.compose.foundation.layout.*
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
 * Bottom Navigation Bar principal según Material Design 3
 * Implementa las 5 rutas principales con navegación inteligente
 */
@Composable
fun AuraBottomNavigationBar(
    currentRoute: String,
    onNavigateToWallet: () -> Unit,
    onNavigateToIdentity: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToRWA: () -> Unit
) {
    var showMoreOptions by remember { mutableStateOf(false) }
    
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        // Monedero - Principal
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.AccountBalanceWallet, 
                    contentDescription = "Monedero"
                ) 
            },
            label = { 
                Text(
                    "Monedero",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ) 
            },
            selected = currentRoute == "dashboard",
            onClick = onNavigateToWallet,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        // Identidad - Principal
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.Person, 
                    contentDescription = "Identidad"
                ) 
            },
            label = { 
                Text(
                    "Identidad",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ) 
            },
            selected = currentRoute == "did_dashboard",
            onClick = onNavigateToIdentity,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        
        // Más opciones - Expandible
        NavigationBarItem(
            icon = { 
                Icon(
                    Icons.Default.MoreHoriz, 
                    contentDescription = "Más opciones"
                ) 
            },
            label = { 
                Text(
                    "Más",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ) 
            },
            selected = currentRoute in listOf("credentials", "documents", "rwa"),
            onClick = { showMoreOptions = true },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
    
    // Menú expandible que se muestra cuando se toca "Más"
    MoreOptionsBottomSheet(
        isVisible = showMoreOptions,
        onDismiss = { showMoreOptions = false },
        onNavigateToCredentials = onNavigateToCredentials,
        onNavigateToDocuments = onNavigateToDocuments,
        onNavigateToRWA = onNavigateToRWA
    )
}

/**
 * Menú expandible para las opciones secundarias
 * Se muestra como un Bottom Sheet o Dropdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreOptionsBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onNavigateToCredentials: () -> Unit,
    onNavigateToDocuments: () -> Unit,
    onNavigateToRWA: () -> Unit
) {
    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Más opciones",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Credenciales
                MoreOptionItem(
                    title = "Credenciales",
                    subtitle = "Gestiona tus credenciales verificables",
                    icon = Icons.Default.Verified,
                    onClick = {
                        onNavigateToCredentials()
                        onDismiss()
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Documentos
                MoreOptionItem(
                    title = "Documentos",
                    subtitle = "Firma y gestiona documentos PDF",
                    icon = Icons.Default.Description,
                    onClick = {
                        onNavigateToDocuments()
                        onDismiss()
                    }
                )
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // RWA
                MoreOptionItem(
                    title = "RWA - Activos Reales",
                    subtitle = "Gestiona activos del mundo real",
                    icon = Icons.Default.Business,
                    onClick = {
                        onNavigateToRWA()
                        onDismiss()
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ir a $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
