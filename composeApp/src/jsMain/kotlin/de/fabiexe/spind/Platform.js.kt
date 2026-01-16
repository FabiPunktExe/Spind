package de.fabiexe.spind

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardItem
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.js.JsPlainObject
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.kotlincrypto.hash.sha3.SHA3_256
import org.w3c.dom.Window
import kotlin.js.Promise

external class TextEncoder {
    fun encode(input: String): Int8Array
}

@JsPlainObject
external interface Pbkdf2Params {
    val name: String
    val salt: Int8Array
    val iterations: Int
    val hash: String
}

@JsPlainObject
external interface AesDerivedKeyParams {
    val name: String
    val length: Int
}

@JsPlainObject
external interface AesCbcParams {
    val name: String
    val iv: Int8Array
}

external interface Subtle {
    fun importKey(
        format: String,
        keyData: Int8Array,
        algorithm: String,
        extractable: Boolean,
        keyUsages: Array<String>
    ): Promise<dynamic>

    fun deriveKey(
        algorithm: Pbkdf2Params,
        baseKey: dynamic,
        derivedKeyType: AesDerivedKeyParams,
        extractable: Boolean,
        keyUsages: Array<String>
    ): Promise<dynamic>

    fun encrypt(algorithm: AesCbcParams, key: dynamic, data: Int8Array): Promise<ArrayBuffer>

    fun decrypt(algorithm: AesCbcParams, key: dynamic, data: Int8Array): Promise<ArrayBuffer>
}

external interface Crypto {
    val subtle: Subtle
}

val Window.crypto: Crypto
    get() = asDynamic().crypto

external fun encodeURIComponent(str: String): String
external fun decodeURIComponent(str: String): String

actual fun getCookies(): Map<String, String> {
    val cookies = mutableMapOf<String, String>()
    for (cookie in document.cookie.split(";")) {
        val decodedCookie = cookie.trim()
            .split("=")
            .map(::decodeURIComponent)
        if (decodedCookie.size == 2) {
            cookies[decodedCookie[0]] = decodedCookie[1]
        }
    }
    return cookies.toMap()
}

actual fun getCookie(name: String): String? = getCookies()[name]

actual fun setCookie(name: String, value: String) {
    document.cookie = "${encodeURIComponent(name)}=${encodeURIComponent(value)}"
}

actual fun hashSHA3256(data: ByteArray): ByteArray {
    return SHA3_256().digest(data)
}

private suspend fun createAES256CBCKey(password: String, salt: Int8Array): dynamic {
    val baseKey = window.crypto.subtle.importKey(
        "raw",
        TextEncoder().encode(password),
        "PBKDF2",
        false,
        arrayOf("deriveKey")
    ).await()
    return window.crypto.subtle.deriveKey(
        Pbkdf2Params("PBKDF2", salt, 65536, "SHA-256"),
        baseKey,
        AesDerivedKeyParams("AES-CBC", 256),
        true,
        arrayOf("encrypt", "decrypt")
    ).await()
}

actual suspend fun encryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray {
    val salt = salt.encodeToByteArray().unsafeCast<Int8Array>()
    val encrypted: ArrayBuffer = window.crypto.subtle.encrypt(
        AesCbcParams("AES-CBC", salt),
        createAES256CBCKey(key, salt),
        TextEncoder().encode(data.decodeToString())
    ).await()
    return Int8Array(encrypted).unsafeCast<ByteArray>()
}

actual suspend fun decryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray {
    val salt = salt.encodeToByteArray().unsafeCast<Int8Array>()
    val decrypted: ArrayBuffer = window.crypto.subtle.decrypt(
        AesCbcParams("AES-CBC", salt),
        createAES256CBCKey(key, salt),
        data.unsafeCast<Int8Array>()
    ).await()
    return Int8Array(decrypted).unsafeCast<ByteArray>()
}

@OptIn(ExperimentalComposeUiApi::class)
private fun createClipboardItem(text: String): ClipboardItem =
    js("new ClipboardItem({'text/plain': new Blob([text], { type: 'text/plain' })})")

@OptIn(ExperimentalComposeUiApi::class)
actual fun String.toClipEntry(): ClipEntry {
    return ClipEntry(arrayOf(createClipboardItem(this)))
}