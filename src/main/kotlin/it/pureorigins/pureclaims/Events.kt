package it.pureorigins.pureclaims

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

class Events: Listener {
    @EventHandler
    fun onChunkLoad(e: ChunkLoadEvent) {
        val pos = e.chunk
        claims[e.world to pos] = PureClaims.getClaimedChunkNotCached(world, pos)
    }

    @EventHandler
    fun onChunkUnload(e: ChunkUnloadEvent) {

    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {

    }

    @EventHandler
    fun onLeave(e: PlayerQuitEvent) {

    }

}