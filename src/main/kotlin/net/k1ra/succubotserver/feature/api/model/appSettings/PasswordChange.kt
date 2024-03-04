package net.k1ra.succubotserver.feature.api.model.appSettings

data class PasswordChange(
    val old: String,
    val new: String
)