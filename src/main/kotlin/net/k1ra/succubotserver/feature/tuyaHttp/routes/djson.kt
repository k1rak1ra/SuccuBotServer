package net.k1ra.succubotserver.feature.tuyaHttp.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.feature.api.model.deviceInteraction.RobotInfo
import net.k1ra.succubotserver.feature.logging.Logger
import net.k1ra.succubotserver.feature.tuyaHttp.Crypto
import java.util.stream.Collectors

object WebKeyVerifyForRobot {
    val hashMap = mutableMapOf<String, Boolean?>()
}

fun Route.djson() {
    route("/d.json") {
        post {
            val body = call.receiveText()

            val queryParams = call.request.queryParameters.entries().stream().collect(Collectors.toMap({it.key},{it.value}))
            val respT = queryParams["t"]!!.first().toLong() + queryParams["et"]!!.first().toLong()
            Logger.log("TuyaHttp", "Got d.json request for ${queryParams["a"]?.first()} from ${queryParams["devId"]?.first()}")

            val robotInfo = RobotInfo.getRobotByDid(queryParams["devId"]!!.first())
            if (robotInfo == null) {
                Logger.log("TuyaHttp-ERR", "RobotInfo for ${queryParams["devId"]?.first()} is missing WebKey")
                call.respond(HttpStatusCode.InternalServerError)
                return@post
            }

            //Decrypt request in order to validate key
            try {
                Crypto(robotInfo).decryptRequest(body.replace("data=",""))
                WebKeyVerifyForRobot.hashMap[queryParams["devId"]!!.first()] = true
            } catch (e: Exception) {
                WebKeyVerifyForRobot.hashMap[queryParams["devId"]!!.first()] = false
                Logger.log("TuyaHttp-ERR", "RobotInfo for ${queryParams["devId"]?.first()} has invalid WebKey, aborting")
                call.respond(HttpStatusCode.InternalServerError)
                return@post
            }

            if (queryParams["a"]?.first() == "tuya.device.upgrade.silent.get") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse("{\"t\":$respT,\"success\":true}") +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")

            } else if (queryParams["a"]?.first() == "tuya.device.dynamic.config.get") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse("{\"result\":{\"validTime\":1800,\"time\":$respT,\"config\":{}},\"t\":$respT,\"success\":true}") +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")

            } else if (queryParams["a"]?.first() == "atop.online.debug.log") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse("{\"result\":true,\"t\":$respT,\"success\":true}") +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")

            } else if (queryParams["a"]?.first() == "tuya.device.timer.astronomical.list") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse("{\"result\":{\"code\":200,\"data\":{}},\"t\":$respT,\"success\":true}") +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")

            } else if (queryParams["a"]?.first() == "tuya.device.timer.count") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse(
                            "{\"result\":{\"devId\":\"bfcf49fe2e9d55e8aasaxe\",\"count\":0,\"lastFetchTime\":0},\"t\":$respT,\"success\":true}"
                        ) +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")

            } else if (queryParams["a"]?.first() == "tuya.device.storage.config.get") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse(
                            "{\"result\":{\"pathConfig\":{\"common\":\"/${queryParams["devId"]?.first()}/common\"}," +
                                    "\"eventSilenceInterval\":120,\"ak\":\"ASIAUTPMUJJJREGZ3ODO\",\"videoLongestTime\":300,\"" +
                                    "token\":\"FwoGZXIvYXdzEO7//////////wEaDMl0bO223Y8IkK5uWCKKArK3k+1CL6CIhd7vkFpQ1sEXoogZMljofF" +
                                    "mKEev/pSiTPDhOxfR53NoniKKwyw1sHAjqL9MI9Tz1+w3UbXAjI/qRBEWWLLLBxVDoyFt07KRamNT+Yc+pMkRtbFUzo7O" +
                                    "J9aG0YA2oBOkorXg8wH7glWXHah18PftRWQbWC93a3bYfDj57rkKamd3b/WcRtAQc1AENCNG0nf0fooIg8A4M6sITCUyci" +
                                    "6V+ZThWSeOPQsdkyvl6IbNmTV3qLfKqo8ZcLj6PLyrU5tnDJzBY+0xekBXoh1Ti1JCiZ1tFQ2RqlqTylkYoWUJJT3b4HTE" +
                                    "iPv9V7900eI3+J7WWRl9gxl/G0b9eZNc0GeaOCZw4KI6H6qsGMile2Z6hYHMU9RVVDN0p3UNtEuR3cWUiBbalIm4tZAGNZ" +
                                    "fAw/gg/dSpmAA==\",\"bucket\":\"ty-eu-storage-permanent\",\"countLeftToday\":30," +
                                    "\"endpoint\":\"tuyaeu.com\",\"provider\":\"s3\",\"sk\":\"AMsiYnpV5xUj6AKZw6DIuTLCa6EMUiknaB/YMPSr\"," +
                                    "\"expiration\":\"3000-12-15T16:24:46Z\",\"region\":\"eu-central-1\"," +
                                    "\"lifeCycle\":2592000},\"t\":$respT,\"success\":true}"
                        ) +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")
            } else if (queryParams["a"]?.first() == "tuya.device.common.recode.file.list") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse("{\"result\":[],\"t\":$respT,\"success\":true}") +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")

            } else if (queryParams["a"]?.first() == "tuya.device.common.file.upload.complete") {
                call.respond("{\"result\":\"" +
                        Crypto(robotInfo).encryptResponse("{\"result\":true,\"t\":$respT,\"success\":true}") +
                        "\",\"sign\":\"10b33d83ecf4b670\",\"t\":$respT}")

            } else if (queryParams["a"]?.first() == "tuya.device.active") {

            }
        }
    }
}
