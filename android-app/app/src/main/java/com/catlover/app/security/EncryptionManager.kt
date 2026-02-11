package com.catlover.app.security

import android.util.Base64
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
    private const val RSA_ALGORITHM = "RSA/ECB/PKCS1Padding"
    private const val AES_ALGORITHM = "AES/GCM/NoPadding"

    // Генерирует пару RSA ключей для пользователя
    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        return kpg.generateKeyPair()
    }

    // Шифрует AES ключ с помощью публичного RSA ключа получателя
    fun encryptSecretKey(secretKey: SecretKey, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedKey = cipher.doFinal(secretKey.encoded)
        return Base64.encodeToString(encryptedKey, Base64.NO_WRAP)
    }

    // Расшифровывает AES ключ с помощью приватного RSA ключа
    fun decryptSecretKey(encryptedKeyBase64: String, privateKey: PrivateKey): SecretKey {
        val encryptedKey = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(RSA_ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decryptedKey = cipher.doFinal(encryptedKey)
        return SecretKeySpec(decryptedKey, "AES")
    }

    // Генерирует временный AES ключ для сообщения
    fun generateAESKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        return keyGen.generateKey()
    }

    // Шифрует текст сообщения с помощью AES
    fun encryptMessage(text: String, secretKey: SecretKey): Pair<String, String> {
        val cipher = Cipher.getInstance(AES_ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(text.toByteArray())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encryptedText = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        return Pair(encryptedText, iv)
    }

    // Восстанавливает Public Key из строки
    fun getPublicKeyFromString(keyString: String): PublicKey {
        val publicBytes = Base64.decode(keyString, Base64.NO_WRAP)
        val keySpec = X509EncodedKeySpec(publicBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec)
    }
}
