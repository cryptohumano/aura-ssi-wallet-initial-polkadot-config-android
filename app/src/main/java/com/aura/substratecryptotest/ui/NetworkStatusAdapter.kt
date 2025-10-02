package com.aura.substratecryptotest.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aura.substratecryptotest.databinding.ItemNetworkStatusBinding
import com.aura.substratecryptotest.network.NetworkConfig
import com.aura.substratecryptotest.network.ConnectionState

class NetworkStatusAdapter(
    private val onNetworkToggle: (NetworkConfig) -> Unit
) : ListAdapter<NetworkStatusItem, NetworkStatusAdapter.NetworkViewHolder>(NetworkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkViewHolder {
        val binding = ItemNetworkStatusBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NetworkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NetworkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NetworkViewHolder(
        private val binding: ItemNetworkStatusBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NetworkStatusItem) {
            binding.apply {
                // Información básica de la red
                textNetworkName.text = item.networkConfig.displayName
                textNetworkDescription.text = item.networkConfig.description
                textNetworkUrl.text = item.networkConfig.wsUrl
                textSS58Prefix.text = "SS58: ${item.networkConfig.ss58Prefix}"
                
                // Estado de conexión
                updateConnectionStatus(item.connectionState, item.isConnected)
                
                // Botón de toggle
                buttonToggle.setOnClickListener {
                    onNetworkToggle(item.networkConfig)
                }
                
                // Indicador de tipo de red
                if (item.networkConfig.isTestnet) {
                    textNetworkType.text = "TESTNET"
                    textNetworkType.setTextColor(binding.root.context.getColor(android.R.color.holo_orange_dark))
                } else {
                    textNetworkType.text = "MAINNET"
                    textNetworkType.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                }
            }
        }
        
        private fun updateConnectionStatus(state: ConnectionState, isConnected: Boolean) {
            binding.apply {
                when (state) {
                    ConnectionState.CONNECTED -> {
                        textConnectionStatus.text = "✅ Conectado"
                        textConnectionStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                        buttonToggle.text = "Desconectar"
                        buttonToggle.isEnabled = true
                        progressBar.visibility = android.view.View.GONE
                    }
                    ConnectionState.CONNECTING -> {
                        textConnectionStatus.text = "🔄 Conectando..."
                        textConnectionStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_blue_dark))
                        buttonToggle.text = "Conectando..."
                        buttonToggle.isEnabled = false
                        progressBar.visibility = android.view.View.VISIBLE
                    }
                    ConnectionState.RECONNECTING -> {
                        textConnectionStatus.text = "🔄 Reconectando..."
                        textConnectionStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_orange_dark))
                        buttonToggle.text = "Reconectando..."
                        buttonToggle.isEnabled = false
                        progressBar.visibility = android.view.View.VISIBLE
                    }
                    ConnectionState.DISCONNECTED -> {
                        textConnectionStatus.text = "❌ Desconectado"
                        textConnectionStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                        buttonToggle.text = "Conectar"
                        buttonToggle.isEnabled = true
                        progressBar.visibility = android.view.View.GONE
                    }
                    ConnectionState.ERROR -> {
                        textConnectionStatus.text = "⚠️ Error"
                        textConnectionStatus.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                        buttonToggle.text = "Reintentar"
                        buttonToggle.isEnabled = true
                        progressBar.visibility = android.view.View.GONE
                    }
                }
            }
        }
    }
}

class NetworkDiffCallback : DiffUtil.ItemCallback<NetworkStatusItem>() {
    override fun areItemsTheSame(oldItem: NetworkStatusItem, newItem: NetworkStatusItem): Boolean {
        return oldItem.networkConfig.name == newItem.networkConfig.name
    }

    override fun areContentsTheSame(oldItem: NetworkStatusItem, newItem: NetworkStatusItem): Boolean {
        return oldItem == newItem
    }
}

