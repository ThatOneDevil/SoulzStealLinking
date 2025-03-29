package me.thatonedevil.soulzStealLinking.data

import java.util.*

data class LinkingData(
    val uuid: UUID,
    var linked: Boolean = false,
    var userId: String = "0"
)
