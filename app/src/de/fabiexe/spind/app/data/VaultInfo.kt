package de.fabiexe.spind.app.data

import io.ktor.http.Url
import kotlinx.serialization.Serializable

@Serializable
data class VaultInfo(val name: String, val url: String)

val VaultInfo.username: String
    get() = Url(this.url).user ?: ""

val VaultInfo.address: String
    get() {
        val parsed = Url(this.url)
        return "${parsed.protocol.name}://${parsed.hostWithPort}"
    }