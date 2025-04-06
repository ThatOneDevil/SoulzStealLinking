package me.thatonedevil.soulzStealLinking


object Utils {

    fun convertLegacyToMiniMessage(input: String): String {
        val replacements = mapOf(
            "&0" to "<black>", "&1" to "<dark_blue>", "&2" to "<dark_green>", "&3" to "<dark_aqua>",
            "&4" to "<dark_red>", "&5" to "<dark_purple>", "&6" to "<gold>", "&7" to "<gray>",
            "&8" to "<dark_gray>", "&9" to "<blue>", "&a" to "<green>", "&b" to "<aqua>",
            "&c" to "<red>", "&d" to "<light_purple>", "&e" to "<yellow>", "&f" to "<white>",
            "&l" to "<bold>", "&o" to "<italic>", "&n" to "<underlined>", "&m" to "<strikethrough>",
            "&r" to "<reset>"
        )

        val regex = Regex(replacements.keys.joinToString("|") { Regex.escape(it) })

        return regex.replace(input) { matchResult ->
            replacements[matchResult.value] ?: matchResult.value
        }
    }

}