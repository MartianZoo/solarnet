package dev.martianzoo.viewer

public data class SavedGame(
    public val name: String,
    public val create: () -> RecordedGame,
)
