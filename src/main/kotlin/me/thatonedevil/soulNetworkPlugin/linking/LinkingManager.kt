package me.thatonedevil.soulNetworkPlugin.linking

import java.util.*

object LinkingManager {

    private val linkedUsers = hashSetOf<UUID>()

    fun addLinkedUser(uuid: UUID) {
        linkedUsers.add(uuid)
    }

    fun removeLinkedUser(uuid: UUID) {
        linkedUsers.remove(uuid)
    }

    fun isLinked(uuid: UUID): Boolean {
        return linkedUsers.contains(uuid)
    }

    fun getLinkedUsers(): Set<UUID> {
        return linkedUsers
    }
}