package de.fabiexe.spind.app.data

data class UnlockedVault(
    val address: String,
    val username: String,
    val passwordHash: String,
    val secret: String,
    val passwords: PasswordGroup,
    val securityQuestions: List<SecurityQuestion>
) {
    fun sameAddressAndUsername(other: VaultInfo): Boolean {
        return other.address == address && other.username == username
    }

    fun sameAddressAndUsername(other: UnlockedVault): Boolean {
        return other.address == address && other.username == username
    }

    fun toVault() = VaultInfo(address, username, passwordHash)
}