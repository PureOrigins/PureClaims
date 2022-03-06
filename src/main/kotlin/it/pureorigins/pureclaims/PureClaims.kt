package it.pureorigins.pureclaims

import it.pureorigins.common.*
import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import net.minecraft.network.chat.ChatType.GAME_INFO
import net.minecraft.world.level.border.WorldBorder
import org.bukkit.*
import org.bukkit.Bukkit.getOfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class PureClaims : JavaPlugin() {
    companion object {
        private lateinit var database: Database
        private lateinit var claims: Claims
        private lateinit var permissions: Permissions
        private lateinit var settings: Config.Settings
        private val scope = CoroutineScope(SupervisorJob() + CoroutineName("PureClaims"))
        internal lateinit var server: Server
        lateinit var instance: PureClaims
            private set

        private fun <R> asyncTransaction(statement: Transaction.() -> R) =
            scope.async(Dispatchers.IO) { transaction(database, statement) }

        private fun launchTransaction(statement: Transaction.() -> Unit) =
            scope.launch(Dispatchers.IO) { transaction(database, statement) }

        fun getClaimedChunkNotCached(world: World, Chunk: Chunk): Deferred<ClaimedChunk?> = asyncTransaction {
            PlayerClaimsTable.getClaim(world, Chunk)
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

        fun getPermissions(player: Player, claim: ClaimedChunk): ClaimPermissions {
            return permissions[player, claim]
        }

        fun isClaimed(world: World, Chunk: Chunk): Boolean {
            return world to Chunk in claims
        }

        fun isClaimed(world: World, location: Location): Boolean {
            return isClaimed(world, location.chunk)
        }

        fun getClaim(world: World, Chunk: Chunk): ClaimedChunk? {
            return claims[world, Chunk]
        }

        fun getClaim(world: World, location: Location): ClaimedChunk? {
            return claims[world, location.chunk]
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

        fun hasPermissions(player: Player, chunk: Chunk, requiredPermissions: ClaimPermissions.() -> Boolean): Boolean {
            val claim = claims[player.world, chunk] ?: return true
            val permissions = getPermissions(player, claim)
            return permissions.requiredPermissions()
        }

        fun hasPermissions(
            player: Player,
            location: Location,
            requiredPermissions: ClaimPermissions.() -> Boolean
        ): Boolean {
            return hasPermissions(player, location.chunk, requiredPermissions)
        }

        fun checkPermissions(
            player: Player,
            chunk: Chunk,
            requiredPermissions: ClaimPermissions.() -> Boolean
        ): Boolean {
            return hasPermissions(player, chunk, requiredPermissions).also {
                if (!it) {
                    chunk.highlight(player)
                    when (requiredPermissions) {
                        ClaimPermissions.EDIT -> player.sendNullableMessage(settings.cannotEdit?.templateText(), GAME_INFO)
                        ClaimPermissions.INTERACT -> player.sendNullableMessage(settings.cannotInteract?.templateText(), GAME_INFO)
                        ClaimPermissions.DAMAGE_MOBS -> player.sendNullableMessage(settings.cannotDamageMobs?.templateText(), GAME_INFO)
                    }
                }
            }
        }

        fun checkPermissions(
            player: Player,
            location: Location,
            requiredPermissions: ClaimPermissions.() -> Boolean
        ): Boolean {
            return checkPermissions(player, location.chunk, requiredPermissions)
        }

        fun sendClaimChangeMessage(player: Player, oldClaim: ClaimedChunk, newClaim: ClaimedChunk?) {
            scope.launch(Dispatchers.IO) {
                if (newClaim != oldClaim) {
                    if (newClaim == null) {
                        player.sendNullableMessage(
                            settings.exitingClaim?.templateText(
                                "owner" to getOfflinePlayer(oldClaim.owner).name,
                            ), GAME_INFO
                        )
                    } else {
                        player.sendNullableMessage(
                            settings.enteringClaim?.templateText(
                                "owner" to getOfflinePlayer(newClaim.owner).name,
                            ), GAME_INFO
                        )
                    }
                }
            }
        }

        fun worldBorder(chunk: Chunk) = WorldBorder().apply {
            setCenter(chunk.x * 16.0 + 8.0, chunk.z * 16.0 + 8.0)
            size = 16.0
            damagePerBlock = 0.0
            warningTime = 0
            warningBlocks = 0
        }
    }

    override fun onEnable() {
        val (db, commands, settings) = json.readFileAs(file("pureclaims.json"), Config())
        PureClaims.settings = settings
        require(db.url.isNotEmpty()) { "Database url is empty" }
        PureClaims.server = Bukkit.getServer()
        database = Database.connect(db.url, user = db.username, password = db.password)
        transaction(database) {
            SchemaUtils.createMissingTablesAndColumns(
                PlayerClaimsTable,
                PermissionsTable,
                PlayerTable
            )
        }
        claims = Claims()
        permissions = Permissions()
        registerEvents(Events())
        registerCommand(ClaimCommands(commands).command)
    }

    override fun onDisable() {
        scope.cancel()
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