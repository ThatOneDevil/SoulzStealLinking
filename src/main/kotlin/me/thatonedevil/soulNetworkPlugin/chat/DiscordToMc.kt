package me.thatonedevil.soulNetworkPlugin.chat

import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChat
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.Utils.convertLegacyToMiniMessage
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit


class DiscordToMc : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!jdaEnabled) return

        val text = event.message.contentRaw
        val channelID = event.channel.id
        val authorID = event.author.id

        // webhook id
        if (authorID == instance.config.getString("webhook.webhookId") || event.author.isBot) {
            return
        }

        if (channelID != serverChat?.id) {
            return
        }

        val role = event.member?.roles?.firstOrNull()
        val discordName = event.author.name
        val color = role?.color
        val roleColor = color?.let { String.format("#%02X%02X%02X", it.red, it.green, it.blue) }
        val roleName = role?.name ?: ""

        val rawMessage = instance.config.getString("messages.discordToMcMessage")!!

        val formattedMessage = rawMessage
            .replace("<roleColor>", "<color:$roleColor>")
            .replace("<roleName>", roleName)
            .replace("<discordName>", discordName)
            .replace("<text>", text)

        Bukkit.broadcast(convertLegacyToMiniMessage(formattedMessage))

    }


}