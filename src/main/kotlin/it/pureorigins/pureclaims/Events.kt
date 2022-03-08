package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.ClaimPermissions.Companion.EDIT
import it.pureorigins.pureclaims.PureClaims.Companion.plugin
import org.bukkit.entity.EntityType.ARMOR_STAND
import org.bukkit.entity.EntityType.ITEM_FRAME
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

class Events : Listener {

    @EventHandler
    fun onBreakBlock(e: BlockBreakEvent) {
        if (!plugin.hasPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler
    fun onFireSpread(e: BlockIgniteEvent) {
        if (!plugin.isClaimed(e.block.chunk)) return
        val claim = plugin.getClaim(e.block.chunk)
        when (e.cause) {
            BlockIgniteEvent.IgniteCause.SPREAD -> {
                val claimFrom = plugin.getClaim(e.ignitingBlock!!.chunk)
                if (claimFrom != null && claimFrom.owner != claim!!.owner) e.isCancelled = true
            }

            BlockIgniteEvent.IgniteCause.FLINT_AND_STEEL, BlockIgniteEvent.IgniteCause.FIREBALL -> {
                if (!plugin.hasPermissions(e.player!!, e.block.chunk, ClaimPermissions.INTERACT)) e.isCancelled = true
            }

            else -> {}
        }
    }

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (!plugin.hasPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        if (e.clickedBlock != null && !plugin.hasPermissions(e.player, e.clickedBlock!!.chunk, ClaimPermissions.INTERACT))
            e.isCancelled = true
    }

    @EventHandler
    fun onInteractEntity(e: PlayerInteractAtEntityEvent) {
        when(e.rightClicked.type) {
            ARMOR_STAND, ITEM_FRAME ->
                if (!plugin.hasPermissions(e.player, e.rightClicked.chunk, ClaimPermissions.INTERACT)) e.isCancelled = true

            else -> {}
        }
    }

    @EventHandler
    fun onEntityDamage(e: EntityDamageByEntityEvent) {
        if(e.damager is Player && e.entity !is Player && !plugin.hasPermissions(e.damager as Player, e.entity.chunk, ClaimPermissions.DAMAGE_MOBS))
            e.isCancelled = true
    }
}