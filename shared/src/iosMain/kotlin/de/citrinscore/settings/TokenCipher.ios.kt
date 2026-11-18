package de.citrinscore.settings

actual object TokenCipher {
    actual fun encrypt(token: String): String = throw UnsupportedOperationException("Synchronization is not available on iOS")

    actual fun decrypt(stored: String): String = throw UnsupportedOperationException("Synchronization is not available on iOS")

    actual fun isEncrypted(stored: String) = false
}
