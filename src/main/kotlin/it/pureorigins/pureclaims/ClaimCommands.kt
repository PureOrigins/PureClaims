package it.pureorigins.pureclaims

import it.pureorigins.framework.configuration.*
import kotlinx.serialization.Serializable

class ClaimCommands(private val config: Config) {

    val command
        get() = literal(config.commandName) {
            requiresPermission("fancyparticles.particles")
            success { source.sendFeedback(config.commandUsage?.templateText()) }
            then(claimCommand)
        }

    private val claimCommand
        get() = literal(config.claim.commandName) {
            requiresPermission("fancyparticles.claims.claim")
        }

    @Serializable
    data class Config(
        val commandName: String = "particles",
        val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/$commandName <set | info>\", \"color\": \"gray\"}]",
        val nullparticleName: String = "none",
        val claim: Claim = Claim(),
    ) {
        @Serializable
        data class Claim(
            val commandName: String = "set",
            val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/particles set <particle | none>\", \"color\": \"gray\"}]",
            val particleNotFound: String? = "{\"text\": \"Particle not found.\", \"color\": \"dark_gray\"}",
            val particleNotOwned: String? = "{\"text\": \"particle not owned.\", \"color\": \"dark_gray\"}",
            val success: String? = "{\"text\": \"particle set.\", \"color\": \"gray\"}"
        )
    }
}
