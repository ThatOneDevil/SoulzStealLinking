package me.thatonedevil.soulzStealLinking.linking

import me.thatonedevil.soulzStealLinking.PlayerLinkedEvent
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.guild
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.verifiedRole
import me.thatonedevil.soulzStealLinking.Utils.convertLegacyToMiniMessage
import me.thatonedevil.soulzStealLinking.data.DataManager
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import net.dv8tion.jda.api.interactions.modals.Modal
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import java.awt.Color


class LinkEmbed : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name == "linkembed") {
            if (!event.member!!.hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ You do not have permission to use this command!").setEphemeral(true).queue()
                return
            }

            val embed = EmbedBuilder()
                .setTitle("🔗 Link Your Minecraft Account")
                .setColor(Color(46, 204, 113)) // Nice green color for linking success
                .setThumbnail("https://media.discordapp.net/attachments/1069014782524526643/1345116633760534543/soulz-01.png?ex=67d332fc&is=67d1e17c&hm=c1f59c6f1bce71e7d3a23c207bb2387084fe80fed0b06d5dfd2a81ae227a6d55&=&format=webp&quality=lossless&width=1149&height=813")
                .setDescription(
                    "⚡ **Easily connect your Discord & Minecraft accounts!**\n\n" +
                            "Follow the steps below to complete the linking process."
                )
                .addField("📌 **Step 1:**", "Use `/link` in Minecraft to generate a unique code.", false)
                .addField("📌 **Step 2:**", "Click the button below and enter your 6-digit code.", false)
                .addField("📌 **Step 3:**", "If the code is correct, your accounts will be linked automatically!", false)
                .setFooter("SoulzSteal Linking System • ThatOneDevil", null)
                .build()

            val button: Button = Button.primary("link", "Click to link")

            event.channel.sendMessageEmbeds(embed).addActionRow(button).queue()
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (event.componentId == "link") {
            val codeInput = TextInput.create("code_input", "Put code here", TextInputStyle.SHORT)
                .setPlaceholder("Enter your 6-digit code")
                .setRequired(true)
                .setMinLength(6)
                .setMaxLength(6)
                .build()

            val modal = Modal.create("link_modal", "Link Your Minecraft Account")
                .addActionRow(codeInput)
                .build()

            event.replyModal(modal).queue()
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        if (event.modalId == "link_modal") {
            val code = event.getValue("code_input")?.asString ?: return

            val uuid = LinkCode.getUUIDFromCode(code) // Retrieve UUID from code

            if (DataManager.getCachedUserMap().contains(event.user.id)) {
                event.reply("❌ User is already linked to an account!").setEphemeral(true).queue()
                return
            }

            if (uuid == null) {
                event.reply("❌ Invalid or expired code!").setEphemeral(true).queue()
                return
            }

            val player = Bukkit.getPlayer(uuid)
            val playerName = player?.name.toString()

            val embed = EmbedBuilder()
                .setTitle("✅ Account Linked Successfully!")
                .setColor(Color.GREEN)
                .setDescription("Your Discord account has been linked to your Minecraft profile.")
                .addField("🔗 Discord: ", "**${event.user.name}**", false)
                .addField("🎮 Minecraft: ", "**${playerName}**", false)
                .setThumbnail("http://cravatar.eu/head/${uuid}.png")
                .setFooter("Linking system by ThatOneDevil", null)
                .setTimestamp(java.time.Instant.now())
                .build()

            event.replyEmbeds(embed).setEphemeral(true).queue()

            val message = instance.config.getString("messages.linkedBroadcast")!!
            val formattedMessage = message
                .replace("<player>", playerName)

            val miniMessageFormatted = convertLegacyToMiniMessage(formattedMessage)
            val serializedMessage = MiniMessage.miniMessage().deserialize(miniMessageFormatted)

            Bukkit.broadcast(serializedMessage)

            val data = DataManager.getPlayerData(uuid)
            data.linked = true
            data.userId = event.user.id

            val playerLinkedEvent = PlayerLinkedEvent(player!!, event.user.id, true)
            Bukkit.getPluginManager().callEvent(playerLinkedEvent)

            guild?.addRoleToMember(event.user, verifiedRole!!)?.queue()

            DataManager.savePlayerData(data)
            LinkCode.removeCode(code)
        }
    }
}