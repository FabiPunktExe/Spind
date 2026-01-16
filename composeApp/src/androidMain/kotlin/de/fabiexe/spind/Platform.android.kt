package de.fabiexe.spind

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

actual fun hashSHA3256(data: ByteArray): ByteArray {
    return MessageDigest.getInstance("SHA3-256").digest(data)
}

private fun createAES256CBCKey(password: String, salt: String): SecretKey {
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val spec = PBEKeySpec(password.toCharArray(), salt.toByteArray(), 65536, 256)
    val secret = SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    return secret
}

actual suspend fun encryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray {
    val key = createAES256CBCKey(key, salt)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(salt.toByteArray()))
    return cipher.doFinal(data)
}

actual suspend fun decryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray {
    val key = createAES256CBCKey(key, salt)
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(salt.toByteArray()))
    return cipher.doFinal(data)
}

actual fun String.toClipEntry(): ClipEntry {
    return ClipEntry(ClipData(this, arrayOf("text/plain"), ClipData.Item(this)))
}