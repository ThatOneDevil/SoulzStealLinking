package me.thatonedevil.soulzStealLinking.linking

import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.Utils.convertLegacyToMiniMessage
import me.thatonedevil.soulzStealLinking.data.LinkManager
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

object LinkCode : CommandExecutor {

    private val linkingCodes: MutableMap<String, UUID> = mutableMapOf()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Only players can use this command!")
            return true
        }

        if (LinkManager(sender.uniqueId).isLinked) {
            val miniMessageFormatted =
                convertLegacyToMiniMessage(instance.config.getString("messages.linkedError").toString())
            val serializedMessage = MiniMessage.miniMessage().deserialize(miniMessageFormatted)
            sender.sendMessage(serializedMessage)
            return false
        }

        val code = (1..6)
            .map { ('A'..'Z') + ('0'..'9') }
            .flatten()
            .shuffled()
            .take(6)
            .joinToString("")

        linkingCodes[code] = sender.uniqueId

        val rawMessage = instance.config.getString("messages.linkCodeMessage")

        val formattedMessage = rawMessage?.replace("<code>", code)

        val miniMessageFormatted = convertLegacyToMiniMessage(formattedMessage.toString())
        val serializedMessage = MiniMessage.miniMessage().deserialize(miniMessageFormatted)

        sender.sendMessage(serializedMessage.clickEvent(ClickEvent.copyToClipboard(code)))

        Bukkit.getScheduler().runTaskLater(instance, Runnable {
            if (linkingCodes.containsKey(code)) {
                linkingCodes.remove(code)
                val expiredMessage = instance.config.getString("messages.linkCodeExpired")
                val miniMessageExpired = convertLegacyToMiniMessage(expiredMessage.toString())
                val serializedExpiredMessage = MiniMessage.miniMessage().deserialize(miniMessageExpired)
                sender.sendMessage(serializedExpiredMessage)
            }
        }, 600L)

        return true
    }

    fun getUUIDFromCode(code: String): UUID? {
        return linkingCodes[code]
    }

    fun removeCode(code: String) {
        linkingCodes.remove(code)
    }


}