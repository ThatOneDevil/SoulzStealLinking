package me.thatonedevil.soulNetworkPlugin.linking

import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChat
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.awt.Color
import java.time.Instant
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class PlayerJoinEvents : Listener {

    private val timeJoined: HashMap<UUID, Long> = hashMapOf()

    companion object {
        private val embedQueue = LinkedBlockingQueue<MessageEmbed>()
        private const val SEND_DELAY = 2500L
        private var isProcessingQueue = false
        private var lastEmbedSentTime = 0L
        private var backoffTime = 2500L
        private const val MAX_BACKOFF_TIME = 60000L

        fun startEmbedDispatcher() {
            if (isProcessingQueue) return
            isProcessingQueue = true

            Bukkit.getScheduler().runTaskTimerAsynchronously(
                instance,
                createEmbedDispatcherRunnable(),
                0L,
                20L
            )
        }

        private fun createEmbedDispatcherRunnable(): Runnable = Runnable {
            if (shouldDelayNextEmbed()) return@Runnable

            val embed = embedQueue.poll() ?: return@Runnable
            sendEmbedToDiscord(embed)
        }

        private fun shouldDelayNextEmbed(): Boolean {
            val currentTime = System.currentTimeMillis()
            return currentTime - lastEmbedSentTime < backoffTime
        }

        private fun sendEmbedToDiscord(embed: MessageEmbed) {
            try {
                serverChat?.sendMessageEmbeds(embed)?.queue(
                    { _ -> handleSuccessfulSend() },
                    { error -> handleSendError(error, embed) }
                )
            } catch (e: Exception) {
                instance.logger.warning("Exception while sending Discord embed: ${e.message}")
                lastEmbedSentTime = System.currentTimeMillis()
            }
        }

        private fun handleSuccessfulSend() {
            lastEmbedSentTime = System.currentTimeMillis()
            reduceBackoffTime()
        }

        private fun reduceBackoffTime() {
            if (backoffTime > SEND_DELAY) {
                backoffTime = SEND_DELAY.coerceAtLeast(backoffTime / 2)
            }
        }

        private fun handleSendError(error: Throwable, embed: MessageEmbed) {
            if (error.toString().contains("429")) {
                handleRateLimit(embed)
            } else {
                instance.logger.warning("Error sending Discord embed: ${error.message}")
            }
            lastEmbedSentTime = System.currentTimeMillis()
        }

        private fun handleRateLimit(embed: MessageEmbed) {
            embedQueue.offer(embed)
            increaseBackoffTime()
            instance.logger.warning("Discord rate limit hit! Backing off for ${backoffTime/1000} seconds.")
        }

        private fun increaseBackoffTime() {
            backoffTime *= 2
            if (backoffTime > MAX_BACKOFF_TIME) backoffTime = MAX_BACKOFF_TIME
        }
    }

    @EventHandler
    fun onPlayerJoinEvent(event: PlayerJoinEvent) {
        if (!jdaEnabled) return

        val player = event.player
        val joinEmbed = createJoinEmbed(player)

        embedQueue.offer(joinEmbed)
        startEmbedDispatcher()

        timeJoined[player.uniqueId] = System.currentTimeMillis()
    }

    private fun createJoinEmbed(player: Player): MessageEmbed {
        return EmbedBuilder()
            .setColor(Color.GREEN)
            .setAuthor("${player.name} joined the server!", null, getPlayerAvatarUrl(player))
            .setTimestamp(Instant.now())
            .build()
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        if (!jdaEnabled) return

        val player = event.player
        val playTimeMillis = getPlayTime(player)
        val playTimeFormatted = formatPlayTime(playTimeMillis)

        val leaveEmbed = createLeaveEmbed(player, playTimeFormatted)

        embedQueue.offer(leaveEmbed)
        startEmbedDispatcher()

        timeJoined.remove(player.uniqueId)
    }

    private fun getPlayTime(player: Player): Long {
        return System.currentTimeMillis() - (timeJoined[player.uniqueId] ?: System.currentTimeMillis())
    }

    private fun createLeaveEmbed(player: Player, playTime: String): MessageEmbed {
        return EmbedBuilder()
            .setColor(Color.RED)
            .setAuthor("${player.name} left the server!", null, getPlayerAvatarUrl(player))
            .setDescription("**Time played:** ``$playTime``\n**IP: soulnetwork.minehut.gg**")
            .build()
    }

    private fun getPlayerAvatarUrl(player: Player): String {
        return "https://cravatar.eu/head/${player.uniqueId}.png"
    }

    private fun formatPlayTime(milliseconds: Long): String {
        val totalSeconds = milliseconds / 1000
        val days = totalSeconds / (24 * 3600)
        val hours = (totalSeconds % (24 * 3600)) / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            days > 0 -> "${days}d, ${hours}h, ${minutes}m and ${seconds}s"
            hours > 0 -> "${hours}h, ${minutes}m and ${seconds}s"
            minutes > 0 -> "${minutes}m and ${seconds}s"
            else -> "${seconds}s"
        }
    }
}