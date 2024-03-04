package net.k1ra.succubotserver.feature.api.model.appSettings

@Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE", "UNUSED_PARAMETER")
class UserRequest(
    vararg nothingsSoParamsByNameAreForced: Nothing,
    val passwordChange: PasswordChange? = null,
    val uploadImage: String? = null,
    val changeName: String? = null,
    val changeEmail: String? = null,
    val logout: Boolean? = null,
    val logoutOutOfAll: Boolean? = null
)