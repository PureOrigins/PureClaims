package it.pureorigins.pureclaims

import it.pureorigins.common.runTaskAsynchronously
import it.pureorigins.pureclaims.PureClaims.Companion.worldBorder
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import org.bukkit.Chunk
import org.bukkit.World
import org.bukkit.craftbukkit.v1_18_R2.CraftWorld
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import java.lang.Thread.sleep
import java.util.*

data class ClaimedChunk(
    val owner: UUID,
    val world: World,
    val chunkPos: Chunk
)

fun Chunk.highlight(player: Player) {
    PureClaims.instance.runTaskAsynchronously {
        val newBorder = worldBorder(this)
        val craftPlayer = player as CraftPlayer
        val defaultBorder = (craftPlayer.world as CraftWorld).handle.worldBorder

        craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(newBorder))
        sleep(333)
        craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(defaultBorder))
        sleep(333)
        craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(newBorder))
        sleep(333)
        craftPlayer.handle.connection.send(ClientboundInitializeBorderPacket(defaultBorder))
        sleep(333)
    }
}
