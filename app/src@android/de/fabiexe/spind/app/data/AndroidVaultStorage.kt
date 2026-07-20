package de.fabiexe.spind.app.data

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.FileNotFoundException

class AndroidVaultInfoStorage(val context: Context) : VaultInfoStorage {
    override fun load(): List<VaultInfo> {
        val text = try {
            context.openFileInput("servers.json").use { it.reader().readText() }.ifBlank { "[]" }
        } catch (_: FileNotFoundException) {
            "[]"
        }
        return Json.decodeFromString(text)
    }

    override fun save(vaults: List<VaultInfo>) {
        context.openFileOutput("servers.json", Context.MODE_PRIVATE).use {
            it.writer().write(Json.encodeToString(vaults))
        }
    }
}

class AndroidVaultStorage(val context: Context) : VaultStorage {
    override fun loadRevision(url: String): Long? {
        val name = url.replace("[^a-zA-Z0-9]+".toRegex(), "_")
        val file = context.getDir("cache", Context.MODE_PRIVATE).resolve("$name.vault-rev")
        return if (file.exists()) {
            file.readText().toLongOrNull()
        } else {
            null
        }
    }

    override fun loadData(url: String): ByteArray? {
        val name = url.replace("[^a-zA-Z0-9]+".toRegex(), "_")
        val file = context.getDir("cache", Context.MODE_PRIVATE).resolve("$name.vault-data")
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }

    override fun save(vault: VaultInfo, revision: Long, data: ByteArray) {
        val name = vault.url.replace("[^a-zA-Z0-9]+".toRegex(), "_")
        val dir = context.getDir("cache", Context.MODE_PRIVATE)
        dir.mkdirs()
        dir.resolve("$name.vault-data").writeBytes(data)
        dir.resolve("$name.vault-rev").writeText(revision.toString())
    }
}
