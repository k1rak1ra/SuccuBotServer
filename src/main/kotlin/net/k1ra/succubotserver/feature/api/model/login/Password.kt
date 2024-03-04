package net.k1ra.succubotserver.feature.api.model.login

import org.mindrot.jbcrypt.BCrypt

object Password {
    private val saltLength = 13

    fun hash(pw: String?): String {
        return BCrypt.hashpw(pw, BCrypt.gensalt(saltLength))
    }

    fun verify(given: String, hash: String): Boolean {
        return BCrypt.checkpw(given, hash.replace("$2y$", "$2a$"))
    }
}