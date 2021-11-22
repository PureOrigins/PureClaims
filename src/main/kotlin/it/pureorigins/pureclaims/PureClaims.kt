package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.*
import kotlinx.serialization.Serializable
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.MinecraftServer
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

    fun getClaim(world: World, chunkPos: ChunkPos): ClaimedChunk? {
        return claims[world, chunkPos]
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
    
    fun checkEditPermissions(player: PlayerEntity, chunk: ChunkPos): Boolean {
        return hasPermissions(player, chunk, ClaimPermissions::canEdit).also {
            if (!it) player.sendActionBar(settings.cannotEdit?.templateText())
        }
    }
    
    fun checkEditPermissions(player: PlayerEntity, block: BlockPos): Boolean =
        checkEditPermissions(player, ChunkPos(block))
    
    fun checkInteractPermissions(player: PlayerEntity, chunk: ChunkPos): Boolean {
        return hasPermissions(player, chunk, ClaimPermissions::canInteract).also {
            if (!it) player.sendActionBar(settings.cannotInteract?.templateText())
        }
    }
    
    fun checkInteractPermissions(player: PlayerEntity, block: BlockPos): Boolean =
        checkInteractPermissions(player, ChunkPos(block))
    
    fun checkDamageMobPermissions(player: PlayerEntity, chunk: ChunkPos): Boolean {
        return hasPermissions(player, chunk, ClaimPermissions::canDamageMobs).also {
            if (!it) player.sendActionBar(settings.cannotDamageMobs?.templateText())
        }
    }
    
    fun checkDamageMobPermissions(player: PlayerEntity, block: BlockPos): Boolean =
        checkDamageMobPermissions(player, ChunkPos(block))

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