package it.pureorigins.pureclaims

import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.jetbrains.exposed.sql.*
import java.util.*

object PlayerClaimsTable : Table("player_claims") {
    val id = integer("id").autoIncrement()
    val world = text("world")
    val chunkPos = long("chunk_pos")
    val uniqueId = uuid("player_id")
    override val primaryKey = PrimaryKey(world, chunkPos)

    fun getClaims(uniqueId: UUID): List<ClaimedChunk> = select(where = { PlayerClaimsTable.uniqueId eq uniqueId }).map {
        it.toClaimedChunk()
    }

    fun getClaim(world: World, chunkPos: ChunkPos): ClaimedChunk = select(
        where = { (PlayerClaimsTable.world eq world.registryKey.value.toString()) and (PlayerClaimsTable.chunkPos eq chunkPos.toLong()) }).map { it.toClaimedChunk() }[0]


    fun add(chunk: ClaimedChunk): Boolean = insertIgnore {
        it[uniqueId] = chunk.owner
        it[chunkPos] = chunk.chunkPos.toLong()
        it[world] = chunk.world
    }.insertedCount > 0

    fun remove(chunk: ClaimedChunk): Boolean = deleteWhere {
        (chunkPos eq chunk.chunkPos.toLong()) and (world eq chunk.world)
    } > 0
}

object ClaimPermissionsTable : Table("claim_permissions") {
    val claim = integer("id") references PlayerClaimsTable.id
    val uniqueId = uuid("player_id")
    val permission = text("permission")
    override val primaryKey = PrimaryKey(claim, uniqueId, permission)

    fun getPermissions(uniqueId: UUID): List<ClaimedChunk> =
        select(where = { PlayerClaimsTable.uniqueId eq uniqueId }).map {
            it.toClaimedChunk()
        }

    fun getClaim(world: World, chunkPos: ChunkPos): ClaimedChunk = select(
        where = { (PlayerClaimsTable.world eq world.registryKey.value.toString()) and (PlayerClaimsTable.chunkPos eq chunkPos.toLong()) }).map { it.toClaimedChunk() }[0]


    fun add(chunk: ClaimedChunk, uuid: UUID, perm: String): Boolean = insertIgnore {
        it[uniqueId] = uuid
        it[claim] = 0//TODO chunk.id
        it[permission] = perm
    }.insertedCount > 0

    fun remove(chunk: ClaimedChunk, uuid: UUID, perm: String): Boolean = deleteWhere {
        (uniqueId eq uuid) and (permission eq perm)//TODO
    } > 0
}

private fun ResultRow.toClaimedChunk() = ClaimedChunk(
    get(PlayerClaimsTable.uniqueId),
    get(PlayerClaimsTable.world),
    ChunkPos(get(PlayerClaimsTable.chunkPos))
)