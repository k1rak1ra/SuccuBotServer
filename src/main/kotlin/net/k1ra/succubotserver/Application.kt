package net.k1ra.succubotserver

import net.k1ra.succubotserver.feature.api.ApiApplication
import net.k1ra.succubotserver.feature.api.AutoCleanWatchdog
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotStatus
import net.k1ra.succubotserver.feature.api.model.login.Session
import net.k1ra.succubotserver.feature.api.model.login.User
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.tuyaHttp.Crypto
import net.k1ra.succubotserver.feature.tuyaHttp.TuyaHttpApplication
import net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt.TuyaMqtt
import net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt.TuyaUdpBroadcastListener
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.io.File
import java.net.URI
import java.sql.Connection


fun main() {
    //Setup file storage dir
    if (!File(StorageManager.getLocalStorageDir()).exists())
        File(StorageManager.getLocalStorageDir()).mkdir()

    if (!File(StorageManager.getUserImageFileWithName("")).exists())
        File(StorageManager.getUserImageFileWithName("")).mkdir()

    if (!File(StorageManager.getMapStorageDir("")).exists())
        File(StorageManager.getMapStorageDir("")).mkdir()

    //Setup DB
    //Check for Postgres connection string. If it exists, use postgres instead of SQLite
    val postgresStr = System.getenv("DATABASE_URL")
    if (postgresStr == null) {
        Database.connect("jdbc:sqlite:${StorageManager.getLocalStorageDir()}data.db", "org.sqlite.JDBC")
    } else {
        val dbUri = URI(postgresStr)
        val username: String = dbUri.getUserInfo().split(":")[0]
        val password: String = dbUri.getUserInfo().split(":")[1]
        val dbUrl = "jdbc:postgresql://" + dbUri.host + ':' + dbUri.port + dbUri.getPath()
        Database.connect(dbUrl, "org.postgresql.Driver", user = username, password = password)
    }
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    //Set up DB if it doesn't exist yet
    Session.initialSetup()
    User.initialSetup()
    RobotInfo.initialSetup()
    RobotStatus.initialSetup()
    ServerSetting.initialSetup()

    //Start internal API
    ApiApplication.startAndSetUp()

    //Start tuyaHttp application
    TuyaHttpApplication().start()

    //Start UDP broadcast listener
    TuyaUdpBroadcastListener.listenForBroadcast()

    //Start MQTT listener
    TuyaMqtt.startMqttListener()

    //Start Autoclean watchdog
    AutoCleanWatchdog.start()
}


