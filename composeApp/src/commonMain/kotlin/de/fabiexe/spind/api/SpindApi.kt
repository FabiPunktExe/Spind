package de.fabiexe.spind.api

import de.fabiexe.spind.*
import de.fabiexe.spind.data.*
import de.fabiexe.spind.endpoint.ErrorResponse
import de.fabiexe.spind.endpoint.v1.V1VaultSecurityQuestionsResponse
import de.fabiexe.spind.endpoint.v1.V1VaultSecurityRequest
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.readByteArray
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

class SpindApi(httpClientEngineFactory: HttpClientEngineFactory<*>, private val storage: Storage) {
    @OptIn(ExperimentalSerializationApi::class)
    val httpClient = HttpClient(httpClientEngineFactory) {
        install(ContentNegotiation) {
            json()
            cbor()
        }
        install(ContentEncoding) {
            deflate(1.0F)
            gzip(0.9F)
        }
    }

    suspend fun getVault(vault: Vault, secret: String): Either<ByteArray, ErrorResponse> {
        try {
            val localRevision = storage.getCachedVaultRevision(vault)
            val response = httpClient.get("${vault.address}/v1/vault") {
                basicAuth(vault.username, secret)
                parameter("revision", localRevision)
            }

            return when (response.status) {
                HttpStatusCode.OK -> {
                    // First time fetch or server has newer content
                    val serverRevision = response.etag()?.toLongOrNull() ?: 0
                    val data = response.bodyAsBytes()
                    storage.cacheVault(vault, data, serverRevision)
                    Either.Left(data)
                }
                HttpStatusCode.NotModified -> {
                    // Server has same or older content
                    val data = storage.getCachedVault(vault)
                    if (data != null) {
                        Either.Left(data)
                    } else {
                        Either.Right(ErrorResponse(ApiError.VAULT_NOT_INITIALIZED))
                    }
                }
                HttpStatusCode.PreconditionFailed -> {
                    // Vault not initialized
                    Either.Right(ErrorResponse(ApiError.VAULT_NOT_INITIALIZED))
                }
                else -> {
                    // Some other error
                    Either.Right(response.body())
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to cache if anything blew up
            val data = storage.getCachedVault(vault)
            return if (data != null) {
                Either.Left(data)
            } else {
                Either.Right(ErrorResponse(ApiError.NETWORK_ERROR))
            }
        }
    }

    suspend fun getVaultRecovering(vault: Vault, backupSecret: String): Either<ByteArray, ErrorResponse> {
        try {
            val response = httpClient.get("${vault.address}/v1/vault/recovery") {
                basicAuth(vault.username, backupSecret)
            }
            return if (response.status.isSuccess()) {
                Either.Left(response.bodyAsBytes())
            } else if (response.status == HttpStatusCode.PreconditionFailed) {
                Either.Right(ErrorResponse(ApiError.VAULT_NOT_INITIALIZED))
            } else {
                Either.Right(response.body())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Either.Right(ErrorResponse(ApiError.NETWORK_ERROR))
        }
    }

    fun decodeVault(data: ByteArray): Either<DecodedVault,  ErrorResponse> {
        try {
            val buffer = Buffer()
            buffer.write(data)
            val version = buffer.readInt()
            return when {
                version == 1 -> Either.Left(decodeVaultV1(buffer))
                version < 1 -> Either.Right(ErrorResponse(ApiError.VAULT_VERSION_TOO_OLD))
                else -> Either.Right(ErrorResponse(ApiError.VAULT_VERSION_TOO_NEW))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return Either.Right(ErrorResponse(ApiError.CORRUPTED_VAULT))
        }
    }

    private fun decodeVaultV1(buffer: Buffer): DecodedVault {
        val passwordEncryptedData = buffer.readByteArray(buffer.readInt())
        val securityQuestionData = buffer.readByteArray(buffer.readInt())
        val securityQuestions: List<String> = Json.decodeFromString(securityQuestionData.decodeToString())
        val passwordEncryptedBackupPassword = if (securityQuestions.isEmpty()) {
            null
        } else {
            buffer.readByteArray(buffer.readInt())
        }
        val backupPasswordEncryptedData = if (securityQuestions.isEmpty()) {
            null
        } else {
            buffer.readByteArray(buffer.readInt())
        }
        return DecodedVault(
            passwordEncryptedData,
            securityQuestions,
            passwordEncryptedBackupPassword,
            backupPasswordEncryptedData
        )
    }

    suspend fun unlockVaultUsingPasswordHash(
        vault: Vault,
        decodedVault: DecodedVault,
        passwordHashStr: String,
        secretStr: String
    ): Either<UnlockedVault, ErrorResponse> {
        val key = passwordHashStr.substring(0, 32)
        val salt = "salt".repeat(4)

        val decryptedData = try {
            decryptAES256CBC(decodedVault.passwordEncryptedData, key, salt)
        } catch (_: Throwable) {
            return Either.Right(ErrorResponse(ApiError.INVALID_PASSWORD))
        }
        val passwords: PasswordGroup = Json.decodeFromString(decryptedData.decodeToString())

        val passwordEncryptedBackupPassword = decodedVault.passwordEncryptedBackupPassword
        val decryptedBackupPassword = if (passwordEncryptedBackupPassword == null) {
            null
        } else {
            try {
                decryptAES256CBC(passwordEncryptedBackupPassword, key, salt)
            } catch (_: Throwable) {
                return Either.Right(ErrorResponse(ApiError.INVALID_PASSWORD))
            }
        }
        val securityQuestionAnswers: List<String> = if (decryptedBackupPassword == null) {
            listOf()
        } else {
            Json.decodeFromString(decryptedBackupPassword.decodeToString())
        }
        val securityQuestions = decodedVault.securityQuestions.mapIndexed { index, question ->
            SecurityQuestion(question, securityQuestionAnswers[index])
        }

        return Either.Left(UnlockedVault(
            vault.address,
            vault.username,
            passwordHashStr,
            secretStr,
            passwords,
            securityQuestions
        ))
    }

    suspend fun unlockVaultUsingPassword(vault: Vault, password: String): Either<UnlockedVault, ErrorResponse> {
        val passwordHash = hashSHA3256(password.encodeToByteArray())
        val passwordHashStr = passwordHash.toHexString()
        val secret = hashSHA3256(passwordHash)
        val secretStr = secret.toHexString()

        val data = when (val result = getVault(vault, secretStr)) {
            is Either.Left -> result.value
            is Either.Right -> return result
        }
        val decodedVault = when (val result = decodeVault(data)) {
            is Either.Left -> result.value
            is Either.Right -> return result
        }

        return unlockVaultUsingPasswordHash(vault, decodedVault, passwordHashStr, secretStr)
    }

    suspend fun encodeVault(vault: UnlockedVault): ByteArray {
        val key = vault.passwordHash.substring(0, 32)
        val backupPassword = vault.securityQuestions.joinToString(";") { it.answer }
        val backupPasswordHash = hashSHA3256(backupPassword.encodeToByteArray())
        val backupPasswordHashStr = backupPasswordHash.toHexString()
        val backupKey = backupPasswordHashStr.substring(0, 32)
        val salt = "salt".repeat(4)

        val data = Json.encodeToString(PasswordGroup.serializer(), vault.passwords)
        val passwordEncryptedData = encryptAES256CBC(data.encodeToByteArray(), key, salt)
        val securityQuestions = Json.encodeToString(vault.securityQuestions.map(SecurityQuestion::question)).encodeToByteArray()
        val securityQuestionAnswers = Json.encodeToString(vault.securityQuestions.map(SecurityQuestion::answer))
        val passwordEncryptedBackupPassword = encryptAES256CBC(securityQuestionAnswers.encodeToByteArray(), key, salt)
        val backupPasswordEncryptedPasswordHash = encryptAES256CBC(vault.passwordHash.hexToByteArray(), backupKey, salt)

        val buffer = Buffer()
        buffer.writeInt(1) // Version
        buffer.writeInt(passwordEncryptedData.size)
        buffer.write(passwordEncryptedData)
        buffer.writeInt(securityQuestions.size)
        buffer.write(securityQuestions)
        buffer.writeInt(passwordEncryptedBackupPassword.size)
        buffer.write(passwordEncryptedBackupPassword)
        buffer.writeInt(backupPasswordEncryptedPasswordHash.size)
        buffer.write(backupPasswordEncryptedPasswordHash)
        return buffer.readByteArray()
    }

    suspend fun uploadVault(unlockedVault: UnlockedVault): ErrorResponse? {
        val vault = unlockedVault.toVault()
        try {
            val revision = (storage.getCachedVaultRevision(vault) ?: 0) + 1
            val data = encodeVault(unlockedVault)
            val response = httpClient.put("${unlockedVault.address}/v1/vault") {
                basicAuth(unlockedVault.username, unlockedVault.secret)
                contentType(ContentType.Application.OctetStream)
                parameter("revision", revision)
                setBody(ByteArrayContent(data))
            }
            return if (response.status.isSuccess()) {
                storage.cacheVault(vault, data, revision)
                null
            } else {
                response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ErrorResponse(ApiError.NETWORK_ERROR)
        }
    }

    suspend fun updateSecurity(
        vault: Vault,
        oldSecret: String,
        newSecret: String,
        securityQuestions: List<SecurityQuestion>
    ): ErrorResponse? {
        try {
            val backupPassword = securityQuestions.joinToString(";") { it.answer }
            val backupPasswordHash = hashSHA3256(backupPassword.encodeToByteArray())
            val backupSecret = hashSHA3256(backupPasswordHash)
            val response = httpClient.patch("${vault.address}/v1/vault/security") {
                basicAuth(vault.username, oldSecret)
                contentType(ContentType.Application.Json)
                setBody(V1VaultSecurityRequest(
                    newSecret,
                    securityQuestions.map(SecurityQuestion::question),
                    backupSecret.toHexString()
                ))
            }
            return if (response.status.isSuccess()) {
                null
            } else {
                response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ErrorResponse(ApiError.NETWORK_ERROR)
        }
    }

    suspend fun getSecurityQuestions(vault: Vault): Either<List<String>, ErrorResponse> {
        try {
            val response = httpClient.get("${vault.address}/v1/vault/security-questions") {
                url { parameters["name"] = vault.username }
            }
            return if (response.status.isSuccess()) {
                val securityQuestions = response.body<V1VaultSecurityQuestionsResponse>().securityQuestions
                if (securityQuestions.isEmpty()) {
                    Either.Right(ErrorResponse(ApiError.RECOVERY_NOT_POSSIBLE))
                } else {
                    Either.Left(securityQuestions)
                }
            } else {
                Either.Right(response.body())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Either.Right(ErrorResponse(ApiError.NETWORK_ERROR))
        }
    }

    suspend fun unlockVaultUsingSecurityQuestions(vault: Vault, answers: List<String>): Either<UnlockedVault, ErrorResponse> {
        val backupPassword = answers.joinToString(";")
        val backupPasswordHash = hashSHA3256(backupPassword.encodeToByteArray())
        val backupPasswordHashStr = backupPasswordHash.toHexString()
        val backupSecret = hashSHA3256(backupPasswordHash)
        val backupSecretStr = backupSecret.toHexString()
        val backupKey = backupPasswordHashStr.substring(0, 32)
        val salt = "salt".repeat(4)

        val data = when (val result = getVaultRecovering(vault, backupSecretStr)) {
            is Either.Left -> result.value
            is Either.Right -> return result
        }
        val decodedVault = when (val result = decodeVault(data)) {
            is Either.Left -> result.value
            is Either.Right -> return result
        }
        if (decodedVault.backupPasswordEncryptedPasswordHash == null) {
            return Either.Right(ErrorResponse(ApiError.VAULT_NOT_INITIALIZED))
        }

        val passwordHash = decryptAES256CBC(decodedVault.backupPasswordEncryptedPasswordHash, backupKey, salt)
        val passwordHashStr = passwordHash.toHexString()
        val secret = hashSHA3256(passwordHash)
        val secretStr = secret.toHexString()

        return unlockVaultUsingPasswordHash(vault, decodedVault, passwordHashStr, secretStr)
    }
}