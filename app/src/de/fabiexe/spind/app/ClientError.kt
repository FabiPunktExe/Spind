package de.fabiexe.spind

/**
 * Represents various client error codes and their corresponding HTTP status codes.
 *
 * @param code The internal error code
 * @param httpCode The corresponding HTTP status code
 */
enum class ClientError(val code: Int, val httpCode: Int) {
    UNKNOWN_ERROR(1, 500),
    NETWORK_ERROR(2, 503),
    CORRUPTED_VAULT(3, 500),
    VAULT_VERSION_TOO_OLD(4, 400),
    VAULT_VERSION_TOO_NEW(5, 400),
    INVALID_PASSWORD(6, 400),
    RECOVERY_NOT_POSSIBLE(7, 400),
    REVISION_PARAMETER_MISSING(8, 400)
}