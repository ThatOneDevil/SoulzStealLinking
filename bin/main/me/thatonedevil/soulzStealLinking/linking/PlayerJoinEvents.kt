package me.thatonedevil.soulzStealLinking.linking

import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.serverChat
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.updateChannelTopic
import me.thatonedevil.soulzStealLinking.data.DataManager
import me.thatonedevil.soulzStealLinking.data.DataManager.getPlayerData
import me.thatonedevil.soulzStealLinking.data.DataManager.loadPlayerData
import me.thatonedevil.soulzStealLinking.data.DataManager.savePlayerData
import net.dv8tion.jda.api.EmbedBuilder
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.awt.Color
import java.util.*
import kotlin.collections.HashMap

class PlayerJoinEvents : Listener {

    private val timejoined: HashMap<UUID, Long> = hashMapOf()


    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        val player = event.player
        loadPlayerData(player.uniqueId)

        val embed = EmbedBuilder()
            .setColor(Color.GREEN)
            .setAuthor("${player.name} joined the server!", null, "http://cravatar.eu/head/${player.uniqueId}.png")
            .setTimestamp(java.time.Instant.now())
            .build()

        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, Runnable {
            serverChat?.sendMessageEmbeds(embed)?.queue()
            updateChannelTopic()
        }, 20L)

        timejoined[player.uniqueId] = System.currentTimeMillis()

    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        savePlayerData(getPlayerData(player.uniqueId))

        val playTimeMillis = System.currentTimeMillis() - (timejoined[player.uniqueId] ?: System.currentTimeMillis())
        val playTimeFormatted = formatPlayTime(playTimeMillis)

        val embed = EmbedBuilder()
            .setColor(Color.RED)
            .setAuthor("${player.name} left the server!", null, "http://cravatar.eu/head/${player.uniqueId}.png")
            .setDescription("**Time played:** ``$playTimeFormatted``\n**IP: soulzsteal.minehut.gg**")
            .build()

        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, Runnable {
            serverChat?.sendMessageEmbeds(embed)?.queue()
            updateChannelTopic()
        }, 20L)

        timejoined.remove(player.uniqueId)
        DataManager.removePLayerData(player.uniqueId)

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