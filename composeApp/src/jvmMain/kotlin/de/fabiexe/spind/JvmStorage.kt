package de.fabiexe.spind

import de.fabiexe.spind.data.Storage
import de.fabiexe.spind.data.Vault
import kotlinx.serialization.json.Json
import kotlin.io.path.*

object JvmStorage : Storage {
    val path = if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
        Path(System.getProperty("user.home"), "AppData", "Spind", "v1")
    } else {
        Path(System.getProperty("user.home"), ".spind", "v1")
    }

    override fun getVaults(): List<Vault> {
        val path = this.path / "servers.json"
        return if (path.exists()) {
            Json.decodeFromString(path.toFile().readText())
        } else {
            listOf()
        }
    }

    override fun setVaults(vaults: List<Vault>) {
        val path = this.path / "servers.json"
        path.createParentDirectories()
        path.writeText(Json.encodeToString(vaults))
    }

    override fun getCachedVault(vault: Vault): ByteArray? {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+".toRegex(), "_")}_${vault.username}"
        val path = this.path / "cache" / "$name.vault-data"
        return if (path.exists()) {
            path.readBytes()
        } else {
            null
        }
    }

    override fun getCachedVaultRevision(vault: Vault): Long? {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+".toRegex(), "_")}_${vault.username}"
        val path = this.path / "cache" / "$name.vault-rev"
        return if (path.exists()) {
            path.readText().toLongOrNull()
        } else {
            null
        }
    }

    override fun cacheVault(vault: Vault, data: ByteArray, revision: Long) {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+".toRegex(), "_")}_${vault.username}"
        val path = this.path / "cache"
        path.createDirectories()
        (path / "$name.vault-data").writeBytes(data)
        (path / "$name.vault-rev").writeText(revision.toString())
    }
}