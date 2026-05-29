package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * End-to-End Encryption engine providing:
 * 1. AES-GCM 256-bit symmetric encryption/decryption of transit-level chat payloads.
 * 2. SHA-256 fingerprint generation for secure handshakes between peer devices.
 * 3. RSA/AES public-private key simulation for low-latency decentralized sessions.
 */
object E2eeEngine {
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    /**
     * Generates a secure random 256-bit AES key.
     */
    fun generateSessionKey(): ByteArray {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(AES_KEY_SIZE)
        return keyGen.generateKey().encoded
    }

    /**
     * Generates a readable verification fingerprint from a session key using SHA-256.
     * Splitting into 4-character colon-separated chunks (e.g. "AE:45:9C:F1...").
     */
    fun getFingerprint(key: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(key)
        return hash.take(12)
            .joinToString(":") { String.format("%02X", it) }
    }

    /**
     * Derives a 256-bit AES key from a passphrase (useful for passphrase-locked rooms).
     */
    fun deriveKeyFromPassphrase(passphrase: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(passphrase.toByteArray(Charsets.UTF_8))
    }

    /**
     * Encrypts plain text using AES-GCM 256-bit.
     * Returns a formatted Base64 string that contains both the random IV and the ciphertext.
     */
    fun encrypt(plainText: String, secretKey: ByteArray): String {
        return try {
            val keySpec = SecretKeySpec(secretKey, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            
            // Generate a random 12-byte IV for GCM
            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            SecureRandom().nextBytes(iv)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            // Combine IV and Ciphertext for delivery in transit
            val combined = ByteArray(iv.size + cipherBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherBytes, 0, combined, iv.size, cipherBytes.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            "ENCRYPTION_ERROR: ${e.message}"
        }
    }

    /**
     * Decrypts a combined Base64 string (IV + Ciphertext) using AES-GCM 256-bit.
     */
    fun decrypt(base64Payload: String, secretKey: ByteArray): String {
        return try {
            val keySpec = SecretKeySpec(secretKey, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            
            val decoded = Base64.decode(base64Payload, Base64.NO_WRAP)
            if (decoded.size <= GCM_IV_LENGTH_BYTES) {
                return "DECRYPTION_ERROR: Invalid payload length"
            }
            
            // Extract IV
            val iv = ByteArray(GCM_IV_LENGTH_BYTES)
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH_BYTES)
            
            // Extract Ciphertext
            val ciphertextLength = decoded.size - GCM_IV_LENGTH_BYTES
            val ciphertext = ByteArray(ciphertextLength)
            System.arraycopy(decoded, GCM_IV_LENGTH_BYTES, ciphertext, 0, ciphertextLength)
            
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            
            val decryptedBytes = cipher.doFinal(ciphertext)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "DECRYPTION_ERROR: Authentic E2EE integrity check failed"
        }
    }
}
