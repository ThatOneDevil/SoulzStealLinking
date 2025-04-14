package me.thatonedevil.soulNetworkPlugin.linking

import com.google.common.io.ByteStreams
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.messaging.PluginMessageListener

class PluginMessageListener : PluginMessageListener {
    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {

        val input = ByteStreams.newDataInput(message)

        if (channel == "soulzproxy:main") {
            val discordId = input.readUTF()
            val linked = input.readBoolean()

            if (linked) {
                Bukkit.getScheduler().runTask(instance, Runnable {
                    val playerLinkedEvent = PlayerLinkedEvent(player, discordId, true)
                    Bukkit.getPluginManager().callEvent(playerLinkedEvent)
                })
            }

        }

    }
}