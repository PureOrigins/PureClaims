package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.*
import kotlinx.serialization.Serializable
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.TntEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.projectile.ProjectileEntity
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PureClaims : ModInitializer {
  private lateinit var database: Database
  private lateinit var claims: Claims
  private lateinit var permissions: Permissions
  private lateinit var settings: Config.Settings
  internal lateinit var server: MinecraftServer

  fun getClaimedChunkNotCached(world: ServerWorld, chunkPos: ChunkPos): ClaimedChunk? =
    transaction(database) { PlayerClaimsTable.getClaim(world, chunkPos) }

  fun getPermissionsNotCached(uuid: UUID): Map<UUID, ClaimPermissions> =
    transaction(database) { PermissionsTable.getPermissions(uuid) }

  fun getClaimCount(playerUniqueId: UUID): Int =
    transaction(database) { PlayerClaimsTable.getClaimCount(playerUniqueId) }.toInt()

  fun getMaxClaims(playerUniqueId: UUID): Int =
    transaction(database) { PlayerTable.getMaxClaims(playerUniqueId) }

  fun setMaxClaims(playerUniqueId: UUID, maxClaims: Int): Boolean =
    transaction(database) { PlayerTable.setMaxClaims(playerUniqueId, maxClaims) }

  fun incrementMaxClaims(playerUniqueId: UUID, maxClaims: Int): Boolean =
    transaction(database) { PlayerTable.incrementMaxClaims(playerUniqueId, maxClaims) }

  fun getPermissions(player: PlayerEntity, claim: ClaimedChunk): ClaimPermissions {
    return permissions[player, claim]
  }

  fun isClaimed(world:World, chunkPos: ChunkPos): Boolean {
    return world to chunkPos in claims
  }

  fun isClaimed(world:World, blockPos: BlockPos): Boolean {
    return isClaimed(world, ChunkPos(blockPos))
  }

  fun getClaim(world: World, chunkPos: ChunkPos): ClaimedChunk? {
    return claims[world, chunkPos]
  }

  fun getClaim(world: World, block: BlockPos): ClaimedChunk? {
    return claims[world, ChunkPos(block)]
  }

  fun addClaim(claim: ClaimedChunk) {
    if (transaction(database) { PlayerClaimsTable.add(claim) }) {
      claims[claim.world, claim.chunkPos] = claim
    }
  }

  fun removeClaim(claim: ClaimedChunk) {
    if (transaction(database) { PlayerClaimsTable.remove(claim) }) {
      claims -= claim.world to claim.chunkPos
    }
  }
  
  fun hasPermissions(player: PlayerEntity, chunk: ChunkPos, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
    val claim = claims[player.world, chunk] ?: return true
    val permissions = getPermissions(player, claim)
    return permissions.requiredPermissions()
  }
  
  fun hasPermissions(player: PlayerEntity, block: BlockPos, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
    return hasPermissions(player, ChunkPos(block), requiredPermissions)
  }
  
  fun checkPermissions(player: PlayerEntity, chunk: ChunkPos, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
    return hasPermissions(player, chunk, requiredPermissions).also {
      if (!it) {
        when (requiredPermissions) {
          ClaimPermissions.EDIT -> player.sendActionBar(settings.cannotEdit?.templateText())
          ClaimPermissions.INTERACT -> player.sendActionBar(settings.cannotInteract?.templateText())
          ClaimPermissions.DAMAGE_MOBS -> player.sendActionBar(settings.cannotDamageMobs?.templateText())
        }
      }
    }
  }
  
  fun checkPermissions(player: PlayerEntity, block: BlockPos, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
    return checkPermissions(player, ChunkPos(block), requiredPermissions)
  }
  
  fun Entity.inferPlayer(): ServerPlayerEntity? {
    return when (this) {
      is ServerPlayerEntity -> this
      is MobEntity -> target?.inferPlayer()
      is ProjectileEntity -> owner?.inferPlayer()
      is ItemEntity -> if (thrower != null) this@PureClaims.server.playerManager.getPlayer(thrower) else null
      is TntEntity -> causingEntity?.inferPlayer()
      else -> null
    }
  }
  
  @JvmOverloads
  fun hasIndirectPermissions(entity: Entity, block: BlockPos, default: Boolean = false, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
    return hasPermissions(entity.inferPlayer() ?: return default, block, requiredPermissions)
  }
  
  @JvmOverloads
  fun checkIndirectPermissions(entity: Entity, block: BlockPos, default: Boolean = false, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
    return if (entity is PlayerEntity) checkPermissions(entity, block, requiredPermissions) else hasIndirectPermissions(entity, block, default, requiredPermissions)
  }

  override fun onInitialize() {
    val (db, commands, settings) = json.readFileAs(configFile("pureclaims.json"), Config())
    this.settings = settings
    require(db.url.isNotEmpty()) { "Database url is empty" }
    ServerLifecycleEvents.SERVER_STARTING.register {
      server = it
      database = Database.connect(db.url, user = db.username, password = db.password)
      transaction(database) { SchemaUtils.createMissingTablesAndColumns(PlayerClaimsTable, PermissionsTable, PlayerTable) }
      claims = Claims()
      permissions = Permissions()
    }
    Events.registerEvents()
    CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
      dispatcher.register(ClaimCommands(commands).command)
    }

    //Test
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
      claims[handler.player.world to ChunkPos(0, 0)] = ClaimedChunk(UUID.fromString("00000000-0000-0000-0000-000000000000"), server.overworld, ChunkPos(0,0))
    }
  }


  @Serializable
  data class Config(
    val database: Database = Database(),
    val commands: ClaimCommands.Config = ClaimCommands.Config(),
    val settings: Settings = Settings()
  ) {
    @Serializable
    data class Database(
      val url: String = "jdbc:sqlite:claims.db",
      val username: String = "",
      val password: String = ""
    )

    @Serializable
    data class Settings(
      val cannotEdit: String? = "{\"text\": \"You don't have permission to edit!\", \"color\": \"red\"}",
      val cannotInteract: String? = "{\"text\": \"You don't have permission to interact!\", \"color\": \"red\"}",
      val cannotDamageMobs: String? = "{\"text\": \"You don't have permission to damage mobs!\", \"color\": \"red\"}",
      val maxClaims: Int = 10
    )
  }
}