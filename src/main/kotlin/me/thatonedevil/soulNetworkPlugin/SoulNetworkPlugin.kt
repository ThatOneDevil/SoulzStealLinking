package me.thatonedevil.soulNetworkPlugin

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.thatonedevil.soulNetworkPlugin.chat.McToDiscord
import me.thatonedevil.soulNetworkPlugin.chatfilter.ChatFilter
import me.thatonedevil.soulNetworkPlugin.commads.ConfigReload
import me.thatonedevil.soulNetworkPlugin.linking.PlayerJoinEvents
import me.thatonedevil.soulNetworkPlugin.linking.PluginMessageListener
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import sun.tools.jconsole.Messages.ATTRIBUTES
import java.sql.DriverManager
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.logging.Logger

class SoulNetworkPlugin : JavaPlugin() {

    companion object {
        lateinit var instance: SoulNetworkPlugin
        var lpApi: LuckPerms? = null
        var soulLogger: Logger? = null
        var chatFilter: ChatFilter? = null
    }

    override fun onEnable() {
        instance = this
        soulLogger = logger

        chatFilter = ChatFilter()

        config.options().copyDefaults(true)
        saveDefaultConfig()
        reloadConfig()

        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            lpApi = provider.provider
        }

        JdaManager.init()

        server.pluginManager.registerEvents(McToDiscord(), this)
        server.pluginManager.registerEvents(PlayerJoinEvents(), this)
        server.messenger.registerIncomingPluginChannel(this, "soulzproxy:main", PluginMessageListener())
        server.messenger.registerOutgoingPluginChannel(this, "soulzproxy:main")


        getCommand("configReload")?.setExecutor(ConfigReload())
        chatFilter?.reloadBadWords()

    }

    override fun onDisable() {
        val threadFactory = ThreadFactoryBuilder().setNameFormat("SoulzStealLinkingShutdown").build()
        val executor = Executors.newSingleThreadExecutor(threadFactory)

        try {
            executor.invokeAll(listOf(Callable {
                DriverManager.getConnection(config.getString("database.jdbcString")).close()
            }))
        } catch (e: Exception) {
            logger.severe("Shutdown error: ${e.message}")
        }

        server.messenger.unregisterIncomingPluginChannel(this)
        server.messenger.unregisterOutgoingPluginChannel(this)

        JdaManager.shutdown()
        executor.shutdownNow()
    }
}
