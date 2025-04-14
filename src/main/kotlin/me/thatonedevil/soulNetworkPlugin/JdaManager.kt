package me.thatonedevil.soulNetworkPlugin

import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.soulLogger
import me.thatonedevil.soulNetworkPlugin.chat.DiscordToMc
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.bukkit.Bukkit
import java.awt.Color
import java.time.Duration
import java.util.concurrent.CompletableFuture

object JdaManager {
    lateinit var jda: JDA
    var jdaEnabled: Boolean = instance.config.getBoolean("jdaEnabled")
    private var isReady: Boolean = false
    private var guild: Guild? = null
    var serverChat: TextChannel? = null
    private var verifiedRole: Role? = null

    fun init() {
        if (!jdaEnabled) return

        val token = instance.config.getString("token")
        if (token.isNullOrEmpty()) {
            instance.logger.severe("Missing bot token in config.yml")
            instance.server.pluginManager.disablePlugin(instance)
            return
        }

        CompletableFuture.runAsync {
            runCatching {
                jda = JDABuilder.createDefault(token)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setBulkDeleteSplittingEnabled(false)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(DiscordToMc())
                    .build()
                    .awaitReady()

                isReady = true
                validateJDAConfig()
                registerCommands()
                sendStartupEmbed()
            }.onFailure {
                soulLogger!!.severe("Failed to init JDA: ${it.message}")
            }
        }
    }

    fun shutdown() {
        if (!jdaEnabled) return
        if (!isReady) return

        sendShutdownEmbed()
        updateChannelTopic(false)

        jda.shutdown()
        if (!jda.awaitShutdown(Duration.ofSeconds(10))) {
            jda.shutdownNow()
            jda.awaitShutdown()
        }
    }

    private fun validateJDAConfig() {
        if (!jdaEnabled) return

        val config = instance.config
        val guildId = config.getString("guildId")
        val serverChatId = config.getString("serverChatChannel")
        val verifiedRoleId = config.getString("verifiedRole")

        guild = jda.getGuildById(guildId ?: "")
        serverChat = jda.getTextChannelById(serverChatId ?: "")
        verifiedRole = guild?.getRoleById(verifiedRoleId ?: "")

        if (guild == null) logError("Guild not found ($guildId)")
        if (serverChat == null) logError("Server chat channel not found ($serverChatId)")
        if (verifiedRole == null) logError("Verified role not found ($verifiedRoleId)")
    }

    private fun registerCommands() {
        if (!jdaEnabled) return

        guild?.updateCommands()?.addCommands(
            Commands.slash("linkembed", "Makes the link embed"),
            Commands.slash("userinfo", "Shows user info")
                .addOptions(OptionData(OptionType.USER, "user", "The user to show info for").setRequired(true))
        )?.queue()
    }

    fun updateChannelTopic(online: Boolean = true) {
        if (!jdaEnabled) return

        if (!isReady || serverChat == null) return

        val topic = if (online) "Players: ${Bukkit.getOnlinePlayers().size}" else "Server Offline"
        jda.presence.activity = Activity.watching(topic)

        soulLogger?.info("Updated channel topic to: $topic")
    }

    private fun sendStartupEmbed() {
        if (!jdaEnabled) return

        val embed = EmbedBuilder().setColor(Color.GREEN)
            .setAuthor("Server started ✅", null, null)
            .build()

        serverChat?.sendMessageEmbeds(embed)?.queue()
    }

    private fun sendShutdownEmbed() {
        if (!jdaEnabled) return

        val embed = EmbedBuilder().setColor(Color.RED)
            .setAuthor("Server stopped ❌", null, null)
            .build()

        serverChat?.sendMessageEmbeds(embed)?.queue()
    }

    private fun logError(msg: String) {
        instance.logger.severe(msg)
    }
}
