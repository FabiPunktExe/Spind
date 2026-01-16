package de.fabiexe.spind

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.ClipboardItem
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.khronos.webgl.toInt8Array
import org.kotlincrypto.hash.sha3.SHA3_256
import org.w3c.dom.Window
import kotlin.js.Promise

external class TextEncoder {
    fun encode(input: String): Int8Array
}

@OptIn(ExperimentalWasmJsInterop::class)
fun createPbkdf2Params(name: String, salt: Int8Array, iterations: Int, hash: String): JsAny =
    js("({ name: name, salt: salt, iterations: iterations, hash: hash })")

@OptIn(ExperimentalWasmJsInterop::class)
fun createAesDerivedKeyParams(name: String, length: Int): JsAny =
    js("( {name: name, length: length })")

@OptIn(ExperimentalWasmJsInterop::class)
fun createAesCbcParams(name: String, iv: Int8Array): JsAny =
    js("({ name: name, iv: iv })")

external interface Subtle {
    @OptIn(ExperimentalWasmJsInterop::class)
    fun importKey(
        format: String,
        keyData: Int8Array,
        algorithm: String,
        extractable: Boolean,
        keyUsages: JsArray<JsString>
    ): Promise<JsAny>

    @OptIn(ExperimentalWasmJsInterop::class)
    fun deriveKey(
        algorithm: JsAny,
        baseKey: JsAny,
        derivedKeyType: JsAny,
        extractable: Boolean,
        keyUsages: JsArray<JsString>
    ): Promise<JsAny>

    @OptIn(ExperimentalWasmJsInterop::class)
    fun encrypt(algorithm: JsAny, key: JsAny, data: Int8Array): Promise<ArrayBuffer>

    @OptIn(ExperimentalWasmJsInterop::class)
    fun decrypt(algorithm: JsAny, key: JsAny, data: Int8Array): Promise<ArrayBuffer>
}

external interface Crypto {
    val subtle: Subtle
}

external interface CryptoWindow {
    val crypto: Crypto
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
val Window.crypto: Crypto
    get() = (this as CryptoWindow).crypto

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

@OptIn(ExperimentalWasmJsInterop::class)
suspend fun createAES256CBCKey(password: String, salt: Int8Array): JsAny {
    val baseKey: JsAny = window.crypto.subtle.importKey(
        "raw",
        TextEncoder().encode(password),
        "PBKDF2",
        false,
        listOf("deriveKey".toJsString()).toJsArray()
    ).await()
    return window.crypto.subtle.deriveKey(
        createPbkdf2Params("PBKDF2", salt, 65536, "SHA-256"),
        baseKey,
        createAesDerivedKeyParams("AES-CBC", 256),
        true,
        listOf("encrypt".toJsString(), "decrypt".toJsString()).toJsArray()
    ).await()
}

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun encryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray {
    val salt = salt.encodeToByteArray().toInt8Array()
    val encrypted: ArrayBuffer = window.crypto.subtle.encrypt(
        createAesCbcParams("AES-CBC", salt),
        createAES256CBCKey(key, salt),
        TextEncoder().encode(data.decodeToString())
    ).await()
    return Int8Array(encrypted).toByteArray()
}

@OptIn(ExperimentalWasmJsInterop::class)
actual suspend fun decryptAES256CBC(data: ByteArray, key: String, salt: String): ByteArray {
    val salt = salt.encodeToByteArray().toInt8Array()
    val decrypted: ArrayBuffer = window.crypto.subtle.decrypt(
        createAesCbcParams("AES-CBC", salt),
        createAES256CBCKey(key, salt),
        data.toInt8Array()
    ).await()
    return Int8Array(decrypted).toByteArray()
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
private fun createClipboardItem(text: String): ClipboardItem =
    js("new ClipboardItem({'text/plain': new Blob([text], { type: 'text/plain' })})")

@OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)
actual fun String.toClipEntry(): ClipEntry {
    return ClipEntry(arrayOf(createClipboardItem(this)).toJsArray())
}