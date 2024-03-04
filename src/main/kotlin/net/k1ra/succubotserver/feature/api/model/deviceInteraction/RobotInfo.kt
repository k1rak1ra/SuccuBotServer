package net.k1ra.succubotserver.feature.api.model.deviceInteraction

import net.k1ra.succubotserver.feature.api.model.deviceManagement.DeviceManagementRequest
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.awt.Robot
import java.time.LocalDateTime
import java.time.ZoneOffset

class RobotInfo(
    val did: String,
    val ip: String,
    val mqttKey: String,
    val webKey: String,
    val lastPingAt: LocalDateTime
) {

    init {
        initialSetup()
    }

   companion object {
       private object RobotInfoTable: IntIdTable() {
           val did = text("did")
           val ip = text("ip")
           val mqttKey = text("mqttKey")
           val webKey = text("webKey")
           val lastPingAt = datetime("lastPingAt")
       }

       fun updateFromBroadcast(ip: String, did: String) {
            transaction {
                if (RobotInfoTable.select(RobotInfoTable.did eq did).count() > 0) {
                    RobotInfoTable.update({RobotInfoTable.did eq did}){
                        it[RobotInfoTable.ip] = ip
                        it[lastPingAt] = LocalDateTime.now(ZoneOffset.UTC)
                    }
                } else {
                    RobotInfoTable.insert {
                        it[RobotInfoTable.did] = did
                        it[RobotInfoTable.ip] = ip
                        it[mqttKey] = ""
                        it[webKey] = ""
                        it[lastPingAt] = LocalDateTime.now(ZoneOffset.UTC)
                    }
                }
            }
       }

       fun updateKeys(request: DeviceManagementRequest) {
           transaction {
               RobotInfoTable.update({RobotInfoTable.did eq request.did}){
                   it[webKey] = request.webKey
                   it[mqttKey] = request.mqttKey
               }
           }
       }

       fun getAllRobots() : ArrayList<RobotInfo> {
           val list: ArrayList<RobotInfo> = arrayListOf()

           transaction {
               RobotInfoTable.selectAll().forEach{
                   list.add(RobotInfo(
                       it[RobotInfoTable.did],
                       it[RobotInfoTable.ip],
                       it[RobotInfoTable.mqttKey],
                       it[RobotInfoTable.webKey],
                       it[RobotInfoTable.lastPingAt]
                   ))
               }
           }

           return list
       }

       fun getRobotByDid(did: String) : RobotInfo? {
           var robotInfo: RobotInfo? = null

           transaction {
               RobotInfoTable.select(RobotInfoTable.did eq did).forEach {
                   robotInfo = RobotInfo(
                       it[RobotInfoTable.did],
                       it[RobotInfoTable.ip],
                       it[RobotInfoTable.mqttKey],
                       it[RobotInfoTable.webKey],
                       it[RobotInfoTable.lastPingAt]
                   )
               }
           }

           return robotInfo
       }

       @Synchronized
       fun initialSetup() {
           transaction {
               if (!RobotInfoTable.exists()) {
                   SchemaUtils.create(RobotInfoTable)
               }
           }
       }
   }
}