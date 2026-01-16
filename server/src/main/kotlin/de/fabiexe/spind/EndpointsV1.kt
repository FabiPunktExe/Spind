package de.fabiexe.spind

import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.endpointsV1(storage: Storage) {
    authenticate("vault-basic") {
        get("/v1/vault") {
            val principal = call.principal<UserPasswordCredential>()!!

            if (!storage.exists(principal.name)) {
                throw ApiException(ApiError.VAULT_NOT_FOUND)
            }
            if (storage.readSecret(principal.name) == null) {
                throw ApiException(ApiError.VAULT_NOT_INITIALIZED)
            }

            storage.read(principal.name).use { inputStream ->
                call.respondOutputStream(status = HttpStatusCode.OK) {
                    inputStream.transferTo(this)
                }
            }
        }

        put("/v1/vault") {
            val principal = call.principal<UserPasswordCredential>()!!

            storage.write(principal.name).use { outputStream ->
                call.receiveStream().transferTo(outputStream)
            }

            if (storage.readSecret(principal.name) == null) {
                storage.writeSecret(principal.name, principal.password)
            }

            call.respond(HttpStatusCode.OK)
        }

        patch("/v1/vault/secret") {
            val principal = call.principal<UserPasswordCredential>()!!
            val newSecret = call.receiveText()

            if (newSecret.isBlank()) {
                call.respond(HttpStatusCode.BadRequest)
                return@patch
            }

            storage.writeSecret(principal.name, newSecret)
            call.respond(HttpStatusCode.OK)
        }
    }
}