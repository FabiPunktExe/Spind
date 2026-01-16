package de.fabiexe.spind

expect fun getCookies(): Map<String, String>
expect fun getCookie(name: String): String?
expect fun setCookie(name: String, value: String)