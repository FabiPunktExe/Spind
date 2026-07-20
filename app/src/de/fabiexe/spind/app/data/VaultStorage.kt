package de.fabiexe.spind.app.data

interface VaultStorage {
    fun loadRevision(url: String): Long?
    fun loadData(url: String): ByteArray?
    fun save(vault: VaultInfo, revision: Long, data: ByteArray)
}