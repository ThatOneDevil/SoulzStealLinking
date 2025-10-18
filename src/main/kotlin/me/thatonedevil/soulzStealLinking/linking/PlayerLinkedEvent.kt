package me.thatonedevil.soulzStealLinking.linking

import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

/**
 * This event is called when a player is linked or unlinked.
 * This event is only called using skript-reflect thus it is not used in the plugin.
 *
 * @param player The player that was linked or unlinked.
 * @param isLinked True if the player was linked, false if the player was unlinked.
 * @param uuid The uuid of the player that was linked or unlinked.
 */
@Suppress("unused")
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