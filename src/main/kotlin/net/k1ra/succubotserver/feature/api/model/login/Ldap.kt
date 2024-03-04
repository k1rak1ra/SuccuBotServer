package net.k1ra.succubotserver.feature.api.model.login


import kotlinx.coroutines.delay
import net.k1ra.succubotserver.feature.api.model.settings.ServerSetting
import org.ldaptive.*
import org.ldaptive.auth.*
import org.ldaptive.handler.LdapEntryHandler
import org.ldaptive.handler.ResultHandler
import java.time.Duration


class Ldap {
    private val connection: ConnectionConfig? = if (ServerSetting.getLdapServer().isEmpty()) {
        null
    } else if (ServerSetting.getLdapBindUser().isEmpty()) {
        ConnectionConfig.builder()
            .url(ServerSetting.getLdapServer())
            .useStartTLS(ServerSetting.getLdapTlsEnabled())
            .connectTimeout(Duration.ofSeconds(30))
            .build()
    } else {
        ConnectionConfig.builder()
            .url(ServerSetting.getLdapServer())
            .useStartTLS(ServerSetting.getLdapTlsEnabled())
            .connectionInitializers(
                BindConnectionInitializer.builder()
                    .dn(ServerSetting.getLdapBindUser())
                    .credential(ServerSetting.getLdapBindPassword())
                    .build()
            )
            .connectTimeout(Duration.ofSeconds(30))
            .build()
    }

    suspend fun groupTest(user: String) : LdapGroupTestResponse {
        val result = LdapGroupTestResponse()
        var done = false

        try {
            SearchOperation.builder()
                .factory(DefaultConnectionFactory(connection))
                .onEntry(LdapEntryHandler {
                    result.groups.add(it.getAttribute("cn").stringValue)
                    it
                })
                .onResult(ResultHandler {
                    result.isAdmin = result.groups.contains(ServerSetting.getLdapAdminGroupName())
                    done = true
                }).build()
                .send(SearchRequest.builder()
                    .dn(ServerSetting.getLdapGroupDn())
                    .filter(ServerSetting.getLdapGroupFilter().replace("{user}", user))
                    .returnAttributes("cn")
                    .build()
                )
        } catch (e: Exception) {
            e.printStackTrace()
            result.error = e.toString()
            done = true
        }

        //Wait for request to complete, since we want to use direct returns and not callbacks
        while (!done)
            delay(100)

        return result
    }

    suspend fun userTest(user: String) : LdapUserTestResponse {
        val result = LdapUserTestResponse()
        var done = false

        try {
            SearchOperation.builder()
                .factory(DefaultConnectionFactory(connection))
                .onEntry(LdapEntryHandler {
                    result.elements.add(it.dn)
                    it.attributes.forEach {
                        result.elements.add("   ${it.name} : ${it.stringValue}")
                    }
                    it
                }).onResult(ResultHandler {
                    done = true
                }).build()
                .send(SearchRequest.builder()
                    .dn(ServerSetting.getLdapUserDn())
                    .filter(ServerSetting.getLdapUserFilter().replace("{user}", user))
                    .build()
            )

        } catch (e: Exception) {
            e.printStackTrace()
            result.error = e.toString()
            done = true
        }

        //Wait for request to complete, since we want to use direct returns and not callbacks
        while (!done)
            delay(100)

        return result
    }

    suspend fun isUserAdmin(user: String) : Boolean {
        var done = false
        var userIsAdmin = false

        try {
            SearchOperation.builder()
                .factory(DefaultConnectionFactory(connection))
                .onEntry(LdapEntryHandler {
                    if (it.getAttribute("cn").stringValue == ServerSetting.getLdapAdminGroupName())
                        userIsAdmin = true
                    it
                })
                .onResult(ResultHandler {
                    done = true
                }).build()
                .send(SearchRequest.builder()
                    .dn(ServerSetting.getLdapGroupDn())
                    .filter(ServerSetting.getLdapGroupFilter().replace("{user}", user))
                    .returnAttributes("cn")
                    .build()
                )
        } catch (e: Exception) {
            e.printStackTrace()
            done = true
        }

        //Wait for request to complete, since we want to use direct returns and not callbacks
        while (!done)
            delay(100)

        return userIsAdmin
    }

    suspend fun doLogin(request: LoginRequest) : User? {
        var doneUserSearch = false

        try {
            val authResolver = SearchDnResolver.builder()
                .factory(DefaultConnectionFactory(connection))
                .dn(ServerSetting.getLdapUserDn())
                .filter(ServerSetting.getLdapUserFilter().replace("{user}", request.email))
                .build()

            val authHandler = SimpleBindAuthenticationHandler(DefaultConnectionFactory(connection))
            val auth = Authenticator(authResolver, authHandler)
            val response = auth.authenticate(AuthenticationRequest(request.email, Credential(request.password)))

            if (response.isSuccess) {
                val attributes: MutableMap<String, String> = mutableMapOf()
                SearchOperation.builder()
                    .factory(DefaultConnectionFactory(connection))
                    .onEntry(LdapEntryHandler {
                        it.attributes.forEach {
                            attributes[it.name] = it.stringValue
                        }
                        it
                    }).onResult(ResultHandler {
                        doneUserSearch = true
                    }).build()
                    .send(SearchRequest.builder()
                        .dn(ServerSetting.getLdapUserDn())
                        .filter(ServerSetting.getLdapUserFilter().replace("{user}", request.email))
                        .build()
                    )

                while (doneUserSearch)
                    delay(100)

                val userIsAdmin  = isUserAdmin(request.email)
                val uid = "LDAP-${attributes[ServerSetting.getLdapUserUidAttribute()]!!}"

                //If user does not yet exist in DB, insert user
                if (!User.containsUid(uid)) {
                    User.insertUserRaw(
                        User(
                            name = attributes["displayName"]!!,
                            picture = "null",
                            email = request.email,
                            uid = uid,
                            token = "",
                            admin = userIsAdmin,
                            native = false
                        ), null
                    )
                } else {
                    User.updateUserName(uid, attributes["displayName"]!!)
                    User.updateUserAdminStatus(uid, userIsAdmin)
                }

                return User.createSession(uid)
            } else {
                return null
            }

        } catch (e: Exception) {
            return null
        }
    }
}