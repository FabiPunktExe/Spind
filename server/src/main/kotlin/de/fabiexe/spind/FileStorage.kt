package de.fabiexe.spind

import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import kotlin.io.path.*

class FileStorage(val path: Path) : Storage {
    override fun exists(vault: String): Boolean = (path / "$vault.spind-data").exists()
    override fun read(vault: String): InputStream = (path / "$vault.spind-data").inputStream()
    override fun write(vault: String): OutputStream = (path / "$vault.spind-data")
        .createParentDirectories()
        .outputStream()

    private fun readSecurityInfoV1(vault: String): SecurityInfoV1? {
        val path = this.path / "$vault.spind-security-v1"
        return if (path.exists()) {
            Json.decodeFromString<SecurityInfoV1>(path.readText())
        } else {
            null
        }
    }

    private fun writeSecurityInfoV1(vault: String, info: SecurityInfoV1) {
        val path = this.path / "$vault.spind-security-v1"
        path.createParentDirectories()
        path.writeText(Json.encodeToString(info))
    }

    override fun readSecret(vault: String): String? {
        return readSecurityInfoV1(vault)?.secret
    }

    override fun writeSecret(vault: String, secret: String) {
        val securityInfo = readSecurityInfoV1(vault)
            ?: SecurityInfoV1("", listOf(), null)
        writeSecurityInfoV1(vault, securityInfo.copy(secret = secret))
    }

    override fun readSecurityQuestions(vault: String): List<String>? {
        return readSecurityInfoV1(vault)?.securityQuestions
    }

    override fun writeSecurityQuestions(vault: String, questions: List<String>) {
        val securityInfo = readSecurityInfoV1(vault)
            ?: SecurityInfoV1("", listOf(), null)
        writeSecurityInfoV1(vault, securityInfo.copy(securityQuestions = questions))
    }

    override fun readBackupSecret(vault: String): String? {
        return readSecurityInfoV1(vault)?.backupSecret
    }

    override fun writeBackupSecret(vault: String, backupSecret: String?) {
        val securityInfo = readSecurityInfoV1(vault)
            ?: SecurityInfoV1("", listOf(), null)
        writeSecurityInfoV1(vault, securityInfo.copy(backupSecret = backupSecret))
    }

    override fun readRevision(vault: String): Long {
        val path = this.path / "$vault.spind-rev"
        return if (path.exists()) {
            path.readText().toLongOrNull() ?: 0
        } else {
            0L
        }
    }

    override fun writeRevision(vault: String, revision: Long) {
        val path = this.path / "$vault.spind-rev"
        path.createParentDirectories()
        path.writeText(revision.toString())
    }
}