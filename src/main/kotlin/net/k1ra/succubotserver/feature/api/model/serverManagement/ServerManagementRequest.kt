package net.k1ra.succubotserver.feature.api.model.serverManagement

@Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE", "UNUSED_PARAMETER")
class ServerManagementRequest(
    vararg nothingsSoParamsByNameAreForced: Nothing,
    val changeMinPasswordLength: Int? = null,
    val changeMaxImageSize: Int? = null,
    val changeCurrentBaseUrl: String? = null,
    val changeLdapLoginEnabled: Boolean? = null,

    val changeLdapServer: String? = null,
    val changeLdapBindUser: String? = null,
    val changeLdapBindPassword: String? = null,
    val changeLdapTlsEnabled: Boolean? = null,

    val changeLdapUserDn: String? = null,
    val changeLdapUserFilter: String? = null,
    val changeLdapUserUidAttribute: String? = null,

    val changeLdapAdminGroupName: String? = null,
    val changeLdapGroupDn: String? = null,
    val changeLdapGroupFilter: String? = null,

    val changeMqttServer: String? = null,
    val changeMqttCertificate: String? = null,

    val runLdapUserTestOnUser: String? = null,
    val runLdapGroupTestOnUser: String? = null
)