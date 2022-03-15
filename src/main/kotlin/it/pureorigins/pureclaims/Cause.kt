package it.pureorigins.pureclaims

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.block.Block
import org.bukkit.entity.Item
import org.bukkit.entity.Mob
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.entity.TNTPrimed
import org.bukkit.projectiles.BlockProjectileSource

sealed interface Cause
data class PlayerCause(val player: OfflinePlayer) : Cause
data class ChunkCause(val chunk: Chunk) : Cause

fun inferCause(obj: Any?, maxDepth: Int = 5): Cause? {
    if (maxDepth <= 0) return null
    return when (obj) {
        is Cause -> obj
        is Player -> PlayerCause(obj)
        is Chunk -> ChunkCause(obj)
        is Block -> inferCause(obj.chunk, maxDepth - 1)
        is Location -> inferCause(obj.chunk, maxDepth - 1)
        is BlockProjectileSource -> inferCause(obj.block, maxDepth - 1)
        is Projectile -> inferCause(obj.shooter, maxDepth - 1)
        is Item -> inferCause(obj.owner?.let { Bukkit.getOfflinePlayer(it) }, maxDepth - 1)
        is TNTPrimed -> inferCause(obj.source, maxDepth - 1)
        is Mob -> inferCause(obj.target, maxDepth - 1)
        else -> null
    }
}

fun PureClaims.hasPermissions(cause: Cause, chunk: Chunk, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean = when(cause) {
    is PlayerCause -> hasPermissions(cause.player, chunk, requiredPermissions)
    is ChunkCause -> hasPermissions(cause.chunk, chunk)
}

fun PureClaims.hasPermissions(cause: Any?, chunk: Chunk, requiredPermissions: ClaimPermissions.() -> Boolean, maxDepth: Int = 5): Boolean  {
    return hasPermissions(inferCause(cause, maxDepth) ?: return false, chunk, requiredPermissions)
}

fun PureClaims.checkPermissions(cause: Cause, chunk: Chunk, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean = when (cause) {
    is PlayerCause -> checkPermissions(cause.player, chunk, requiredPermissions)
    is ChunkCause -> hasPermissions(cause.chunk, chunk)
}

fun PureClaims.checkPermissions(cause: Any?, chunk: Chunk, requiredPermissions: ClaimPermissions.() -> Boolean, maxDepth: Int = 5): Boolean {
    return checkPermissions(inferCause(cause, maxDepth) ?: return false, chunk, requiredPermissions)
}
