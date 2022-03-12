package it.pureorigins.pureclaims

import it.pureorigins.common.registerEvents
import it.pureorigins.common.runTaskAsynchronously
import org.bukkit.Chunk
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

class Claims(private val plugin: PureClaims, private val claims: MutableMap<Chunk, ClaimedChunk?> = HashMap()) : MutableMap<Chunk, ClaimedChunk?> by claims, Listener {
  init {
    plugin.registerEvents(this)
  }
  
  fun register(chunk: Chunk) {
    plugin.runTaskAsynchronously {
      claims[chunk] = plugin.getClaimedChunkDatabase(chunk)
    }
  }
  
  fun unregister(chunk: Chunk) {
    claims -= chunk
  }
  
  @EventHandler
  fun onChunkLoad(event: ChunkLoadEvent) {
    register(event.chunk)
  }
  
  @EventHandler
  fun onChunkUnload(event: ChunkUnloadEvent) {
    unregister(event.chunk)
  }
}