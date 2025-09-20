package com.aura.substratecryptotest.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.databinding.ItemWalletBinding
import com.aura.substratecryptotest.wallet.Wallet

class WalletAdapter(
    private val onWalletAction: (Wallet, WalletAction) -> Unit
) : ListAdapter<Wallet, WalletAdapter.WalletViewHolder>(WalletDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WalletViewHolder {
        val binding = ItemWalletBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WalletViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WalletViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class WalletViewHolder(
        private val binding: ItemWalletBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(wallet: Wallet) {
            binding.apply {
                textViewWalletName.text = wallet.name
                textViewCryptoType.text = wallet.cryptoType.name
                textViewAddress.text = wallet.address
                textViewDerivationPath.text = wallet.derivationPath
                textViewMnemonic.text = "Mnemonic: ${wallet.mnemonic}"
                
                // Mostrar información del par de claves
                val publicKeyHex = wallet.publicKey?.joinToString("") { "%02x".format(it) } ?: "N/A"
                val privateKeyHex = wallet.privateKey?.joinToString("") { "%02x".format(it) } ?: "N/A"
                
                textViewPublicKey.text = "Public Key: 0x${publicKeyHex.take(16)}..."
                textViewPrivateKey.text = "Private Key: 0x${privateKeyHex.take(16)}..."

                buttonSelect.setOnClickListener {
                    onWalletAction(wallet, WalletAction.SELECT)
                }

                buttonExport.setOnClickListener {
                    onWalletAction(wallet, WalletAction.EXPORT)
                }

                buttonDelete.setOnClickListener {
                    onWalletAction(wallet, WalletAction.DELETE)
                }
            }
        }
    }
}

class WalletDiffCallback : DiffUtil.ItemCallback<Wallet>() {
    override fun areItemsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Wallet, newItem: Wallet): Boolean {
        return oldItem == newItem
    }
}
