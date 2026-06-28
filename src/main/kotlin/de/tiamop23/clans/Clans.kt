package de.tiamop23.clans

import org.bukkit.plugin.java.JavaPlugin

class Clans : JavaPlugin() {

    override fun onEnable() {
        // Plugin startup logic
        logger.info("${this.name} enabled2")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
