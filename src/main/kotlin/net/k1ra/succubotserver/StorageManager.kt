package net.k1ra.succubotserver

import java.util.*

class StorageManager {
    companion object {
        fun getLocalStorageDir(): String {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            return if (os.contains("win"))
                System.getenv("APPDATA") + "/succubot-server/"
            else if (os.contains("mac"))
                System.getProperty("user.home") + "/Library/Application Support/succubot-server/"
            else if (os.contains("nux"))
                System.getProperty("user.home") + "/succubot-server/"
            else
                System.getProperty("user.dir") + "/succubot-server/"
        }

        fun getUserImageFileWithName(name: String) : String {
            return "${getLocalStorageDir()}user_images/$name"
        }

        fun getMapStorageDir(did: String) : String {
            return "${getLocalStorageDir()}maps/$did"
        }
    }
}