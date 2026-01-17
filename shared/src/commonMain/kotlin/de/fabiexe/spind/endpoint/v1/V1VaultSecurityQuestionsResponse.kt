package de.fabiexe.spind.endpoint.v1

import kotlinx.serialization.Serializable

@Serializable
data class V1VaultSecurityQuestionsResponse(val securityQuestions: List<String>)