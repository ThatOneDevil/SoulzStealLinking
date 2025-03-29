package me.thatonedevil.soulzStealLinking

import me.thatonedevil.soulzStealLinking.data.DataManager.getDebugMessage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class DebugCommand : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender is Player) {
            sender.sendMessage(getDebugMessage())
        } else {
            println(getDebugMessage())
        }

        return true
    }
}
