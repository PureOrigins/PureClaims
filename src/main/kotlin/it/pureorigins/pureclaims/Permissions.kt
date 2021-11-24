package it.pureorigins.pureclaims

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.player.PlayerEntity
import java.util.*

class Permissions(private val permissions: MutableMap<UUID, MutableMap<UUID, ClaimPermissions>> = HashMap()): MutableMap<UUID, MutableMap<UUID, ClaimPermissions>> by permissions {
  init {
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
      val uuid = handler.player.uuid
      PureClaims.getPermissionsNotCached(uuid).forEach { (ownerUuid, permissions) ->
        this.permissions.computeIfAbsent(uuid) { HashMap() }[ownerUuid] = permissions
      }
    }
    ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
      permissions -= handler.player.uuid
    }
  }

  operator fun get(player: PlayerEntity, claim: ClaimedChunk): ClaimPermissions {
    return permissions[player.uuid]?.get(claim.owner)
      ?: (if (claim.owner == player.uuid) ClaimPermissions.ALL else ClaimPermissions.NONE)
  }
}