package com.aura.substratecryptotest.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.aura.substratecryptotest.MainActivity
import com.aura.substratecryptotest.R
import com.aura.substratecryptotest.databinding.FragmentWalletListBinding
import com.aura.substratecryptotest.wallet.Wallet
import com.aura.substratecryptotest.wallet.WalletManager
import com.google.android.material.snackbar.Snackbar

class WalletListFragment : Fragment() {

    private var _binding: FragmentWalletListBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var walletManager: WalletManager
    private lateinit var walletAdapter: WalletAdapter

    companion object {
        fun newInstance(): WalletListFragment {
            return WalletListFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Obtener WalletManager desde la actividad principal
        walletManager = (activity as? MainActivity)?.walletManager 
            ?: throw IllegalArgumentException("WalletManager es requerido")
        
        setupRecyclerView()
        observeWalletManager()
    }

    private fun setupRecyclerView() {
        walletAdapter = WalletAdapter { wallet, action ->
            when (action) {
                WalletAction.SELECT -> {
                    walletManager.selectWallet(wallet.id)
                    Snackbar.make(binding.root, "Wallet seleccionado: ${wallet.name}", Snackbar.LENGTH_SHORT).show()
                }
                WalletAction.EXPORT -> {
                    exportWallet(wallet)
                }
                WalletAction.DELETE -> {
                    deleteWallet(wallet)
                }
            }
        }
        
        binding.recyclerViewWallets.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = walletAdapter
        }
    }

    private fun observeWalletManager() {
        walletManager.wallets.observe(viewLifecycleOwner, Observer { wallets ->
            if (wallets.isEmpty()) {
                binding.textViewEmptyState.visibility = View.VISIBLE
                binding.recyclerViewWallets.visibility = View.GONE
            } else {
                binding.textViewEmptyState.visibility = View.GONE
                binding.recyclerViewWallets.visibility = View.VISIBLE
                walletAdapter.submitList(wallets)
            }
        })

        walletManager.isLoading.observe(viewLifecycleOwner, Observer { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        walletManager.error.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                walletManager.clearError()
            }
        })
    }

    private fun exportWallet(wallet: Wallet) {
        val json = walletManager.exportWalletToJson(wallet.id)
        if (json != null) {
            // En una implementación real, mostrarías el JSON o lo compartirías
            Snackbar.make(binding.root, "Wallet exportado", Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, "Error al exportar wallet", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun deleteWallet(wallet: Wallet) {
        walletManager.deleteWallet(wallet.id)
        Snackbar.make(binding.root, "Wallet eliminado: ${wallet.name}", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

enum class WalletAction {
    SELECT, EXPORT, DELETE
}
