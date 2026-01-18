package de.fabiexe.spind

/**
 * Represents various API error codes and their corresponding HTTP status codes.
 *
 * 1-999: General errors
 * 1000-1999: Server errors
 * 2000-2999: Client errors
 */
enum class ApiError(val code: Int, val httpCode: Int) {
    // General errors
    UNKNOWN_ERROR(1, 500),
    NETWORK_ERROR(2, 503),

    // Server errors
    INTERNAL_SERVER_ERROR(1001, 500),
    VAULT_NOT_INITIALIZED(1002, 412),
    INVALID_CREDENTIALS(1003, 401),
    VAULT_NOT_FOUND(1004, 404),

    // Client errors
    CORRUPTED_VAULT(2001, 500),
    VAULT_VERSION_TOO_OLD(2002, 400),
    VAULT_VERSION_TOO_NEW(2003, 400),
    INVALID_PASSWORD(2004, 400),
    RECOVERY_NOT_POSSIBLE(2005, 400)
}