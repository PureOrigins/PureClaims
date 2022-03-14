package it.pureorigins.pureclaims

import com.mojang.brigadier.arguments.StringArgumentType.getString
import com.mojang.brigadier.arguments.StringArgumentType.greedyString
import it.pureorigins.common.*
import kotlinx.serialization.Serializable
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.TextColor
import net.minecraft.commands.arguments.EntityArgument.getPlayer
import net.minecraft.commands.arguments.EntityArgument.player
import org.bukkit.Bukkit

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
                    plugin.isClaimed(player.chunk) ->
                        source.sendNullableMessage(config.add.alreadyClaimed?.templateText())
                    // TODO remove comment, this is for testing
                    // PureClaims.getClaimCount(player.uuid) >= PureClaims.getMaxClaims(player.uuid) ->
                    //     source.sendNullableMessage(config.add.noClaimSlotsAvailable?.templateText())
                    else -> {
                        plugin.addClaimDatabase(ClaimedChunk(player.uniqueId, player.chunk))
                        source.sendNullableMessage(config.add.success?.templateText())
                    }
                }
            }
        }

    private val removeCommand
        get() = literal(config.remove.commandName) {
            requiresPermission("$PERM_PREFIX.remove")
            success {
                val player = source.player
                if (!plugin.isLoaded(player.chunk) || !plugin.isLoaded(player)) return@success source.sendNullableMessage(
                    config.chunkNotLoaded?.templateText()
                )
                when {
                    !plugin.isClaimed(player.chunk) ->
                        source.sendNullableMessage(config.remove.notClaimed?.templateText())
                    plugin.getClaim(player.chunk)?.owner != player.uniqueId ->
                        source.sendNullableMessage(config.remove.claimedByAnotherPlayer?.templateText())
                    else -> {
                        plugin.removeClaimDatabase(plugin.getClaim(player.chunk)!!)
                        source.sendNullableMessage(config.add.success?.templateText())
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
                player.sendNullableMessage(config.view.message?.templateText("claim" to claim))
                plugin.highlightChunk(player, player.chunk)
            }
        }

    private fun allowCommand(allow: Boolean) =
        literal(if (allow) config.allow.allowCommandName else config.allow.denyCommandName) {
            requiresPermission("$PERM_PREFIX.allow")
            success { source.sendNullableMessage(config.allow.usage?.templateText("allow" to allow)) }
            then(argument("player", player()) {
                if (allow) suggestions { Bukkit.getOnlinePlayers().map { it.name } }
                else suggestions { Bukkit.getOnlinePlayers().map { it.name } }
                success { source.sendNullableMessage(config.allow.usage?.templateText("allow" to allow)) }
                then(argument("permission", greedyString()) {
                    suggestions { listOf("all", "edit", "interact", "damageMobs") }
                    success {
                        val owner = source.player
                        val player = getPlayer(this, "player")
                        val permissions = plugin.getPermissions(player.uuid, owner.uniqueId)
                        when (val permission = getString(this, "permission")) {
                            "all" -> plugin.setPermissionsDatabase(
                                owner.uniqueId,
                                player.uuid,
                                if (allow) ClaimPermissions.ALL else ClaimPermissions.NONE
                            )
                            "edit" -> plugin.setPermissionsDatabase(
                                owner.uniqueId,
                                player.uuid,
                                permissions.withCanEdit(allow)
                            )
                            "interact" -> plugin.setPermissionsDatabase(
                                owner.uniqueId,
                                player.uuid,
                                permissions.withCanInteract(allow)
                            )
                            "damageMobs" -> plugin.setPermissionsDatabase(
                                owner.uniqueId,
                                player.uuid,
                                permissions.withCanDamageMobs(allow)
                            )
                            else -> source.sendNullableMessage(
                                config.allow.unknownPermission?.templateText(
                                    "allow" to allow,
                                    "permission" to permission
                                )
                            )
                        }
                    }
                })
            })
        }

    //TODO to be finished
    private val permissionsCommand
        get() = literal(config.permissions.commandName) {
            requiresPermission("$PERM_PREFIX.permissions")
            success { source.sendNullableMessage(config.permissions.usage?.templateText()) }
            then(argument("player", player()) {
                success {
                    val player = source.player
                    val target = getPlayer(this, "player")
                    val claim = plugin.getClaim(player.chunk)
                    if (claim?.owner == player.uniqueId) {
                        val perms = plugin.getPermissions(target.uuid, player.uniqueId)

                        //TODO
                        val canEditString = "EDIT".toPaperText()
                        val colored = if (perms.canEdit) canEditString.color(TextColor.fromHexString("#00FF00"))
                        else canEditString.color(TextColor.fromHexString("#FF0000"))
                        val action = if (perms.canEdit) "allow" else "deny"
                        val clickable = colored.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/claim $action ${target.name} edit"))


                        player.sendNullableMessage(
                            config.permissions.success?.templateText(
                                "player" to target,
                                "permissions" to perms
                            )
                        )
                    } else {
                        player.sendNullableMessage(config.permissions.notOwner?.templateText())
                    }
                }
            })
        }

    @Serializable
    data class Config(
        val commandName: String = "claim",
        val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim <add | remove | info>\", \"color\": \"gray\"}]",
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
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim add\", \"color\": \"gray\"}]",
            val noClaimSlotsAvailable: String? = "{\"text\": \"you have reached the max claim slots.\", \"color\": \"dark_gray\"}",
            val alreadyClaimed: String? = "{\"text\": \"this land have already been claimed by another player.\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"chunk claimed.\", \"color\": \"gray\"}"
        )

        @Serializable
        data class Remove(
            val commandName: String = "remove",
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim remove\", \"color\": \"gray\"}]",
            val notClaimed: String? = "{\"text\": \"this land is not claimed\", \"color\": \"dark_gray\"}",
            val claimedByAnotherPlayer: String? = "{\"text\": \"this land is not claimed by you\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"chunk unclaimed.\", \"color\": \"gray\"}"
        )

        @Serializable
        data class Info(
            val commandName: String = "info",
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim info\", \"color\": \"gray\"}]",
            val success: String? = "{\"text\": \"Max claims: \${max}\nUsed claims: \${used}\nRemained claims: \${remained}\", \"color\": \"gray\"}"
        )

        @Serializable
        data class View(
            val commandName: String = "view",
            val message: String? = "<#if claim??>[{\"text\": \"Claim of \", \"color\": \"gray\"}, {\"text\": \"\${claim.owner.getName()}\", \"color\": \"gold\"}, {\"text\": \".\", \"color\": \"gray\"}]<#else>{\"text\": \"Unclaimed chunk.\", \"color\": \"gray\"}</#if>"
        )

        @Serializable
        data class Allow(
            val allowCommandName: String = "allow",
            val denyCommandName: String = "deny",
            val usage: String? = "",
            val unknownPermission: String? = "",
            val message: String? = ""
        )

        @Serializable
        data class Permissions(
            val commandName: String = "permissions",
            val usage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim permissions [player]\", \"color\": \"gray\"}]",
            val success: String? = "{\"text\": \"Permissions for \${player}: \n\${permissions}, \"color\": \"gray\"}",
            val notOwner: String? = "{\"text\": \"You are not the owner of this claim.\", \"color\": \"red\"}"
        ) {

        }
    }
}
