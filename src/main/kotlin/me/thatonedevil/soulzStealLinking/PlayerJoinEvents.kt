package me.thatonedevil.soulzStealLinking

import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.serverChat
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.updateChannelTopic
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

class PlayerJoinEvents : Listener {
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
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        val player = event.player
        savePlayerData(getPlayerData(player.uniqueId))

        val embed = EmbedBuilder()
            .setColor(Color.RED)
            .setAuthor("${player.name} left the server!", null, "http://cravatar.eu/head/${player.uniqueId}.png")
            .setDescription("**IP: soulzsteal.minehut.gg**")
            .build()

        Bukkit.getScheduler().runTaskLaterAsynchronously(instance, Runnable {
            serverChat?.sendMessageEmbeds(embed)?.queue()
            updateChannelTopic()
        }, 20L)

    }


}