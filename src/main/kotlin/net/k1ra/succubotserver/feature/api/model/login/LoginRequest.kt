package net.k1ra.succubotserver.feature.api.model.login

data class LoginRequest(
    val email: String,
    val password: String
)