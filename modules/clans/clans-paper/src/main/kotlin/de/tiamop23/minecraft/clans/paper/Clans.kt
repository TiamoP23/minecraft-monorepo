package de.tiamop23.minecraft.clans.paper

import de.tiamop23.minecraft.clans.core.pvp.InMemoryPvPManager
import de.tiamop23.minecraft.clans.core.pvp.PvPManager
import de.tiamop23.minecraft.clans.paper.command.PvPCommand
import de.tiamop23.minecraft.clans.paper.listener.PvPListener
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class Clans : JavaPlugin() {

    private lateinit var pvpManager: PvPManager

    override fun onEnable() {
        pvpManager = InMemoryPvPManager()

        registerCommands()
        registerListeners()

        logger.info("${this.name} enabled")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }

    private fun registerCommands() {
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
            commands.registrar().register(PvPCommand.create(pvpManager), "Manage PvP settings")
        }
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(PvPListener(pvpManager), this)
    }
}
