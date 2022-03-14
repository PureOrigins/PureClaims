package it.pureorigins.pureclaims

import it.pureorigins.common.registerEvents
import it.pureorigins.common.runTaskAsynchronously
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

class Permissions(
    private val plugin: PureClaims,
    private val permissions: MutableMap<UUID, MutableMap<UUID, ClaimPermissions>> = HashMap()
) : MutableMap<UUID, MutableMap<UUID, ClaimPermissions>> by permissions, Listener {
    init {
        plugin.registerEvents(this)
    }
    
    fun register(player: Player) {
        val uuid = player.uniqueId
        plugin.runTaskAsynchronously {
            this.permissions[uuid] = plugin.getPermissionsFromPlayerDatabase(uuid)
        }
    }
    
    fun unregister(player: Player) {
        permissions -= player.uniqueId
    }
    
    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        register(event.player)
    }
    
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        unregister(event.player)
    }
    
    operator fun get(playerUniqueId: UUID, ownerUniqueId: UUID): ClaimPermissions {
        return permissions[playerUniqueId]?.get(ownerUniqueId)
            ?: if (ownerUniqueId == playerUniqueId) ClaimPermissions.ALL else ClaimPermissions.NONE
    }
    
    operator fun set(playerUniqueId: UUID, ownerUniqueId: UUID, value: ClaimPermissions) {
        permissions[playerUniqueId]?.set(ownerUniqueId, value)
    }
}