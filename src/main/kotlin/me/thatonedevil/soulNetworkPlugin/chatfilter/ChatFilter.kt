package me.thatonedevil.soulNetworkPlugin.chatfilter

import ObfuscatedChar
import ObfuscatedDigit
import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import kotlin.io.path.Path

class ChatFilter {
    @Volatile
    private var badWords: List<String> = emptyList()

    @Volatile
    private var compiledPatterns: Map<String, Regex> = emptyMap()

    private fun toRegexPattern(word: String): Regex {
        val parts = buildList {
            for (c in word) {
                val variants = when {
                    c.isLetter() -> ObfuscatedChar.getVariants(c)
                    c.isDigit() -> ObfuscatedDigit.getVariants(c)
                    else -> listOf(Regex.escape(c.toString()))
                }

                val escaped = variants?.distinct()?.joinToString("|") { Regex.escape(it) }
                    ?: Regex.escape(c.toString())

                add("(?:$escaped)[\\W\\d_]*")
            }
        }

        return Regex(".*${parts.joinToString("")}.*", RegexOption.IGNORE_CASE)
    }

    private fun readBadWordsAsync(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            val filePath = Path(instance.dataFolder.path).resolve("badwords.txt")
            try {
                if (Files.exists(filePath) && Files.isReadable(filePath)) {
                    Files.readAllLines(filePath)
                        .asSequence()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .toList()
                } else {
                    println("Bad words file not found or not readable. Using empty list.")
                    emptyList()
                }
            } catch (e: Exception) {
                println("Error reading bad words file: ${e.message}")
                emptyList()
            }
        }
    }

    fun reloadBadWords() {
        readBadWordsAsync().thenAccept { words ->
            val compiled = HashMap<String, Regex>(words.size)
            for (word in words) {
                compiled[word] = toRegexPattern(word)
            }
            badWords = words
            compiledPatterns = compiled
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