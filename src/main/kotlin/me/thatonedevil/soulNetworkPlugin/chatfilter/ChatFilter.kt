package me.thatonedevil.soulNetworkPlugin.chatfilter

import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path
import kotlin.text.isNotEmpty
import kotlin.text.trim

class ChatFilter {

    @Volatile
    private var badWords: List<String> = emptyList()

    @Volatile
    private var compiledPatterns: Map<String, Regex> = emptyMap()

    private fun toRegexPattern(word: String): Regex {
        val regex = buildString {
            append(".*")
            word.forEachIndexed { index, c ->
                val part = when {
                    c.isLetter() -> ObfuscatedChar.getRegexChar(c)
                    c.isDigit() -> ObfuscatedDigit.getRegexChar(c)
                    else -> Regex.escape(c.toString())
                }
                append("[").append(part ?: Regex.escape(c.toString())).append("]")
                if (index != word.lastIndex) append("[\\W\\d_]*")
            }
            append(".*")
        }
        return Regex(regex, RegexOption.IGNORE_CASE)
    }

    private fun readBadWordsAsync(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            val filePath = Path(instance.dataFolder.path).resolve("badwords.txt")
            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                try {
                    Files.readAllLines(filePath)
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                } catch (e: Exception) {
                    println("Error reading bad words file: ${e.message}")
                    emptyList()
                }
            } else {
                println("Bad words file not found or not readable. Using empty list.")
                emptyList()
            }
        }
    }

    fun reloadBadWords() {
        readBadWordsAsync().thenAccept { words ->
            badWords = words
            compiledPatterns = words.associateWith { toRegexPattern(it) }
            println("Reloaded bad words. Total: ${words.size}")
        }.exceptionally {
            println("Reload failed: ${it.message}")
            null
        }
    }

    fun findBadWord(message: String): String? {
        return compiledPatterns.entries.firstOrNull { (_, regex) ->
            regex.containsMatchIn(message)
        }?.key
    }



}