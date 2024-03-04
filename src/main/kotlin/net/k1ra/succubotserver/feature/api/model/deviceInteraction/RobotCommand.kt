package net.k1ra.succubotserver.feature.api.model.deviceInteraction

import java.time.LocalTime

@Suppress("FORBIDDEN_VARARG_PARAMETER_TYPE", "UNUSED_PARAMETER")
class RobotCommand(
    vararg nothingsSoParamsByNameAreForced: Nothing,
    val runCleaningCycle: Boolean? = null,
    val goToCharge: Boolean? = null,
    val continueCleaningAfterCharge: Boolean? = null,
    val suctionPower: SuctionPowers? = null,
    val waterFlow: WaterFlowLevels? = null,
    val volumeLevels: VolumeLevels? = null,
    val changeName: String? = null,
    val changeAutoCleanEnabled: Boolean? = null,
    val changeAutoCleanTime: LocalTime? = null
)