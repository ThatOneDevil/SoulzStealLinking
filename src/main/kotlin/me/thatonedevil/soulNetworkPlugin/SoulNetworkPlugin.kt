package me.thatonedevil.soulNetworkPlugin

import me.thatonedevil.soulNetworkPlugin.JdaManager.serverChatToggle
import me.thatonedevil.soulNetworkPlugin.chat.McToDiscord
import me.thatonedevil.soulNetworkPlugin.chatfilter.ChatFilter
import me.thatonedevil.soulNetworkPlugin.commands.ConfigReload
import me.thatonedevil.soulNetworkPlugin.linking.PlayerJoinEvents
import me.thatonedevil.soulNetworkPlugin.linking.PluginMessageListener
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
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

        if (serverChatToggle) {
            server.pluginManager.registerEvents(McToDiscord(), this)
        }
        server.pluginManager.registerEvents(PlayerJoinEvents(), this)
        server.messenger.registerIncomingPluginChannel(this, "soulzproxy:main", PluginMessageListener())
        server.messenger.registerOutgoingPluginChannel(this, "soulzproxy:main")


        getCommand("configReload")?.setExecutor(ConfigReload())
        chatFilter?.reloadBadWords()

        McToDiscord.startDispatcher()
        PlayerJoinEvents.startEmbedDispatcher()


    }

    override fun onDisable() {
        server.messenger.unregisterIncomingPluginChannel(this)
        server.messenger.unregisterOutgoingPluginChannel(this)

        JdaManager.shutdown()
    }
}
