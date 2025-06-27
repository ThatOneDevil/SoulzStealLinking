package me.thatonedevil.soulNetworkPlugin.chat

import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChat
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChatToggle
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.Utils.convertLegacyToMiniMessage
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit
import java.awt.Color

class DiscordToMc : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!shouldProcessMessage(event)) return

        val text = event.message.contentRaw
        val member = event.member ?: return

        val messageData = extractMessageData(member, text)
        sendToMinecraft(messageData)
    }

    private fun shouldProcessMessage(event: MessageReceivedEvent): Boolean {
        if (!jdaEnabled || !serverChatToggle) return false

        val authorID = event.author.id
        val channelID = event.channel.id

        // Ignore webhook messages or bot messages
        if (authorID == instance.config.getString("webhook.webhookId") || event.author.isBot) return false

        // Only process messages from the designated chat channel
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

    private fun sendToMinecraft(data: MessageData) {
        val rawMessage = instance.config.getString("messages.discordToMcMessage")!!
        val formattedMessage = rawMessage
            .replace("<roleColor>", "<color:${data.roleColor}>")
            .replace("<roleName>", data.roleName)
            .replace("<discordName>", data.discordName)
            .replace("<text>", data.text)

        Bukkit.broadcast(convertLegacyToMiniMessage(formattedMessage))
    }

    private data class MessageData(
        val roleColor: String,
        val roleName: String,
        val discordName: String,
        val text: String
    )
}