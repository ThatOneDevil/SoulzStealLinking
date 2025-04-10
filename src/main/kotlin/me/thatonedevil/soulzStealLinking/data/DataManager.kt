package me.thatonedevil.soulzStealLinking.data

import com.google.gson.Gson
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

object DataManager {
    private val GSON = Gson()
    private val PATH = "${instance.dataFolder}/linking-data"

    private val playerDataMap = ConcurrentHashMap<UUID, LinkingData>()
    private val cachedUserMap = ConcurrentHashMap<String, UUID>()

    private val useMySQL = instance.config.getString("database.type")?.lowercase() == "mysql"
    private val dbUrl = instance.config.getString("database.jdbcString")

    private fun getConnection(): Connection? =
        if (useMySQL) runCatching { DriverManager.getConnection(dbUrl) }
            .onFailure { println("Database connection error: ${it.message}") }
            .getOrNull()
        else null

    init {
        if (useMySQL) createTable() else File(PATH).mkdirs()
    }

    private fun createTable() {
        getConnection()?.use {
            it.prepareStatement(
                """
            CREATE TABLE IF NOT EXISTS linking_data (
                uuid CHAR(36) PRIMARY KEY,
                name CHAR(50) NOT NULL,
                linked BOOLEAN NOT NULL DEFAULT FALSE,
                user_id VARCHAR(50) NOT NULL
            )
        """
            ).executeUpdate()
        }
    }

    fun loadPlayerData(uuid: UUID): CompletableFuture<LinkingData> = CompletableFuture.supplyAsync {
        playerDataMap[uuid] ?: fetchPlayerData(uuid) ?: LinkingData(uuid)
    }.exceptionally {
        println("Failed to load player data for $uuid: ${it.message}")
        LinkingData(uuid)
    }

    private fun fetchPlayerData(uuid: UUID): LinkingData? =
        if (useMySQL) fetchFromDatabase(uuid) else fetchFromFile(uuid)

    private fun fetchFromDatabase(uuid: UUID): LinkingData? =
        getConnection()?.use { conn ->
            conn.prepareStatement("SELECT * FROM linking_data WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        LinkingData(
                            uuid,
                            rs.getString("name"),
                            rs.getBoolean("linked"),
                            rs.getString("user_id")
                        ).also { cachePlayerData(it) }
                    } else null
                }
            }
        }

    private fun fetchFromFile(uuid: UUID): LinkingData? =
        File("$PATH/$uuid.json").takeIf { it.exists() }?.readText()
            ?.let { runCatching { deserialize(it) }.getOrNull() }
            ?.also { cachePlayerData(it) }

    fun savePlayerData(playerData: LinkingData): CompletableFuture<Void> = CompletableFuture.runAsync {
        if (playerData.linked) {
            if (useMySQL) saveToDatabase(playerData) else saveToFile(playerData)
            cachePlayerData(playerData)
        }
    }.exceptionally {
        println("Failed to save player data for ${playerData.uuid}: ${it.message}")
        null
    }

    private fun saveToDatabase(playerData: LinkingData) {
        getConnection()?.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO linking_data (uuid, name, linked, user_id) 
                VALUES (?, ?, ?, ?) 
                ON DUPLICATE KEY UPDATE name = VALUES(name), linked = VALUES(linked), user_id = VALUES(user_id)
            """
            ).use { stmt ->
                stmt.setString(1, playerData.uuid.toString())
                stmt.setString(2, playerData.name)
                stmt.setBoolean(3, playerData.linked)
                stmt.setString(4, playerData.userId)
                stmt.executeUpdate()
            }
        }
    }

    private fun saveToFile(playerData: LinkingData) {
        File("$PATH/${playerData.uuid}.json").writeText(serialize(playerData))
    }

    fun deletePlayerData(uuid: UUID): CompletableFuture<Void> = CompletableFuture.runAsync {
        if (useMySQL) deleteFromDatabase(uuid) else deleteFromFile(uuid)
        playerDataMap.remove(uuid)
        cachedUserMap.values.remove(uuid)
    }.exceptionally {
        println("Failed to delete player data for $uuid: ${it.message}")
        null
    }

    private fun deleteFromDatabase(uuid: UUID) {
        getConnection()?.use { conn ->
            conn.prepareStatement("DELETE FROM linking_data WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeUpdate()
            }
        }
    }

    private fun deleteFromFile(uuid: UUID) {
        File("$PATH/$uuid.json").delete()
    }

    fun cacheAllData(): CompletableFuture<Void> = CompletableFuture.runAsync {
        cachedUserMap.clear()
        if (useMySQL) loadFromDatabase() else loadFromFiles()
    }.exceptionally {
        println("Failed to cache all data: ${it.message}")
        null
    }

    private fun loadFromDatabase() {
        getConnection()?.use { conn ->
            conn.prepareStatement("SELECT * FROM linking_data").use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) {
                        cachePlayerData(
                            LinkingData(
                                UUID.fromString(rs.getString("uuid")),
                                rs.getString("name"),
                                rs.getBoolean("linked"),
                                rs.getString("user_id")
                            )
                        )
                    }
                }
            }
        }
    }

    private fun loadFromFiles() {
        File(PATH).listFiles { file -> file.extension == "json" }?.forEach {
            runCatching { deserialize(it.readText()) }.getOrNull()?.let { cachePlayerData(it) }
        }
    }

    fun clearAllData(): CompletableFuture<Void> = CompletableFuture.runAsync {
        cachedUserMap.clear()
        playerDataMap.clear()
        if (useMySQL) getConnection()?.use { it.prepareStatement("DELETE FROM linking_data").executeUpdate() }
        else File(PATH).listFiles { file -> file.extension == "json" }?.forEach { it.delete() }
    }.exceptionally {
        println("Failed to clear all data: ${it.message}")
        null
    }

    private fun cachePlayerData(playerData: LinkingData) {
        playerDataMap[playerData.uuid] = playerData
        if (playerData.linked) cachedUserMap[playerData.userId] = playerData.uuid
    }

    fun removePLayerData(uuid: UUID) {
        playerDataMap.remove(uuid)
    }

    fun getCachedUserMap(): Map<String, UUID> = cachedUserMap
    fun getPlayerDataMap(): Map<UUID, LinkingData> = playerDataMap
    fun getUUIDFromDiscordId(discordId: String): UUID? = cachedUserMap[discordId]
    fun getPlayerData(uuid: UUID): LinkingData = playerDataMap[uuid] ?: LinkingData(uuid)

    private fun serialize(playerData: LinkingData): String = GSON.toJson(playerData)
    private fun deserialize(json: String): LinkingData = GSON.fromJson(json, LinkingData::class.java)

    fun getDebugMessage(): String = buildString {
        appendLine("----- Debug Information -----")
        appendLine("Player Data Map:")
        if (playerDataMap.isEmpty()) appendLine("  No player data available.")
        else playerDataMap.forEach { (uuid, data) -> appendLine("  UUID: $uuid -> Data: $data") }

        appendLine()
        appendLine("Cached User IDs:")
        if (cachedUserMap.isEmpty()) appendLine("  No cached user IDs available.")
        else cachedUserMap.forEach { (userId, uuid) -> appendLine("  User ID: $userId -> UUID: $uuid") }

        appendLine("-----------------------------")
    }
}
