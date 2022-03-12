package it.pureorigins.pureclaims

import it.pureorigins.common.*
import it.pureorigins.pureclaims.ClaimPermissions.Companion.DAMAGE_MOBS
import it.pureorigins.pureclaims.ClaimPermissions.Companion.EDIT
import it.pureorigins.pureclaims.ClaimPermissions.Companion.INTERACT
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.ChatType.GAME_INFO
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import net.minecraft.world.level.border.WorldBorder
import org.bukkit.Bukkit
import org.bukkit.Bukkit.getOfflinePlayer
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

internal lateinit var plugin: PureClaims

class PureClaims : JavaPlugin() {
    private lateinit var database: Database
    private lateinit var claims: Claims
    private lateinit var permissions: Permissions
    private lateinit var settings: Config.Settings

    fun getClaimedChunkDatabase(chunk: Chunk): ClaimedChunk? = transaction(database) {
        PlayerClaimsTable.getClaim(chunk)
    }

    fun getPermissionsDatabase(uuid: UUID): MutableMap<UUID, ClaimPermissions> = transaction(database) {
        PermissionsTable.getPermissions(uuid)
    }

    fun getClaimCountDatabase(playerUniqueId: UUID): Int = transaction(database) {
        PlayerClaimsTable.getClaimCount(playerUniqueId)
    }.toInt()

    fun getMaxClaimsDatabase(playerUniqueId: UUID): Int = transaction(database) {
        PlayerTable.getMaxClaims(playerUniqueId)
    }

    fun setMaxClaimsDatabase(playerUniqueId: UUID, maxClaims: Int): Boolean = transaction(database) {
        PlayerTable.setMaxClaims(playerUniqueId, maxClaims)
    }

    fun incrementMaxClaimsDatabase(playerUniqueId: UUID, maxClaims: Int): Boolean = transaction(database) {
        PlayerTable.incrementMaxClaims(playerUniqueId, maxClaims)
    }

    fun isLoaded(player: OfflinePlayer): Boolean = player.uniqueId in permissions

    fun getPermissions(player: OfflinePlayer, claim: ClaimedChunk): ClaimPermissions = permissions[player, claim]

    fun isLoaded(chunk: Chunk): Boolean = chunk in claims

    fun isClaimed(chunk: Chunk): Boolean = claims[chunk] != null

    fun isClaimed(location: Location): Boolean = isClaimed(location.chunk)

    fun getClaim(chunk: Chunk): ClaimedChunk? = claims[chunk]

    fun getClaim(location: Location): ClaimedChunk? = claims[location.chunk]

    fun addClaimDatabase(claim: ClaimedChunk) = transaction(database) {
        if (PlayerClaimsTable.add(claim)) claims[claim.chunk] = claim
    }

    fun removeClaimDatabase(claim: ClaimedChunk) = transaction(database) {
        if (PlayerClaimsTable.remove(claim)) claims -= claim.chunk
    }

    fun hasPermissions(
        player: OfflinePlayer,
        chunk: Chunk,
        requiredPermissions: ClaimPermissions.() -> Boolean
    ): Boolean {
        if (!isLoaded(player) || !isLoaded(chunk)) return false

        val claim = claims[chunk] ?: return true
        val permissions = getPermissions(player, claim)
        return permissions.requiredPermissions()
    }

    fun hasPermissions(cause: Chunk, chunk: Chunk): Boolean {
        if (!isLoaded(chunk)) return false
        val claim = claims[chunk] ?: return true
        if (!isLoaded(cause)) return false
        return claim.owner == claims[cause]?.owner
    }

    fun checkPermissions(
        player: OfflinePlayer,
        chunk: Chunk,
        requiredPermissions: ClaimPermissions.() -> Boolean
    ): Boolean {
        return hasPermissions(player, chunk, requiredPermissions).also {
            if (!it && player is Player) {
                highlightChunk(player, chunk)
                when (requiredPermissions) {
                    EDIT -> player.sendNullableMessage(settings.cannotEdit?.templateText(), GAME_INFO)
                    INTERACT -> player.sendNullableMessage(settings.cannotInteract?.templateText(), GAME_INFO)
                    DAMAGE_MOBS -> player.sendNullableMessage(settings.cannotDamageMobs?.templateText(), GAME_INFO)
                }
            }
        }
    }

    fun highlightChunk(player: Player, chunk: Chunk) {
        val nmsWorld = player.world.nms
        fun worldBorder(chunk: Chunk) = WorldBorder().apply {
            world = nmsWorld
            setCenter(chunk.x * 16.0 + 8.0, chunk.z * 16.0 + 8.0)
            size = 16.0
            damagePerBlock = 0.0
            warningTime = 0
            warningBlocks = 0
        }

        val newBorder = worldBorder(chunk)
        val defaultBorder = nmsWorld.worldBorder
        val nmsPlayer = player.nms
        nmsPlayer.connection.send(ClientboundInitializeBorderPacket(newBorder))
        runTaskLater(7) {
            nmsPlayer.connection.send(ClientboundInitializeBorderPacket(defaultBorder))
        }
        runTaskLater(14) {
            nmsPlayer.connection.send(ClientboundInitializeBorderPacket(newBorder))
        }
        runTaskLater(21) {
            nmsPlayer.connection.send(ClientboundInitializeBorderPacket(defaultBorder))
        }
    }

    fun sendClaimChangeMessage(player: Player, oldClaim: ClaimedChunk?, newClaim: ClaimedChunk?) {
        if (newClaim?.owner == oldClaim?.owner) return
        if (newClaim != null)
            player.sendNullableMessage(
                settings.enteringClaim?.templateText("owner" to getOfflinePlayer(newClaim.owner)), GAME_INFO
            )
        else
            player.sendNullableMessage(
                settings.exitingClaim?.templateText("owner" to getOfflinePlayer(oldClaim!!.owner)), GAME_INFO
            )
    }

    override fun onLoad() {
        plugin = this
    }

    override fun onEnable() {
        val (db, commands, settings) = json.readFileAs(file("pureclaims.json"), Config())
        require(db.url.isNotEmpty()) { "Database url is empty" }
        this.settings = settings
        database = Database.connect(db.url, user = db.username, password = db.password)
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(PlayerClaimsTable, PermissionsTable, PlayerTable)
        }
        claims = Claims(this)
        permissions = Permissions(this)
        registerEvents(Events)
        registerCommand(ClaimCommands(this, commands).command)
        Bukkit.getWorld("world")?.loadedChunks?.forEach {
            runTaskAsynchronously { claims[it] = plugin.getClaimedChunkDatabase(it) }
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
            val url: String = "",
            val username: String = "",
            val password: String = ""
        )

        @Serializable
        data class Settings(
            val cannotEdit: String? = "{\"text\": \"You don't have permission to edit!\", \"color\": \"red\"}",
            val cannotInteract: String? = "{\"text\": \"You don't have permission to interact!\", \"color\": \"red\"}",
            val cannotDamageMobs: String? = "{\"text\": \"You don't have permission to damage mobs!\", \"color\": \"red\"}",
            val enteringClaim: String? = "[{\"text\": \"You entered the claim of \", \"color\": \"gray\"}, {\"text\": \"\${owner.getName()}\", \"color\": \"gold\"}, {\"text\": \".\", \"color\": \"gray\"}]",
            val exitingClaim: String? = "[{\"text\": \"You exiting the claim of \", \"color\": \"gray\"}, {\"text\": \"\${owner.getName()}\", \"color\": \"gold\"}, {\"text\": \".\", \"color\": \"gray\"}]",
            val maxClaims: Int = 10
        )
    }
}