package com.aura.substratecryptotest.network

/**
 * Configuraciones predefinidas para diferentes redes de Substrate
 */
object NetworkConfigs {
    
    // Redes principales
    val POLKADOT = NetworkConfig(
        name = "polkadot",
        displayName = "Polkadot",
        wsUrl = "wss://rpc.polkadot.io",
        rpcUrl = "https://rpc.polkadot.io",
        ss58Prefix = 0,
        isTestnet = false,
        description = "Red principal de Polkadot"
    )
    
    val KUSAMA = NetworkConfig(
        name = "kusama",
        displayName = "Kusama",
        wsUrl = "wss://kusama-rpc.polkadot.io",
        rpcUrl = "https://kusama-rpc.polkadot.io",
        ss58Prefix = 2,
        isTestnet = false,
        description = "Red principal de Kusama"
    )
    
    val SUBSTRATE = NetworkConfig(
        name = "substrate",
        displayName = "Substrate",
        wsUrl = "wss://substrate-rpc.parity.io",
        rpcUrl = "https://substrate-rpc.parity.io",
        ss58Prefix = 42,
        isTestnet = true,
        description = "Red de desarrollo de Substrate"
    )
    
    // Parachains principales
    val KILT = NetworkConfig(
        name = "kilt",
        displayName = "KILT Protocol",
        wsUrl = "wss://spiritnet.kilt.io",
        rpcUrl = "https://spiritnet.kilt.io",
        ss58Prefix = 38,
        isTestnet = false,
        description = "KILT Protocol - Red de identidad descentralizada"
    )
    
    val ACALA = NetworkConfig(
        name = "acala",
        displayName = "Acala",
        wsUrl = "wss://acala-rpc.aca-api.network",
        rpcUrl = "https://acala-rpc.aca-api.network",
        ss58Prefix = 10,
        isTestnet = false,
        description = "Acala - DeFi Hub de Polkadot"
    )
    
    val MOONBEAM = NetworkConfig(
        name = "moonbeam",
        displayName = "Moonbeam",
        wsUrl = "wss://wss.api.moonbeam.network",
        rpcUrl = "https://rpc.api.moonbeam.network",
        ss58Prefix = 1284,
        isTestnet = false,
        description = "Moonbeam - EVM compatible en Polkadot"
    )
    
    val ASTAR = NetworkConfig(
        name = "astar",
        displayName = "Astar",
        wsUrl = "wss://astar.api.onfinality.io/public-ws",
        rpcUrl = "https://astar.api.onfinality.io/public",
        ss58Prefix = 5,
        isTestnet = false,
        description = "Astar - Smart contract hub de Polkadot"
    )
    
    val PHALA = NetworkConfig(
        name = "phala",
        displayName = "Phala Network",
        wsUrl = "wss://api.phala.network/ws",
        rpcUrl = "https://api.phala.network/rpc",
        ss58Prefix = 30,
        isTestnet = false,
        description = "Phala Network - Computación confidencial"
    )
    
    val BIFROST = NetworkConfig(
        name = "bifrost",
        displayName = "Bifrost",
        wsUrl = "wss://bifrost-rpc.liebi.com/ws",
        rpcUrl = "https://bifrost-rpc.liebi.com",
        ss58Prefix = 6,
        isTestnet = false,
        description = "Bifrost - Liquid staking de Kusama"
    )
    
    // Redes de test
    val WESTEND = NetworkConfig(
        name = "westend",
        displayName = "Westend",
        wsUrl = "wss://westend-rpc.polkadot.io",
        rpcUrl = "https://westend-rpc.polkadot.io",
        ss58Prefix = 42,
        isTestnet = true,
        description = "Westend - Red de test de Polkadot"
    )
    
    val ROCOCO = NetworkConfig(
        name = "rococo",
        displayName = "Rococo",
        wsUrl = "wss://rococo-rpc.polkadot.io",
        rpcUrl = "https://rococo-rpc.polkadot.io",
        ss58Prefix = 42,
        isTestnet = true,
        description = "Rococo - Red de test de parachains"
    )
    
    // Nuevas redes de test agregadas
    val KILT_PEREGRINE = NetworkConfig(
        name = "kilt_peregrine",
        displayName = "KILT Peregrine",
        wsUrl = "wss://peregrine.kilt.io",
        rpcUrl = "https://peregrine.kilt.io",
        ss58Prefix = 38,
        isTestnet = true,
        description = "KILT Peregrine - Red de test de KILT Protocol"
    )
    
    val PASEO = NetworkConfig(
        name = "paseo",
        displayName = "Paseo",
        wsUrl = "wss://paseo-rpc.dwellir.com",
        rpcUrl = "https://paseo-rpc.dwellir.com",
        ss58Prefix = 42,
        isTestnet = true,
        description = "Paseo - Red de test estable para parachains y dApps"
    )
    
    val PASEO_ASSET_HUB = NetworkConfig(
        name = "paseo_asset_hub",
        displayName = "Paseo Asset Hub",
        wsUrl = "wss://asset-hub-paseo-rpc.dwellir.com",
        rpcUrl = "https://asset-hub-paseo-rpc.dwellir.com",
        ss58Prefix = 42,
        isTestnet = true,
        description = "Paseo Asset Hub - System parachain de Paseo para assets"
    )
    
    val WESTEND_ASSET_HUB = NetworkConfig(
        name = "westend_asset_hub",
        displayName = "Westend Asset Hub",
        wsUrl = "wss://asset-hub-westend-rpc.dwellir.com",
        rpcUrl = "https://asset-hub-westend-rpc.dwellir.com",
        ss58Prefix = 42,
        isTestnet = true,
        description = "Westend Asset Hub - System parachain de Westend para assets"
    )
    
    // Lista de todas las redes principales
    val MAIN_NETWORKS = listOf(
        POLKADOT,
        KUSAMA,
        KILT,
        ACALA,
        MOONBEAM,
        ASTAR,
        PHALA,
        BIFROST
    )
    
    // Lista de redes de test
    val TEST_NETWORKS = listOf(
        SUBSTRATE,
        WESTEND,
        ROCOCO,
        KILT_PEREGRINE,
        PASEO,
        PASEO_ASSET_HUB,
        WESTEND_ASSET_HUB
    )
    
    // Todas las redes
    val ALL_NETWORKS = MAIN_NETWORKS + TEST_NETWORKS
    
    /**
     * Obtiene una configuración de red por nombre
     */
    fun getByName(name: String): NetworkConfig? {
        return ALL_NETWORKS.find { it.name == name }
    }
    
    /**
     * Obtiene una configuración de red por prefijo SS58
     */
    fun getBySS58Prefix(prefix: Int): NetworkConfig? {
        return ALL_NETWORKS.find { it.ss58Prefix == prefix }
    }
    
    /**
     * Obtiene redes por tipo
     */
    fun getByType(isTestnet: Boolean): List<NetworkConfig> {
        return if (isTestnet) TEST_NETWORKS else MAIN_NETWORKS
    }
    
    /**
     * Obtiene redes recomendadas para la aplicación
     */
    fun getRecommendedNetworks(): List<NetworkConfig> {
        return listOf(
            POLKADOT,
            KUSAMA,
            KILT,
            ACALA,
            MOONBEAM,
            WESTEND,
            PASEO,
            KILT_PEREGRINE
        )
    }
}
