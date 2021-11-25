package it.pureorigins.pureclaims

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.player.PlayerEntity
import java.util.*
import java.util.concurrent.Future

class Permissions(private val permissions: MutableMap<UUID, Future<MutableMap<UUID, ClaimPermissions>>> = HashMap()): MutableMap<UUID, Future<MutableMap<UUID, ClaimPermissions>>> by permissions {
  init {
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
      val uuid = handler.player.uuid
      @Suppress("UNCHECKED_CAST")
      this.permissions[uuid] = PureClaims.getPermissionsNotCached(uuid) as Future<MutableMap<UUID, ClaimPermissions>>
    }
    ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
      permissions -= handler.player.uuid
    }
  }

  operator fun get(player: PlayerEntity, claim: ClaimedChunk): ClaimPermissions {
    return permissions[player.uuid]?.get()?.get(claim.owner)
      ?: if (claim.owner == player.uuid) ClaimPermissions.ALL else ClaimPermissions.NONE
  }
}