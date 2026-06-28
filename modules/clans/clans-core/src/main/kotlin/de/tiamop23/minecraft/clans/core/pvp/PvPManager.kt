package de.tiamop23.minecraft.clans.core.pvp

import java.util.UUID

interface PvPManager {
    fun getMode(playerId: UUID): PvPMode

    fun setMode(playerId: UUID, mode: PvPMode)

    fun isPvPAllowed(attackerId: UUID, victimId: UUID): Boolean
}
