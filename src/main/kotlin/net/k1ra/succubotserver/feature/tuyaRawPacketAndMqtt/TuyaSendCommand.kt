package net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt

import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.logging.Logger
import org.json.JSONObject
import java.net.Socket

class TuyaSendCommand(private val robot: RobotInfo) {
    //Commands to start vac cycle
    val setMapCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"16\":\"get_both\"}}"), robot.mqttKey, 11)
    val startCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"1\":true}} }"), robot.mqttKey, 11)

    //Command to pause vac cycle
    val pauseCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"1\":false}} }"), robot.mqttKey, 11)

    //Command to go charge
    val gotoChargeCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"3\":true}} }"), robot.mqttKey, 11)

    //Command to stop going to charge
    val stopGotoChargeCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"3\":false}} }"), robot.mqttKey, 11)

    //Continue cleaning after charge commands
    val continueCleaningAfterChargeCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"27\":true}} }"), robot.mqttKey, 11)
    val stopCleaningAfterChargeCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"27\":false}} }"), robot.mqttKey, 11)

    //Commands to set suction level
    val lowSuctionCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"9\":\"gentle\"}} }"), robot.mqttKey, 11)
    val normalSuctionCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"9\":\"normal\"}} }"), robot.mqttKey, 11)
    val strongSuctionCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"9\":\"strong\"}} }"), robot.mqttKey, 11)

    //Commands to set water level
    val lowWaterCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"10\":\"low\"}} }"), robot.mqttKey, 11)
    val normalWaterCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"10\":\"middle\"}} }"), robot.mqttKey, 11)
    val highWaterCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"10\":\"high\"}} }"), robot.mqttKey, 11)

    //Commands to set volume
    val volumeMuteCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"120\":\"qgACAwAA\"}} }"), robot.mqttKey, 11)
    val volumeLowCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"120\":\"qgACAzIA\"}} }"), robot.mqttKey, 11)
    val volumeMediumCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"120\":\"qgACA0YA\"}} }"), robot.mqttKey, 11)
    val volumeHighCommand = TuyaMessage.buildCommandFromPayload(JSONObject("{\"dps\":{\"120\":\"qgACA2QA\"}} }"), robot.mqttKey, 11)

    fun sendCommands(commands: ArrayList<TuyaMessage>) : Boolean {
        try {
            val botSocket = Socket(robot.ip, 6668)
            commands.forEach {
                botSocket.getOutputStream().write(it.bytes)
                Logger.log("CommandSocket", "Sent command ${it.payload}")
            }
            botSocket.close()
            return true
        } catch (e: Exception) {
            Logger.log("CommandSocket-ERR", "Send command $e")
            return false
        }
    }
}