package de.fabiexe.spind.server

import de.fabiexe.spind.protocol.ServerError
import de.fabiexe.spind.protocol.endpoint.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.respond(error: ServerError) {
    respond(HttpStatusCode.fromValue(error.httpCode), ErrorResponse(error))
}