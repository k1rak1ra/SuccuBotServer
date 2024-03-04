package net.k1ra.succubotserver.feature.api.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import net.k1ra.succubotserver.feature.api.GsonProvider
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotStatus
import net.k1ra.succubotserver.feature.api.model.deviceManagement.DeviceManagementRequest
import net.k1ra.succubotserver.feature.api.model.deviceManagement.DeviceManagementResponse
import net.k1ra.succubotserver.feature.api.model.login.Ldap
import net.k1ra.succubotserver.feature.api.model.login.Session
import net.k1ra.succubotserver.feature.api.model.serverManagement.CurrentServerConfiguration
import net.k1ra.succubotserver.feature.api.model.serverManagement.ServerManagementRequest
import net.k1ra.succubotserver.feature.api.model.serverManagement.ServerManagementResponse
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.api.plugins.AdminConfirmationPlugin
import net.k1ra.succubotserver.feature.api.plugins.AuthenticationPlugin
import net.k1ra.succubotserver.feature.logging.Logger
import net.k1ra.succubotserver.feature.tuyaHttp.routes.WebKeyVerifyForRobot
import net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt.TuyaMqtt
import net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt.TuyaSendCommand

fun Route.deviceManagement() {
    route("/deviceManagement") {
        install(AuthenticationPlugin.authenticationPlugin)
        install(AdminConfirmationPlugin.adminConfirmationPlugin)

        get {
            call.respond(GsonProvider.gson.toJson(RobotInfo.getAllRobots()).toString())
        }

        post {
            val session = Session.headerToSession(call.request.headers)!!
            val body = call.receiveText()
            Logger.log("API", "Device setting update request made by ${session.uid}")

            val request = GsonProvider.gson.fromJson(body, DeviceManagementRequest::class.java)
            RobotInfo.updateKeys(request)

            val response = DeviceManagementResponse(false, null)
            val robot = RobotInfo.getRobotByDid(request.did)!!

            var tsc: TuyaSendCommand? = null
            WebKeyVerifyForRobot.hashMap[robot.did] = null

            try {
                tsc = TuyaSendCommand(robot)
                tsc.sendCommands(arrayListOf(tsc.gotoChargeCommand))
                delay(1000)

                response.mqttKeyValid = tsc.sendCommands(arrayListOf(tsc.startCommand))
            } catch (e: Exception) {
                //Do nothing, default state is already fail
            }

            if (response.mqttKeyValid) {
                while (WebKeyVerifyForRobot.hashMap[robot.did] == null) {
                    delay(100)
                }

                response.webKeyValid = WebKeyVerifyForRobot.hashMap[robot.did]
                response.mqttKeyValid = tsc?.sendCommands(arrayListOf(tsc.gotoChargeCommand)) == true
            }

            if (response.webKeyValid == true && response.mqttKeyValid)
                RobotStatus.insertDeviceIfNew(robot.did)

            call.respond(GsonProvider.gson.toJson(response).toString())
        }
    }
}