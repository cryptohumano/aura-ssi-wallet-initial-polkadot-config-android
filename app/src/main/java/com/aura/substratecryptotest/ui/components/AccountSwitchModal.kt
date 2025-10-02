package com.aura.substratecryptotest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aura.substratecryptotest.data.WalletInfo

/**
 * Modal para cambiar entre cuentas/wallets
 */
@Composable
fun AccountSwitchModal(
    availableWallets: List<WalletInfo>,
    currentWalletName: String?,
    onWalletSelected: (String) -> Unit,
    onWalletDeleted: (String) -> Unit,
    onWalletRenamed: (String, String) -> Unit, // ✅ Nuevo callback para renombrar
    onDismiss: () -> Unit,
    onRefresh: () -> Unit = {}
) {
    var showDeleteDialog by remember { mutableStateOf<String?>(null) }
    var editingWalletName by remember { mutableStateOf<String?>(null) } // ✅ Estado para edición
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cambiar Cuenta",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row {
                            // Botón de refrescar
                            IconButton(onClick = onRefresh) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refrescar cuentas"
                                )
                            }
                            
                            // Botón de cerrar
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cerrar"
                                )
                            }
                        }
                    }
                    
                    // ✅ Estadísticas de cuentas con DID
                    if (availableWallets.isNotEmpty()) {
                        val accountsWithDid = availableWallets.count { !it.kiltDid.isNullOrEmpty() }
                        val totalAccounts = availableWallets.size
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "Cuentas con DID",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$accountsWithDid de $totalAccounts cuentas tienen DID",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lista de usuarios
                if (availableWallets.isEmpty()) {
                    // Estado vacío
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        
                        Text(
                            text = "No hay usuarios disponibles",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = "Crea un nuevo usuario para comenzar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableWallets) { wallet ->
                            AccountItem(
                                wallet = wallet,
                                isSelected = wallet.name == currentWalletName,
                                isEditing = editingWalletName == wallet.name,
                                onClick = { onWalletSelected(wallet.name) },
                                onDelete = { showDeleteDialog = wallet.name },
                                onEdit = { editingWalletName = wallet.name },
                                onSaveEdit = { newName -> 
                                    onWalletRenamed(wallet.name, newName)
                                    editingWalletName = null
                                },
                                onCancelEdit = { editingWalletName = null }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botón de cerrar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
    
    // Diálogo de confirmación para borrar wallet
    showDeleteDialog?.let { walletName ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Borrar Wallet") },
            text = { 
                Text("¿Estás seguro de que quieres borrar la wallet \"$walletName\"? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onWalletDeleted(walletName)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Borrar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountItem(
    wallet: WalletInfo,
    isSelected: Boolean,
    isEditing: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSaveEdit: (String) -> Unit,
    onCancelEdit: () -> Unit
) {
    val hasDid = !wallet.kiltDid.isNullOrEmpty()
    val didAddress = wallet.kiltDids?.get("authentication") ?: wallet.kiltDid
    var editedName by remember { mutableStateOf(wallet.name) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else if (hasDid) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else if (hasDid) 3.dp else 1.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Foto de perfil usando ProfileImageSelector
            if (hasDid && didAddress != null) {
                ProfileImageSelector(
                    didAddress = didAddress,
                    onImageSelected = { /* La imagen se guarda automáticamente */ },
                    modifier = Modifier.size(48.dp)
                )
            } else {
                // Icono por defecto si no hay DID
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Sin perfil",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Información de la cuenta
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // ✅ Nombre del usuario con funcionalidad de edición
                if (isEditing) {
                    // Modo edición
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        singleLine = true,
                        trailingIcon = {
                            Row {
                                IconButton(onClick = { onSaveEdit(editedName) }) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Guardar",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { 
                                    editedName = wallet.name
                                    onCancelEdit()
                                }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Cancelar",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    )
                } else {
                    // Modo visualización
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    
                    // ✅ Indicador de DID disponible
                    if (hasDid) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Verified,
                                contentDescription = "DID disponible",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "DID",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    // Indicador de cuenta activa
                    if (isSelected) {
                        Text(
                            text = "ACTIVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                }
                
                // Información del DID si está disponible
                if (hasDid && didAddress != null) {
                    Text(
                        text = "DID: ${didAddress.take(20)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        }
                    )
                }
                
                // Dirección base
                Text(
                    text = "Dirección: ${wallet.address.take(20)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
                
                // Fecha de creación
                Text(
                    text = "Creado: ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date(wallet.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }
            
            // ✅ Botones de acción
            Row {
                // Botón de editar (siempre disponible)
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar nombre",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Botón de borrar (solo si no es la wallet actual)
                if (!isSelected) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Borrar wallet",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    return formatter.format(date)
}
