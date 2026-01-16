package de.fabiexe.spind.data

interface Storage {
    fun getVaults(): List<Vault>
    fun setVaults(vaults: List<Vault>)
    fun getCachedVaults(): List<Vault>
    fun getCachedVault(vault: Vault): ByteArray
    fun cacheVault(vault: Vault, data: ByteArray)
}