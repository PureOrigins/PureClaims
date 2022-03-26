package it.pureorigins.pureclaims

import it.pureorigins.pureclaims.ClaimPermissions.Companion.DAMAGE_MOBS
import it.pureorigins.pureclaims.ClaimPermissions.Companion.EDIT
import it.pureorigins.pureclaims.ClaimPermissions.Companion.INTERACT
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority.HIGH
import org.bukkit.event.Listener
import org.bukkit.event.block.*
import org.bukkit.event.block.Action.PHYSICAL
import org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent

object Events : Listener {
    @EventHandler(ignoreCancelled = true)
    fun onBreakBlock(e: BlockBreakEvent) {
        if (!plugin.checkPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockExplosion(e: BlockExplodeEvent) {
        e.blockList().removeIf { !plugin.hasPermissions(e.block, it.chunk, EDIT) }
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityExplosion(e: EntityExplodeEvent) {
        e.blockList().removeIf { !plugin.hasPermissions(e.entity, it.chunk, EDIT) }
    }

    @EventHandler(ignoreCancelled = true)
    fun onFireSpread(e: BlockIgniteEvent) {
        if (!plugin.checkPermissions(e.ignitingEntity ?: e.ignitingBlock, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(e: BlockPlaceEvent) {
        if (!plugin.checkPermissions(e.player, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler(priority = HIGH, ignoreCancelled = true)
    fun onInteract(e: PlayerInteractEvent) {
        when(e.action) {
            RIGHT_CLICK_BLOCK, PHYSICAL ->
                if (!plugin.checkPermissions(e.player, e.clickedBlock!!.chunk, INTERACT)) e.isCancelled = true
            else -> {}
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onInteractEntity(e: PlayerInteractAtEntityEvent) {
        if (!plugin.checkPermissions(e.player, e.rightClicked.chunk, INTERACT)) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onHangingBreak(e: HangingBreakByEntityEvent) {
        if (e.entity is Player) {
            if (!plugin.checkPermissions(e.entity, e.entity.chunk, EDIT)) e.isCancelled = true
        }
    }

    @EventHandler(ignoreCancelled = true)
    fun onHangingPlace(e: HangingPlaceEvent) {
        if (!plugin.checkPermissions(e.player, e.entity.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDamage(e: EntityDamageByEntityEvent) {
        val entity = e.entity
        if (entity !is Player)
            if (entity is LivingEntity && entity !is ArmorStand) {
                if (!plugin.checkPermissions(e.damager, e.entity.chunk, DAMAGE_MOBS)) e.isCancelled = true
            } else if (!plugin.checkPermissions(e.damager, e.entity.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onEntityDestroyBlock(e: EntityChangeBlockEvent) {
        if (!plugin.checkPermissions(e.entity, e.block.chunk, EDIT)) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonRetract(e: BlockPistonRetractEvent) {
        val chunks = e.blocks.map { it.chunk }.toSet()
        if (chunks.any { !plugin.checkPermissions(e.block, it, EDIT) }) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPistonExtend(e: BlockPistonExtendEvent) {
        val chunks = e.blocks.map { it.getRelative(e.direction).chunk }.toSet()
        if (chunks.any { !plugin.checkPermissions(e.block, it, EDIT) }) e.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onChunkChange(e: PlayerMoveEvent) {
        if (e.from.chunk != e.to.chunk)
            plugin.sendClaimChangeMessage(e.player, plugin.getClaim(e.from), plugin.getClaim(e.to))
    }

    @EventHandler(ignoreCancelled = true)
    fun onLavaFlow(e: BlockFromToEvent) {
        if (!plugin.checkPermissions(e.block, e.block.chunk, EDIT)) e.isCancelled = true
    }
}