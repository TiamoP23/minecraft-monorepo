package de.tiamop23.minecraft.clans.paper.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import de.tiamop23.minecraft.clans.core.pvp.PvPManager
import de.tiamop23.minecraft.clans.core.pvp.PvPMode
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

object PvPCommand {
    fun create(pvpManager: PvPManager): LiteralCommandNode<CommandSourceStack> {
        return Commands.literal("pvp")
            .executes { ctx ->
                val executor = ctx.source.executor

                if (executor is Player) {
                    val currentMode = pvpManager.getMode(executor.uniqueId)
                    val newMode = currentMode.toggle()

                    pvpManager.setMode(executor.uniqueId, newMode)
                    executor.sendMessage(
                        Component.text("PvP ")
                            .append(Component.text(newMode.name, modeColor(newMode)))
                            .append(Component.text("."))
                    )
                }

                Command.SINGLE_SUCCESS
            }
            .then(
                Commands.literal("on").executes { ctx ->
                    val executor = ctx.source.executor

                    if (executor is Player) {
                        pvpManager.setMode(executor.uniqueId, PvPMode.ON)
                        executor.sendMessage(
                            Component.text("PvP ")
                                .append(Component.text("ON", NamedTextColor.GREEN))
                                .append(Component.text("."))
                        )
                    }

                    Command.SINGLE_SUCCESS
                }
            )
            .then(
                Commands.literal("off").executes { ctx ->
                    val executor = ctx.source.executor

                    if (executor is Player) {
                        pvpManager.setMode(executor.uniqueId, PvPMode.OFF)
                        executor.sendMessage(
                            Component.text("PvP ")
                                .append(Component.text("OFF", NamedTextColor.RED))
                                .append(Component.text("."))
                        )
                    }

                    Command.SINGLE_SUCCESS
                }
            )
            .then(
                Commands.literal("status").executes { ctx ->
                    val executor = ctx.source.executor

                    if (executor is Player) {
                        val mode = pvpManager.getMode(executor.uniqueId)

                        executor.sendMessage(
                            Component.text("PvP: ")
                                .append(Component.text(mode.name, modeColor(mode)))
                        )
                    }

                    Command.SINGLE_SUCCESS
                }
            )
            .build()
    }

    private fun modeColor(mode: PvPMode): NamedTextColor =
        when (mode) {
            PvPMode.ON -> NamedTextColor.GREEN
            PvPMode.OFF -> NamedTextColor.RED
        }
}
