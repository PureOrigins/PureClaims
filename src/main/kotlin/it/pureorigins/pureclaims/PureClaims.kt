package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.*
import kotlinx.coroutines.*
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
import net.minecraft.network.packet.s2c.play.WorldBorderInitializeS2CPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import net.minecraft.world.border.WorldBorder
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PureClaims : ModInitializer {
  private lateinit var database: Database
  private lateinit var claims: Claims
  private lateinit var permissions: Permissions
  private lateinit var settings: Config.Settings
  private val scope = CoroutineScope(SupervisorJob() + CoroutineName("PureClaims"))
  internal lateinit var server: MinecraftServer
  
  private fun <R> asyncTransaction(statement: Transaction.() -> R) = scope.async(Dispatchers.IO) { transaction(database, statement) }
  private fun launchTransaction(statement: Transaction.() -> Unit) = scope.launch(Dispatchers.IO) { transaction(database, statement) }

  fun getClaimedChunkNotCached(world: ServerWorld, chunkPos: ChunkPos): Deferred<ClaimedChunk?> = asyncTransaction {
    PlayerClaimsTable.getClaim(world, chunkPos)
  }

  fun getPermissionsNotCached(uuid: UUID): Deferred<Map<UUID, ClaimPermissions>> = asyncTransaction {
    transaction(database) { PermissionsTable.getPermissions(uuid) }
  }

  fun getClaimCount(playerUniqueId: UUID): Deferred<Int> = asyncTransaction {
    transaction(database) { PlayerClaimsTable.getClaimCount(playerUniqueId) }.toInt()
  }

  fun getMaxClaims(playerUniqueId: UUID): Deferred<Int> = asyncTransaction {
    transaction(database) { PlayerTable.getMaxClaims(playerUniqueId) }
  }

  fun setMaxClaims(playerUniqueId: UUID, maxClaims: Int): Deferred<Boolean> = asyncTransaction {
    transaction(database) { PlayerTable.setMaxClaims(playerUniqueId, maxClaims) }
  }

  fun incrementMaxClaims(playerUniqueId: UUID, maxClaims: Int): Deferred<Boolean> = asyncTransaction {
    transaction(database) { PlayerTable.incrementMaxClaims(playerUniqueId, maxClaims) }
  }

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
  
  @Suppress("UNCHECKED_CAST")
  fun addClaim(claim: ClaimedChunk): Job = launchTransaction {
    if (PlayerClaimsTable.add(claim)) {
      claims[claim.world, claim.chunkPos] = claim
    }
  }

  @Suppress("UNCHECKED_CAST")
  fun removeClaim(claim: ClaimedChunk): Job = launchTransaction {
    if (PlayerClaimsTable.remove(claim)) {
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
        if (player is ServerPlayerEntity) highlightChunk(player, chunk)
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
  
  fun Entity.inferPlayer(): PlayerEntity? {
    return when (this) {
      is PlayerEntity -> this
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
  
  fun sendClaimChangeMessage(player: PlayerEntity, oldClaim: ClaimedChunk?, newClaim: ClaimedChunk?) {
    scope.launch(Dispatchers.IO) {
      if (newClaim != oldClaim) {
        if (newClaim == null) {
          player.sendActionBar(settings.exitingClaim?.templateText("owner" to MojangApi.getPlayerInfoOrNull(oldClaim!!.owner)))
        } else {
          player.sendActionBar(settings.enteringClaim?.templateText("owner" to MojangApi.getPlayerInfoOrNull(newClaim.owner)))
        }
      }
    }
  }
  
  fun highlightChunk(player: ServerPlayerEntity, chunk: ChunkPos, time: Long) = scope.launch(Dispatchers.IO) {
    player.networkHandler.sendPacket(WorldBorderInitializeS2CPacket(worldBorder(chunk)))
    delay(time)
    player.networkHandler.sendPacket(WorldBorderInitializeS2CPacket(player.world.worldBorder))
  }
  
  private val claimHighlights = mutableMapOf<ServerPlayerEntity, Job>()
  
  fun highlightChunk(player: ServerPlayerEntity, chunk: ChunkPos) {
    if (player in claimHighlights) return
    val job = scope.launch(Dispatchers.IO) {
      val worldBorder = worldBorder(chunk)
      player.networkHandler.sendPacket(WorldBorderInitializeS2CPacket(worldBorder))
      delay(333)
      player.networkHandler.sendPacket(WorldBorderInitializeS2CPacket(player.world.worldBorder))
      delay(333)
      player.networkHandler.sendPacket(WorldBorderInitializeS2CPacket(worldBorder))
      delay(333)
      player.networkHandler.sendPacket(WorldBorderInitializeS2CPacket(player.world.worldBorder))
      delay(333)
      claimHighlights -= player
    }
    claimHighlights[player] = job
  }
  
  private fun worldBorder(chunk: ChunkPos) = WorldBorder().apply {
    setCenter(chunk.x * 16.0 + 8.0, chunk.z * 16.0 + 8.0)
    size = 16.0
    damagePerBlock = 0.0
    warningTime = 0
    safeZone = 0.0
    warningBlocks = 0
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
    ServerLifecycleEvents.SERVER_STOPPED.register {
      scope.cancel()
    }
    Events.registerEvents()
    CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
      dispatcher.register(ClaimCommands(commands).command)
    }

    //Test
    ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
      claims[handler.player.world, ChunkPos(0, 0)] = ClaimedChunk(UUID.fromString("00000000-0000-0000-0000-000000000000"), server.overworld, ChunkPos(0,0))
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
      val enteringClaim: String? = "[{\"text\": \"You entered the claim of \", \"color\": \"gray\"}, {\"text\": \"<#if owner??>\${owner.name}<#else>Unknown</#if>\", \"color\": \"gold\"}, {\"text\": \".\", \"color\": \"gray\"}]",
      val exitingClaim: String? = "[{\"text\": \"You exiting the claim of \", \"color\": \"gray\"}, {\"text\": \"<#if owner??>\${owner.name}<#else>Unknown</#if>\", \"color\": \"gold\"}, {\"text\": \".\", \"color\": \"gray\"}]",
      val maxClaims: Int = 10
    )
  }
}