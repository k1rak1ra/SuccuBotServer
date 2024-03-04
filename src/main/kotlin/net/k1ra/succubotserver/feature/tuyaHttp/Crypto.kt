package net.k1ra.succubotserver.feature.tuyaHttp

import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import org.json.JSONObject
import java.security.Key
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class Crypto(robotInfo: RobotInfo) {
    private val secretKey: Key = SecretKeySpec(robotInfo.webKey.decodeHex(), "AES")
    private val cipher = Cipher.getInstance("AES")

    fun decryptRequest(hex: String) : JSONObject {
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return JSONObject(cipher.doFinal(hex.decodeHex()).decodeToString())
    }

    fun decryptResponse(base64: String) : JSONObject {
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return JSONObject(cipher.doFinal(Base64.getDecoder().decode(base64)).decodeToString())
    }

    fun encryptResponse(resp: String) : String {
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return Base64.getEncoder().encodeToString(cipher.doFinal(resp.encodeToByteArray()))
    }

    fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Must have an even length" }

        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}