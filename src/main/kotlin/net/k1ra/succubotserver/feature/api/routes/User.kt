package net.k1ra.succubotserver.feature.api.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.feature.api.GsonProvider
import net.k1ra.succubotserver.StorageManager
import net.k1ra.succubotserver.feature.api.model.base.ErrorResponse
import net.k1ra.succubotserver.feature.api.model.login.Session
import net.k1ra.succubotserver.feature.api.model.login.User
import net.k1ra.succubotserver.feature.api.model.appSettings.UserRequest
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.api.plugins.AuthenticationPlugin
import net.k1ra.succubotserver.feature.logging.Logger
import java.io.File
import java.util.*

fun Route.user() {
    route("/user") {
        install(AuthenticationPlugin.authenticationPlugin)
        post {
            val session = Session.headerToSession(call.request.headers)!!
            val body = call.receiveText()
            Logger.log("API", "User update request made by ${session.uid}")

            val request = GsonProvider.gson.fromJson(body, UserRequest::class.java)

            if (request.passwordChange != null) {
                if (User.isNative(session.uid) && User.validatePassword(session.uid, request.passwordChange.old)) {
                    if (request.passwordChange.new.length >= ServerSetting.getMinPasswordLength()) {
                        User.updatePassword(session.uid, request.passwordChange.new)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("newLength")))
                        return@post
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("oldIncorrect")))
                    return@post
                }
            }

            if (request.uploadImage != null) {
                val imageBytes = Base64.getDecoder().decode(request.uploadImage)

                if (imageBytes.size <= ServerSetting.getMaxImageSize()) {
                    val imageName = "${session.uid}-${UUID.randomUUID()}"
                    File(StorageManager.getUserImageFileWithName(imageName)).writeBytes(imageBytes)
                    User.setImage(session.uid, "${ServerSetting.getCurrentBaseUrl()}image/${imageName}")
                } else {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("imageSize")))
                    return@post
                }
            }

            if (request.changeName != null) {
                if (User.isNative(session.uid)) {
                    if (request.changeName.isNotEmpty()) {
                        User.updateUserName(session.uid, request.changeName)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("nameEmpty")))
                        return@post
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("userNotNative")))
                    return@post
                }
            }

            if (request.changeEmail != null) {
                if (User.isNative(session.uid)) {
                    if (request.changeEmail.isNotEmpty()) {
                        if (!User.updateUserEmail(session.uid, request.changeEmail)) {
                            call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("emailNotUnique")))
                           return@post
                        }
                    } else {
                        call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("emailEmpty")))
                      return@post
                    }
                } else {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("userNotNative")))
                    return@post
                }
            }

            if (request.logout == true) {
                Session.logoutUser(session)
            }

            if (request.logoutOutOfAll == true) {
                Session.destroyAllUserSessions(session.uid)
            }

            call.respond(GsonProvider.gson.toJson(User.getUserByUidFull(session.uid)).toString())
        }
    }
}