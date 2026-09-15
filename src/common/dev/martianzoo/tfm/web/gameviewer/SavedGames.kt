package dev.martianzoo.tfm.web.gameviewer

/** Discovers the recording filenames packaged by the most recent resource build. */
public object SavedGames {
  public fun fromIndex(text: String): List<SavedGame> =
      text
          .lineSequence()
          .map(String::trim)
          .filter(String::isNotEmpty)
          .onEach { name ->
            require(name.endsWith("Test") && name.none { it == '/' || it == '\\' }) {
              "invalid replay-test filename: $name"
            }
          }
          .distinct()
          .sorted()
          .map(::SavedGame)
          .toList()
}
