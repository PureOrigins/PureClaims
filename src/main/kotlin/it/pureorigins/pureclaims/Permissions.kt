package it.pureorigins.pureclaims

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.player.PlayerEntity
import org.bukkit.entity.Player
import java.util.*

class Permissions(private val permissions: MutableMap<UUID, Deferred<MutableMap<UUID, ClaimPermissions>>> = HashMap()): MutableMap<UUID, Deferred<MutableMap<UUID, ClaimPermissions>>> by permissions {
  init {
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
      val uuid = handler.player.uuid
      @Suppress("UNCHECKED_CAST")
      this.permissions[uuid] = PureClaims.getPermissionsNotCached(uuid) as Deferred<MutableMap<UUID, ClaimPermissions>>
    }
    ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
      permissions -= handler.player.uuid
    }
  }

  operator fun get(player: Player, claim: ClaimedChunk): ClaimPermissions {
    return runBlocking { permissions[player.uniqueId]?.await() }?.get(claim.owner)
      ?: if (claim.owner == player.uniqueId) ClaimPermissions.ALL else ClaimPermissions.NONE
  }
}