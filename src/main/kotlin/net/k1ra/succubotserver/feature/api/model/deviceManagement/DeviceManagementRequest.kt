package net.k1ra.succubotserver.feature.api.model.deviceManagement

data class DeviceManagementRequest(
    val did: String,
    val webKey: String,
    val mqttKey: String
)