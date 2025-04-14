package me.thatonedevil.soulNetworkPlugin.chatfilter

import me.thatonedevil.soulNetworkPlugin.SoulNetworkPlugin.Companion.instance

import java.nio.file.Files
import kotlin.io.path.Path
import java.util.concurrent.CompletableFuture

class ChatFilter {

    @Volatile
    private var badWords: List<String> = emptyList()

    @Volatile
    private var chatFormatBadWords: Map<String, String> = emptyMap()

    private fun toRegex(word: String): String {
        return word.mapNotNull { c ->
            ObfuscatedChar.getRegexChar(c)?.let { "[$it]+" } // Allow repeated occurrences of each character
        }.joinToString("[\\s\\W_]*", prefix = ".*", postfix = ".*")
    }

    private fun readBadWordsAsync(): CompletableFuture<List<String>> {
        return CompletableFuture.supplyAsync {
            val filePath = Path(instance.dataFolder.path).resolve("badwords.txt")
            if (Files.exists(filePath) && Files.isReadable(filePath)) {
                try {
                    Files.readAllLines(filePath)
                        .map { it.trim() } // Trim spaces/tabs around each word
                        .filter { it.isNotEmpty() } // Filter out empty/blank lines
                } catch (e: Exception) {
                    println("Error reading bad words file: ${e.message}")
                    emptyList() // Return an empty list if there is an issue
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
            chatFormatBadWords = badWords.associateWith { toRegex(it) }
            println("Successfully reloaded bad words. Total words: ${badWords.size}")
        }.exceptionally { throwable ->
            println("Failed to reload bad words. Details: ${throwable.message}")
            null
        }
    }

    fun findBadWord(message: String): String? {
        return chatFormatBadWords.entries.firstOrNull { (_, pattern) ->
            Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(message)
        }?.key
    }



}