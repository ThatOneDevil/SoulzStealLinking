enum class ObfuscatedChar(val variants: List<String>) {
    A(listOf("a", "A", "@", "à", "â", "ä", "æ", "ã", "å", "ā", "ạ", "ă", "ȧ", "ǎ", "ª", "₳", "ᴀ")),
    B(listOf("b", "B", "ḃ", "ƀ", "ḅ", "ɓ", "ʙ", "β")),
    C(listOf("c", "C", "ç", "ć", "č", "ƈ", "ƪ", "¢", "ᴄ")),
    D(listOf("d", "D", "ď", "đ", "ȡ", "ð", "ɖ", "ɗ", "ᴅ")),
    E(listOf("e", "E", "è", "é", "ê", "ë", "ē", "ė", "ę", "ě", "ȩ", "ɛ", "ə", "€", "3", "ᴇ")),
    F(listOf("f", "F", "ƒ", "Ғ", "ꜰ")),
    G(listOf("g", "G", "ğ", "ġ", "ģ", "ǧ", "ɠ", "ɢ", "9", "99")),
    H(listOf("h", "H", "ħ", "ȟ", "ɦ", "ʜ", "һ")),
    I(listOf("i", "I", "|", "!", "1", "í", "ï", "î", "ī", "į", "ɪ", "}", ";", ":")),
    J(listOf("j", "J", "ĵ", "ᴊ")),
    K(listOf("k", "K", "ķ", "ǩ", "ʞ", "ᴋ")),
    L(listOf("l", "L", "|", "!", "ļ", "ƚ", "ḷ", "ʟ", ";", ":")),
    M(listOf("m", "M", "ḿ", "ṁ", "ɱ", "ᴍ")),
    N(listOf("n", "N", "ñ", "ń", "ň", "ņ", "ŋ", "ɳ", "ɴ")),
    O(listOf("o", "O", "0", "ò", "ó", "ô", "ö", "õ", "ō", "ȯ", "ɔ", "ᴏ")),
    P(listOf("p", "P", "ρ", "þ", "ᴘ")),
    Q(listOf("q", "Q", "ǫ")),
    R(listOf("r", "R", "ř", "ʀ")),
    S(listOf("s", "S", "š", "ș", "$", "ꜱ")),
    T(listOf("t", "T", "ť", "ț", "ţ", "ƫ", "ᴛ")),
    U(listOf("u", "U", "ù", "ú", "û", "ü", "ũ", "ū", "ų", "ᴜ")),
    V(listOf("v", "V", "ν", "ᴠ")),
    W(listOf("w", "W", "ώ", "Ш", "ω", "ᴡ")),
    X(listOf("x", "X", "χ", "×")),
    Y(listOf("y", "Y", "ÿ", "ý", "¥", "ʏ")),
    Z(listOf("z", "Z", "ž", "ź", "ż", "ᴢ"));

    companion object {
        private val lookup = entries.associateBy { it.name.lowercase()[0] }

        fun getVariants(c: Char): List<String>? = lookup[c.lowercaseChar()]?.variants
    }
}
