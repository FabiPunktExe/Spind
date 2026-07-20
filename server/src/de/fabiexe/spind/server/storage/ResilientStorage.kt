package de.fabiexe.spind.server.storage

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Instant

abstract class ResilientStorage : Storage {
    val locksLock = Mutex()
    val locks = mutableMapOf<String, Mutex>()

    abstract suspend fun readMetadata(vault: String, classifier: Int): VaultMetadata?
    abstract suspend fun writeMetadata(vault: String, classifier: Int, metadata: VaultMetadata)
    abstract suspend fun readData(vault: String, classifier: Int): ByteArray?
    abstract suspend fun writeData(vault: String, classifier: Int, data: ByteArray)

    protected suspend inline fun <T> withLock(vault: String, action: () -> T): T {
        val lock = locksLock.withLock { locks.getOrPut(vault, ::Mutex) }
        return lock.withLock(action = action)
    }

    override suspend fun initialize() {
        for (vault in getVaults()) {
            withLock(vault) {
                val metadata1 = readMetadata(vault, 1)
                val metadata2 = readMetadata(vault, 2)
                when {
                    metadata1 == null && metadata2 == null -> {
                        println("Warning: No metadata of vault \"$vault\" exists.")
                    }
                    metadata1?.state != VaultMetadata.State.FINE && metadata2?.state != VaultMetadata.State.FINE -> {
                        println("Warning: No fine version of vault \"$vault\" exists.")
                    }
                    metadata1?.state != VaultMetadata.State.FINE && metadata2?.state == VaultMetadata.State.FINE -> {
                        val data2 = readData(vault, 2)
                        if (data2 == null) {
                            println("Warning: No fine version of data of vault \"$vault\" exists.")
                        } else {
                            writeData(vault, 1, data2)
                            writeMetadata(vault, 1, metadata2)
                        }
                    }
                    metadata1?.state == VaultMetadata.State.FINE && metadata2?.state != VaultMetadata.State.FINE -> {
                        val data1 = readData(vault, 1)
                        if (data1 == null) {
                            println("Warning: No fine version of data of vault \"$vault\" exists.")
                        } else {
                            writeData(vault, 2, data1)
                            writeMetadata(vault, 2, metadata1)
                        }
                    }
                }
            }
        }
    }

    private suspend fun readMetadata(vault: String): VaultMetadata? = withLock(vault) {
        val metadata1 = readMetadata(vault, 1)
        val metadata2 = readMetadata(vault, 2)
        when {
            metadata1?.state == VaultMetadata.State.FINE && metadata2?.state != VaultMetadata.State.FINE -> metadata1
            metadata1?.state != VaultMetadata.State.FINE && metadata2?.state == VaultMetadata.State.FINE -> metadata2
            metadata1?.state == VaultMetadata.State.FINE && metadata2?.state == VaultMetadata.State.FINE -> {
                if (metadata1.modificationTime > metadata2.modificationTime) {
                    metadata1
                } else {
                    metadata2
                }
            }
            else -> null
        }
    }

    override suspend fun getSecret(vault: String): String? {
        return readMetadata(vault)?.secret
    }

    override suspend fun getModificationTime(vault: String): Instant? {
        return readMetadata(vault)?.modificationTime
    }

    override suspend fun read(vault: String): ByteArray? = withLock(vault) {
        val metadata1 = readMetadata(vault, 1)
        val metadata2 = readMetadata(vault, 2)
        when {
            metadata1?.state == VaultMetadata.State.FINE && metadata2?.state != VaultMetadata.State.FINE -> {
                readData(vault, 1)
            }
            metadata1?.state != VaultMetadata.State.FINE && metadata2?.state == VaultMetadata.State.FINE -> {
                readData(vault, 2)
            }
            metadata1?.state == VaultMetadata.State.FINE && metadata2?.state == VaultMetadata.State.FINE -> {
                if (metadata1.modificationTime > metadata2.modificationTime) {
                    readData(vault, 1)
                } else {
                    readData(vault, 2)
                }
            }
            else -> null
        }
    }

    override suspend fun write(vault: String, data: ByteArray, secret: String?) = withLock(vault) {
        val metadata1 = readMetadata(vault, 1)
        val metadata2 = readMetadata(vault, 2)
        val modificationTime = Clock.System.now()
        if (metadata1?.state != VaultMetadata.State.FINE ||
            (metadata2?.state == VaultMetadata.State.FINE && metadata1.modificationTime < metadata2.modificationTime)) {
            val secret = secret ?: metadata2?.secret ?: ""
            writeMetadata(vault, 1, VaultMetadata(secret, modificationTime, VaultMetadata.State.WRITING))
            writeData(vault, 1, data)
            writeMetadata(vault, 1, VaultMetadata(secret, modificationTime, VaultMetadata.State.FINE))
        }
        if (metadata2?.state != VaultMetadata.State.FINE ||
            (metadata1?.state == VaultMetadata.State.FINE && metadata2.modificationTime < metadata1.modificationTime)) {
            val secret = secret ?: metadata1?.secret ?: ""
            writeMetadata(vault, 2, VaultMetadata(secret, modificationTime, VaultMetadata.State.WRITING))
            writeData(vault, 2, data)
            writeMetadata(vault, 2, VaultMetadata(secret, modificationTime, VaultMetadata.State.FINE))
        }
    }
}