package me.thatonedevil.soulNetworkPlugin.chat

import io.papermc.paper.event.player.AsyncChatEvent
import me.thatonedevil.soulNetworkPlugin.JdaManager.jda
import me.thatonedevil.soulNetworkPlugin.JdaManager.jdaEnabled
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.chatFilter
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.lpApi
import me.thatonedevil.soulNetworkPlugin.Utils.convertLegacyToMiniMessage
import net.dv8tion.jda.api.entities.WebhookClient
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.regex.Pattern


class McToDiscord : Listener {

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {

        if (event.isCancelled) return

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

        if (event.isCancelled) return

        if (!jdaEnabled) return

        val configMessage = instance.config.getString("messages.mcToDiscordMessage")!!

        val user = lpApi?.userManager?.getUser(player.uniqueId)
        val groupName = user?.primaryGroup

        val HEX_COLOR_PATTERN = Pattern.compile("[§&]x")
        val ANY_COLOR_PATTERN = Pattern.compile("(?i)[&§][0-9a-folkrnm]")

        val prefix = groupName?.let { group ->
            lpApi?.groupManager?.getGroup(group)?.cachedData?.metaData?.prefix
        } ?: user?.cachedData?.metaData?.prefix ?: ""

        val rawPrefix = prefix.replace(HEX_COLOR_PATTERN.toRegex(), "").replace(ANY_COLOR_PATTERN.toRegex(), "")

        val mentionPattern = Pattern.compile("@everyone|@here|<@|!")

        val matcher = mentionPattern.matcher(rawMessage)

        if (matcher.find()) {
            if (matcher.group() == "!" && player.isOp) {
                return
            }
            return
        }

        val formattedMessage = configMessage
            .replace("<message>", LegacyComponentSerializer.legacyAmpersand().serialize(message))

        val webhookUrl = instance.config.getString("webhook.url")

        val webhookClient = WebhookClient.createClient(jda, webhookUrl.toString())

        webhookClient.sendMessage(formattedMessage)
            .setAvatarUrl("https://cravatar.eu/head/${player.uniqueId}.png")
            .setUsername("$rawPrefix ${player.name}")
            .queue()

    }
}