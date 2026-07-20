package de.fabiexe.spind.app.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlin.io.path.*

object LocalVaultInfoStorage : VaultInfoStorage {
    val path = if (System.getProperty("os.name").contains("win", ignoreCase = true)) {
        Path(System.getProperty("user.home"), "AppData", "Spind", "servers-v1.json")
    } else {
        Path(System.getProperty("user.home"), ".spind", "servers-v1.json")
    }

    override fun load(): List<VaultInfo> {
        return if (path.exists()) {
            Json.decodeFromString(path.readText())
        } else {
            listOf()
        }
    }

    override fun save(vaults: List<VaultInfo>) {
        path.createParentDirectories()
        path.writeText(Json.encodeToString(vaults))
    }
}