package me.thatonedevil.soulzStealLinking.chat

import io.papermc.paper.event.player.AsyncChatEvent
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.jda
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.lpApi
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import java.util.regex.Pattern


class McToDiscord : Listener {

    @EventHandler
    fun asyncChatEvent(event: AsyncChatEvent) {
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

        if (message.contains(Component.text("@everyone"))) {
            return
        }

        if (message.contains(Component.text("@here"))) {
            return
        }

        if (message.contains(Component.text("@here"))) {
            return
        }

        if (message.contains(Component.text("<@"))) {
            return
        }

        val formattedMessage = configMessage
            .replace("<player>", player.name)
            .replace("<prefix>", rawPrefix)
            .replace("<message>", LegacyComponentSerializer.legacyAmpersand().serialize(message))

        jda.getGuildById(1237080924290682953)
            ?.getTextChannelById(1237120448433487974)
            ?.sendMessage(formattedMessage)
            ?.queue()

    }
}