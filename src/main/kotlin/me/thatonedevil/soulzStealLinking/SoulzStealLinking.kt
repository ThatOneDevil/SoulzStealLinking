package me.thatonedevil.soulzStealLinking

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.thatonedevil.soulzStealLinking.chat.DiscordToMc
import me.thatonedevil.soulzStealLinking.chat.McToDiscord
import me.thatonedevil.soulzStealLinking.data.DataManager
import me.thatonedevil.soulzStealLinking.linking.LinkCode
import me.thatonedevil.soulzStealLinking.linking.LinkEmbed
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
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.awt.Color
import java.sql.DriverManager
import java.util.concurrent.Callable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.logging.Logger

class SoulzStealLinking : JavaPlugin() {

    companion object {
        lateinit var instance: JavaPlugin
        lateinit var jda: JDA
        var lpApi: LuckPerms? = null
        var soulLogger: Logger? = null
        var guild: Guild? = null
        var serverChat: TextChannel? = null
        var verifiedRole: Role? = null
        var isJdaReady = false

        fun updateChannelTopic(online: Boolean = true) {
            if (!isJdaReady || serverChat == null) {
                soulLogger?.severe("JDA is not ready or serverChat is not initialized.")
                return
            }

            val onlinePlayers = Bukkit.getOnlinePlayers().size

            val topic = if (online) {
                "Players: $onlinePlayers"
            } else {
                "Server Offline"
            }

            jda.presence.activity = Activity.watching(topic)

            soulLogger?.info("Updated channel topic to: $topic")
        }
    }

    override fun onEnable() {
        instance = this
        soulLogger = instance.logger

        config.options().copyDefaults(true)
        saveDefaultConfig()
        reloadConfig()

        val token = config.getString("token")
        if (token.isNullOrEmpty()) {
            logger.severe("Bot token is missing! Please set it in config.yml")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        CompletableFuture.runAsync {
            runCatching {
                val builtJDA = JDABuilder.createDefault(token)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setBulkDeleteSplittingEnabled(false)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(DiscordToMc(), LinkEmbed(), UserInfoCommand())
                    .build()
                    .awaitReady()

                jda = builtJDA
                isJdaReady = true

                validateConfig()

                guild?.updateCommands()?.addCommands(
                    Commands.slash("linkembed", "Makes the link embed"),
                    Commands.slash("userinfo", "Shows user info")
                        .addOptions(OptionData(OptionType.USER, "user", "The user to show info for").setRequired(true))
                )?.queue()


            }.onFailure { e ->
                logger.severe("Failed to initialize JDA: ${e.message}")
            }
        }

        server.pluginManager.registerEvents(McToDiscord(), this)
        server.pluginManager.registerEvents(PlayerJoinEvents(), this)

        getCommand("link")?.setExecutor(LinkCode)
        getCommand("configReload")?.setExecutor(ConfigReload())
        getCommand("debug")?.setExecutor(DebugCommand())

        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            lpApi = provider.provider
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, Runnable {
            Bukkit.getOnlinePlayers().forEach { player ->
                DataManager.savePlayerData(DataManager.getPlayerData(player.uniqueId))
            }
        }, 0, 6000)

        val embed = EmbedBuilder()
            .setColor(Color.RED)
            .setAuthor("Server started ✅", null, null)
            .build()

        serverChat?.sendMessageEmbeds(embed)?.queue()

        Bukkit.getScheduler().runTaskTimerAsynchronously(instance, Runnable {
            DataManager.cacheAllData()
        }, 20L, 20L * 60L * 10L)

    }

    override fun onDisable() {

        val threadFactory = ThreadFactoryBuilder().setNameFormat("SoulzStealLinking - Shutdown").build()
        val executor = Executors.newSingleThreadExecutor(threadFactory)

        try {
            executor.invokeAll(listOf(Callable {
                Bukkit.getOnlinePlayers().forEach { player ->
                    DataManager.savePlayerData(DataManager.getPlayerData(player.uniqueId))
                }

                val embed = EmbedBuilder()
                    .setColor(Color.RED)
                    .setAuthor("Server stopped ❌", null, null)
                    .build()

                Bukkit.getScheduler().runTaskLaterAsynchronously(instance, Runnable {
                    updateChannelTopic(false)
                }, 20L)

                serverChat?.sendMessageEmbeds(embed)?.queue()
                DriverManager.getConnection(instance.config.getString("database.jdbcString")).close()
            }))
        } catch (e: Exception) {
            logger.severe("Failed to shutdown JDA: ${e.message}")
        }

        executor.shutdownNow()
    }

    private fun validateConfig() {
        val guildId = instance.config.getString("guildId")
        val serverChatChannelId = instance.config.getString("serverChatChannel")
        val verifiedRoleId = instance.config.getString("verifiedRole")

        if (guildId?.isEmpty() == true || serverChatChannelId?.isEmpty() == true) {
            logger.severe("Missing essential config values!")
            Bukkit.getPluginManager().disablePlugin(this)
            return
        }

        guild = jda.getGuildById(guildId.toString())
        serverChat = jda.getTextChannelById(serverChatChannelId.toString())
        verifiedRole = guild?.getRoleById(verifiedRoleId.toString())

        if (guild == null) logger.severe("Guild not found with ID $guildId")
        if (serverChat == null) logger.severe("Server chat channel not found with ID $serverChatChannelId")
        if (verifiedRole == null) logger.severe("Verified role not found with ID $verifiedRoleId")
    }

}