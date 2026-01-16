package de.fabiexe.spind

import de.fabiexe.spind.endpoint.ErrorResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.io.path.Path

fun main() {
    val port = System.getProperty("SPIND_PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        factory = Netty,
        port = port,
        host = "0.0.0.0",
        module = { module(FileStorage(Path("do-not-touch"))) }
    ).start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module(storage: Storage) {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost()
    }
    install(ContentNegotiation) {
        json()
        cbor()
    }
    install(StatusPages) {
        status(HttpStatusCode.Unauthorized) { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(ApiError.INVALID_CREDENTIALS)
            )
        }
        exception<ApiException> { call, cause ->
            call.respond(
                HttpStatusCode.fromValue(cause.error.httpCode),
                ErrorResponse(cause.error)
            )
        }
        exception<Throwable> { call, cause ->
            cause.printStackTrace()
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(ApiError.INTERNAL_SERVER_ERROR)
            )
        }
    }
    install(Compression) {
        deflate()
        gzip()
    }
    install(Authentication) {
        basic("vault-basic") {
            validate { credentials ->
                if (!storage.exists(credentials.name)) {
                    return@validate null
                }

                val secret = storage.readSecret(credentials.name)
                if (secret != null && credentials.password != secret) {
                    return@validate null
                }

                return@validate credentials
            }
        }
    }
    routing {
        endpointsV1(storage)
    }
}