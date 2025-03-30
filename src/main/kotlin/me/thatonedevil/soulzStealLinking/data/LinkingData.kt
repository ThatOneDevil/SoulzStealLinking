package me.thatonedevil.soulzStealLinking.data

import org.bukkit.Bukkit
import java.util.*

data class LinkingData(
    val uuid: UUID,
    var name: String = Bukkit.getPlayer(uuid)!!.name,
    var linked: Boolean = false,
    var userId: String = "0"
)
