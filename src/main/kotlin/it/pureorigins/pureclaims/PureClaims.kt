package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.*
import kotlinx.serialization.Serializable
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.World
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PureClaims : ModInitializer {
    private lateinit var database: Database
    internal var claimsCache: MutableMap<Pair<World, ChunkPos>, ClaimedChunk> = HashMap()
    internal var permissionsCache: MutableMap<UUID, MutableMap<UUID, ClaimPermissions>> = HashMap()
    internal lateinit var settings: Config.Settings

    fun getClaimedChunkFromDB(world: ServerWorld, chunkPos: ChunkPos): ClaimedChunk? =
        transaction(database) { PlayerClaimsTable.getClaim(world, chunkPos) }

    fun getPermissionsFromDB(uuid: UUID): Map<UUID, ClaimPermissions> =
        transaction(database) { PermissionsTable.getPermissions(uuid) }

    fun getClaimCount(playerUniqueId: UUID): Long =
        transaction(database) { PlayerClaimsTable.getClaimCount(playerUniqueId) }
    
    fun getPermissions(player: ServerPlayerEntity, claim: ClaimedChunk): ClaimPermissions {
        return permissionsCache[player.uuid]?.get(claim.owner)
            ?: (if (claim.owner == player.uuid) ClaimPermissions.ALL else ClaimPermissions.NONE)
    }
    
    fun checkPermissions(player: ServerPlayerEntity, chunk: ChunkPos, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
        val claim = claimsCache[player.world to chunk] ?: return true
        val permissions = getPermissions(player, claim)
        return permissions.requiredPermissions().also {
            if (!it) {
                player.commandSource.sendFeedback(settings.insufficientPermissions?.templateText())
            }
        }
    }

    override fun onInitialize() {
        val (db, commands, settings) = json.readFileAs(configFile("pureclaims.json"), Config())
        this.settings = settings
        require(db.url.isNotEmpty()) { "Database url is empty" }
        database = Database.connect(db.url, user = db.username, password = db.password)
        transaction(database) { SchemaUtils.createMissingTablesAndColumns(PlayerClaimsTable, PermissionsTable) }
        CommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            dispatcher.register(ClaimCommands(commands).command)
        }
        Events.registerCacheHandlers()
        Events.registerEvents()

        //Test
        ServerPlayConnectionEvents.JOIN.register { handler, _, _ ->
            claimsCache[handler.player.world to ChunkPos(0, 0)] = ClaimedChunk(UUID.fromString("00000000-0000-0000-0000-000000000000"), Identifier("test"), ChunkPos(0,0))
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
            val insufficientPermissions: String? = "You don't have permission to do that!",
            val maxClaims: Int = 10
        )
    }
}