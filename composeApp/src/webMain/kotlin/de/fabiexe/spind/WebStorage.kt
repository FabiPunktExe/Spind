package de.fabiexe.spind

import de.fabiexe.spind.data.Storage
import de.fabiexe.spind.data.Vault
import kotlinx.serialization.json.Json

object WebStorage : Storage {
    override fun getVaults(): List<Vault> {
        val servers = getCookie("servers")
        return if (servers.isNullOrBlank()) {
            listOf()
        } else {
            Json.decodeFromString(servers)
        }
    }

    override fun setVaults(vaults: List<Vault>) {
        setCookie("servers", Json.encodeToString(vaults))
    }

    override fun getCachedVaults(): List<Vault> {
        TODO("Not yet implemented")
    }

    override fun getCachedVault(vault: Vault): ByteArray {
        TODO("Not yet implemented")
    }

    override fun cacheVault(vault: Vault, data: ByteArray) {
        TODO("Not yet implemented")
    }
}