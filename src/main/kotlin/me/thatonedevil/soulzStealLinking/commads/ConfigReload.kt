package me.thatonedevil.soulzStealLinking.commads

import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ConfigReload : CommandExecutor {
    override fun onCommand(p0: CommandSender, p1: Command, p2: String, p3: Array<out String>?): Boolean {

        val player = p0 as Player

        if (!player.isOp) {
            return false
        }

        instance.reloadConfig()
        player.sendMessage("Config reloaded!")

        return true
    }
}