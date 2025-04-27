package me.thatonedevil.soulNetworkPlugin.commads.discord

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit

class PlayerList : ListenerAdapter() {

    override fun onSlashCommandInteraction(e: SlashCommandInteractionEvent) {
        if (e.name == "playerlist") {

            val member = e.member
            if (member == null || !member.hasPermission(Permission.ADMINISTRATOR)) {
                e.reply("❌ You do not have permission to use this command!").setEphemeral(true).queue()
                return
            }

            val playerList = Bukkit.getOnlinePlayers()
            val names: MutableList<String> = mutableListOf()
            for (player in playerList) {
                names.add(player.name)
            }

            val message = StringBuilder()
            message.append("**Online Players:**\n")
            if (playerList.isEmpty()) {
                message.append("No players online.")
            }else{
                message.append("\n``${names.joinToString(", ")}``\n")
            }

            message.append("\n**Total Players:** ${playerList.size}")

            e.reply(message.toString()).queue()
        }
    }
}