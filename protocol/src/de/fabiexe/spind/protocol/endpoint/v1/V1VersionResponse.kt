package de.fabiexe.spind.protocol.endpoint.v1

import kotlinx.serialization.Serializable

@Serializable
data class V1VersionResponse(val version: Int)