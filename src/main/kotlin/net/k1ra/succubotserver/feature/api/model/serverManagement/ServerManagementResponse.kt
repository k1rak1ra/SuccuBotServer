package net.k1ra.succubotserver.feature.api.model.serverManagement

import net.k1ra.succubotserver.feature.api.model.login.LdapGroupTestResponse
import net.k1ra.succubotserver.feature.api.model.login.LdapUserTestResponse

class ServerManagementResponse(
    var ldapUserTestResponse: LdapUserTestResponse? = null,
    var ldapGroupTestResponse: LdapGroupTestResponse? = null,
    var mqttIsConnected: Boolean,
    val mqttError: String?
)