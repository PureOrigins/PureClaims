package it.pureorigins.pureclaims

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class Claims(private val claims: MutableMap<Pair<World, ChunkPos>, Future<ClaimedChunk?>> = HashMap()) : MutableMap<Pair<World, ChunkPos>, Future<ClaimedChunk?>> by claims {
  init {
    ServerChunkEvents.CHUNK_LOAD.register { world, chunk ->
      val pos = chunk.pos
      claims[world to pos] = PureClaims.getClaimedChunkNotCached(world, pos)
    }
    ServerChunkEvents.CHUNK_UNLOAD.register { world, chunk ->
      claims -= world to chunk.pos
    }
  }

  operator fun get(world: World, pos: ChunkPos): ClaimedChunk? = claims[world to pos]!!.get()
  operator fun set(world: World, pos: ChunkPos, claim: ClaimedChunk?) {
    claims[world to pos] = object : Future<ClaimedChunk?> {
      override fun cancel(mayInterruptIfRunning: Boolean) = false
      override fun isCancelled() = false
      override fun isDone() = true
      override fun get() = claim
      override fun get(timeout: Long, unit: TimeUnit) = claim
    }
  }
}