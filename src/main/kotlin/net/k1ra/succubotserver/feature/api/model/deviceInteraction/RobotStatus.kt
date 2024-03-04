package net.k1ra.succubotserver.feature.api.model.deviceInteraction

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.transactions.transaction
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset

data class RobotStatus(
    val did: String,
    val lastCleaned: LocalDateTime,
    val dailyCleaningTime: LocalTime,
    val dailyAutoCleanEnabled: Boolean,
    val runningCleaningCycle: Boolean, //1
    val unk2: Boolean, //2: ?? true when free, false when charging, false when running, true when charged in dock
    val gotoCharge: Boolean, //3
    val command: String, //4: chargego, smart
    val status: String, //5: standby, charging, relocating (suction off, moving to new location), goto_charge, sleep, smart (vacuuming)
    val continueCleaningAfterCharge: Boolean, //27
    val minutesCleanedLast: Int, //6
    val errCode: Int, //28 0 = none, 32 = mop missing, 1024 = stuck
    val squareMetersCleanedLast: Int, //7
    val battery: Int, //8 0-100%
    val suctionPower: SuctionPowers, //9 closed, gentle, normal, strong
    val waterFlow: WaterFlowLevels, //10 low, middle, high
    val unk112: String, //112
    val unk101: String, //101
    val volume: VolumeLevels, //120 base64 encoded hex, last 4 digits is volume from 0000 to 6400
    val unk15: String, //15
    val name: String
) {

    init {
        initialSetup()
    }

   companion object {
       private object RobotStatusTable: IntIdTable() {
           val did = text("did")
           val lastCleaned = datetime("lastCleaned")
           val dailyCleaningTime = time("dailyCleaningTime")
           val dailyAutoCleanEnabled = bool("dailyAutoCleanEnabled")
           val runningCleaningCycle = bool("runningCleaningCycle")
           val unk2 = bool("unk2")
           val gotoCharge = bool("gotoCharge")
           val command = text("command")
           val status = text("status")
           val continueCleaningAfterCharge = bool("continueCleaningAfterCharge")
           val minutesCleanedLast = integer("minutesCleanedLast")
           val errCode = integer("errCode")
           val squareMetersCleanedLast = integer("squareMetersCleanedLast")
           val battery = integer("battery")
           val suctionPower = text("suctionPower")
           val waterFlow = text("waterFlow")
           val unk112 = text("unk112")
           val unk101 = text("unk101")
           val volume = text("volume")
           val unk15 = text("unk15")
           val name = text("name")
       }

       fun insertDeviceIfNew(did: String) {
           transaction {
               if (RobotStatusTable.select(RobotStatusTable.did eq did).count().toInt() == 0) {
                   RobotStatusTable.insert {
                       it[RobotStatusTable.did] = did
                       it[lastCleaned] = LocalDateTime.now(ZoneOffset.UTC)
                       it[dailyCleaningTime] = LocalTime.MIDNIGHT
                       it[dailyAutoCleanEnabled] = false
                       it[runningCleaningCycle] = false
                       it[unk2] = true
                       it[gotoCharge] = false
                       it[command] = "chargego"
                       it[status] = "standby"
                       it[continueCleaningAfterCharge] = false
                       it[minutesCleanedLast] = 0
                       it[errCode] = 0
                       it[squareMetersCleanedLast] = 0
                       it[battery] = 100
                       it[waterFlow] = "closed"
                       it[suctionPower] = "low"
                       it[unk112] = "qgACAAEC"
                       it[unk101] = "qgACGwAb"
                       it[volume] = "qgACA2QA"
                       it[unk15] = "qgACOAA4"
                       it[name] = "Robot"
                   }
               }
           }
       }

       fun updateLastCleaned(did: String) {
           insertDeviceIfNew(did)

           transaction {
               RobotStatusTable.update({ RobotStatusTable.did eq did }) {
                   it[lastCleaned] = LocalDateTime.now(ZoneOffset.UTC)
               }
           }
       }

       fun updateStatusFromMqtt(dps: JSONObject, did: String) {
           transaction {
               RobotStatusTable.update({RobotStatusTable.did eq did}) {
                   if (dps.has("1")) {
                       it[runningCleaningCycle] = dps.getBoolean("1")
                   }

                   if (dps.has("2")) {
                       it[unk2] = dps.getBoolean("2")
                   }

                   if (dps.has("3")) {
                       it[gotoCharge] = dps.getBoolean("3")
                   }

                   if (dps.has("4")) {
                       it[command] = dps.getString("4")
                   }

                   if (dps.has("5")) {
                       it[status] = dps.getString("5")
                   }

                   if (dps.has("27")) {
                       it[continueCleaningAfterCharge] = dps.getBoolean("27")
                   }

                   if (dps.has("6")) {
                       it[minutesCleanedLast] = dps.getInt("6")
                   }

                   if (dps.has("28")) {
                       it[errCode] = dps.getInt("28")
                   }

                   if (dps.has("7")) {
                       it[squareMetersCleanedLast] = dps.getInt("7")
                   }

                   if (dps.has("8")) {
                       it[battery] = dps.getInt("8")
                   }

                   if (dps.has("9")) {
                       it[suctionPower] = dps.getString("9")
                   }

                   if (dps.has("10")) {
                       it[waterFlow] = dps.getString("10")
                   }

                   if (dps.has("112")) {
                       it[unk112] = dps.getString("112")
                   }

                   if (dps.has("101")) {
                       it[unk101] = dps.getString("101")
                   }

                   if (dps.has("120")) {
                       it[volume] = dps.getString("120")
                   }

                   if (dps.has("15")) {
                       it[unk15] = dps.getString("15")
                   }
               }
           }
       }

       fun getRobotStatus(did: String) : RobotStatus? {
           var status: RobotStatus? = null

           transaction {
               RobotStatusTable.select(RobotStatusTable.did eq did).forEach {

                   status = RobotStatus(
                       it[RobotStatusTable.did],
                       it[RobotStatusTable.lastCleaned],
                       it[RobotStatusTable.dailyCleaningTime],
                       it[RobotStatusTable.dailyAutoCleanEnabled],
                       it[RobotStatusTable.runningCleaningCycle],
                       it[RobotStatusTable.unk2],
                       it[RobotStatusTable.gotoCharge],
                       it[RobotStatusTable.command],
                       it[RobotStatusTable.status],
                       it[RobotStatusTable.continueCleaningAfterCharge],
                       it[RobotStatusTable.minutesCleanedLast],
                       it[RobotStatusTable.errCode],
                       it[RobotStatusTable.squareMetersCleanedLast],
                       it[RobotStatusTable.battery],
                       if (it[RobotStatusTable.suctionPower] == "strong") {
                           SuctionPowers.STRONG
                       } else if (it[RobotStatusTable.suctionPower] == "normal") {
                           SuctionPowers.NORMAL
                       } else {
                           SuctionPowers.LOW
                       },
                       if (it[RobotStatusTable.waterFlow] == "high") {
                           WaterFlowLevels.HIGH
                       } else if (it[RobotStatusTable.waterFlow] == "middle") {
                           WaterFlowLevels.MEDIUM
                       } else {
                           WaterFlowLevels.LOW
                       },
                       it[RobotStatusTable.unk112],
                       it[RobotStatusTable.unk101],
                       if (it[RobotStatusTable.volume] == "qgACAwAA") {
                           VolumeLevels.OFF
                       } else if (it[RobotStatusTable.volume] == "qgACAzIA") {
                           VolumeLevels.LOW
                       } else if (it[RobotStatusTable.volume] == "qgACA0YA") {
                           VolumeLevels.MEDIUM
                       } else {
                           VolumeLevels.HIGH
                       },
                       it[RobotStatusTable.unk15],
                       it[RobotStatusTable.name]
                   )
               }
           }

           return status
       }

       fun getAll() : ArrayList<RobotStatus> {
           val list: ArrayList<RobotStatus> = arrayListOf()

           transaction {
               RobotStatusTable.selectAll().forEach {
                   list.add(RobotStatus(
                       it[RobotStatusTable.did],
                       it[RobotStatusTable.lastCleaned],
                       it[RobotStatusTable.dailyCleaningTime],
                       it[RobotStatusTable.dailyAutoCleanEnabled],
                       it[RobotStatusTable.runningCleaningCycle],
                       it[RobotStatusTable.unk2],
                       it[RobotStatusTable.gotoCharge],
                       it[RobotStatusTable.command],
                       it[RobotStatusTable.status],
                       it[RobotStatusTable.continueCleaningAfterCharge],
                       it[RobotStatusTable.minutesCleanedLast],
                       it[RobotStatusTable.errCode],
                       it[RobotStatusTable.squareMetersCleanedLast],
                       it[RobotStatusTable.battery],
                       if (it[RobotStatusTable.suctionPower] == "strong") {
                           SuctionPowers.STRONG
                       } else if (it[RobotStatusTable.suctionPower] == "normal") {
                           SuctionPowers.NORMAL
                       } else {
                           SuctionPowers.LOW
                       },
                       if (it[RobotStatusTable.waterFlow] == "high") {
                           WaterFlowLevels.HIGH
                       } else if (it[RobotStatusTable.waterFlow] == "middle") {
                           WaterFlowLevels.MEDIUM
                       } else {
                           WaterFlowLevels.LOW
                       },
                       it[RobotStatusTable.unk112],
                       it[RobotStatusTable.unk101],
                       if (it[RobotStatusTable.volume] == "qgACAwAA") {
                           VolumeLevels.OFF
                       } else if (it[RobotStatusTable.volume] == "qgACAzIA") {
                           VolumeLevels.LOW
                       } else if (it[RobotStatusTable.volume] == "qgACA0YA") {
                           VolumeLevels.MEDIUM
                       } else {
                           VolumeLevels.HIGH
                       },
                       it[RobotStatusTable.unk15],
                       it[RobotStatusTable.name]
                   ))
               }
           }

           return list
       }

       fun changeName(did: String, name: String) {
           transaction {
               RobotStatusTable.update({RobotStatusTable.did eq did}) {
                   it[RobotStatusTable.name] = name
               }
           }
       }

       fun changeAutocleanEnabled(did: String, enabled: Boolean) {
           transaction {
               RobotStatusTable.update({RobotStatusTable.did eq did}) {
                   it[dailyAutoCleanEnabled] = enabled
               }
           }
       }

       fun changeAutocleanTime(did: String, time: LocalTime) {
           transaction {
               RobotStatusTable.update({RobotStatusTable.did eq did}) {
                   it[dailyCleaningTime] = time
               }
           }
       }

       @Synchronized
       fun initialSetup() {
           transaction {
               if (!RobotStatusTable.exists()) {
                   SchemaUtils.create(RobotStatusTable)
               }
           }
       }
   }
}