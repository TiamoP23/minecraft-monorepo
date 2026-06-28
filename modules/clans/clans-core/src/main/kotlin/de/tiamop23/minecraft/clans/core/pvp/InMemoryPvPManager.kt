package de.tiamop23.minecraft.clans.core.pvp

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryPvPManager : PvPManager {
    private val modes = ConcurrentHashMap<UUID, PvPMode>()

    override fun getMode(playerId: UUID): PvPMode = modes.getOrDefault(playerId, PvPMode.OFF)

    override fun setMode(playerId: UUID, mode: PvPMode) {
        modes[playerId] = mode
    }

    override fun isPvPAllowed(attackerId: UUID, victimId: UUID): Boolean {
        val attackerMode = getMode(attackerId)
        val victimMode = getMode(victimId)

        return attackerMode == PvPMode.ON && victimMode == PvPMode.ON
    }
}
