package net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.logging.Logger
import java.net.DatagramPacket
import java.net.DatagramSocket

object TuyaUdpBroadcastListener {
    fun listenForBroadcast() = CoroutineScope(Dispatchers.IO).launch {
        val buffer = ByteArray(172)
        val socket = DatagramSocket(6667, null)
        val packet = DatagramPacket(buffer, buffer.size)

        while(true) {
            try {
                socket.receive(packet)

                val message = TuyaMessage(packet.data, "6c1ec8e2bb9bb59ab50b0daf649b410a")
                Logger.log("Broadcast", message.toString())

                RobotInfo.updateFromBroadcast(message.payload.getString("ip"), message.payload.getString("gwId"))
            } catch (e: Exception) {
                Logger.log("Broadcast-ERR", e.toString())
            }
        }
    }
}