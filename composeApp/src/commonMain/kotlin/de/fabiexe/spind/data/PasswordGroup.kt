package de.fabiexe.spind.data

import kotlinx.serialization.Serializable

@Serializable
data class PasswordGroup(
    val name: String,
    val groups: List<PasswordGroup>,
    val passwords: List<Password>
) {
    val totalSize: Int
        get() = passwords.size + groups.sumOf(PasswordGroup::totalSize)

    operator fun get(index: Int): Password {
        var i = 0
        for (group in groups) {
            try {
                return group[i]
            } catch (_: IndexOutOfBoundsException) {
                i += group.totalSize
            }
        }
        for (password in passwords) {
            if (i == index) {
                return password
            } else {
                i++
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun set(index: Int, password: Password): PasswordGroup {
        var i = 0
        for (group in groups.indices) {
            val totalSize = groups[group].totalSize
            try {
                val newGroups = groups.toMutableList()
                newGroups[group] = groups[group].set(index - i, password)
                return copy(groups = newGroups)
            } catch (_: IndexOutOfBoundsException) {
                i += totalSize
            }
        }
        for (p in passwords.indices) {
            if (i == index) {
                val newPasswords = passwords.toMutableList()
                newPasswords[p] = password
                return copy(passwords = newPasswords)
            } else {
                i++
            }
        }
        throw IndexOutOfBoundsException()
    }

    operator fun plus(password: Password): PasswordGroup {
        return copy(passwords = passwords + password)
    }

    operator fun minus(index: Int): PasswordGroup {
        var i = 0
        for (group in groups.indices) {
            val totalSize = groups[group].totalSize
            try {
                val newGroups = groups.toMutableList()
                newGroups[group] = groups[group] - (index - i)
                return copy(groups = newGroups)
            } catch (_: IndexOutOfBoundsException) {
                i += totalSize
            }
        }
        for (p in passwords.indices) {
            if (i == index) {
                val newPasswords = passwords.toMutableList()
                newPasswords.removeAt(p)
                return copy(passwords = newPasswords)
            } else {
                i++
            }
        }
        throw IndexOutOfBoundsException()
    }
}