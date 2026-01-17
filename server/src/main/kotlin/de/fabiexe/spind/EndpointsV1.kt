package de.fabiexe.spind

import de.fabiexe.spind.endpoint.v1.V1VaultSecurityQuestionsResponse
import de.fabiexe.spind.endpoint.v1.V1VaultSecurityRequest
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.endpointsV1(storage: Storage) {
    authenticate("v1-vault") {
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
            call.respond(HttpStatusCode.OK)
        }

        patch("/v1/vault/security") {
            val principal = call.principal<UserPasswordCredential>()!!
            val request = call.receive<V1VaultSecurityRequest>()
            storage.writeSecret(principal.name, request.secret)
            storage.writeSecurityQuestions(principal.name, request.securityQuestions)
            storage.writeBackupSecret(principal.name, request.backupSecret)
            call.respond(HttpStatusCode.OK)
        }
    }

    get("/v1/vault/security-questions") {
        val name = call.request.queryParameters["name"]
            ?: throw ApiException(ApiError.UNKNOWN_ERROR)
        val securityQuestions = storage.readSecurityQuestions(name)
            ?: throw ApiException(ApiError.VAULT_NOT_FOUND)
        call.respond(HttpStatusCode.OK, V1VaultSecurityQuestionsResponse(securityQuestions))
    }

    authenticate("v1-vault-recovery") {
        get("/v1/vault/recovery") {
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
    }
}