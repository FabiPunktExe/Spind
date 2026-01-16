package de.fabiexe.spind

import androidx.compose.ui.platform.ClipEntry

/**
 * Hash the given data using SHA3-256 algorithm
 *
 * @param data The input data to be hashed
 * @return The SHA3-256 hash of the input data as a ByteArray
 */
expect fun hashSHA3256(data: ByteArray): ByteArray

/**
 * Encrypt the given data using AES-256-CBC algorithm with the provided key
 *
 * @param data The input data to be encrypted
 * @param key The key used for encryption; must be 32 characters long
 * @param salt The salt used for encryption; must be 16 characters long
 * @return The encrypted data as a ByteArray
 */
expect suspend fun encryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray

/**
 * Decrypt the given data using AES-256-CBC algorithm with the provided key
 *
 * @param data The input data to be decrypted
 * @param key The key used for decryption; must be 32 characters long
 * @param salt The salt used for decryption; must be 16 characters long
 * @return The decrypted data as a ByteArray
 */
expect suspend fun decryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray

/**
 * Convert a String to a ClipEntry for clipboard operations
 *
 * @receiver The String to be converted
 * @return The corresponding ClipEntry
 */
expect fun String.toClipEntry(): ClipEntry