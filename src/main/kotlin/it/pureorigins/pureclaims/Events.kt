package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.ClaimPermissions.Companion.DAMAGE_MOBS
import it.pureorigins.pureclaims.ClaimPermissions.Companion.EDIT
import it.pureorigins.pureclaims.ClaimPermissions.Companion.INTERACT
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.block.Action.PHYSICAL
import org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent

object Events : Listener {

    @EventHandler
    fun onBreakBlock(e: BlockBreakEvent) {
        if (!plugin.checkPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler
    fun onBlockExplosion(e: BlockExplodeEvent) {
        e.blockList().removeIf { !plugin.hasPermissions(e.block, it.chunk, EDIT) }
    }

    @EventHandler
    fun onEntityExplosion(e: EntityExplodeEvent) {
        e.blockList().removeIf { !plugin.hasPermissions(e.entity, it.chunk, EDIT) }
    }

    @EventHandler
    fun onFireSpread(e: BlockIgniteEvent) {
        if (!plugin.checkPermissions(e.ignitingEntity ?: e.ignitingBlock, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (!plugin.checkPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler(priority = EventPriority.LOW)
    fun onInteract(e: PlayerInteractEvent) {
        when(e.action) {
            RIGHT_CLICK_BLOCK, PHYSICAL ->
                if (!plugin.checkPermissions(e.player, e.clickedBlock!!.chunk, INTERACT)) e.isCancelled = true
            else -> {}
        }
    }

    @EventHandler
    fun onInteractEntity(e: PlayerInteractAtEntityEvent) {
        if (!plugin.checkPermissions(e.player, e.rightClicked.chunk, INTERACT)) e.isCancelled = true
    }

    @EventHandler
    fun onEntityDamage(e: EntityDamageByEntityEvent) {
        if (e.entity !is Player)
            if (e.entity is LivingEntity && !plugin.checkPermissions(e.damager, e.entity.chunk, DAMAGE_MOBS))
                e.isCancelled = true
            else if (!plugin.checkPermissions(e.damager, e.entity.chunk, INTERACT)) e.isCancelled = true
    }

    @EventHandler
    fun onPistonRetract(e: BlockPistonRetractEvent) {
        val chunks = e.blocks.map { it.chunk }.toSet()
        if (chunks.any { !plugin.checkPermissions(e.block, it, EDIT) }) e.isCancelled = true
    }

    @EventHandler
    fun onPistonExtend(e: BlockPistonExtendEvent) {
        val chunks = e.blocks.map { it.chunk }.toSet()
        if (chunks.any { !plugin.checkPermissions(e.block, it, EDIT) }) e.isCancelled = true
    }

    @EventHandler
    fun onChunkChange(e: PlayerMoveEvent) {
        if (e.from.chunk != e.to.chunk)
            plugin.sendClaimChangeMessage(e.player, plugin.getClaim(e.from), plugin.getClaim(e.to))
    }
}