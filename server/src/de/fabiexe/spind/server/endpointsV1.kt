package de.fabiexe.spind.server

import de.fabiexe.spind.protocol.PROTOCOL_VERSION
import de.fabiexe.spind.protocol.ServerError
import de.fabiexe.spind.protocol.endpoint.v1.V1VaultModificationTimeResponse
import de.fabiexe.spind.protocol.endpoint.v1.V1VersionResponse
import de.fabiexe.spind.server.storage.Storage
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.endpointsV1(storage: Storage) {
    route("v1") {
        get("version") {
            call.respond(V1VersionResponse(PROTOCOL_VERSION))
        }

        get("vault/modification-time") {
            val vault = call.parameters["vault"]
                ?: throw SpindServerException(ServerError.VAULT_PARAMETER_MISSING)
            if (!storage.exists(vault)) {
                throw SpindServerException(ServerError.VAULT_NOT_FOUND)
            }
            val modificationTime = storage.getModificationTime(vault)
                ?: throw SpindServerException(ServerError.VAULT_NOT_INITIALIZED)
            call.respond(V1VaultModificationTimeResponse(modificationTime))
        }

        authenticate("v1") {
            get("/v1/vault") {
                val vault = call.principal<String>()!!
                if (!storage.exists(vault)) {
                    throw SpindServerException(ServerError.VAULT_NOT_FOUND)
                }
                val data = storage.read(vault) ?: throw SpindServerException(ServerError.VAULT_NOT_INITIALIZED)
                call.respondBytes(data)
            }

            put("/v1/vault") {
                val vault = call.principal<String>()!!
                val newSecret = call.parameters["new-secret"]
                storage.write(vault, call.receive<ByteArray>(), newSecret)
            }
        }
    }
}