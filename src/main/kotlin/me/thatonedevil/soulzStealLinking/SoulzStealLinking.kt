package me.thatonedevil.soulzStealLinking

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.thatonedevil.soulzStealLinking.chat.McToDiscord
import me.thatonedevil.soulzStealLinking.commads.ConfigReload
import me.thatonedevil.soulzStealLinking.linking.PlayerJoinEvents
import net.luckperms.api.LuckPerms
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.sql.DriverManager
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.logging.Logger

class SoulzStealLinking : JavaPlugin() {

    companion object {
        lateinit var instance: JavaPlugin
        var lpApi: LuckPerms? = null
        var soulLogger: Logger? = null
    }

    override fun onEnable() {
        instance = this
        soulLogger = logger

        config.options().copyDefaults(true)
        saveDefaultConfig()
        reloadConfig()

        val token = config.getString("token")
        if (token.isNullOrEmpty()) {
            logger.severe("Missing bot token in config.yml")
            server.pluginManager.disablePlugin(this)
            return

        }

        val provider = Bukkit.getServicesManager().getRegistration(LuckPerms::class.java)
        if (provider != null) {
            lpApi = provider.provider
        }

        JdaManager.init(token)

        server.pluginManager.registerEvents(McToDiscord(), this)
        server.pluginManager.registerEvents(PlayerJoinEvents(), this)

        getCommand("configReload")?.setExecutor(ConfigReload())


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

        val item = ItemStack(Material.STONE)
        item.itemMeta.addEnchant(Enchantment.SHARPNESS, 1, false)

        JdaManager.shutdown()
        executor.shutdownNow()
    }
}
