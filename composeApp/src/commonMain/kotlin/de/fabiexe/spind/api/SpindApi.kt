package de.fabiexe.spind.api

import de.fabiexe.spind.*
import de.fabiexe.spind.data.*
import de.fabiexe.spind.endpoint.ErrorResponse
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

class SpindApi(httpClientEngineFactory: HttpClientEngineFactory<*>) {
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
            val response = httpClient.get("${vault.address}/v1/vault") {
                basicAuth(vault.username, secret)
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
            return Either.Right(ErrorResponse(ApiError.UNKNOWN_ERROR))
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

    suspend fun unlockVaultUsingPassword(vault: Vault, password: String): Either<UnlockedVault, ErrorResponse> {
        val passwordHash = hashSHA3256(password.encodeToByteArray()).toHexString()
        val secret = hashSHA3256(passwordHash.encodeToByteArray()).toHexString()
        val key = passwordHash.substring(0, 32)
        val salt = "salt".repeat(4)

        val data = when (val result = getVault(vault, secret)) {
            is Either.Left -> result.value
            is Either.Right -> return result
        }
        val decodedVault = when (val result = decodeVault(data)) {
            is Either.Left -> result.value
            is Either.Right -> return result
        }
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
            passwordHash,
            secret,
            passwords,
            securityQuestions
        ))
    }

    suspend fun encodeVault(vault: UnlockedVault): ByteArray {
        val password = vault.passwordHash.substring(0, 32)
        var backupPassword = vault.securityQuestions.joinToString(";") { it.answer }
        backupPassword = hashSHA3256(backupPassword.encodeToByteArray()).toHexString()
        backupPassword = backupPassword.substring(0, 32)
        val salt = "salt".repeat(4)

        val data = Json.encodeToString(PasswordGroup.serializer(), vault.passwords)
        val passwordEncryptedData = encryptAES256CBC(data.encodeToByteArray(), password, salt)
        val securityQuestions = Json.encodeToString(vault.securityQuestions.map(SecurityQuestion::question)).encodeToByteArray()
        val securityQuestionAnswers = Json.encodeToString(vault.securityQuestions.map(SecurityQuestion::answer))
        val passwordEncryptedBackupPassword = encryptAES256CBC(securityQuestionAnswers.encodeToByteArray(), password, salt)
        val backupPasswordEncryptedPasswordHash = encryptAES256CBC(vault.passwordHash.encodeToByteArray(), backupPassword, salt)

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

    suspend fun uploadVault(vault: UnlockedVault): ErrorResponse? {
        try {
            val data = encodeVault(vault)
            val response = httpClient.put("${vault.address}/v1/vault") {
                basicAuth(vault.username, vault.secret)
                contentType(ContentType.Application.OctetStream)
                setBody(ByteArrayContent(data, ContentType.Application.OctetStream))
            }
            return if (response.status.isSuccess()) {
                null
            } else {
                response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ErrorResponse(ApiError.UNKNOWN_ERROR)
        }
    }

    suspend fun changeSecret(vault: UnlockedVault, newSecret: String): ErrorResponse? {
        try {
            val response = httpClient.patch("${vault.address}/v1/vault/secret") {
                basicAuth(vault.username, vault.secret)
                setBody(newSecret)
            }
            return if (response.status.isSuccess()) {
                null
            } else {
                response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ErrorResponse(ApiError.UNKNOWN_ERROR)
        }
    }
}