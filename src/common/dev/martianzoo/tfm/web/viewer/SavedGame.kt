package dev.martianzoo.tfm.web.viewer

internal data class SavedGame(
    public val name: String,
) {
  public val resourcePath: String = "games/$name.json"
}
