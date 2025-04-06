package me.thatonedevil.soulzStealLinking.chat

import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.serverChat
import me.thatonedevil.soulzStealLinking.Utils.convertLegacyToMiniMessage
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.kyori.adventure.text.minimessage.MiniMessage

import org.bukkit.Bukkit


class DiscordToMc : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val text = event.message.contentRaw
        val channelID = event.channel.id
        val authorID = event.author.id

        if (authorID == "1237115078038524015") {
            return
        }

        if (channelID != serverChat?.id) {
            return
        }

        if (authorID == "1355620686349205504") {
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

        val miniMessageFormatted = convertLegacyToMiniMessage(formattedMessage)

        val serializedMessage = MiniMessage.miniMessage().deserialize(miniMessageFormatted)

        Bukkit.broadcast(serializedMessage)

    }


}