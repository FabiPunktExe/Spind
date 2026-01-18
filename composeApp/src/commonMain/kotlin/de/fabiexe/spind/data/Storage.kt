package de.fabiexe.spind.data

interface Storage {
    fun getVaults(): List<Vault>
    fun setVaults(vaults: List<Vault>)
    fun getCachedVault(vault: Vault): ByteArray?
    fun getCachedVaultRevision(vault: Vault): Long?
    fun cacheVault(vault: Vault, data: ByteArray, revision: Long)
}