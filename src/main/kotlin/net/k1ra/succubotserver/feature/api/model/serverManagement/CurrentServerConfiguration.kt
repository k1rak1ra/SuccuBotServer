package net.k1ra.succubotserver.feature.api.model.serverManagement

class CurrentServerConfiguration(
    val minPasswordLength: Int,
    val maxImageSize: Int,
    val currentBaseUrl: String,
    val ldapLoginEnabled: Boolean,

    val ldapServer: String,
    val ldapBindUser: String,
    val ldapBindPassword: String,
    val ldapTlsEnabled: Boolean,

    val ldapUserDn: String,
    val ldapUserFilter: String,
    val ldapUserUidAttribute: String,

    val ldapAdminGroupName: String,
    val ldapGroupDn: String,
    val ldapGroupFilter: String,

    val mqttServerUrl: String,
    val mqttIsConnected: Boolean,
    val mqttError: String?
)