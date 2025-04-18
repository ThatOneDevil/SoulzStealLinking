package me.thatonedevil.soulNetworkPlugin.chatfilter

enum class ObfuscatedDigit(val chars: String) {
    ZERO("0Oo"),
    ONE("1iIl!|"),
    TWO("2Zz"),
    THREE("3Ee"),
    FOUR("4Aa"),
    FIVE("5Ss"),
    SIX("6Gg"),
    SEVEN("7Tt"),
    EIGHT("8Bb"),
    NINE("9Gg");

    companion object {
        fun getRegexChar(c: Char): String? {
            return if (c in '0'..'9') entries[c.digitToInt()].chars else null
        }
    }
}
