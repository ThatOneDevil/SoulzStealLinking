package me.thatonedevil.soulzStealLinking.chat

import io.papermc.paper.event.player.AsyncChatEvent
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.jda
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.lpApi
import net.dv8tion.jda.api.entities.WebhookClient
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.regex.Pattern


class McToDiscord : Listener {

    private data class QueuedMessage(
        val playerName: String,
        val message: String
    )

    private val messageQueue = ConcurrentLinkedQueue<QueuedMessage>()
    private val BATCH_SIZE = 10
    private val FLUSH_INTERVAL_TICKS = 40L // 2 seconds (20 ticks = 1 second)

    init {
        // Schedule periodic flush of messages
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, Runnable {
            flushMessages()
        }, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS)
    }

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {
        if (event.isCancelled) {
            return
        }

        val player = event.player
        val message = event.message()

        val user = lpApi?.userManager?.getUser(player.uniqueId)
        val groupName = user?.primaryGroup

        val prefix = groupName?.let { group ->
            lpApi?.groupManager?.getGroup(group)?.cachedData?.metaData?.prefix
        } ?: user?.cachedData?.metaData?.prefix ?: ""

        // Strip MiniMessage format tags and additional formatting from prefix
        val cleanPrefix = prefix
            .replace(Regex("<[^>]*>"), "") // Remove all tags like <b>, <gradient:...>, </b>, etc.
            .replace(Regex("&#[0-9a-fA-F]{6}"), "") // Remove hex color codes like &#EC0E0E
            .replace(Regex("[§&][0-9a-fA-Fklmnor]"), "") // Remove remaining color codes
            .trim()

        val mentionPattern = Pattern.compile("@everyone|@here|<@|!")

        val rawMessage = PlainTextComponentSerializer.plainText().serialize(message)

        val matcher = mentionPattern.matcher(rawMessage)

        if (matcher.find()) {
            if (matcher.group() == "!" && player.isOp) {
                return
            }
            return
        }

        // Create username with prefix if available
        val playerDisplayName = if (cleanPrefix.isNotEmpty()) {
            "$cleanPrefix ${player.name}"
        } else {
            player.name
        }

        val queuedMessage = QueuedMessage(
            playerName = playerDisplayName,
            message = rawMessage
        )

        messageQueue.offer(queuedMessage)

        // Flush immediately if batch size is reached
        if (messageQueue.size >= BATCH_SIZE) {
            flushMessages()
        }
    }

    private fun flushMessages() {
        if (messageQueue.isEmpty()) {
            return
        }

        val webhookUrl = instance.config.getString("webhook.url") ?: return
        val webhookClient = WebhookClient.createClient(jda, webhookUrl)

        val messagesToSend = mutableListOf<QueuedMessage>()

        // Drain all messages from the queue
        while (messageQueue.isNotEmpty()) {
            messageQueue.poll()?.let { messagesToSend.add(it) }
        }

        // Combine all messages into a single formatted message
        val combinedMessage = messagesToSend.joinToString("\n") { msg ->
            "${msg.playerName} > ${msg.message}"
        }

        // Send as a single webhook message
        webhookClient.sendMessage(combinedMessage)
            .queue()
    }
}