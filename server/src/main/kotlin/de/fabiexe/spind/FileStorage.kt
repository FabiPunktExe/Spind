package de.fabiexe.spind

import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.*

class FileStorage(val path: Path) : Storage {
    override fun exists(vault: String): Boolean = (path / "$vault.spind.data").exists()
    override fun read(vault: String): InputStream = (path / "$vault.spind.data").inputStream()
    override fun write(vault: String): OutputStream = (path / "$vault.spind.data")
        .createParentDirectories()
        .outputStream()

    override fun readSecret(vault: String): String? {
        val path = this.path / "$vault.spind.secret"
        return if (path.exists()) {
            path.readText()
        } else {
            null
        }
    }

    override fun writeSecret(vault: String, secret: String) {
        val path = this.path / "$vault.spind.secret"
        path.createParentDirectories()
        path.writeText(secret)
    }
}