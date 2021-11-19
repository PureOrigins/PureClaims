package it.pureorigins.pureclaims

import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

object PlayerClaimsTable : Table("player_claims") {
    val world = text("world")
    val chunkPos = long("chunk_pos")
    val uniqueId = uuid("player_id")
    override val primaryKey = PrimaryKey(world, chunkPos)

    fun getClaims(uniqueId: UUID): List<ClaimedChunk> = select { PlayerClaimsTable.uniqueId eq uniqueId }
        .map { it.toClaimedChunk() }

    fun getClaim(world: World, chunkPos: ChunkPos): ClaimedChunk? = select {
        (PlayerClaimsTable.world eq world.registryKey.value.toString()) and (PlayerClaimsTable.chunkPos eq chunkPos.toLong()) }
        .map { it.toClaimedChunk() }.getOrNull(0)


    fun add(chunk: ClaimedChunk): Boolean = insertIgnore {
        it[uniqueId] = chunk.owner
        it[chunkPos] = chunk.chunkPos.toLong()
        it[world] = chunk.world
    }.insertedCount > 0

    fun remove(chunk: ClaimedChunk): Boolean = deleteWhere {
        (chunkPos eq chunk.chunkPos.toLong()) and (world eq chunk.world)
    } > 0

    fun getClaimCount(uuid: UUID?): Long = select { PlayerClaimsTable.uniqueId eq uniqueId }
    .count()
}

object PermissionsTable : Table("claim_permissions") {
    val owner = uuid("owner") references PlayerClaimsTable.uniqueId
    val target = uuid("player_id")
    val canEdit = bool("can_edit")
    val canInteract = bool("can_interact")
    val canOpenChests = bool("can_open_chests")
    val canDamageMobs = bool("can_damage_mobs")
    override val primaryKey = PrimaryKey(owner, target)

    fun getPermissions(targetId: UUID): Map<UUID, ClaimPermissions> =
        select { target eq targetId }.associate { it.toClaimPermission() }

    fun add(
        ownerId: UUID, targetId: UUID,
        canEdit: Boolean = false,
        canInteract: Boolean = false,
        canOpenChests: Boolean = false,
        canDamageMobs: Boolean = false
    ): Boolean = insertIgnore {
        it[target] = targetId
        it[owner] = ownerId
        it[PermissionsTable.canEdit] = canEdit
        it[PermissionsTable.canInteract] = canInteract
        it[PermissionsTable.canOpenChests] = canOpenChests
        it[PermissionsTable.canDamageMobs] = canDamageMobs
    }.insertedCount > 0

    fun remove(ownerId: UUID, targetId: UUID): Boolean = deleteWhere {
        (target eq targetId) and (owner eq ownerId)
    } > 0
}

private fun ResultRow.toClaimedChunk() = ClaimedChunk(
    get(PlayerClaimsTable.uniqueId),
    get(PlayerClaimsTable.world),
    ChunkPos(get(PlayerClaimsTable.chunkPos))
)

private fun ResultRow.toClaimPermission() = get(PermissionsTable.owner) to ClaimPermissions(
    get(PermissionsTable.canEdit),
    get(PermissionsTable.canInteract),
    get(PermissionsTable.canOpenChests),
    get(PermissionsTable.canDamageMobs)
)