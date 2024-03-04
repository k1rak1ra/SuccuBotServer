package net.k1ra.succubotserver.feature.tuyaHttp

import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.k1ra.succubotserver.StorageManager
import net.k1ra.succubotserver.feature.logging.Logger
import net.k1ra.succubotserver.feature.tuyaHttp.routes.*
import java.io.File

class TuyaHttpApplication {
    private val environment: ApplicationEngineEnvironment

    init {
        val keyStoreFile = File("${StorageManager.getLocalStorageDir()}keystore.jks")
        keyStoreFile.delete()
        val keyStore = buildKeyStore {
            certificate("key") {
                password = "dummypass"
                domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
            }
        }
        keyStore.saveToFile(keyStoreFile, "dummypass")

         environment = applicationEngineEnvironment {
            connector {
                port = 80
            }
            sslConnector(
                keyStore = keyStore,
                keyAlias = "key",
                keyStorePassword = { "dummypass".toCharArray() },
                privateKeyPassword = { "dummypass".toCharArray() }) {
                port = 443
                keyStorePath = keyStoreFile
            }
            module {
                Logger.log("TuyaHttp", "TuyaHttp online")

                routing {
                    djson()
                    deviceDnsQuery()
                    uploadRoute()
                    uploadLayout()
                    urlConfig()
                }
            }
         }
    }

    fun start() = CoroutineScope(Dispatchers.IO).launch {
        embeddedServer(Jetty, environment).start(wait = true)
    }
}