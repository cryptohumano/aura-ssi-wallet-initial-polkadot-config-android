package com.aura.substratecryptotest.data

import com.aura.substratecryptotest.wallet.Wallet

/**
 * Estados posibles de la wallet
 */
sealed class WalletState {
    object None : WalletState()
    data class Created(val wallet: Wallet) : WalletState()
    data class Error(val message: String) : WalletState()
}
