package de.fabiexe.spind

import kotlinx.serialization.Serializable

@Serializable
data class SecurityInfoV1(
    val secret: String,
    val securityQuestions: List<String>,
    val backupSecret: String?
)