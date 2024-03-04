package net.k1ra.succubotserver.feature.api.model.login

data class LdapUserTestResponse(
    val elements: ArrayList<String> = arrayListOf(),
    var error: String? = null
)