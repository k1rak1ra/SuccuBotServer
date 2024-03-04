package net.k1ra.succubotserver.feature.logging

import java.time.LocalDateTime

object Logger {
    fun log(tag: String, log: String) {
        println("${LocalDateTime.now()} [$tag] $log")
    }
}