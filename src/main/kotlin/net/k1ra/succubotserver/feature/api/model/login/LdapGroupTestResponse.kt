package net.k1ra.succubotserver.feature.api.model.login

data class LdapGroupTestResponse(
    val groups: ArrayList<String> = arrayListOf(),
    var isAdmin: Boolean = false,
    var error: String? = null
)