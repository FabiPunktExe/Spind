package de.fabiexe.spind.server

import at.favre.lib.crypto.bcrypt.BCrypt
import de.fabiexe.spind.server.storage.FileStorage
import de.fabiexe.spind.server.storage.Storage
import de.fabiexe.spind.protocol.ServerError
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.logging.error
import kotlin.io.path.Path

suspend fun main() {
    val port = System.getProperty("SPIND_PORT")?.toIntOrNull() ?: 8080
    val storage = FileStorage(Path("do-not-touch"))
    storage.initialize()

    embeddedServer(Netty, port, "0.0.0.0") {
        install(CORS) {
            anyHost()
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Patch)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.ContentType)
            allowCredentials = true
        }

        install(ContentNegotiation) {
            json()
        }

        install(StatusPages) {
            status(HttpStatusCode.Unauthorized) { call, _ ->
                call.respond(ServerError.INVALID_CREDENTIALS)
            }
            exception<SpindServerException> { call, cause ->
                call.respond(cause.error)
            }
            exception<Throwable> { call, cause ->
                this@embeddedServer.log.error(cause)
                call.respond(ServerError.INTERNAL_SERVER_ERROR)
            }
        }

        install(Compression) {
            deflate()
            gzip()
        }

        install(Authentication) {
            basic("v1") {
                validate { credentials ->
                    if (storage.exists(credentials.name)) {
                        val secret = storage.getSecret(credentials.name)
                        if (secret != null) {
                            val password = secret.encodeToByteArray()
                            val bcryptHash = credentials.password.hexToByteArray()
                            if (BCrypt.verifyer().verify(password, bcryptHash).verified) {
                                credentials.name
                            } else {
                                null
                            }
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
        }

        routing {
            endpointsV1(storage)
        }
    }.startSuspend(true)
}