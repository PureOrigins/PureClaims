package it.pureorigins.pureclaims

import kotlinx.coroutines.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World

class Claims(private val claims: MutableMap<Pair<World, ChunkPos>, Deferred<ClaimedChunk?>> = HashMap()) : MutableMap<Pair<World, ChunkPos>, Deferred<ClaimedChunk?>> by claims {
  init {
    ServerChunkEvents.CHUNK_LOAD.register { world, chunk ->
      val pos = chunk.pos
      claims[world to pos] = PureClaims.getClaimedChunkNotCached(world, pos)
    }
    ServerChunkEvents.CHUNK_UNLOAD.register { world, chunk ->
      claims -= world to chunk.pos
    }
  }
  
  operator fun contains(key: Pair<World, ChunkPos>): Boolean = runBlocking { claims[key]?.await() != null }

  operator fun get(world: World, pos: ChunkPos): ClaimedChunk? = runBlocking { claims[world to pos]!!.await() }
  operator fun set(world: World, pos: ChunkPos, claim: ClaimedChunk?) {
    claims[world to pos] = CompletableDeferred(claim)
  }
}