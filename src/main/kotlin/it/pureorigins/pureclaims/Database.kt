package it.pureorigins.pureclaims

import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.jetbrains.exposed.sql.*
import java.util.*

object PlayerClaimsTable : Table("player_claims") {
    val playerUniqueId = uuid("player_id")
    val world = text("world")
    val chunkPos = long("chunk_pos")
    
    override val primaryKey = PrimaryKey(world, chunkPos)

    fun getClaims(playerUniqueId: UUID): List<ClaimedChunk> =
        select { PlayerClaimsTable.playerUniqueId eq playerUniqueId }
        .map { it.toClaimedChunk() }

    fun getClaim(world: World, chunkPos: ChunkPos): ClaimedChunk? =
        select { (PlayerClaimsTable.world eq world.registryKey.value.toString()) and (PlayerClaimsTable.chunkPos eq chunkPos.toLong()) }
        .map { it.toClaimedChunk() }.singleOrNull()


    fun add(chunk: ClaimedChunk): Boolean = insertIgnore {
        it[playerUniqueId] = chunk.owner
        it[chunkPos] = chunk.chunkPos.toLong()
        it[world] = chunk.world.toString()
    }.insertedCount > 0

    fun remove(chunk: ClaimedChunk): Boolean = deleteWhere {
        (chunkPos eq chunk.chunkPos.toLong()) and (world eq chunk.world.toString())
    } > 0

    fun getClaimCount(playerUniqueId: UUID): Long = select { PlayerClaimsTable.playerUniqueId eq playerUniqueId }.count()
}

object PermissionsTable : Table("claim_permissions") {
    val ownerUniqueId = uuid("owner_uuid")
    val playerUniqueId = uuid("player_uuid")
    val canEdit = bool("can_edit")
    val canInteract = bool("can_interact")
    val canOpenChests = bool("can_open_chests")
    val canDamageMobs = bool("can_damage_mobs")
    override val primaryKey = PrimaryKey(ownerUniqueId, playerUniqueId)

    fun getPermissions(targetId: UUID): Map<UUID, ClaimPermissions> =
        select { playerUniqueId eq targetId }.associate { it.toClaimPermission() }

    fun add(ownerId: UUID, targetId: UUID, permissions: ClaimPermissions): Boolean = insertIgnore {
        it[playerUniqueId] = targetId
        it[ownerUniqueId] = ownerId
        it[canEdit] = permissions.canEdit
        it[canInteract] = permissions.canInteract
        it[canOpenChests] = permissions.canOpenChests
        it[canDamageMobs] = permissions.canDamageMobs
    }.insertedCount > 0

    fun remove(ownerId: UUID, targetId: UUID): Boolean = deleteWhere {
        (playerUniqueId eq targetId) and (ownerUniqueId eq ownerId)
    } > 0
}

private fun ResultRow.toClaimedChunk() = ClaimedChunk(
    get(PlayerClaimsTable.playerUniqueId),
    Identifier(get(PlayerClaimsTable.world)),
    ChunkPos(get(PlayerClaimsTable.chunkPos))
)

private fun ResultRow.toClaimPermission() = get(PermissionsTable.ownerUniqueId) to ClaimPermissions(
    get(PermissionsTable.canEdit),
    get(PermissionsTable.canInteract),
    get(PermissionsTable.canOpenChests),
    get(PermissionsTable.canDamageMobs)
)