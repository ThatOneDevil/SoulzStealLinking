package me.thatonedevil.soulzStealLinking

import me.thatonedevil.soulzStealLinking.data.DataManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit
import java.awt.Color

class UserInfoCommand : ListenerAdapter() {

    override fun onSlashCommandInteraction(e: SlashCommandInteractionEvent) {
        if (e.name == "userinfo") {

            if (!e.member!!.hasPermission(Permission.ADMINISTRATOR)) {
                e.reply("❌ You do not have permission to use this command!").setEphemeral(true).queue()
                return
            }

            val user = e.getOption("user")?.asUser!!
            val uuidOfUser = DataManager.getUUIDFromDiscordId(user.id)

            val minecraftName = uuidOfUser?.let { Bukkit.getPlayer(it)?.name } ?: "Not linked"

            val embed = EmbedBuilder()
                .setTitle("User Information")
                .setColor(Color(46, 204, 113))
                .setThumbnail(user.avatarUrl)
                .addField("Discord ID", user.id, false)
                .addField("Minecraft Name", minecraftName, false)
                .setFooter("SoulzSteal Linking System • ThatOneDevil", null)
                .build()

            e.replyEmbeds(embed).setEphemeral(true).queue()

        }

    }
}