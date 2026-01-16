package de.fabiexe.spind

import java.io.InputStream
import java.io.OutputStream

interface Storage {
    fun exists(vault: String): Boolean
    fun read(vault: String): InputStream
    fun write(vault: String): OutputStream
    fun readSecret(vault: String): String?
    fun writeSecret(vault: String, secret: String)
}