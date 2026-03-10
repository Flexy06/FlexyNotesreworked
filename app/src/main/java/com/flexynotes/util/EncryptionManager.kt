package com.flexynotes.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256"
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val TAG_LENGTH = 128

    // Generates a 256-bit secret key from the user password and a random salt
    private fun generateKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM)
        val secretKeyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretKeyBytes, "AES")
    }

    // Encrypts plain text using a master password and returns a Base64 string
    fun encrypt(plainText: String, password: String): String {
        val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(IV_LENGTH).apply { SecureRandom().nextBytes(this) }

        val secretKey = generateKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))

        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Combine salt, IV, and ciphertext into a single byte array for easy storage
        val combined = ByteArray(salt.size + iv.size + cipherText.size)
        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(cipherText, 0, combined, salt.size + iv.size, cipherText.size)

        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    // Decrypts the encrypted Base64 string back to plain text using the master password
    fun decrypt(encryptedBase64: String, password: String): String {
        val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)

        val salt = ByteArray(SALT_LENGTH)
        val iv = ByteArray(IV_LENGTH)
        val cipherText = ByteArray(combined.size - SALT_LENGTH - IV_LENGTH)

        System.arraycopy(combined, 0, salt, 0, SALT_LENGTH)
        System.arraycopy(combined, SALT_LENGTH, iv, 0, IV_LENGTH)
        System.arraycopy(combined, SALT_LENGTH + IV_LENGTH, cipherText, 0, cipherText.size)

        val secretKey = generateKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH, iv))

        val plainTextBytes = cipher.doFinal(cipherText)
        return String(plainTextBytes, Charsets.UTF_8)
    }
}