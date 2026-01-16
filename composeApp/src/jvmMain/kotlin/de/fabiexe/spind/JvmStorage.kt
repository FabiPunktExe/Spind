package de.fabiexe.spind

import de.fabiexe.spind.data.Storage
import de.fabiexe.spind.data.Vault
import kotlinx.serialization.json.Json
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText

object JvmStorage : Storage {
    val path = Path(System.getProperty("user.home"), ".spind", "servers.json")

    override fun getVaults(): List<Vault> {
        return if (path.exists()) {
            Json.decodeFromString(path.toFile().readText())
        } else {
            listOf()
        }
    }

    override fun setVaults(vaults: List<Vault>) {
        path.createParentDirectories()
        path.writeText(Json.encodeToString(vaults))
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