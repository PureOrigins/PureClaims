package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.configFile
import it.pureorigins.framework.configuration.json
import it.pureorigins.framework.configuration.readFileAs
import kotlinx.serialization.Serializable
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
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

object PureClaims : ModInitializer {
    private lateinit var database: Database
    lateinit var server: MinecraftServer
    internal var claimsCache: HashMap<World, HashMap<ChunkPos, ClaimedChunk>> = HashMap()

    fun getClaimedChunk(world: ServerWorld, chunkPos: ChunkPos): ClaimedChunk =
        transaction(database) { PlayerClaimsTable.getClaim(world, chunkPos) }

    override fun onInitialize() {
        val (db, commands) = json.readFileAs(configFile("fancyparticles.json"), Config())
        ServerLifecycleEvents.SERVER_STARTED.register { server = it }
        require(db.url.isNotEmpty()) { "Database url is empty" }
        database = Database.connect(db.url, user = db.username, password = db.password)
        transaction(database) { SchemaUtils.createMissingTablesAndColumns(PlayerClaimsTable) }
        CommandRegistrationCallback.EVENT.register { d, _ ->
            d.register(ClaimCommands(commands).command)
        }
        Events.registerEvents()
        println("PureClaims has been initialized!")
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
