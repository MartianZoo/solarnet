package dev.martianzoo.tfm.web.gameviewer

internal data class SavedGame(
    public val name: String,
) {
  public val resourcePath: String = "games/$name.json"
}
