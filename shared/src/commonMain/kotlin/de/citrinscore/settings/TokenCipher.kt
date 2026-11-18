package de.citrinscore.settings

expect object TokenCipher {
    fun encrypt(token: String): String

    fun decrypt(stored: String): String

    fun isEncrypted(stored: String): Boolean
}
