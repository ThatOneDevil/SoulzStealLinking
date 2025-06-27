package me.thatonedevil.soulNetworkPlugin.chat

import io.papermc.paper.event.player.AsyncChatEvent
import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChatToggle
import me.thatonedevil.soulNetworkPlugin.JdaManager.webhookClient
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.chatFilter
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.lpApi
import me.thatonedevil.soulNetworkPlugin.Utils.convertLegacyToMiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.concurrent.LinkedBlockingQueue
import java.util.regex.Pattern

class McToDiscord : Listener {

    companion object {
        private val messageQueue = LinkedBlockingQueue<QueuedMessage>()
        private const val TICKS_BETWEEN_MESSAGES = 60L
        private var lastMessageTime = System.currentTimeMillis()
        private const val BURST_LIMIT = 5
        private var messagesSentInBurst = 0
        private const val BURST_COOLDOWN = 10000L
        private val HEX_COLOR_PATTERN = Pattern.compile("[§&]x")
        private val ANY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9a-folkrnm]")
        private val MENTION_PATTERN = Pattern.compile("@everyone|@here|<@|!")

        fun startDispatcher() {
            Bukkit.getScheduler().runTaskTimerAsynchronously(
                instance,
                createDispatcherRunnable(),
                0L,
                TICKS_BETWEEN_MESSAGES
            )
            // Start queue cleaner
            Bukkit.getScheduler().runTaskTimerAsynchronously(
                instance,
                Runnable { cleanUpQueueIfNeeded() },
                0L,
                1200L // 60 seconds (20 ticks * 60)
            )
        }

        private fun createDispatcherRunnable(): Runnable = Runnable {
            if (shouldSkipDueToRateLimit()) return@Runnable

            resetBurstIfNeeded()

            val msg = messageQueue.poll() ?: return@Runnable
            sendMessageToDiscord(msg)
        }

        private fun shouldSkipDueToRateLimit(): Boolean {
            val currentTime = System.currentTimeMillis()
            return messagesSentInBurst >= BURST_LIMIT &&
                    currentTime - lastMessageTime < BURST_COOLDOWN
        }

        private fun resetBurstIfNeeded() {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastMessageTime > BURST_COOLDOWN) {
                messagesSentInBurst = 0
            }
        }

        private fun sendMessageToDiscord(msg: QueuedMessage) {
            try {
                webhookClient?.sendMessage(msg.content)
                    ?.setAvatarUrl(msg.avatarUrl)
                    ?.setUsername(msg.username)
                    ?.queue(
                        { _ -> handleSuccessfulSend() },
                        { error -> handleSendError(error, msg) }
                    )
            } catch (e: Exception) {
                instance.logger.warning("Error sending Discord message: ${e.message}")
            }
        }

        private fun handleSuccessfulSend() {
            lastMessageTime = System.currentTimeMillis()
            messagesSentInBurst++
        }

        private fun handleSendError(error: Throwable, msg: QueuedMessage) {
            if (error.toString().contains("429")) {
                messageQueue.offer(msg)
                lastMessageTime = System.currentTimeMillis()
                messagesSentInBurst = BURST_LIMIT
            }
        }

        private const val MAX_QUEUE_SIZE = 50
        private fun cleanUpQueueIfNeeded() {
            if (messageQueue.size > MAX_QUEUE_SIZE) {
                instance.logger.warning("[McToDiscord] Message queue exceeded $MAX_QUEUE_SIZE. Clearing old messages.")
                messageQueue.clear()
            }
        }
    }

    data class QueuedMessage(
        val content: String,
        val username: String,
        val avatarUrl: String
    )

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {
        if (event.isCancelled || !jdaEnabled || !serverChatToggle) return

        val message = event.message()
        val rawMessage = PlainTextComponentSerializer.plainText().serialize(message)
        val player = event.player

        if (checkAndHandleBadWords(rawMessage, player, event)) return
        if (checkAndBlockMentions(rawMessage, player)) return

        val prefix = getPlayerPrefix(player)
        val rawPrefix = stripColors(prefix)

        sendMessageToQueue(message, player, rawPrefix)
    }

    private fun checkAndHandleBadWords(rawMessage: String, player: Player, event: AsyncChatEvent): Boolean {
        val badWord = chatFilter?.findBadWord(rawMessage)
        if (badWord != null) {
            val chatFilterMessage = instance.config.getString("messages.chatFilter.blockedMessage")!!
                .replace("<badWord>", badWord)
            player.sendMessage(convertLegacyToMiniMessage(chatFilterMessage))
            event.isCancelled = true
            return true
        }
        return false
    }

    private fun checkAndBlockMentions(rawMessage: String, player: Player): Boolean {
        val matcher = MENTION_PATTERN.matcher(rawMessage)
        if (matcher.find()) {
            if (matcher.group() == "!" && player.isOp) return false
            return true
        }
        return false
    }

    private fun getPlayerPrefix(player: Player): String {
        val user = lpApi?.userManager?.getUser(player.uniqueId)
        val groupName = user?.primaryGroup

        return groupName?.let {
            lpApi?.groupManager?.getGroup(it)?.cachedData?.metaData?.prefix
        } ?: user?.cachedData?.metaData?.prefix ?: ""
    }

    private fun stripColors(prefix: String): String {
        return prefix.replace(HEX_COLOR_PATTERN.toRegex(), "")
            .replace(ANY_COLOR_PATTERN.toRegex(), "")
    }

    private fun sendMessageToQueue(message: net.kyori.adventure.text.Component, player: Player, rawPrefix: String) {
        val configMessage = instance.config.getString("messages.mcToDiscordMessage")!!
        val formattedMessage = configMessage.replace("<message>",
            LegacyComponentSerializer.legacyAmpersand().serialize(message))
        val avatarUrl = "https://cravatar.eu/head/${player.uniqueId}.png"

        messageQueue.offer(
            QueuedMessage(
                content = formattedMessage,
                username = "$rawPrefix${player.name}",
                avatarUrl = avatarUrl
            )
        )
    }
}
