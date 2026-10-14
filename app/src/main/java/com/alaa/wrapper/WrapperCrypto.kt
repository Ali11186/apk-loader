package com.alaa.wrapper

import android.content.Context
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object WrapperCrypto {

    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val PBKDF2_ITERATIONS = 250_000
    private const val KEY_BITS = 256

    fun decryptFromAssets(context: Context, password: String, outputFile: File): File {
        val all = context.assets.open("payload.enc").use { it.readBytes() }

        require(all.size > SALT_LEN + IV_LEN + 16) { "payload too small" }

        val salt = all.copyOfRange(0, SALT_LEN)
        val iv = all.copyOfRange(SALT_LEN, SALT_LEN + IV_LEN)
        val ciphertext = all.copyOfRange(SALT_LEN + IV_LEN, all.size)

        val key = deriveKey(password.toCharArray(), salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))

        val plain = cipher.doFinal(ciphertext)
        outputFile.writeBytes(plain)
        return outputFile
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }
}
