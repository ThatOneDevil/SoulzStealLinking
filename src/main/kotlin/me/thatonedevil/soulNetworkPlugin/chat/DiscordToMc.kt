package me.thatonedevil.soulNetworkPlugin.chat

import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChat
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChatToggle
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.Utils.convertLegacyToMiniMessage
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit
import java.awt.Color
import java.util.concurrent.ConcurrentLinkedQueue

class DiscordToMc : ListenerAdapter() {

    private data class MessageData(
        val roleColor: String,
        val roleName: String,
        val discordName: String,
        val text: String
    )

    private val messageQueue = ConcurrentLinkedQueue<MessageData>()
    private val BATCH_SIZE = 10
    private val FLUSH_INTERVAL_TICKS = 40L // 2 seconds (20 ticks = 1 second)

    init {
        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, Runnable {
            flushMessages()
        }, FLUSH_INTERVAL_TICKS, FLUSH_INTERVAL_TICKS)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!shouldProcessMessage(event)) return

        val text = event.message.contentRaw
        val member = event.member ?: return

        val messageData = extractMessageData(member, text)
        messageQueue.offer(messageData)

        if (messageQueue.size >= BATCH_SIZE) {
            flushMessages()
        }
    }

    private fun shouldProcessMessage(event: MessageReceivedEvent): Boolean {
        if (!jdaEnabled || !serverChatToggle) return false

        val authorID = event.author.id
        val channelID = event.channel.id

        if (authorID == instance.config.getString("webhook.webhookId") || event.author.isBot) return false
        if (channelID != serverChat?.id) return false

        return true
    }

    private fun extractMessageData(member: Member, text: String): MessageData {
        val role = member.roles.firstOrNull()
        val discordName = member.user.name
        val roleColor = formatRoleColor(role?.color)
        val roleName = role?.name ?: ""

        return MessageData(roleColor, roleName, discordName, text)
    }

    private fun formatRoleColor(color: Color?): String {
        return color?.let { String.format("#%02X%02X%02X", it.red, it.green, it.blue) } ?: "#FFFFFF"
    }

    private fun flushMessages() {
        if (messageQueue.isEmpty()) return

        val rawTemplate = instance.config.getString("messages.discordToMcMessage") ?: return

        val messagesToSend = mutableListOf<String>()
        while (messageQueue.isNotEmpty()) {
            val data = messageQueue.poll() ?: continue
            val formatted = rawTemplate
                .replace("<roleColor>", "<color:${data.roleColor}>")
                .replace("<roleName>", data.roleName)
                .replace("<discordName>", data.discordName)
                .replace("<text>", data.text)
            messagesToSend.add(formatted)
        }

        if (messagesToSend.isEmpty()) return

        val combined = messagesToSend.joinToString("\n")
        Bukkit.broadcast(convertLegacyToMiniMessage(combined))
    }
}