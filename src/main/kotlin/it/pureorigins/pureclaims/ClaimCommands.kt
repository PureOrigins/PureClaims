package it.pureorigins.pureclaims

import it.pureorigins.common.*
import kotlinx.serialization.Serializable
import org.bukkit.entity.Player

class ClaimCommands(private val config: Config) {

  private val PERM_PREFIX = "pureclaims.claim"

  val command
    get() = literal(config.commandName) {
      requiresPermission(PERM_PREFIX)
      success { source.sendNullableMessage(config.commandUsage?.templateText()) }
      then(addCommand)
      then(removeCommand)
    }

  private val addCommand
    get() = literal(config.add.commandName) {
      requiresPermission("$PERM_PREFIX.add")
      success {
        val player = source.bukkitSender as Player
        val chunk = player.chunk
        when {
          PureClaims.isClaimed(player.world, chunk) ->
            source.sendNullableMessage(config.add.alreadyClaimed?.templateText())
          // PureClaims.getClaimCount(player.uuid) >= PureClaims.getMaxClaims(player.uuid) ->
          //     source.sendNullableMessage(config.add.noClaimSlotsAvailable?.templateText())
          else -> {
            PureClaims.addClaim(ClaimedChunk(player.uniqueId, player.world, chunk))
            source.sendNullableMessage(config.add.success?.templateText())
          }
        }
      }
    }

  private val removeCommand
    get() = literal(config.remove.commandName) {
      requiresPermission("$PERM_PREFIX.remove")
      success {
        val player = source.bukkitSender as Player
        val chunk = player.chunk
        when {
          !PureClaims.isClaimed(player.world, chunk) ->
            source.sendNullableMessage(config.remove.notClaimed?.templateText())
          PureClaims.getClaim(player.world, chunk)?.owner != player.uniqueId ->
            source.sendNullableMessage(config.remove.claimedByAnotherPlayer?.templateText())
          else -> {
            PureClaims.removeClaim(PureClaims.getClaim(player.world, chunk)!!)
            source.sendNullableMessage(config.add.success?.templateText())
          }
        }
      }
    }

  private val infoCommand
    get() = literal(config.info.commandName) {
      requiresPermission("$PERM_PREFIX.info")
      success {
        source.sendNullableMessage(config.add.success?.templateText())
      }
    }

  @Serializable
  data class Config(
    val commandName: String = "claim",
    val commandUsage: String? = "[{\"text\": \"Usage: \", \"color\": \"dark_gray\"}, {\"text\": \"/claim <add | remove | info>\", \"color\": \"gray\"}]",
    val add: Add = Add(),
    val remove: Remove = Remove(),
    val info: Info = Info()
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
      val success: String? = "{\"text\": \"eeeee.\", \"color\": \"gray\"}"
    )
  }
}
