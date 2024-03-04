package net.k1ra.succubotserver.feature.tuyaHttp.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.logging.Logger


fun Route.urlConfig() {
    route("/v1/url_config") {
        post {
            //This response is just a hardcoded certificate for the storage endpoint that's bypassed by patched robot_app
            val body = call.receiveText()
            Logger.log("TuyaHttp", "Got device url_config request")

            val caLines = ServerSetting.getMqttCertificate()
                .replace("-----END CERTIFICATE-----","")
                .replace("-----BEGIN CERTIFICATE-----","")
                .replace("\n","")

            call.respond("{\"caArr\":[\"$caLines\"]," +
                    "\"httpUrl\":{\"addr\":\"http://a.tuyaeu.com/d.json\"," +
                    "\"ips\":[\"18.156.111.21\",\"3.64.97.180\",\"3.122.139.185\"]}," +
                    "\"httpsSelfUrl\":{\"addr\":\"https://a2.tuyaeu.com/d.json\"," +
                    "\"ips\":[\"35.157.101.18\",\"3.125.199.146\",\"3.125.234.101\"]}," +
                    "\"mqttUrl\":{\"addr\":\"m2.tuyaeu.com:1883\"," +
                    "\"ips\":[\"18.185.218.106\",\"52.57.38.165\",\"3.121.210.75\"]}," +
                    "\"mqttsSelfUrl\":{\"addr\":\"m2.tuyaeu.com:8883\"," +
                    "\"ips\":[\"18.185.218.106\",\"52.57.38.165\",\"3.121.210.75\"]},\"ttl\":600}")
        }
    }
}
