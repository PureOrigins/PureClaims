package it.pureorigins.pureclaims

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

class PlayerInitializer(private val maxClaims: Int) : Listener {
    @EventHandler
    fun onPlayerFirstJoin(e: PlayerJoinEvent) {
        if (!e.player.hasPlayedBefore()) {
            plugin.setMaxClaimsDatabase(e.player.uniqueId, maxClaims)
        }
    }
}