package de.fabiexe.spind.protocol

/**
 * Represents various server error codes and their corresponding HTTP status codes.
 *
 * @param code The internal error code
 * @param httpCode The corresponding HTTP status code
 */
enum class ServerError(val code: Int, val httpCode: Int) {
    INTERNAL_SERVER_ERROR(1001, 500),
    VAULT_PARAMETER_MISSING(1002, 500),
    VAULT_NOT_INITIALIZED(1003, 425),
    INVALID_CREDENTIALS(1004, 401),
    VAULT_NOT_FOUND(1005, 404)
}