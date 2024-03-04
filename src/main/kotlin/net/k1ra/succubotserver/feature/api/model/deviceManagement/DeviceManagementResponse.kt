package net.k1ra.succubotserver.feature.api.model.deviceManagement

data class DeviceManagementResponse(
    var mqttKeyValid: Boolean,
    var webKeyValid: Boolean?
)