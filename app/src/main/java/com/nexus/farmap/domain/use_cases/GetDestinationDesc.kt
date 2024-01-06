package com.nexus.farmap.domain.use_cases

import android.content.Context
import com.nexus.farmap.R

class GetDestinationDesc {

    operator fun invoke(number: String, context: Context): String {

        val building = number[0]
        val floor = number[1]
        val room = number.drop(2)
        val floorStr = context.getString(R.string.floor)
        val roomStr = context.getString(R.string.room)

        return "$building, $floorStr$floor, $roomStr$room"
    }

}