package net.k1ra.succubotserver.feature.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.feature.api.GsonProvider
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.*
import net.k1ra.succubotserver.feature.api.plugins.AuthenticationPlugin
import net.k1ra.succubotserver.feature.logging.Logger
import net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt.TuyaSendCommand

fun Route.robot() {
    route("/robot/{did}") {
        install(AuthenticationPlugin.authenticationPlugin)
        get {
            if (call.parameters["did"] == "all") {
                Logger.log("API", "Fetching all robots")
                call.respondText(GsonProvider.gson.toJson(RobotStatus.getAll()).toString())
            } else {
                val status = RobotStatus.getRobotStatus(call.parameters["did"]!!)
                Logger.log("API", "Fetching robot ${call.parameters["did"]}")

                if (status != null)
                    call.respondText(GsonProvider.gson.toJson(status).toString())
                else
                    call.respond(HttpStatusCode.NotFound)
            }
        }
        post {
            //If missing did or did not found, return 404 and abort
            if (call.parameters["did"] == null || RobotStatus.getRobotStatus(call.parameters["did"]!!) == null) {
                call.respond(HttpStatusCode.NotFound)
                return@post
            }

            val body = call.receiveText()
            Logger.log("API", "Updating robot ${call.parameters["did"]} with $body")
            val request = GsonProvider.gson.fromJson(body, RobotCommand::class.java)
            val commandSender = TuyaSendCommand(RobotInfo.getRobotByDid(call.parameters["did"]!!)!!)

            if (request.runCleaningCycle != null) {
                if (request.runCleaningCycle) {
                    RobotStatus.updateLastCleaned(call.parameters["did"]!!)
                    commandSender.sendCommands(arrayListOf(commandSender.startCommand))
                } else
                    commandSender.sendCommands(arrayListOf(commandSender.pauseCommand))
            }

            if (request.goToCharge != null) {
                if (request.goToCharge)
                    commandSender.sendCommands(arrayListOf(commandSender.gotoChargeCommand))
                else
                    commandSender.sendCommands(arrayListOf(commandSender.stopGotoChargeCommand))
            }

            if (request.continueCleaningAfterCharge != null) {
                if (request.continueCleaningAfterCharge)
                    commandSender.sendCommands(arrayListOf(commandSender.continueCleaningAfterChargeCommand))
                else
                    commandSender.sendCommands(arrayListOf(commandSender.stopCleaningAfterChargeCommand))
            }

            if (request.suctionPower != null) {
                when(request.suctionPower) {
                    SuctionPowers.LOW -> commandSender.sendCommands(arrayListOf(commandSender.lowSuctionCommand))
                    SuctionPowers.NORMAL -> commandSender.sendCommands(arrayListOf(commandSender.normalSuctionCommand))
                    SuctionPowers.STRONG -> commandSender.sendCommands(arrayListOf(commandSender.strongSuctionCommand))
                }
            }

            if (request.waterFlow != null) {
                when(request.waterFlow) {
                    WaterFlowLevels.LOW -> commandSender.sendCommands(arrayListOf(commandSender.lowWaterCommand))
                    WaterFlowLevels.MEDIUM -> commandSender.sendCommands(arrayListOf(commandSender.normalWaterCommand))
                    WaterFlowLevels.HIGH -> commandSender.sendCommands(arrayListOf(commandSender.highWaterCommand))
                }
            }

            if (request.volumeLevels != null) {
                when (request.volumeLevels) {
                    VolumeLevels.OFF -> commandSender.sendCommands(arrayListOf(commandSender.volumeMuteCommand))
                    VolumeLevels.LOW -> commandSender.sendCommands(arrayListOf(commandSender.volumeLowCommand))
                    VolumeLevels.MEDIUM -> commandSender.sendCommands(arrayListOf(commandSender.volumeMediumCommand))
                    VolumeLevels.HIGH -> commandSender.sendCommands(arrayListOf(commandSender.volumeHighCommand))
                }
            }

            if (request.changeName != null) {
                RobotStatus.changeName(call.parameters["did"]!!, request.changeName)
            }

            if (request.changeAutoCleanEnabled != null) {
                RobotStatus.changeAutocleanEnabled(call.parameters["did"]!!, request.changeAutoCleanEnabled)
            }

            if (request.changeAutoCleanTime != null) {
                RobotStatus.changeAutocleanTime(call.parameters["did"]!!, request.changeAutoCleanTime)
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}