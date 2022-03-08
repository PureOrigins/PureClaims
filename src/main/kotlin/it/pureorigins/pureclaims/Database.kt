package it.pureorigins.pureclaims

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.jetbrains.exposed.sql.*
import java.util.*
import kotlin.collections.HashMap

object PlayerClaimsTable : Table("player_claims") {
  val playerUniqueId = uuid("player_id")
  val world = varchar("world", length = 45)
  val chunkPos = long("chunk_pos")

  override val primaryKey = PrimaryKey(world, chunkPos)

  fun getClaims(playerUniqueId: UUID): List<ClaimedChunk> =
    select { PlayerClaimsTable.playerUniqueId eq playerUniqueId }
      .map { it.toClaimedChunk() }

  fun getClaim(chunk: Chunk): ClaimedChunk? =
    select { (world eq chunk.world.name) and (chunkPos eq chunk.chunkKey) }
      .map { it.toClaimedChunk() }.singleOrNull()


  fun add(chunk: ClaimedChunk): Boolean = insertIgnore {
    it[playerUniqueId] = chunk.owner
    it[chunkPos] = chunk.chunk.chunkKey
    it[world] = chunk.chunk.world.name
  }.insertedCount > 0

  fun remove(chunk: ClaimedChunk): Boolean = deleteWhere {
    (chunkPos eq chunk.chunk.chunkKey) and (world eq chunk.chunk.world.name)
  } > 0

  fun getClaimCount(playerUniqueId: UUID): Long = select { PlayerClaimsTable.playerUniqueId eq playerUniqueId }.count()
}

object PermissionsTable : Table("claim_permissions") {
  val ownerUniqueId = uuid("owner_uuid") references PlayerTable.uniqueId
  val playerUniqueId = uuid("player_uuid") references PlayerTable.uniqueId
  val canEdit = bool("can_edit")
  val canInteract = bool("can_interact")
  val canDamageMobs = bool("can_damage_mobs")
  override val primaryKey = PrimaryKey(ownerUniqueId, playerUniqueId)

  fun getPermissions(targetId: UUID): MutableMap<UUID, ClaimPermissions> =
    select { playerUniqueId eq targetId }.associateTo(HashMap()) { it.toClaimPermission() }

  fun add(ownerId: UUID, targetId: UUID, permissions: ClaimPermissions): Boolean = insertIgnore {
    it[playerUniqueId] = targetId
    it[ownerUniqueId] = ownerId
    it[canEdit] = permissions.canEdit
    it[canInteract] = permissions.canInteract
    it[canDamageMobs] = permissions.canDamageMobs
  }.insertedCount > 0

  fun remove(ownerId: UUID, targetId: UUID): Boolean = deleteWhere {
    (playerUniqueId eq targetId) and (ownerUniqueId eq ownerId)
  } > 0
}

object PlayerTable : Table("players") {
  val uniqueId = uuid("uuid")
  val maxClaims = integer("max_claims").default(0)

  override val primaryKey = PrimaryKey(uniqueId)

  fun getMaxClaims(uniqueId: UUID): Int = select { PlayerTable.uniqueId eq uniqueId }.map { it[maxClaims] }.singleOrNull() ?: 0
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
  get(PlayerClaimsTable.playerUniqueId),
  Bukkit.getWorld(get(PlayerClaimsTable.world))?.getChunkAt(get(PlayerClaimsTable.chunkPos))
    ?: error("Unknown world '${get(PlayerClaimsTable.world)}'"),
)

private fun ResultRow.toClaimPermission() = get(PermissionsTable.ownerUniqueId) to ClaimPermissions(
  get(PermissionsTable.canEdit),
  get(PermissionsTable.canInteract),
  get(PermissionsTable.canDamageMobs)
)