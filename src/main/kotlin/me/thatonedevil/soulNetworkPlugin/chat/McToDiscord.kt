package me.thatonedevil.soulNetworkPlugin.chat

import io.papermc.paper.event.player.AsyncChatEvent
import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.JdaManager.webhookClient
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.chatFilter
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.lpApi
import me.thatonedevil.soulNetworkPlugin.Utils.convertLegacyToMiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.concurrent.LinkedBlockingQueue
import java.util.regex.Pattern


class McToDiscord : Listener {

    companion object {
        private val messageQueue = LinkedBlockingQueue<QueuedMessage>()
        private const val TICKS_BETWEEN_MESSAGES = 40L // 2 seconds (20 ticks = 1 second)

        fun startDispatcher() {
            Bukkit.getScheduler().runTaskTimerAsynchronously(
                instance, Runnable {
                    val msg = messageQueue.poll() ?: return@Runnable

                    webhookClient?.sendMessage(msg.content)
                        ?.setAvatarUrl(msg.avatarUrl)
                        ?.setUsername(msg.username)
                        ?.queue()
                },
                0L,
                TICKS_BETWEEN_MESSAGES
            )
        }
    }

    data class QueuedMessage(
        val content: String,
        val username: String,
        val avatarUrl: String
    )

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {
        if (event.isCancelled || !jdaEnabled) return

        val message = event.message()
        val rawMessage = PlainTextComponentSerializer.plainText().serialize(message)
        val player = event.player

        val badWord = chatFilter?.findBadWord(rawMessage)
        if (badWord != null) {
            val chatFilterMessage = instance.config.getString("messages.chatFilter.blockedMessage")!!
                .replace("<badWord>", badWord)
            player.sendMessage(convertLegacyToMiniMessage(chatFilterMessage))
            event.isCancelled = true
            return
        }

        val configMessage = instance.config.getString("messages.mcToDiscordMessage")!!
        val user = lpApi?.userManager?.getUser(player.uniqueId)
        val groupName = user?.primaryGroup

        val prefix = groupName?.let {
            lpApi?.groupManager?.getGroup(it)?.cachedData?.metaData?.prefix
        } ?: user?.cachedData?.metaData?.prefix ?: ""

        val HEX_COLOR_PATTERN = Pattern.compile("[§&]x")
        val ANY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9a-folkrnm]")
        val mentionPattern = Pattern.compile("@everyone|@here|<@|!")

        val rawPrefix = prefix.replace(HEX_COLOR_PATTERN.toRegex(), "")
            .replace(ANY_COLOR_PATTERN.toRegex(), "")

        val matcher = mentionPattern.matcher(rawMessage)
        if (matcher.find()) {
            if (matcher.group() == "!" && player.isOp) return
            return
        }

        val formattedMessage = configMessage.replace("<message>", LegacyComponentSerializer.legacyAmpersand().serialize(message))
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
