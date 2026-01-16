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