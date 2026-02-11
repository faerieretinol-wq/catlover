package com.catlover.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class E2EKeyManager(private val context: Context) {
    private val alias = "catlover_identity_key"
    private val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun getOrCreateIdentityPublicKey(): String {
        val keyPair = loadKeyPair() ?: generateKeyPair()
        return Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
    }

    private fun loadKeyPair(): KeyPair? {
        val entry = ks.getEntry(alias, null) as? KeyStore.PrivateKeyEntry ?: return null
        return KeyPair(entry.certificate.publicKey, entry.privateKey)
    }

    private fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_AGREE_KEY)
            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
            .setUserAuthenticationRequired(false)
            .build()
        kpg.initialize(spec)
        return kpg.generateKeyPair()
    }

    // --- E2E Encryption Logic ---

    private fun deriveSharedSecret(theirPublicKeyBase64: String): ByteArray {
        val myPrivateKey = (ks.getEntry(alias, null) as KeyStore.PrivateKeyEntry).privateKey
        val kf = KeyFactory.getInstance("EC")
        val theirPublicKey = kf.generatePublic(X509EncodedKeySpec(Base64.decode(theirPublicKeyBase64, Base64.DEFAULT)))
        
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(myPrivateKey)
        ka.doPhase(theirPublicKey, true)
        return ka.generateSecret()
    }

    fun encrypt(text: String, theirPublicKeyBase64: String): String {
        val secret = deriveSharedSecret(theirPublicKeyBase64)
        val secretKey = SecretKeySpec(secret.take(16).toByteArray(), "AES") // Use first 128 bits
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(text.toByteArray())
        
        // Result: IV + Ciphertext
        return Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, theirPublicKeyBase64: String): String {
        return try {
            val secret = deriveSharedSecret(theirPublicKeyBase64)
            val secretKey = SecretKeySpec(secret.take(16).toByteArray(), "AES")
            
            val data = Base64.decode(encryptedBase64, Base64.DEFAULT)
            val iv = data.take(12).toByteArray() // GCM standard IV is 12 bytes
            val ciphertext = data.drop(12).toByteArray()
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext))
        } catch (e: Exception) {
            "[Decryption Error]"
        }
    }
}
