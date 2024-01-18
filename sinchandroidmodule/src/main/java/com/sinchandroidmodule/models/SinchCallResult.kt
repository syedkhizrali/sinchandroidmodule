package com.sinchandroidmodule.models

import com.sinchandroidmodule.helpers.SinchConstants
import java.io.Serializable

data class SinchCallResult(
    var callDurationInSec: Int = 0,
    var callEndBy: String = SinchConstants.UserType.receiver,
) : Serializable
