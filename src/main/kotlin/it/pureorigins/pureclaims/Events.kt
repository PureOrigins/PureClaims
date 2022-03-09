package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.ClaimPermissions.Companion.EDIT
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

object Events : Listener {
    @EventHandler
    fun onBreakBlock(e: BlockBreakEvent) {
        if (!plugin.checkPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler
    fun onFireSpread(e: BlockIgniteEvent) {
        if (!plugin.checkPermissions(e.ignitingEntity ?: e.ignitingBlock, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (!plugin.checkPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler
    fun onInteract(e: PlayerInteractEvent) {
        if (e.clickedBlock != null && !plugin.checkPermissions(e.player, e.clickedBlock!!.chunk, ClaimPermissions.INTERACT))
            e.isCancelled = true
    }

    @EventHandler
    fun onInteractEntity(e: PlayerInteractAtEntityEvent) {
        if (!plugin.checkPermissions(e.player, e.rightClicked.chunk, ClaimPermissions.INTERACT)) e.isCancelled = true
    }

    @EventHandler
    fun onEntityDamage(e: EntityDamageByEntityEvent) {
        if(e.entity !is Player && !plugin.checkPermissions(e.damager as Player, e.entity.chunk, ClaimPermissions.DAMAGE_MOBS))
            e.isCancelled = true
    }
}