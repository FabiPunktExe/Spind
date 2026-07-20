package de.fabiexe.spind.server.storage

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class VaultMetadata(
    val secret: String,
    val modificationTime: Instant,
    val state: State
) {
    enum class State {
        WRITING, FINE
    }
}