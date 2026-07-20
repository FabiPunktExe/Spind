package de.fabiexe.spind.app.data

interface VaultInfoStorage {
    fun load(): List<VaultInfo>
    fun save(vaults: List<VaultInfo>)
}