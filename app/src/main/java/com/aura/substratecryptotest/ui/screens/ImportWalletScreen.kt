package com.aura.substratecryptotest.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

/**
 * Pantalla para importar wallet existente
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWalletScreen(
    onNavigateBack: () -> Unit,
    onWalletImported: () -> Unit
) {
    LanguageAware {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.import_wallet_title)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.import_wallet_back))
                        }
                    }
                )
            }
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.import_wallet_development),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = stringResource(R.string.import_wallet_development_message),
                    fontSize = 16.sp
                )
                
                Button(onClick = onNavigateBack) {
                    Text(stringResource(R.string.import_wallet_back))
                }
            }
        }
        }
    }
}




