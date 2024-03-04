package net.k1ra.succubotserver.feature.api.routes

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.k1ra.succubotserver.feature.api.GsonProvider
import net.k1ra.succubotserver.feature.api.model.login.Ldap
import net.k1ra.succubotserver.feature.api.model.login.Session
import net.k1ra.succubotserver.feature.api.model.serverManagement.CurrentServerConfiguration
import net.k1ra.succubotserver.feature.api.model.serverManagement.ServerManagementRequest
import net.k1ra.succubotserver.feature.api.model.serverManagement.ServerManagementResponse
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import net.k1ra.succubotserver.feature.api.plugins.AdminConfirmationPlugin
import net.k1ra.succubotserver.feature.api.plugins.AuthenticationPlugin
import net.k1ra.succubotserver.feature.logging.Logger
import net.k1ra.succubotserver.feature.tuyaRawPacketAndMqtt.TuyaMqtt

fun Route.serverManagement() {
    route("/serverManagement") {
        install(AuthenticationPlugin.authenticationPlugin)
        install(AdminConfirmationPlugin.adminConfirmationPlugin)

        get {
            val currentConfig = CurrentServerConfiguration(
                minPasswordLength = ServerSetting.getMinPasswordLength(),
                maxImageSize = ServerSetting.getMaxImageSize(),
                currentBaseUrl = ServerSetting.getCurrentBaseUrl(),
                ldapLoginEnabled = ServerSetting.getLdapLoginEnabled(),
                ldapServer = ServerSetting.getLdapServer(),
                ldapBindUser = ServerSetting.getLdapBindUser(),
                ldapBindPassword = ServerSetting.getLdapBindPassword(),
                ldapTlsEnabled = ServerSetting.getLdapTlsEnabled(),
                ldapUserDn = ServerSetting.getLdapUserDn(),
                ldapUserFilter = ServerSetting.getLdapUserFilter(),
                ldapUserUidAttribute = ServerSetting.getLdapUserUidAttribute(),
                ldapAdminGroupName = ServerSetting.getLdapAdminGroupName(),
                ldapGroupDn = ServerSetting.getLdapGroupDn(),
                ldapGroupFilter = ServerSetting.getLdapGroupFilter(),
                mqttServerUrl = ServerSetting.getMqttServer(),
                mqttIsConnected = TuyaMqtt.client?.isConnected == true,
                mqttError = TuyaMqtt.mqttError
            )

            call.respond(GsonProvider.gson.toJson(currentConfig).toString())
        }

        post {
            val session = Session.headerToSession(call.request.headers)!!
            val body = call.receiveText()
            Logger.log("API", "Server setting update request made by ${session.uid}")

            val request = GsonProvider.gson.fromJson(body, ServerManagementRequest::class.java)

            if (request.changeMinPasswordLength != null)
                ServerSetting.setMinPasswordLength(request.changeMinPasswordLength)

            if (request.changeMaxImageSize != null)
                ServerSetting.setMaxImageSize(request.changeMaxImageSize)

            if (request.changeCurrentBaseUrl != null)
                ServerSetting.setCurrentBaseUrl(request.changeCurrentBaseUrl)

            if (request.changeLdapLoginEnabled != null)
                ServerSetting.setLdapLoginEnabled(request.changeLdapLoginEnabled)

            if (request.changeLdapServer != null)
                ServerSetting.setLdapServer(request.changeLdapServer)

            if (request.changeLdapBindUser != null)
                ServerSetting.setLdapBindUser(request.changeLdapBindUser)

            if (request.changeLdapBindPassword != null)
                ServerSetting.setLdapBindPassword(request.changeLdapBindPassword)

            if (request.changeLdapTlsEnabled != null)
                ServerSetting.setLdapTlsEnabled(request.changeLdapTlsEnabled)

            if (request.changeLdapUserDn != null)
                ServerSetting.setLdapUserDn(request.changeLdapUserDn)

            if (request.changeLdapUserFilter != null)
                ServerSetting.setLdapUserFilter(request.changeLdapUserFilter)

            if (request.changeLdapUserUidAttribute != null)
                ServerSetting.setLdapUserUidAttribute(request.changeLdapUserUidAttribute)

            if (request.changeLdapAdminGroupName != null)
                ServerSetting.setLdapAdminGroupName(request.changeLdapAdminGroupName)

            if (request.changeLdapGroupDn != null)
                ServerSetting.setLdapGroupDn(request.changeLdapGroupDn)

            if (request.changeLdapGroupFilter != null)
                ServerSetting.setLdapGroupFilter(request.changeLdapGroupFilter)

            if (request.changeMqttServer != null) {
                ServerSetting.setMqttSever(request.changeMqttServer)
                TuyaMqtt.startMqttListener()
            }

            if (request.changeMqttCertificate != null) {
                ServerSetting.setMqttCertificate(request.changeMqttCertificate)
                TuyaMqtt.startMqttListener()
            }

            val response = ServerManagementResponse(
                mqttIsConnected = TuyaMqtt.client?.isConnected == true,
                mqttError = TuyaMqtt.mqttError
            )

            if (request.runLdapUserTestOnUser != null)
                response.ldapUserTestResponse = Ldap().userTest(request.runLdapUserTestOnUser)

            if (request.runLdapGroupTestOnUser != null)
                response.ldapGroupTestResponse = Ldap().groupTest(request.runLdapGroupTestOnUser)

            call.respond(GsonProvider.gson.toJson(response).toString())
        }
    }
}