package de.tiamop23.minecraft.clans.paper.listener

import de.tiamop23.minecraft.clans.core.pvp.PvPManager
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class PvPListener(private val pvpManager: PvPManager) : Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val damager = event.damager as? Player ?: return
        val victim = event.entity as? Player ?: return

        if (!pvpManager.isPvPAllowed(damager.uniqueId, victim.uniqueId)) {
            event.isCancelled = true
        }
    }
}
