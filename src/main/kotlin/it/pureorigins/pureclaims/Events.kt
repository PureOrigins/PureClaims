package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.PureClaims.claimsCache
import it.pureorigins.pureclaims.PureClaims.permsCache
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.block.Blocks
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World

object Events {
    fun registerCacheHandlers() {
        ServerWorldEvents.LOAD.register { _, serverWorld ->
            claimsCache[serverWorld] = HashMap()
        }
        ServerChunkEvents.CHUNK_LOAD.register { world, chunk ->
            claimsCache[world]!![chunk.pos] = PureClaims.getClaimedChunkFromDB(world, chunk.pos)
        }
        ServerChunkEvents.CHUNK_UNLOAD.register { world, chunk ->
            claimsCache[world]?.remove(chunk.pos)
        }
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            PureClaims.getPermissionsFromDB(handler.player.uuid).forEach {
                if (permsCache[it.key] == null) permsCache[it.key] = HashMap()
                permsCache[it.key]!![handler.player.uuid] = it.value
            }
        }

        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ ->
            PureClaims.getPermissionsFromDB(handler.player.uuid).forEach {
                permsCache[it.key]?.remove(handler.player.uuid)
            }
        }
    }

    fun registerEvents() {
        PlayerBlockBreakEvents.BEFORE.register { world, player, blockPos, _, _ ->
            val pos = ChunkPos(blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]?.get(pos)
                if (claimedChunk != null)
                    if (claimedChunk.owner == player.uuid)
                        return@register true
                    else if (permsCache[claimedChunk.owner]?.get(player.uuid)?.canEdit == true)
                        return@register true
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
                return@register ActionResult.FAIL
            }
            return@register ActionResult.PASS
        }

        UseBlockCallback.EVENT.register { player: PlayerEntity, world: World, _: Hand, blockHitResult: BlockHitResult ->
            val pos = ChunkPos(blockHitResult.blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]!![pos]
                if (claimedChunk != null)
                    if (claimedChunk.owner == player.uuid)
                        return@register ActionResult.PASS
                    else if (permsCache[claimedChunk.owner]?.get(player.uuid)?.canInteract == true)
                        return@register ActionResult.PASS
                    else {
                        val blockState = world.getBlockState(blockHitResult.blockPos)
                        if (blockState.block == Blocks.CHEST || blockState.block == Blocks.TRAPPED_CHEST) {
                            if (permsCache[claimedChunk.owner]?.get(player.uuid)?.canOpenChests == true)
                                return@register ActionResult.PASS
                        }
                    }
                return@register ActionResult.FAIL
            }
            return@register ActionResult.PASS
        }
    }
}