package me.thatonedevil.soulNetworkPlugin.commads

import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.chatFilter
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class ConfigReload : CommandExecutor {
    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>?): Boolean {

        instance.reloadConfig()
        chatFilter?.reloadBadWords()
        p0.sendMessage("Config reloaded!")

        return true
    }
}