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

class Permissions(private val plugin: PureClaims, private val permissions: MutableMap<UUID, MutableMap<UUID, ClaimPermissions>> = HashMap()): MutableMap<UUID, MutableMap<UUID, ClaimPermissions>> by permissions, Listener {
  init {
    plugin.registerEvents(this)
  }
  
  @EventHandler
  fun onPlayerJoin(event: PlayerJoinEvent) {
    val uuid = event.player.uniqueId
    plugin.runTaskAsynchronously {
      this.permissions[uuid] = plugin.getPermissionsDatabase(uuid)
    }
  }
  
  @EventHandler
  fun onPlayerQuit(event: PlayerQuitEvent) {
    permissions -= event.player.uniqueId
  }

  operator fun get(player: OfflinePlayer, claim: ClaimedChunk): ClaimPermissions {
    return permissions[player.uniqueId]?.get(claim.owner)
      ?: if (claim.owner == player.uniqueId) ClaimPermissions.ALL else ClaimPermissions.NONE
  }
}