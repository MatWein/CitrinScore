package de.citrinscore.settings

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64

private const val TRANSFORMATION = "AES/GCM/NoPadding"
private const val IV_BYTES = 12
private const val TAG_BITS = 128
private const val PREFIX = "aes-gcm:"
private val Key = SecretKeySpec(Base64.decode("wxcjzBiyDipNdanFwtMG5wEpa3Lu9uJH8oFQuwqT4wk="), "AES")

actual object TokenCipher {

    actual fun encrypt(token: String): String {
        val iv = ByteArray(IV_BYTES).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, Key, GCMParameterSpec(TAG_BITS, iv)) }
        return PREFIX + Base64.encode(iv + cipher.doFinal(token.encodeToByteArray()))
    }

    actual fun decrypt(stored: String): String {
        if (!isEncrypted(stored)) return stored
        val bytes = Base64.decode(stored.removePrefix(PREFIX))
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.DECRYPT_MODE, Key, GCMParameterSpec(TAG_BITS, bytes, 0, IV_BYTES)) }
        return cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES).decodeToString()
    }

    actual fun isEncrypted(stored: String) = stored.startsWith(PREFIX)
}
