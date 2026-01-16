package de.fabiexe.spind.endpoint

import de.fabiexe.spind.ApiError
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(val error: ApiError)