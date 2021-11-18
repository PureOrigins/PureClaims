package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.PureClaims.claimsCache
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World

object Events {
    fun registerEvents() {
        ServerWorldEvents.LOAD.register { _, serverWorld ->
            claimsCache[serverWorld] = HashMap()
        }
        ServerChunkEvents.CHUNK_LOAD.register { world, chunk ->
            claimsCache[world]!![chunk.pos] = PureClaims.getClaimedChunk(world, chunk.pos)
        }
        ServerChunkEvents.CHUNK_UNLOAD.register { world, chunk ->
            claimsCache[world]?.remove(chunk.pos)
        }
        PlayerBlockBreakEvents.BEFORE.register { world: World, playerEntity: PlayerEntity, blockPos: BlockPos, _: BlockState, _: BlockEntity ->
            val pos = ChunkPos(blockPos)
            if (claimsCache[world]?.contains(pos) == true) {
                val claimedChunk = claimsCache[world]?.get(ChunkPos(blockPos))
                if (claimedChunk != null && claimedChunk.owner == playerEntity.uuid) {
                    return@register false
                }
            }
            return@register true
        }
    }
}