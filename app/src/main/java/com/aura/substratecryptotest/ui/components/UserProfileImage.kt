package com.aura.substratecryptotest.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.substratecryptotest.data.user.User
import com.aura.substratecryptotest.data.user.ProfileImageService

/**
 * Componente para mostrar la imagen de perfil del usuario
 */
@Composable
fun UserProfileImage(
    user: User?,
    profileImageService: ProfileImageService,
    modifier: Modifier = Modifier,
    size: Int = 60
) {
    var profileImage by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(user?.profileImagePath) {
        if (user?.profileImagePath != null) {
            profileImage = profileImageService.loadProfileImage(user.profileImagePath)
        } else {
            profileImage = null
        }
    }
    
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        if (profileImage != null) {
            Image(
                bitmap = profileImage!!.asImageBitmap(),
                contentDescription = "Imagen de perfil de ${user?.name}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Mostrar iniciales si no hay imagen
            val initials = user?.let { getUserInitials(it.name) } ?: "U"
            Text(
                text = initials,
                color = Color.White,
                fontSize = (size * 0.4).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Componente para mostrar información del usuario con imagen de perfil
 */
@Composable
fun UserProfileCard(
    user: User?,
    profileImageService: ProfileImageService,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserProfileImage(
                user = user,
                profileImageService = profileImageService,
                size = 50
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user?.name ?: "Usuario no disponible",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (user?.walletAddress != null) {
                    Text(
                        text = "Wallet: ${user.walletAddress.take(10)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (user?.did != null) {
                    Text(
                        text = "DID: ${user.did.take(10)}...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (user?.biometricEnabled == true) {
                Text(
                    text = "🔒",
                    fontSize = 16.sp,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Obtiene las iniciales del nombre del usuario
 */
private fun getUserInitials(userName: String): String {
    val words = userName.trim().split("\\s+".toRegex())
    return when {
        words.isEmpty() -> "U"
        words.size == 1 -> words[0].take(1).uppercase()
        else -> "${words[0].take(1)}${words[1].take(1)}".uppercase()
    }
}
