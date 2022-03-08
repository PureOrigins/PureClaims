package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.ClaimPermissions.Companion.EDIT
import it.pureorigins.pureclaims.PureClaims.Companion.claims
import it.pureorigins.pureclaims.PureClaims.Companion.getClaim
import it.pureorigins.pureclaims.PureClaims.Companion.hasPermissions
import it.pureorigins.pureclaims.PureClaims.Companion.isClaimed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

class Events: Listener {
    @EventHandler
    fun onChunkLoad(e: ChunkLoadEvent) {
        claims[e.chunk] = PureClaims.getClaimedChunkNotCached(e.chunk)
    }

    @EventHandler
    fun onChunkUnload(e: ChunkUnloadEvent) {
        claims -= e.chunk
    }

    @EventHandler
    fun onJoin(e: PlayerJoinEvent) {

    }

    @EventHandler
    fun onLeave(e: PlayerQuitEvent) {

    }

    @EventHandler
    fun onBreakBlock(e: BlockBreakEvent) {
        if(hasPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = false
    }

    @EventHandler
    fun onFireSpread(e: BlockIgniteEvent) {
        if(!isClaimed(e.block.chunk)) return
        val claim = getClaim(e.block.chunk)
        when (e.cause) {
            BlockIgniteEvent.IgniteCause.SPREAD -> {
                val claimFrom = getClaim(e.ignitingBlock?.chunk)
                if(claimFrom.owner != claim.owner) e.isCancelled = true
            }

            BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, BlockIgniteEvent.IgniteCause.FIREBALL  -> {
                if(e.player?.let { !hasPermissions(it, e.block.chunk, ClaimPermissions.INTERACT) } == true) e.isCancelled = true
            }

            else -> {}
        }
    }

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if(!isClaimed(e.block.chunk)) return
        val claim = getClaim(e.block.chunk)
        if(!hasPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }
}