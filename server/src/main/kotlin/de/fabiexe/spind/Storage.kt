package de.fabiexe.spind

import java.io.InputStream
import java.io.OutputStream

interface Storage {
    fun exists(vault: String): Boolean
    fun read(vault: String): InputStream
    fun write(vault: String): OutputStream
    fun readSecret(vault: String): String?
    fun writeSecret(vault: String, secret: String)
    fun readSecurityQuestions(vault: String): List<String>?
    fun writeSecurityQuestions(vault: String, questions: List<String>)
    fun readBackupSecret(vault: String): String?
    fun writeBackupSecret(vault: String, backupSecret: String?)
    fun readRevision(vault: String): Long
    fun writeRevision(vault: String, revision: Long)
}