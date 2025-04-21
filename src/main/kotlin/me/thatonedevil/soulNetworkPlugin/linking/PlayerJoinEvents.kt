package me.thatonedevil.soulNetworkPlugin.linking

import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChat
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import net.dv8tion.jda.api.EmbedBuilder
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.awt.Color
import java.util.*

class PlayerJoinEvents : Listener {

    private val timeJoined: HashMap<UUID, Long> = hashMapOf()


    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        if (!jdaEnabled) return
        val player = event.player

        val embed = EmbedBuilder()
            .setColor(Color.GREEN)
            .setAuthor("${player.name} joined the server!", null, "https://cravatar.eu/head/${player.uniqueId}.png")
            .setTimestamp(java.time.Instant.now())
            .build()

        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, Runnable {
            serverChat?.sendMessageEmbeds(embed)?.queue()
        }, 20L)

        timeJoined[player.uniqueId] = System.currentTimeMillis()

    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        if (!jdaEnabled) return

        val player = event.player

        val playTimeMillis = System.currentTimeMillis() - (timeJoined[player.uniqueId] ?: System.currentTimeMillis())
        val playTimeFormatted = formatPlayTime(playTimeMillis)

        val embed = EmbedBuilder()
            .setColor(Color.RED)
            .setAuthor("${player.name} left the server!", null, "https://cravatar.eu/head/${player.uniqueId}.png")
            .setDescription("**Time played:** ``$playTimeFormatted``\n**IP: soulzsteal.minehut.gg**")
            .build()

        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, Runnable {
            serverChat?.sendMessageEmbeds(embed)?.queue()
        }, 20L)

        timeJoined.remove(player.uniqueId)

    }

    private fun formatPlayTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        if (days > 0) {
            return "${days}d, ${hours}h, ${minutes}m and ${seconds}s"
        }

        if (hours > 0) {
            return "${hours}h, ${minutes}m and ${seconds}s"
        }

        if (minutes > 0) {
            return "${minutes}m and ${seconds}s"
        }

        if (seconds > 0) {
            return "${seconds}s"
        }

        return "${days}d, ${hours}h, ${minutes}m and ${seconds}s"
    }


}