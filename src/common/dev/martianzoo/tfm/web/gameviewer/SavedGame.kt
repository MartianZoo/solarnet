package dev.martianzoo.tfm.web.gameviewer

public data class SavedGame(
    public val name: String,
    public val create: () -> RecordedGame,
)
