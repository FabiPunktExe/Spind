package de.fabiexe.spind.data

class DecodedVault(
    val passwordEncryptedData: ByteArray,
    val securityQuestions: List<String>,
    val passwordEncryptedBackupPassword: ByteArray?,
    val backupPasswordEncryptedData: ByteArray?
)