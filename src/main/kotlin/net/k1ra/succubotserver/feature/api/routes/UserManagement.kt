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
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.api.model.userManagement.UserManagementRequest
import net.k1ra.succubotserver.feature.api.plugins.AdminConfirmationPlugin
import net.k1ra.succubotserver.feature.api.plugins.AuthenticationPlugin
import net.k1ra.succubotserver.feature.logging.Logger
import java.io.File
import java.util.*

fun Route.userManagement() {
    route("/userManagement/{uid}") {
        install(AuthenticationPlugin.authenticationPlugin)
        install(AdminConfirmationPlugin.adminConfirmationPlugin)

        get {
            if (call.parameters["uid"]!! == "all")
                call.respond(GsonProvider.gson.toJson(User.getAllUsersForAdminPanel()).toString())
            else {
                val user = User.getUserByUid(call.parameters["uid"]!!)

                if (user != null)
                    call.respondText(GsonProvider.gson.toJson(user).toString())
                else
                    call.respond(HttpStatusCode.NotFound)
            }
        }

        post {
            val session = Session.headerToSession(call.request.headers)!!
            val body = call.receiveText()
            val targetUid = call.parameters["uid"]!!
            Logger.log("API", "User update request made by ${session.uid}")

            val request = GsonProvider.gson.fromJson(body, UserManagementRequest::class.java)

            val response = if (targetUid != "new") {
                //Updating existing user
                //Make sure user exists first
                if (!User.containsUid(targetUid)) {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("userNotFound")))
                    return@post
                }

                if (request.changeName != null) {
                    if (User.isNative(targetUid)) {
                        if (request.changeName.isNotEmpty()) {
                            User.updateUserName(targetUid, request.changeName)
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
                    if (User.isNative(targetUid)) {
                        if (request.changeEmail.isNotEmpty()) {
                            if (!User.updateUserEmail(targetUid, request.changeEmail)) {
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

                if (request.passwordChange != null) {
                    if (User.isNative(targetUid)) {
                        User.updatePassword(targetUid, request.passwordChange)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("userNotNative")))
                        return@post
                    }
                }

                if (request.uploadImage != null) {
                    val imageBytes = Base64.getDecoder().decode(request.uploadImage)
                    val imageName = "${targetUid}-${UUID.randomUUID()}"
                    File(StorageManager.getUserImageFileWithName(imageName)).writeBytes(imageBytes)
                    User.setImage(targetUid, "${ServerSetting.getCurrentBaseUrl()}image/${imageName}")
                }

                if (request.changeAdminStatus != null) {
                    if (User.isNative(targetUid)) {
                        User.updateUserAdminStatus(targetUid, request.changeAdminStatus)
                    } else {
                        call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("userNotNative")))
                        return@post
                    }
                }

                if (request.logoutOutOfAll == true) {
                    Session.destroyAllUserSessions(targetUid)
                }

                User.getUserByUid(targetUid)
            } else {
                val uid = User.createUid()

                if (request.changeName.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("nameEmpty")))
                    return@post
                }

                if (request.changeEmail.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("emailEmpty")))
                    return@post
                }

                if (User.doesEmailExistOutsideUid(request.changeEmail, null)) {
                    call.respond(HttpStatusCode.Forbidden, GsonProvider.gson.toJson(ErrorResponse("emailNotUnique")))
                    return@post
                }

                val imageUrl = if (request.uploadImage != null) {
                    val imageBytes = Base64.getDecoder().decode(request.uploadImage)
                    val imageName = "${uid}-${UUID.randomUUID()}"
                    File(StorageManager.getUserImageFileWithName(imageName)).writeBytes(imageBytes)
                    "${ServerSetting.getCurrentBaseUrl()}image/${imageName}"
                } else ""

                User.insertUserRaw(User(
                    request.changeName,
                    imageUrl,
                    request.changeEmail,
                    uid,
                    "",
                    request.changeAdminStatus == true,
                    true
                ), request.passwordChange)

                User.getUserByUid(uid)
            }

            call.respond(GsonProvider.gson.toJson(response).toString())
        }

        delete {
            val session = Session.headerToSession(call.request.headers)!!
            val targetUid = call.parameters["uid"]!!
            Logger.log("API", "User delete request made by ${session.uid}")

            User.deleteUser(targetUid)

            call.respond(HttpStatusCode.OK)
        }
    }
}