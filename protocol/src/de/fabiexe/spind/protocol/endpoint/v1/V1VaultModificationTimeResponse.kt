package de.fabiexe.spind.protocol.endpoint.v1

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class V1VaultModificationTimeResponse(val modificationTime: Instant)