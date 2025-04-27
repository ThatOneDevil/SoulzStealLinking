package me.thatonedevil.soulNetworkPlugin.linking

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class PlayerLinkedEvent (
    val player: Player,
    val isLinked: Boolean,
    val uuid: String,
) : Event() {

    override fun getHandlers(): HandlerList = handlerList

    companion object {
        @JvmStatic
        private val handlerList = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList = handlerList
    }
}