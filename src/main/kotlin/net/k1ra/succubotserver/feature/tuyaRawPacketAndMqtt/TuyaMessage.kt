package net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt

import org.json.JSONObject
import java.security.Key
import java.util.zip.CRC32
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class TuyaMessage(val bytes: ByteArray, key: String, payloadOffset: Int = 0) {

    val header: TuyaMessageHeader
    val retCode: String
    val crc: UInt
    val payload: JSONObject

    init {
        val data = bytes.toList()
        header = TuyaMessageHeader(data)
        retCode = data.subList(16, 20).toByteArray().toHex()

        val crc32 = CRC32()
        crc32.update(data.subList(0,header.totalLength-8).toByteArray())
        crc = data.subList(header.totalLength-8, header.totalLength-4).toUInt()

        if (crc32.value.toUInt() != crc)
            throw Exception("CRC does not match")

        val secretKey: Key = SecretKeySpec(key.decodeHex(), "AES")
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)

        payload = JSONObject(cipher.doFinal(data.subList(20 + payloadOffset, header.totalLength - 8).toByteArray()).decodeToString())
    }

    override fun toString(): String {
        return "{ header: $header, retCode: $retCode, crc: $crc, payload: $payload }"
    }

    companion object {
        @OptIn(ExperimentalStdlibApi::class)
        fun buildCommandFromPayload(payload: JSONObject, key: String, payloadOffset: Int = 0) : TuyaMessage {
            val secretKey: Key = SecretKeySpec(key.decodeHex(), "AES")
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val payloadBuilder = StringBuilder()
            for (i in 0..<payloadOffset)
                payloadBuilder.append("00")

            payloadBuilder.append(cipher.doFinal(payload.toString().encodeToByteArray()).toHex())
            val header = TuyaMessageHeader(payloadBuilder.toString())
            val retCode = "00000000"

            val dataBeforeCrc = "${header.toHexString()}$retCode$payloadBuilder"

            val crc32 = CRC32()
            crc32.update(dataBeforeCrc.decodeHex())
            val crc = crc32.value.toUInt().toHexString()

            val finalData = "$dataBeforeCrc${crc}0000aa55"

            return TuyaMessage(finalData.decodeHex(), key, payloadOffset)
        }
    }

    class TuyaMessageHeader(
        val seqNo: Int,
        val cmd: Int,
        val payloadLength: Int,
        val totalLength: Int) {


        constructor(payload: String) : this(
            0,
            13,
            payload.length/2 + 12,
            payload.length/2 + 12 + 16
        )

        constructor(data: List<Byte>) : this(
            data.subList(4, 8).toUInt().toInt(),
            data.subList(8, 12).toUInt().toInt(),
            data.subList(12, 16).toUInt().toInt(),
            data.subList(12, 16).toUInt().toInt() + 16
        )

        @OptIn(ExperimentalStdlibApi::class)
        fun toHexString() : String {
            return "000055aa${seqNo.toHexString()}${cmd.toHexString()}${payloadLength.toHexString()}"
        }

        override fun toString(): String {
            return "{seqNo: $seqNo, cmd: $cmd, payloadLength: $payloadLength, totalLength: $totalLength}"
        }
    }
}

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun String.decodeHex(): ByteArray {
    check(length % 2 == 0) { "Must have an even length" }

    return chunked(2)
        .map { it.toInt(16).toByte() }
        .toByteArray()
}

fun List<Byte>.toUInt(): UInt {
    return (this[0].toUInt() and 255u shl 24) or
            (this[1].toUInt() and 255u shl 16) or
            (this[2].toUInt() and 255u shl 8) or
            (this[3].toUInt() and 255u)
}