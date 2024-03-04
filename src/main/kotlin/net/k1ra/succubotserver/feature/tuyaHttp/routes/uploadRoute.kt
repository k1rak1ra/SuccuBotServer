package net.k1ra.succubotserver.feature.tuyaHttp.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.StorageManager
import net.k1ra.succubotserver.feature.logging.Logger
import java.io.File
import java.io.FileOutputStream


fun Route.uploadRoute() {
    route("/{did}/common/route/rou.bin") {
        put {
            val data = call.receiveStream().readAllBytes()
            Logger.log("TuyaHttp", "Got upload_route request from ${call.parameters["did"]}")

            if (!File(StorageManager.getMapStorageDir(call.parameters["did"]!!)).exists())
                File(StorageManager.getMapStorageDir(call.parameters["did"]!!)).mkdir()

            val os = FileOutputStream(StorageManager.getMapStorageDir(call.parameters["did"]!!)+"/rou.bin", false)
            os.write(data)
            os.flush()
            os.close()

            call.respond(HttpStatusCode.Created)
        }
    }
}
