package me.thatonedevil.soulzStealLinking

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerLinkedEvent(
    val player: Player,
    var name: String,
    var linked: Boolean,
    var userId: String
) : Event() {

    companion object {
        @JvmStatic
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }

    override fun getHandlers(): HandlerList = handlerList
}
