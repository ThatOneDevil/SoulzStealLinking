package me.thatonedevil.soulNetworkPlugin

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.soulLogger
import me.thatonedevil.soulNetworkPlugin.chat.DiscordToMc
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.session.ShutdownEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.MemberCachePolicy
import java.awt.Color
import java.util.concurrent.*
import java.util.function.Consumer


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

        val threadFactory = ThreadFactoryBuilder().setNameFormat("SoulNetworkPluginShutdown").build()
        val executor = Executors.newSingleThreadExecutor(threadFactory)

        try {
            if (!isReady || jda.status != JDA.Status.CONNECTED || !jdaEnabled) {
                logError("JDA is not ready, skipping shutdown")
                return
            }

            executor.invokeAll(listOf(Callable {
                jda.eventManager.registeredListeners.forEach(Consumer { listener: Any? ->
                    jda.eventManager.unregister(
                        listener!!
                    )
                })

                val shutdownTask = CompletableFuture<Void?>()
                jda.addEventListener(object : ListenerAdapter() {
                    override fun onShutdown(event: ShutdownEvent) {
                        shutdownTask.complete(null)
                    }
                })
                jda.shutdownNow()
                try {
                    shutdownTask[5, TimeUnit.SECONDS]
                } catch (e: TimeoutException) {
                    logError("JDA took too long to shut down, skipping")
                }

            }), 15, TimeUnit.SECONDS);
        } catch (e: Exception) {
            logError("$e")
        }
        executor.shutdownNow()
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
