package me.thatonedevil.soulNetworkPlugin.chatfilter

enum class ObfuscatedChar(val chars: String) {
    A("aA@àâäæãåāạăȧǎª₳ᴀ"),
    B("bBḃƀḅɓʙβʙ"),
    C("cCçćčƈƪ¢ᴄ"),
    D("dDďđȡðɖɗᴅ"),
    E("eEèéêëēėęěȩɛə€3ᴇ"),
    F("fFƒҒꜰ"),
    G("gGğġģǧɠɢ"),
    H("hHħȟɦʜһʜ"),
    I("iI|!1íïîīįɪ"),
    J("jJĵᴊ"),
    K("kKķǩʞᴋ"),
    L("lL|!ļƚḷʟ"),
    M("mMḿṁɱᴍ"),
    N("nNñńňņŋɳɴ"),
    O("oO0òóôöõōȯɔᴏ"),
    P("pPρþᴘ"),
    Q("qQǫ"),
    R("rRřʀ"),
    S("sSšș\$ꜱ"),
    T("tTťțţƫᴛ"),
    U("uUùúûüũūųᴜ"),
    V("vVνᴠ"),
    W("wWώШωᴡ"),
    X("xXχ×x"),
    Y("yYÿý¥ʏ"),
    Z("zZžźżᴢ");

    companion object {
        private val map = entries.associateBy { it.name.lowercase()[0] }

        fun getRegexChar(c: Char): String? {
            return map[c.lowercaseChar()]?.chars
        }
    }
}
