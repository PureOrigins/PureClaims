package it.pureorigins.pureclaims

import it.pureorigins.common.*
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.ChatType.GAME_INFO
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import net.minecraft.world.level.border.WorldBorder
import org.bukkit.*
import org.bukkit.Bukkit.getOfflinePlayer
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

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
    
    fun isLoaded(player: Player): Boolean {
        return player.uniqueId in permissions
    }
    
    fun getPermissions(player: Player, claim: ClaimedChunk): ClaimPermissions {
        return permissions[player, claim]
    }
    
    fun isLoaded(chunk: Chunk): Boolean {
        return chunk in claims
    }

    fun isClaimed(chunk: Chunk): Boolean {
        return claims[chunk] != null
    }

    fun isClaimed(location: Location): Boolean {
        return isClaimed(location.chunk)
    }

    fun getClaim(chunk: Chunk): ClaimedChunk? {
        return claims[chunk]
    }

    fun getClaim(location: Location): ClaimedChunk? {
        return claims[location.chunk]
    }

    fun addClaimDatabase(claim: ClaimedChunk) = transaction(database) {
        if (PlayerClaimsTable.add(claim)) {
            claims[claim.chunk] = claim
        }
    }

    fun removeClaimDatabase(claim: ClaimedChunk) = transaction(database) {
        if (PlayerClaimsTable.remove(claim)) {
            claims -= claim.chunk
        }
    }

    fun hasPermissions(player: Player, chunk: Chunk, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
        if (!isLoaded(player) || !isLoaded(chunk)) return false
        
        val claim = claims[chunk] ?: return true
        val permissions = getPermissions(player, claim)
        return permissions.requiredPermissions()
    }

    fun hasPermissions(player: Player, location: Location, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
        return hasPermissions(player, location.chunk, requiredPermissions)
    }

    fun checkPermissions(player: Player, chunk: Chunk, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
        return hasPermissions(player, chunk, requiredPermissions).also {
            if (!it) {
                highlightChunk(player, chunk)
                when (requiredPermissions) {
                    ClaimPermissions.EDIT -> player.sendNullableMessage(settings.cannotEdit?.templateText(), GAME_INFO)
                    ClaimPermissions.INTERACT -> player.sendNullableMessage(settings.cannotInteract?.templateText(), GAME_INFO)
                    ClaimPermissions.DAMAGE_MOBS -> player.sendNullableMessage(settings.cannotDamageMobs?.templateText(), GAME_INFO)
                }
            }
        }
    }

    fun checkPermissions(player: Player, location: Location, requiredPermissions: ClaimPermissions.() -> Boolean
    ): Boolean {
        return checkPermissions(player, location.chunk, requiredPermissions)
    }
    
    fun highlightChunk(player: Player, chunk: Chunk) {
        fun worldBorder(chunk: Chunk) = WorldBorder().apply {
            setCenter(chunk.x * 16.0 + 8.0, chunk.z * 16.0 + 8.0)
            size = 16.0
            damagePerBlock = 0.0
            warningTime = 0
            warningBlocks = 0
        }
        
        runTaskAsynchronously {
            val newBorder = worldBorder(chunk)
            val craftPlayer = player as CraftPlayer
            val defaultBorder = (craftPlayer.world as CraftWorld).handle.worldBorder
            
            craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(newBorder))
            Thread.sleep(333)
            craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(defaultBorder))
            Thread.sleep(333)
            craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(newBorder))
            Thread.sleep(333)
            craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(defaultBorder))
            Thread.sleep(333)
        }
    }

    fun sendClaimChangeMessage(player: Player, oldClaim: ClaimedChunk, newClaim: ClaimedChunk?) {
        if (newClaim != oldClaim) {
            if (newClaim == null) {
                player.sendNullableMessage(settings.exitingClaim?.templateText("owner" to getOfflinePlayer(oldClaim.owner).name), GAME_INFO)
            } else {
                player.sendNullableMessage(settings.enteringClaim?.templateText("owner" to getOfflinePlayer(newClaim.owner).name), GAME_INFO)
            }
        }
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
        registerCommand(ClaimCommands(this, commands).command)
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