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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.substratecryptotest.ui.viewmodels.UserManagementViewModel
import com.aura.substratecryptotest.security.UserManager

/**
 * Pantalla de gestión de usuarios
 * Permite crear, cambiar y gestionar múltiples usuarios con autenticación biométrica
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserManagementScreen(
    onNavigateToWallet: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: UserManagementViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadRegisteredUsers()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
            }
            
            Text(
                text = "Gestión de Usuarios",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = { viewModel.showCreateUserDialog() }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Crear Usuario")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Usuario actual
        val currentUser = uiState.currentUser
        if (currentUser != null) {
            CurrentUserCard(
                user = currentUser,
                onLogout = { viewModel.logoutCurrentUser() }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Lista de usuarios registrados
        if (uiState.registeredUsers.isNotEmpty()) {
            Text(
                text = "Usuarios Registrados",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.registeredUsers) { user ->
                    UserCard(
                        user = user,
                        isCurrentUser = user.id == uiState.currentUser?.id,
                        onSwitchToUser = { viewModel.switchToUser(user.id) },
                        onDeleteUser = { viewModel.showDeleteUserDialog(user) }
                    )
                }
            }
        } else {
            // Estado vacío
            EmptyUsersState(
                onCreateUser = { viewModel.showCreateUserDialog() }
            )
        }
        
        // Botón para ir a wallet si hay usuario activo
        if (uiState.currentUser != null) {
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onNavigateToWallet,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ir a Mi Wallet")
            }
        }
    }
    
    // Dialog para crear usuario
    if (uiState.showCreateUserDialog) {
        CreateUserDialog(
            onDismiss = { viewModel.hideCreateUserDialog() },
            onCreateUser = { userName -> viewModel.createNewUser(userName) },
            isLoading = uiState.isLoading
        )
    }
    
    // Dialog para eliminar usuario
    val userToDelete = uiState.userToDelete
    if (userToDelete != null) {
        DeleteUserDialog(
            user = userToDelete,
            onDismiss = { viewModel.hideDeleteUserDialog() },
            onDeleteUser = { viewModel.deleteUser(userToDelete) },
            isLoading = uiState.isLoading
        )
    }
    
    // Snackbar para mensajes
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            viewModel.clearMessage()
        }
        
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearMessage() }) {
                    Text("Cerrar")
                }
            }
        ) {
            Text(message)
        }
    }
}

@Composable
private fun CurrentUserCard(
    user: UserManager.User,
    onLogout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = "Cerrar Sesión")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Usuario Activo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun UserCard(
    user: UserManager.User,
    isCurrentUser: Boolean,
    onSwitchToUser: () -> Unit,
    onDeleteUser: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = if (isCurrentUser) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Creado: ${formatDate(user.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Row {
                if (!isCurrentUser) {
                    IconButton(onClick = onSwitchToUser) {
                        Icon(
                            Icons.Default.Login,
                            contentDescription = "Cambiar Usuario",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                IconButton(onClick = onDeleteUser) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar Usuario",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyUsersState(
    onCreateUser: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PersonAdd,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No hay usuarios registrados",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Crea tu primer usuario para comenzar a usar la aplicación",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onCreateUser,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Crear Primer Usuario")
        }
    }
}

@Composable
private fun CreateUserDialog(
    onDismiss: () -> Unit,
    onCreateUser: (String) -> Unit,
    isLoading: Boolean
) {
    var userName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Crear Nuevo Usuario")
        },
        text = {
            Column {
                Text("Ingresa un nombre para el nuevo usuario:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Nombre de Usuario") },
                    singleLine = true,
                    enabled = !isLoading
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreateUser(userName) },
                enabled = userName.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun DeleteUserDialog(
    user: UserManager.User,
    onDismiss: () -> Unit,
    onDeleteUser: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Eliminar Usuario")
        },
        text = {
            Text("¿Estás seguro de que quieres eliminar al usuario \"${user.name}\"? Esta acción no se puede deshacer y se eliminarán todos sus datos.")
        },
        confirmButton = {
            Button(
                onClick = onDeleteUser,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Eliminar")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
    return formatter.format(date)
}
