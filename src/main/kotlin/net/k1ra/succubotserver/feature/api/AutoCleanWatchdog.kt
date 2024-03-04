package net.k1ra.succubotserver.feature.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotStatus
import net.k1ra.succubotserver.feature.logging.Logger
import net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt.TuyaSendCommand
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

object AutoCleanWatchdog {
    private val datesRobotLastCleaned = hashMapOf<String, LocalDate>()

    fun start() = CoroutineScope(Dispatchers.IO).launch {
        Logger.log("AutoClean", "AutoClean service started")

        while (true) {
            try {
                RobotStatus.getAll().forEach {
                    if (it.dailyAutoCleanEnabled
                        && LocalTime.now().isAfter(it.dailyCleaningTime)
                        && datesRobotLastCleaned[it.did] != LocalDate.now()
                        && Duration.between(LocalTime.now(), it.dailyCleaningTime).abs() < Duration.ofMinutes(5)) {
                        datesRobotLastCleaned[it.did] = LocalDate.now()

                        val commandSender = TuyaSendCommand(RobotInfo.getRobotByDid(it.did)!!)
                        RobotStatus.updateLastCleaned(it.did)
                        commandSender.sendCommands(arrayListOf(commandSender.startCommand))
                        delay(3000)

                        val maxRetryCount = 10
                        var retryIndex = 0
                        while (RobotStatus.getRobotStatus(it.did)?.runningCleaningCycle == false && retryIndex < maxRetryCount) {
                            retryIndex++
                            commandSender.sendCommands(arrayListOf(commandSender.startCommand))
                            delay(3000)
                        }


                        Logger.log("AutoClean", "${it.name} started autoclean")
                    }
                }
            } catch (e: Exception) {
                Logger.log("AutoClean-ERR", "AutoClean service caught error $e")
            }

            delay(1000 * 10) //Run every 10 seconds
        }
    }
}