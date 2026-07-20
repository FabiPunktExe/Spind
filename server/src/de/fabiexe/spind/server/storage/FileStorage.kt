package de.fabiexe.spind.server.storage

import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.io.path.*

class FileStorage(val path: Path) : ResilientStorage() {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun readMetadata(vault: String, classifier: Int): VaultMetadata? {
        val path = (path / vault / "$classifier.meta")
        return if (path.exists()) {
            json.decodeFromString<VaultMetadata>(path.readText())
        } else {
            null
        }
    }

    override suspend fun writeMetadata(vault: String, classifier: Int, metadata: VaultMetadata) {
        val path = (path / vault / "$classifier.meta")
        path.createParentDirectories()
        path.writeText(json.encodeToString(metadata))
    }

    override suspend fun readData(vault: String, classifier: Int): ByteArray? {
        val path = (path / vault / "$classifier.data")
        return if (path.exists()) {
            path.readBytes()
        } else {
            null
        }
    }

    override suspend fun writeData(vault: String, classifier: Int, data: ByteArray) {
        val path = (path / vault / "$classifier.data")
        path.createParentDirectories()
        path.writeBytes(data)
    }

    override suspend fun exists(vault: String): Boolean {
        return (path / vault).exists()
    }

    override suspend fun getVaults(): List<String> {
        return if (path.exists()) {
            path.listDirectoryEntries()
                .filter(Path::isDirectory)
                .map { it.fileName.toString() }
        } else {
            listOf()
        }
    }
}