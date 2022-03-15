package it.pureorigins.pureclaims

import com.mojang.authlib.GameProfile
import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import com.mojang.brigadier.context.CommandContext
import it.pureorigins.common.*
import kotlinx.serialization.Serializable
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.GameProfileArgument.gameProfile
import net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles
import net.minecraft.network.chat.ChatType
import org.bukkit.Bukkit.getOfflinePlayer
import org.bukkit.Bukkit.getOnlinePlayers

class ClaimCommands(private val plugin: PureClaims, private val config: Config) {
    private val PERM_PREFIX = "pureclaims.claim"
    
    val command
        get() = literal(config.commandName) {
            requiresPermission(PERM_PREFIX)
            success { source.sendNullableMessage(config.commandUsage?.templateText()) }
            then(addCommand)
            then(removeCommand)
            then(infoCommand)
            then(viewCommand)
            then(allowCommand(true))
            then(allowCommand(false))
            then(permissionsCommand)
        }
    
    private val addCommand
        get() = literal(config.add.commandName) {
            requiresPermission("$PERM_PREFIX.add")
            success {
                val player = source.player
                if (!plugin.isLoaded(player.chunk) || !plugin.isLoaded(player)) return@success source.sendNullableMessage(
                    config.chunkNotLoaded?.templateText()
                )
                when {
                    plugin.isClaimed(player.chunk) -> {
                        source.sendNullableMessage(config.add.alreadyClaimed?.templateText())
                        plugin.highlightChunk(player, player.chunk)
                    }
                    // TODO remove comment, this is for testing
                    // PureClaims.getClaimCount(player.uuid) >= PureClaims.getMaxClaims(player.uuid) ->
                    //     source.sendNullableMessage(config.add.noClaimSlotsAvailable?.templateText())
                    else -> {
                        plugin.addClaimDatabase(ClaimedChunk(player.uniqueId, player.chunk))
                        source.sendNullableMessage(config.add.success?.templateText())
                        plugin.highlightChunk(player, player.chunk)
                    }
                }
            }
        }
    
    private val removeCommand
        get() = literal(config.remove.commandName) {
            requiresPermission("$PERM_PREFIX.remove")
            success {
                val player = source.player
                if (!plugin.isLoaded(player.chunk) || !plugin.isLoaded(player))
                    return@success source.sendNullableMessage(config.chunkNotLoaded?.templateText())
                when {
                    plugin.getClaim(player.chunk)?.owner != player.uniqueId -> {
                        source.sendNullableMessage(config.remove.notClaimed?.templateText())
                        plugin.highlightChunk(player, player.chunk)
                    }
                    else -> {
                        plugin.removeClaimDatabase(plugin.getClaim(player.chunk)!!)
                        source.sendNullableMessage(config.remove.success?.templateText())
                        plugin.highlightChunk(player, player.chunk)
                    }
                }
            }
        }
    
    private val infoCommand
        get() = literal(config.info.commandName) {
            requiresPermission("$PERM_PREFIX.info")
            success {
                val player = source.player
                val max = plugin.getMaxClaimsDatabase(player.uniqueId)
                val used = plugin.getClaimCountDatabase(player.uniqueId)
                val remained = max - used
                source.sendNullableMessage(
                    config.info.success?.templateText(
                        "max" to max,
                        "remained" to remained,
                        "used" to used
                    )
                )
            }
        }
    
    private val viewCommand
        get() = literal(config.view.commandName) {
            requiresPermission("$PERM_PREFIX.view")
            success {
                val player = source.player
                val claim = plugin.getClaim(player.chunk)
                player.sendNullableMessage(config.view.message?.templateText("claim" to claim, "owner" to claim?.let { getOfflinePlayer(it.owner) }), position = ChatType.GAME_INFO)
                plugin.highlightChunk(player, player.chunk)
            }
        }
    
    private fun allowCommand(allow: Boolean) =
        literal(if (allow) config.allow.allowCommandName else config.allow.denyCommandName) {
            requiresPermission("$PERM_PREFIX.allow")
            success { source.sendNullableMessage(config.allow.usage?.templateText("allow" to allow)) }
            then(argument("player", gameProfile()) {
                suggestions { getOnlinePlayers().map { it.name } }
                success { source.sendNullableMessage(config.allow.usage?.templateText("allow" to allow)) }
                then(argument("permission", greedyString()) {
                    suggestions { listOf("all", "edit", "interact", "damageMobs") }
                    success {
                        val owner = source.player
                        val player = getGameProfiles(this, "player").singleOrNull()
                            ?: throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.create()
                        val oldPermissions = plugin.getPermissions(player.id, owner.uniqueId)
                        val permissions = when (val permission = getString(this, "permission")) {
                            "all" -> if (allow) ClaimPermissions.ALL else ClaimPermissions.NONE
                            "edit" -> oldPermissions.withCanEdit(allow)
                            "interact" -> oldPermissions.withCanInteract(allow)
                            "damageMobs" -> oldPermissions.withCanDamageMobs(allow)
                            else -> return@success source.sendNullableMessage(
                                config.allow.unknownPermission?.templateText(
                                    "allow" to allow,
                                    "permission" to permission
                                )
                            )
                        }
                        plugin.setPermissionsDatabase(owner.uniqueId, player.id, permissions)
                        source.sendNullableMessage(config.permissions.message?.templateText(
                            "player" to player,
                            "permissions" to permissions
                        ))
                    }
                })
            })
        }
    
    private val permissionsCommand
        get() = literal(config.permissions.commandName) {
            requiresPermission("$PERM_PREFIX.permissions")
            success { source.sendNullableMessage(config.permissions.usage?.templateText()) }
            then(argument("player", gameProfile()) {
                suggestions { getOnlinePlayers().map { it.name } }
                success {
                    val player = source.player
                    val target = getGameProfile(this, "player")
                    val permissions = plugin.getPermissions(target.id, player.uniqueId)
                    player.sendNullableMessage(
                        config.permissions.message?.templateText(
                            "player" to target,
                            "permissions" to permissions
                        )
                    )
                }
            })
        }
    
    private fun getGameProfile(context: CommandContext<CommandSourceStack>, name: String): GameProfile {
        val profiles = getGameProfiles(context, name)
        return profiles.singleOrNull() ?: throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.create()
    }
    
    @Serializable
    data class Config(
        val commandName: String = "claim",
        val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim <add | remove | info | view | permissions | allow | deny>\", \"color\": \"gray\"}]",
        val chunkNotLoaded: String? = "{\"text\": \"this chunk is not loaded yet.\", \"color\": \"dark_gray\"}",
        val add: Add = Add(),
        val remove: Remove = Remove(),
        val info: Info = Info(),
        val view: View = View(),
        val allow: Allow = Allow(),
        val permissions: Permissions = Permissions()
    ) {
        @Serializable
        data class Add(
            val commandName: String = "add",
            val noClaimSlotsAvailable: String? = "{\"text\": \"You have reached the max claim slots.\", \"color\": \"dark_gray\"}",
            val alreadyClaimed: String? = "{\"text\": \"This land have already been claimed by another player.\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"Chunk claimed.\", \"color\": \"gray\"}"
        )
        
        @Serializable
        data class Remove(
            val commandName: String = "remove",
            val notClaimed: String? = "{\"text\": \"This chunk is not claimed by you.\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"Chunk unclaimed.\", \"color\": \"gray\"}"
        )
        
        @Serializable
        data class Info(
            val commandName: String = "info",
            val success: String? = "{\"text\": \"Max claims: \${max}\nUsed claims: \${used}\nRemained claims: \${remained}\", \"color\": \"gray\"}"
        )
        
        @Serializable
        data class View(
            val commandName: String = "view",
            val message: String? = "<#if owner??>[{\"text\": \"Claim of \", \"color\": \"gray\"}, {\"text\": \"\${owner.getName()}\", \"color\": \"gold\"}, {\"text\": \".\", \"color\": \"gray\"}]<#else>{\"text\": \"Unclaimed chunk.\", \"color\": \"gray\"}</#if>"
        )
        
        @Serializable
        data class Allow(
            val allowCommandName: String = "allow",
            val denyCommandName: String = "deny",
            val usage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim <#if allow>allow<#else>deny</#if> <player> <interact | edit | damageMobs | all>\", \"color\": \"gray\"}]",
            val unknownPermission: String? = "{\"text\": \"Invalid permission '\${permission}'.\", \"color\": \"dark_gray\"}"
        )
        
        @Serializable
        data class Permissions(
            val commandName: String = "permissions",
            val usage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim permissions <player>\", \"color\": \"gray\"}]",
            val message: String? = "[{\"text\": \"Permissions of \${player.name}: \", \"color\": \"gray\"}, {\"text\": \"edit\", \"color\": \"<#if permissions.canEdit>green<#else>red</#if>\", \"clickEvent\": {\"action\": \"run_command\", \"value\": \"/claim <#if permissions.canEdit>deny<#else>allow</#if> \${player.name} edit\"}}, {\"text\": \" -> \", \"color\": \"gray\"}, {\"text\": \"interact\", \"color\": \"<#if permissions.canInteract>green<#else>red</#if>\", \"clickEvent\": {\"action\": \"run_command\", \"value\": \"/claim <#if permissions.canInteract>deny<#else>allow</#if> \${player.name} interact\"}}, {\"text\": \", \", \"color\": \"gray\"}, {\"text\": \"damage mobs\", \"color\": \"<#if permissions.canDamageMobs>green<#else>red</#if>\", \"clickEvent\": {\"action\": \"run_command\", \"value\": \"/claim <#if permissions.canDamageMobs>deny<#else>allow</#if> \${player.name} damageMobs\"}}]"
        )
    }
}
