package de.fabiexe.spind.endpoint.v1

import kotlinx.serialization.Serializable

@Serializable
data class V1VaultSecurityRequest(
    val secret: String,
    val securityQuestions: List<String>,
    val backupSecret: String?
)