package de.fabiexe.spind.app.data

import de.fabiexe.spind.app.data.VaultInfo
import de.fabiexe.spind.app.data.VaultStorage
import kotlin.io.path.*

class CachingVaultStorage(val fallback: VaultStorage) : VaultStorage {
    val path = if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
        Path(System.getProperty("user.home"), "AppData", "Spind", "cache-v1")
    } else {
        Path(System.getProperty("user.home"), ".spind", "cache-v1")
    }

    override fun loadRevision(url: String): Long? {
        val name = url.replace("[^a-zA-Z0-9]+".toRegex(), "_")
        val path = this.path / "cache" / "$name.vault-rev"
        return if (path.exists()) {
            path.readText().toLongOrNull()
        } else {
            fallback.loadRevision(url)
        }
    }

    override fun loadData(url: String): ByteArray? {
        val name = url.replace("[^a-zA-Z0-9]+".toRegex(), "_")
        val path = this.path / "cache" / "$name.vault-data"
        return if (path.exists()) {
            path.readBytes()
        } else {
            fallback.loadData(url)
        }
    }

    override fun save(vault: VaultInfo, revision: Long, data: ByteArray) {
        val name = vault.url.replace("[^a-zA-Z0-9]+".toRegex(), "_")
        val cacheDir = this.path / "cache"
        cacheDir.createDirectories()
        (cacheDir / "$name.vault-data").writeBytes(data)
        (cacheDir / "$name.vault-rev").writeText(revision.toString())
        fallback.save(vault, revision, data)
    }
}