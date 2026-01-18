package de.fabiexe.spind

import de.fabiexe.spind.data.Storage
import de.fabiexe.spind.data.Vault
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

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

    override fun getCachedVault(vault: Vault): ByteArray? {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+", "_")}_${vault.username}"
        val data = getCookie("cache_vault_data_${name}")
        return data?.let(Base64::decode)
    }

    override fun getCachedVaultRevision(vault: Vault): Long? {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+", "_")}_${vault.username}"
        val rev = getCookie("cache_vault_rev_${name}")
        return rev?.toLongOrNull()
    }

    override fun cacheVault(vault: Vault, data: ByteArray, revision: Long) {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+", "_")}_${vault.username}"
        setCookie("cache_vault_data_${name}", Base64.encode(data))
        setCookie("cache_vault_rev_${name}", revision.toString())
    }
}