package de.fabiexe.spind.server

import de.fabiexe.spind.protocol.ServerError

data class SpindServerException(val error: ServerError) : Exception(error.name)