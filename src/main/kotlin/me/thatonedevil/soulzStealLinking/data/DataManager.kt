package me.thatonedevil.soulzStealLinking.data

import com.google.gson.Gson
import me.thatonedevil.soulzStealLinking.SoulzStealLinking.Companion.instance
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
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

    init {
        if (useMySQL) createTable() else File(PATH).mkdirs()
    }

    private fun getConnection(): Connection? =
        if (useMySQL) try {
            DriverManager.getConnection(dbUrl)
        } catch (e: SQLException) {
            println("Database connection error: ${e.message}"); null
        } else null

    private fun createTable() {
        getConnection()?.use {
            it.prepareStatement(
                """
                CREATE TABLE IF NOT EXISTS linking_data (
                    uuid CHAR(36) PRIMARY KEY,
                    linked BOOLEAN NOT NULL DEFAULT FALSE,
                    user_id VARCHAR(50) NOT NULL
                )
            """
            ).executeUpdate()
        }
    }

    fun loadPlayerData(uuid: UUID): CompletableFuture<LinkingData> = CompletableFuture.supplyAsync {
        playerDataMap[uuid] ?: fetchPlayerData(uuid) ?: LinkingData(uuid)
    }

    private fun fetchPlayerData(uuid: UUID): LinkingData? {
        return if (useMySQL) fetchFromDatabase(uuid) else fetchFromFile(uuid)
    }

    private fun fetchFromDatabase(uuid: UUID): LinkingData? {
        return getConnection()?.use { conn ->
            conn.prepareStatement("SELECT * FROM linking_data WHERE uuid = ?").use { stmt ->
                stmt.setString(1, uuid.toString())
                stmt.executeQuery().use { rs ->
                    if (rs.next()) {
                        LinkingData(uuid, rs.getBoolean("linked"), rs.getString("user_id")).also { cachePlayerData(it) }
                    } else null
                }
            }
        }
    }

    private fun fetchFromFile(uuid: UUID): LinkingData? {
        val file = File("$PATH/$uuid.json")
        return if (file.exists()) runCatching { deserialize(file.readText()) }.getOrNull()?.also { cachePlayerData(it) } else null
    }

    fun savePlayerData(playerData: LinkingData): CompletableFuture<Void> = CompletableFuture.runAsync {
        if (playerData.linked) {
            if (useMySQL) saveToDatabase(playerData) else saveToFile(playerData)
            cachePlayerData(playerData)
        }
    }

    private fun saveToDatabase(playerData: LinkingData) {
        getConnection()?.use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO linking_data (uuid, linked, user_id) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE linked = VALUES(linked), user_id = VALUES(user_id)
                """
            ).use { stmt ->
                stmt.setString(1, playerData.uuid.toString())
                stmt.setBoolean(2, playerData.linked)
                stmt.setString(3, playerData.userId)
                stmt.executeUpdate()
            }
        }
    }

    private fun saveToFile(playerData: LinkingData) {
        File("$PATH/${playerData.uuid}.json").writeText(serialize(playerData))
    }

    fun cacheAllData(): CompletableFuture<Void> = CompletableFuture.runAsync {
        cachedUserMap.clear()
        if (useMySQL) loadAllFromDatabase() else loadAllFromFiles()
    }

    private fun loadAllFromDatabase() {
        val conn = getConnection() ?: return

        conn.use {
            val stmt = conn.prepareStatement("SELECT * FROM linking_data") ?: return@use
            stmt.use {
                val rs = stmt.executeQuery() ?: return
                rs.use {
                    while (rs.next()) {
                        val uuid = UUID.fromString(rs.getString("uuid"))
                        val linked = rs.getBoolean("linked")
                        val userId = rs.getString("user_id")

                        cachePlayerData(LinkingData(uuid, linked, userId))
                    }
                }
            }
        }
    }


    private fun loadAllFromFiles() {
        File(PATH).listFiles { file -> file.extension == "json" }?.forEach {
            runCatching { deserialize(it.readText()) }.getOrNull()?.let { cachePlayerData(it) }
        }
    }

    private fun cachePlayerData(playerData: LinkingData) {
        playerDataMap[playerData.uuid] = playerData
        if (playerData.linked) cachedUserMap[playerData.userId] = playerData.uuid
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