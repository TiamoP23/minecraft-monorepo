package de.tiamop23.minecraft.clans.core.pvp

enum class PvPMode {
    OFF,
    ON;

    fun toggle(): PvPMode =
        when (this) {
            OFF -> ON
            ON -> OFF
        }
}
