package net.k1ra.succubotserver.feature.api

import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k1ra.succubotserver.StorageManager
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotStatus
import net.k1ra.succubotserver.feature.api.model.login.Session
import net.k1ra.succubotserver.feature.api.model.login.User
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.api.routes.*
import net.k1ra.succubotserver.feature.logging.Logger
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.net.URI
import java.sql.Connection

object ApiApplication {
    fun startAndSetUp() {
        CoroutineScope(Dispatchers.IO).launch {
            io.ktor.server.engine.embeddedServer(Netty, port = 8085, host = "0.0.0.0", module = {
                Logger.log("API", "API online")

                routing {
                    login()
                    robot()
                    layout()
                    route()
                    user()
                    image()
                    serverManagement()
                    deviceManagement()
                    userManagement()
                }
            }).start(wait = true)
        }
    }
}