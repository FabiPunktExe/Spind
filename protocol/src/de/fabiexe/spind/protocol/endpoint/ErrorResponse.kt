package de.fabiexe.spind.protocol.endpoint

import de.fabiexe.spind.protocol.ServerError
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: ServerError)