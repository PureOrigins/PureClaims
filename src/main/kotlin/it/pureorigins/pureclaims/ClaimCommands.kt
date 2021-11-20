package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.*
import kotlinx.serialization.Serializable

class ClaimCommands(private val config: Config) {

    private val PERM_PREFIX = "purclaims"

    val command
        get() = literal(config.commandName) {
            requiresPermission(PERM_PREFIX)
            success { source.sendFeedback(config.commandUsage?.templateText()) }
            then(claimCommand)
            then(unclaimCommand)
        }

    private val claimCommand
        get() = literal(config.claim.commandName) {
            requiresPermission("$PERM_PREFIX.claim")
            success {
                val player = source.player
                val chunkPos = player.chunkPos
                when {
                    PureClaims.isClaimed(player.world, chunkPos) ->
                        source.sendFeedback(config.claim.alreadyClaimed?.templateText())
                    PureClaims.getClaimCount(player.uuid) >= PureClaims.settings.maxClaims ->
                        source.sendFeedback(config.claim.noClaimSlotsAvailable?.templateText())
                    else -> {
                        PureClaims.addClaim(player, chunkPos)
                        source.sendFeedback(config.claim.success?.templateText())
                    }
                }
            }
        }

    private val unclaimCommand
        get() = literal(config.unclaim.commandName) {
            requiresPermission("$PERM_PREFIX.unclaim")
            success {
                val player = source.player
                val chunkPos = player.chunkPos
                when {
                    !PureClaims.isClaimed(player.world, chunkPos) ->
                        source.sendFeedback(config.unclaim.notClaimed?.templateText())
                    PureClaims.getClaim(player.world, chunkPos)?.owner != source.player.uuid ->
                        source.sendFeedback(config.unclaim.claimedByAnotherPlayer?.templateText())
                    else -> {
                        PureClaims.removeClaim(source.player, PureClaims.getClaim(player.world, chunkPos)!!)
                        source.sendFeedback(config.claim.success?.templateText())
                    }
                }
            }
        }

    private val infoCommand
        get() = literal(config.info.commandName) {
            requiresPermission("$PERM_PREFIX.info")
            success {
                source.sendFeedback(config.claim.success?.templateText())
            }
        }

    @Serializable
    data class Config(
        val commandName: String = "pc",
        val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/$commandName <claim | unclaim | info>\", \"color\": \"gray\"}]",
        val claim: Claim = Claim(),
        val unclaim: Unclaim = Unclaim(),
        val info: Info = Info()
    ) {
        @Serializable
        data class Claim(
            val commandName: String = "claim",
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/pc $commandName\", \"color\": \"gray\"}]",
            val noClaimSlotsAvailable: String? = "{\"text\": \"you have reached the max claim slots.\", \"color\": \"dark_gray\"}",
            val alreadyClaimed: String? = "{\"text\": \"this land have already been claimed by another player.\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"chunk claimed.\", \"color\": \"gray\"}"
        )

        @Serializable
        data class Unclaim(
            val commandName: String = "unclaim",
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/pc $commandName\", \"color\": \"gray\"}]",
            val notClaimed: String? = "{\"text\": \"this land is not claimed\", \"color\": \"dark_gray\"}",
            val claimedByAnotherPlayer: String? = "{\"text\": \"this land is not claimed by you\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"chunk unclaimed.\", \"color\": \"gray\"}"
        )

        @Serializable
        data class Info(
            val commandName: String = "info",
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/pc $commandName\", \"color\": \"gray\"}]",
            val success: String? = "{\"text\": \"eeeee.\", \"color\": \"gray\"}"
        )
    }
}
