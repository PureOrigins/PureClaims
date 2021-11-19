package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.PureClaims.claimsCache
import it.pureorigins.pureclaims.PureClaims.permissionsCache
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents

object Events {
    fun registerCacheHandlers() {
        ServerChunkEvents.CHUNK_LOAD.register { world, chunk ->
            val pos = chunk.pos
            val claim = PureClaims.getClaimedChunkFromDB(world, pos) ?: return@register
            claimsCache[world to pos] = claim
        }
        ServerChunkEvents.CHUNK_UNLOAD.register { world, chunk ->
            claimsCache -= world to chunk.pos
        }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            val uuid = handler.player.uuid
            PureClaims.getPermissionsFromDB(uuid).forEach { (ownerUuid, permissions) ->
                permissionsCache.computeIfAbsent(uuid) { HashMap() }[ownerUuid] = permissions
            }
        }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            permissionsCache -= handler.player.uuid
        }
    }

    fun registerEvents() {
        /*
        PlayerBlockBreakEvents.BEFORE.register { world, player, blockPos, _, _ ->
            val pos = ChunkPos(blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]?.get(pos)
                if (claimedChunk != null)
                    if (claimedChunk.owner == player.uuid)
                        return@register true
                    else if (permsCache[claimedChunk.owner]?.get(player.uuid)?.canEdit == true)
                        return@register true
                player.sendMessage(Text.of(PureClaims.settings.no_perm),false)
                return@register false
            }
            return@register true
        }

        AttackEntityCallback.EVENT.register { player, world, _, entity, _ ->
            val pos = ChunkPos(entity?.blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]!![pos]
                if (claimedChunk != null)
                    if (claimedChunk.owner == player?.uuid)
                        return@register ActionResult.PASS
                    else if (permsCache[claimedChunk.owner]?.get(player?.uuid)?.canDamageMobs == true)
                        return@register ActionResult.PASS
                player.sendMessage(Text.of(PureClaims.settings.no_perm),false)
                return@register ActionResult.FAIL
            }
            return@register ActionResult.PASS
        }


        UseBlockCallback.EVENT.register { player, world, _, block ->
            val pos = ChunkPos(block.blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]!![pos]
                if (claimedChunk != null)
                    when {
                        claimedChunk.owner == player.uuid -> return@register ActionResult.PASS
                        permsCache[claimedChunk.owner]?.get(player.uuid)?.canInteract == true -> return@register ActionResult.PASS
                        else -> {
                            val blockState = world.getBlockState(block.blockPos)
                            if (blockState.block == Blocks.CHEST || blockState.block == Blocks.TRAPPED_CHEST) {
                                if (permsCache[claimedChunk.owner]?.get(player.uuid)?.canOpenChests == true)
                                    return@register ActionResult.PASS
                            }
                        }
                    }
                player.sendMessage(Text.of(PureClaims.settings.no_perm),false)
                return@register ActionResult.FAIL
            }
            return@register ActionResult.PASS
        }
        */
    }
}