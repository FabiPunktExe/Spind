package de.fabiexe.spind.app.data

class DecodedVault(
    val passwordEncryptedData: ByteArray,
    val securityQuestions: List<String>,
    val passwordEncryptedBackupPassword: ByteArray?,
    val backupPasswordEncryptedPasswordHash: ByteArray?
)