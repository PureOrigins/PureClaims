package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.configFile
import it.pureorigins.framework.configuration.json
import it.pureorigins.framework.configuration.readFileAs
import kotlinx.serialization.Serializable
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.registry.Registry
import net.minecraft.util.registry.RegistryKey
import net.minecraft.world.World
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object PureClaims : ModInitializer {
    private lateinit var database: Database
    lateinit var server: MinecraftServer
    internal var claimsCache: HashMap<World, HashMap<ChunkPos, ClaimedChunk>> = HashMap()
    internal var permsCache: HashMap<UUID, HashMap<UUID, ClaimPermissions>> = HashMap()

    fun getClaimedChunkFromDB(world: ServerWorld, chunkPos: ChunkPos): ClaimedChunk? =
        transaction(database) { PlayerClaimsTable.getClaim(world, chunkPos) }

    fun getPermissionsFromDB(uuid: UUID): HashMap<UUID, ClaimPermissions> =
        transaction(database) { HashMap(PermissionsTable.getPermissions(uuid)) }

    override fun onInitialize() {
        val (db, commands) = json.readFileAs(configFile("fancyparticles.json"), Config())
        ServerLifecycleEvents.SERVER_STARTED.register { server = it }
        require(db.url.isNotEmpty()) { "Database url is empty" }
        database = Database.connect(db.url, user = db.username, password = db.password)
        transaction(database) { SchemaUtils.createMissingTablesAndColumns(PlayerClaimsTable, PermissionsTable) }
        CommandRegistrationCallback.EVENT.register { d, _ ->
            d.register(ClaimCommands(commands).command)
        }
        Events.registerCacheHandlers()
        Events.registerEvents()
        println("PureClaims has been initialized!")

        //Test
        ServerPlayConnectionEvents.JOIN.register{handler,_,_->
            claimsCache[handler.player.world] = HashMap()
            claimsCache[handler.player.world]!![ChunkPos(0,0)] = ClaimedChunk(UUID.fromString("00000000-0000-0000-0000-000000000000"), "", ChunkPos(0,0))
        }
    }


    fun getWorld(name: String): ServerWorld? = server.getWorld(RegistryKey.of(Registry.WORLD_KEY, Identifier(name)))

    @Serializable
    data class Config(
        val database: Database = Database(),
        val commands: ClaimCommands.Config = ClaimCommands.Config()
    ) {
        @Serializable
        data class Database(
            //Default db
            val url: String = "jdbc:sqlite:claims.db",
            val username: String = "",
            val password: String = ""
        )
    }
}
