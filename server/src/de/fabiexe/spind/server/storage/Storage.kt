package de.fabiexe.spind.server.storage

import kotlin.time.Instant

interface Storage {
    suspend fun initialize()
    suspend fun exists(vault: String): Boolean
    suspend fun getVaults(): List<String>
    suspend fun getSecret(vault: String): String?
    suspend fun getModificationTime(vault: String): Instant?
    suspend fun read(vault: String): ByteArray?
    suspend fun write(vault: String, data: ByteArray, secret: String?)
}