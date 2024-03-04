package net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotStatus
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.logging.Logger
import net.k1ra.succubotserver.feature.tuyaHttp.Crypto
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttClientPersistence
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.security.Key
import java.security.KeyStore
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory

object TuyaMqtt {
    private val publisherId = "SUCCUBOT-SERVER at ${ServerSetting.getCurrentBaseUrl()}"
    var client: MqttClient? = null
    var mqttError: String? = null

    fun startMqttListener() {
        try {
            mqttError = null
            val socketFactory = getSocketFactory()

            val connOpts = MqttConnectOptions()
            connOpts.socketFactory = socketFactory
            connOpts.isCleanSession = true
            connOpts.sslHostnameVerifier = HostnameVerifier { _, _ -> true } //All hosts valid

            stopMqttListener()
            client = MqttClient(ServerSetting.getMqttServer(), publisherId, MemoryPersistence())
            client?.connect(connOpts)

            if (client?.isConnected == true) {
                Logger.log("MQTT", "MQTT initialized")

                RobotInfo.getAllRobots().forEach {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val secretKey: Key = SecretKeySpec(it.mqttKey.decodeHex(), "AES")
                            val cipher = Cipher.getInstance("AES")
                            cipher.init(Cipher.DECRYPT_MODE, secretKey)

                            client!!.subscribe("smart/device/out/${it.did}") { _, msg ->
                                try {
                                    val payloadBody = msg.payload.takeLast(msg.payload.size - 15)
                                    val obj = JSONObject(cipher.doFinal(payloadBody.toByteArray()).decodeToString())
                                    Logger.log("MQTT", obj.toString())

                                    CoroutineScope(Dispatchers.IO).launch {
                                        try {
                                            RobotStatus.updateStatusFromMqtt(
                                                obj.getJSONObject("data").getJSONObject("dps"),
                                                it.did
                                            )
                                        } catch (e: Exception) {
                                            //Will be thrown if no fields to update, ignore...
                                        }
                                    }
                                } catch (e: Exception) {
                                    Logger.log(
                                        "MQTT-ERR",
                                        "Failed to decode message from ${it.did}. Is the encryption key correct?"
                                    )
                                }
                            }

                            Logger.log("MQTT", "Subscribed to ${it.did}")
                        } catch (e: Exception) {
                            Logger.log("MQTT-ERR", "Failed to subscribe to ${it.did}: ${e.stackTraceToString()}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Logger.log("MQTT-ERR", "Failed to start: ${e.stackTraceToString()}")
            mqttError = e.stackTraceToString()
        }
    }

    fun stopMqttListener() {
        try {
            client?.disconnect(1000)
        } catch (_: Exception) {}
        client = null
        Logger.log("MQTT", "MQTT disconnected")
    }

    private fun getSocketFactory(): SSLSocketFactory {
        Security.addProvider(BouncyCastleProvider())

        //Load CA certificate
        val cf = CertificateFactory.getInstance("X.509")
        val caCert = cf.generateCertificate(ServerSetting.getMqttCertificate().byteInputStream()) as X509Certificate

        //CA certificate is used to authenticate server
        val caKs = KeyStore.getInstance(KeyStore.getDefaultType())
        caKs.load(null, null)
        caKs.setCertificateEntry("ca-certificate", caCert)
        val tmf = TrustManagerFactory.getInstance("X509")
        tmf.init(caKs)

        //Finally, create SSL socket factory
        val context = SSLContext.getInstance("TLSv1.2")
        context.init(null, tmf.trustManagers, null)

        return context.socketFactory
    }
}