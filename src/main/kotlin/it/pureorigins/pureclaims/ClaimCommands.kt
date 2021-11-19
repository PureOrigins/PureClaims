package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.*
import kotlinx.serialization.Serializable

class ClaimCommands(private val config: Config) {

    val PERM_PREFIX = "purclaims"

    val command
        get() = literal(config.commandName) {
            requiresPermission(PERM_PREFIX)
            success { source.sendFeedback(config.commandUsage?.templateText()) }
            then(claimCommand)
        }

    private val claimCommand
        get() = literal(config.claim.commandName) {
            requiresPermission("$PERM_PREFIX.claim")
            success {
                val chunkpos = source.player.chunkPos
                when {
                    PureClaims.claimsCache[source.world]?.containsKey(chunkpos) == true ->
                        source.sendFeedback(config.claim.alreadyClaimed?.templateText())
                    PureClaims.getClaimCount(source.player.uuid) >= PureClaims.settings.maxClaims ->
                        source.sendFeedback(config.claim.noClaimSlotsAvailable?.templateText())
                    else -> {
                        //TODO add claim to db
                        PureClaims.claimsCache[source.world]?.put(chunkpos, ClaimedChunk(source.player, chunkpos))
                        source.sendFeedback(config.claim.success?.templateText())
                    }
                }
            }
        }

    @Serializable
    data class Config(
        val commandName: String = "pc",
        val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/$commandName <claim | info>\", \"color\": \"gray\"}]",
        val claim: Claim = Claim(),
    ) {
        @Serializable
        data class Claim(
            val commandName: String = "claim",
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/pc $commandName\", \"color\": \"gray\"}]",
            val noClaimSlotsAvailable: String? = "{\"text\": \"you have reached the max claim slots.\", \"color\": \"dark_gray\"}",
            val alreadyClaimed: String? = "{\"text\": \"this land have already been claimed by another player.\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"chunk claimed.\", \"color\": \"gray\"}"
        )
    }
}
