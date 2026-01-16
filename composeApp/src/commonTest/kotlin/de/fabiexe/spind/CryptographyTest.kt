package de.fabiexe.spind

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CryptographyTest {
    @Test
    fun testSHA3256() = runTest {
        assertEquals(
            "716153e0441ce21dbbf2526449a5cbaa0c74ba423a6362ce79022891354d19de",
            hashSHA3256("example text".encodeToByteArray()).toHexString()
        )
    }

    @Test
    fun testEncrypt() = runTest {
        assertEquals(
            "b7842b662a28c49e80a12a0ca4662b88",
            encryptAES256CBC(
                "example text".encodeToByteArray(),
                "passwordpasswordpasswordpassword",
                "saltsaltsaltsalt"
            ).toHexString()
        )
    }

    @Test
    fun testDecrypt() = runTest {
        assertEquals(
            "example text",
            decryptAES256CBC(
                "b7842b662a28c49e80a12a0ca4662b88".hexToByteArray(),
                "passwordpasswordpasswordpassword",
                "saltsaltsaltsalt"
            ).decodeToString()
        )
    }
}