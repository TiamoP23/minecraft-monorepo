package de.tiamop23.minecraft.clans.paper

import org.bukkit.plugin.java.JavaPlugin

class Clans : JavaPlugin() {

    override fun onEnable() {
        logger.info("${this.name} enabled2")
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
