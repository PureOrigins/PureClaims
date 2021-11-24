package it.pureorigins.pureclaims

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World

class Claims(private val claims: MutableMap<Pair<World, ChunkPos>, ClaimedChunk> = HashMap()) : MutableMap<Pair<World, ChunkPos>, ClaimedChunk> by claims {
  init {
    ServerChunkEvents.CHUNK_LOAD.register { world, chunk ->
      val pos = chunk.pos
      val claim = PureClaims.getClaimedChunkNotCached(world, pos) ?: return@register
      claims[world to pos] = claim
    }
    ServerChunkEvents.CHUNK_UNLOAD.register { world, chunk ->
      claims -= world to chunk.pos
    }
  }

  operator fun get(world: World, pos: ChunkPos): ClaimedChunk? = claims[world to pos]
  operator fun set(world: World, pos: ChunkPos, claim: ClaimedChunk) {
    claims[world to pos] = claim
  }
}