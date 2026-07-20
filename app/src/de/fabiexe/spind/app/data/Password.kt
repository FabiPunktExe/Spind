package de.fabiexe.spind.app.data

import kotlinx.serialization.Serializable

@Serializable
data class Password(
    val name: String,
    val fields: Map<String, String>
) {
    val password: String
        get() = fields["password"]!!
}