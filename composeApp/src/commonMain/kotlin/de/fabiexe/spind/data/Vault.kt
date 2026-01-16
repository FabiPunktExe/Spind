package de.fabiexe.spind.data

import kotlinx.serialization.Serializable

@Serializable
data class Vault(
    val name: String,
    val address: String,
    val username: String
)