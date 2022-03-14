package it.pureorigins.pureclaims

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.jetbrains.exposed.sql.*
import java.util.*

object ClaimsTable : Table("claims") {
    val ownerUniqueId = uuid("owner_uuid")
    val world = uuid("world_uuid")
    val chunkPos = long("chunk_pos")
    
    override val primaryKey = PrimaryKey(world, chunkPos)
    
    fun get(playerUniqueId: UUID): List<ClaimedChunk> =
        select { ClaimsTable.ownerUniqueId eq playerUniqueId }
            .map { it.toClaimedChunk() }
    
    fun get(chunk: Chunk): ClaimedChunk? =
        select { (world eq chunk.world.uid) and (chunkPos eq chunk.chunkKey) }
            .map { it.toClaimedChunk() }.singleOrNull()
    
    
    fun add(chunk: ClaimedChunk): Boolean = insertIgnore {
        it[ownerUniqueId] = chunk.owner
        it[chunkPos] = chunk.chunk.chunkKey
        it[world] = chunk.chunk.world.uid
    }.insertedCount > 0
    
    fun remove(chunk: ClaimedChunk): Boolean = deleteWhere {
        (chunkPos eq chunk.chunk.chunkKey) and (world eq chunk.chunk.world.uid)
    } > 0
    
    fun getCount(playerUniqueId: UUID): Long = select { ClaimsTable.ownerUniqueId eq playerUniqueId }.count()
}

object PermissionsTable : Table("claim_permissions") {
    val ownerUniqueId = uuid("owner_uuid") references PlayerTable.uniqueId
    val playerUniqueId = uuid("player_uuid") references PlayerTable.uniqueId
    val canEdit = bool("can_edit")
    val canInteract = bool("can_interact")
    val canDamageMobs = bool("can_damage_mobs")
    override val primaryKey = PrimaryKey(ownerUniqueId, playerUniqueId)
    
    fun getFromPlayer(playerId: UUID): MutableMap<UUID, ClaimPermissions> =
        select { playerUniqueId eq playerId }.associateTo(HashMap()) { it.toOwnerClaimPermission() }
    
    fun getFromOwner(ownerId: UUID): MutableMap<UUID, ClaimPermissions> =
        select { ownerUniqueId eq ownerId }.associateTo(HashMap()) { it.toPlayerClaimPermission() }
    
    fun set(ownerId: UUID, targetId: UUID, permissions: ClaimPermissions) {
        update({ (ownerUniqueId eq ownerId) and (playerUniqueId eq targetId) }) {
            it[canEdit] = permissions.canEdit
            it[canInteract] = permissions.canInteract
            it[canDamageMobs] = permissions.canDamageMobs
        }
        insertIgnore {
            it[playerUniqueId] = targetId
            it[ownerUniqueId] = ownerId
            it[canEdit] = permissions.canEdit
            it[canInteract] = permissions.canInteract
            it[canDamageMobs] = permissions.canDamageMobs
        }
    }
    
    fun remove(ownerId: UUID, targetId: UUID): Boolean = deleteWhere {
        (playerUniqueId eq targetId) and (ownerUniqueId eq ownerId)
    } > 0
}

object PlayerTable : Table("players") {
    val uniqueId = uuid("uuid")
    val maxClaims = integer("max_claims").default(0)
    
    override val primaryKey = PrimaryKey(uniqueId)
    
    fun getMaxClaims(uniqueId: UUID): Int =
        select { PlayerTable.uniqueId eq uniqueId }.map { it[maxClaims] }.singleOrNull() ?: 0
    
    fun setMaxClaims(uniqueId: UUID, maxClaims: Int): Boolean {
        insertIgnore {
            it[PlayerTable.uniqueId] = uniqueId
        }
        return update({ PlayerTable.uniqueId eq uniqueId }) {
            it[PlayerTable.maxClaims] = maxClaims
        } > 0
    }
    
    fun incrementMaxClaims(uniqueId: UUID, claims: Int): Boolean {
        insertIgnore {
            it[PlayerTable.uniqueId] = uniqueId
        }
        return update({ PlayerTable.uniqueId eq uniqueId }) {
            with(SqlExpressionBuilder) {
                it[maxClaims] = maxClaims + claims
            }
        } > 0
    }
}

private fun ResultRow.toClaimedChunk() = ClaimedChunk(
    get(ClaimsTable.ownerUniqueId),
    Bukkit.getWorld(get(ClaimsTable.world))?.getChunkAt(get(ClaimsTable.chunkPos))
        ?: error("Unknown world '${get(ClaimsTable.world)}'"),
)

private fun ResultRow.toOwnerClaimPermission() = get(PermissionsTable.ownerUniqueId) to ClaimPermissions(
    get(PermissionsTable.canEdit),
    get(PermissionsTable.canInteract),
    get(PermissionsTable.canDamageMobs)
)

private fun ResultRow.toPlayerClaimPermission() = get(PermissionsTable.playerUniqueId) to ClaimPermissions(
    get(PermissionsTable.canEdit),
    get(PermissionsTable.canInteract),
    get(PermissionsTable.canDamageMobs)
)