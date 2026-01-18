package de.fabiexe.spind

import android.content.Context
import de.fabiexe.spind.data.Storage
import de.fabiexe.spind.data.Vault
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

class AndroidStorage(val context: Context) : Storage {
    override fun getVaults(): List<Vault> {
        val text = try {
            context.openFileInput("servers.json").use { it.reader().readText() }.ifBlank { "[]" }
        } catch (_: FileNotFoundException) {
            "[]"
        }
        return Json.decodeFromString(text)
    }

    override fun setVaults(vaults: List<Vault>) {
        context.openFileOutput("servers.json", Context.MODE_PRIVATE).use {
            it.writer().write(Json.encodeToString(vaults))
        }
    }

    override fun getCachedVault(vault: Vault): ByteArray? {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+".toRegex(), "_")}_${vault.username}"
        val file = context.getDir("cache", Context.MODE_PRIVATE).resolve("$name.vault-data")
        return if (file.exists()) {
            file.readBytes()
        } else {
            null
        }
    }

    override fun getCachedVaultRevision(vault: Vault): Long? {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+".toRegex(), "_")}_${vault.username}"
        val file = context.getDir("cache", Context.MODE_PRIVATE).resolve("$name.vault-rev")
        return if (file.exists()) {
            file.readText().toLongOrNull()
        } else {
            null
        }
    }

    override fun cacheVault(vault: Vault, data: ByteArray, revision: Long) {
        val name = "${vault.address.replace("[^a-zA-Z0-9]+".toRegex(), "_")}_${vault.username}"
        val dir = context.getDir("cache", Context.MODE_PRIVATE)
        dir.mkdirs()
        dir.resolve("$name.vault-data").writeBytes(data)
        dir.resolve("$name.vault-rev").writeText(revision.toString())
    }
}