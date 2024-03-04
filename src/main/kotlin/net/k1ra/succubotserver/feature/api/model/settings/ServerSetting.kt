package net.k1ra.succubotserver.feature.api.model.settings

import net.k1ra.succubotserver.feature.api.utils.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

class ServerSetting(
    val key: String,
    val value: String,
) {

    init {
        initialSetup()
    }

   companion object {
       private object ServerSettingTable: IntIdTable() {
           val key = text("key")
           val value = text("value")
       }

       private fun get(key: String) : String {
           var value: String? = null

           transaction {
               ServerSettingTable.select(ServerSettingTable.key eq key).forEach {
                   value = it[ServerSettingTable.value]
               }
           }

           return value ?: ""
       }

       private fun set(setting: ServerSetting) {
           transaction {
               if (ServerSettingTable.select(ServerSettingTable.key eq setting.key).count().toInt() == 0) {
                   ServerSettingTable.insert {
                       it[key] = setting.key
                       it[value] = setting.value
                   }
               } else {
                   ServerSettingTable.update({ServerSettingTable.key eq setting.key}){
                       it[value] = setting.value
                   }
               }
           }
       }

       fun getLdapLoginEnabled() : Boolean {
           return get(KEY_ENABLE_LDAP_LOGIN) == "1"
       }

       fun getLdapServer() : String {
           return get(KEY_LDAP_SERVER)
       }

       fun getLdapBindUser() : String {
           return get(KEY_LDAP_BIND_USER)
       }

       fun getLdapBindPassword() : String {
           return get(KEY_LDAP_BIND_PASSWORD)
       }

       fun getLdapTlsEnabled() : Boolean {
           return get(KEY_LDAP_TLS) == "1"
       }

       fun getLdapAdminGroupName() : String {
           return get(KEY_LDAP_ADMIN_GROUP_NAME)
       }

       fun getLdapGroupDn() : String {
           return get(KEY_LDAP_GROUP_DN)
       }

       fun getLdapGroupFilter() : String {
           return get(KEY_LDAP_GROUP_FILTER)
       }

       fun getLdapUserDn() : String {
           return get(KEY_LDAP_USER_DN)
       }

       fun getLdapUserFilter() : String {
           return get(KEY_LDAP_USER_FILTER)
       }

       fun getLdapUserUidAttribute() : String {
           return get(KEY_LDAP_USER_UID_ATTRIBUTE)
       }

       fun getMinPasswordLength() : Int {
           return Integer.parseInt(get(KEY_MIN_PASSWORD_LENGTH))
       }

       fun getMaxImageSize() : Int {
           return Integer.parseInt(get(KEY_MAX_IMAGE_SIZE))
       }

       fun getCurrentBaseUrl() : String {
           return get(KEY_CURRENT_BASE_URL)
       }

       fun getMqttCertificate() : String {
           return get(KEY_MQTT_CERTIFICATE)
       }

       fun getMqttServer() : String {
           return get(KEY_MQTT_SERVER)
       }

       fun setLdapLoginEnabled(enabled: Boolean) {
           set(ServerSetting(KEY_ENABLE_LDAP_LOGIN, if (enabled) "1" else "0"))
       }

       fun setLdapServer(value: String) {
           set(ServerSetting(KEY_LDAP_SERVER, value))
       }

       fun setLdapBindUser(value: String) {
           set(ServerSetting(KEY_LDAP_BIND_USER, value))
       }

       fun setLdapBindPassword(value: String) {
           set(ServerSetting(KEY_LDAP_BIND_PASSWORD, value))
       }

       fun setLdapTlsEnabled(enabled: Boolean) {
           set(ServerSetting(KEY_LDAP_TLS, if (enabled) "1" else "0"))
       }

       fun setLdapAdminGroupName(value: String) {
           set(ServerSetting(KEY_LDAP_ADMIN_GROUP_NAME, value))
       }

       fun setLdapGroupDn(value: String) {
           set(ServerSetting(KEY_LDAP_GROUP_DN, value))
       }

       fun setLdapGroupFilter(value: String) {
           set(ServerSetting(KEY_LDAP_GROUP_FILTER, value))
       }

       fun setLdapUserDn(value: String) {
           set(ServerSetting(KEY_LDAP_USER_DN, value))
       }

       fun setLdapUserFilter(value: String) {
           set(ServerSetting(KEY_LDAP_USER_FILTER, value))
       }

       fun setLdapUserUidAttribute(value: String) {
           set(ServerSetting(KEY_LDAP_USER_UID_ATTRIBUTE, value))
       }

       fun setMinPasswordLength(value: Int) {
           set(ServerSetting(KEY_MIN_PASSWORD_LENGTH, value.toString()))
       }

       fun setMaxImageSize(value: Int) {
           set(ServerSetting(KEY_MAX_IMAGE_SIZE, value.toString()))
       }

       fun setCurrentBaseUrl(value: String) {
           set(ServerSetting(KEY_CURRENT_BASE_URL, value))
       }

       fun setMqttCertificate(value: String) {
           set(ServerSetting(KEY_MQTT_CERTIFICATE, value))
       }

       fun setMqttSever(value: String) {
           set(ServerSetting(KEY_MQTT_SERVER, value))
       }

       @Synchronized
       fun initialSetup() {
           transaction {
               if (!ServerSettingTable.exists()) {
                   SchemaUtils.create(ServerSettingTable)

                   setLdapLoginEnabled(false)

                   setLdapServer("")
                   setLdapBindUser("")
                   setLdapBindPassword("")
                   setLdapTlsEnabled(false)

                   setLdapAdminGroupName("admins")
                   setLdapGroupDn("cn=groups,cn=compat,dc=example,dc=com")
                   setLdapGroupFilter("(&(objectClass=posixGroup)(memberUid={user}))")

                   setLdapUserDn("cn=users,cn=accounts,dc=example,dc=com")
                   setLdapUserFilter("uid={user}")
                   setLdapUserUidAttribute("uidNumber")

                   setMinPasswordLength(6)
                   setMaxImageSize(8 * 1048576) //8MB
                   setCurrentBaseUrl("https://example.com/vacbot/")

                   setMqttCertificate("")
                   setMqttSever("ssl://127.0.0.1:8883")
               }
           }
       }
   }
}