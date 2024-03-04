package net.k1ra.succubotserver.feature.tuyaHttp.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.logging.Logger


fun Route.deviceDnsQuery() {
    route("/device/dns_query") {
        post {
            //This response is just a hardcoded certificate for the storage endpoint that's bypassed by patched robot_app
            val body = call.receiveText()
            Logger.log("TuyaHttp", "Got device dns_query request")

            val caLines = ServerSetting.getMqttCertificate()
                .replace("-----END CERTIFICATE-----","")
                .replace("-----BEGIN CERTIFICATE-----","")
                .replace("\n","")

            call.respond("[{\"host\":\"ty-eu-storage-permanent.tuyaeu.com\",\"ca\":\"$caLines\",\"ttl\":600}]\n")
        }
    }
}
