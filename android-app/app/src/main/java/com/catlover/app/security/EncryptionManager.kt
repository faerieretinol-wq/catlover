package com.catlover.app.security

import android.util.Base64
import java.security.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionManager {
    fun generateKeyPair(): KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    
    fun encryptSecretKey(key: SecretKey, pub: PublicKey): String = 
        Base64.encodeToString(
            Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").apply { 
                init(Cipher.ENCRYPT_MODE, pub) 
            }.doFinal(key.encoded), 
            Base64.NO_WRAP
        )
    
    fun decryptSecretKey(enc: String, priv: PrivateKey): SecretKey = 
        SecretKeySpec(
            Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding").apply { 
                init(Cipher.DECRYPT_MODE, priv) 
            }.doFinal(Base64.decode(enc, Base64.NO_WRAP)), 
            "AES"
        )
    
    fun encryptMessage(text: String, key: SecretKey): Pair<String, String> {
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
        return Pair(
            Base64.encodeToString(c.doFinal(text.toByteArray()), Base64.NO_WRAP), 
            Base64.encodeToString(c.iv, Base64.NO_WRAP)
        )
    }
}
