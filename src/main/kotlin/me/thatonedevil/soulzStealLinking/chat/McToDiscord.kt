package me.thatonedevil.soulzStealLinking.chat

import io.papermc.paper.event.player.AsyncChatEvent
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.jda
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.lpApi
import net.dv8tion.jda.api.entities.WebhookClient
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.regex.Pattern


class McToDiscord : Listener {

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {
        if (event.isCancelled){
            return
        }

        val player = event.player
        val message = event.message()

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

        val rawMessage = PlainTextComponentSerializer.plainText().serialize(message)

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
            .setAvatarUrl("http://cravatar.eu/head/${player.uniqueId}.png")
            .setUsername("$rawPrefix ${player.name}")
            .queue()

    }
}