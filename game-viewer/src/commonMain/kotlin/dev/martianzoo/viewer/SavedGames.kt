package dev.martianzoo.viewer

import dev.martianzoo.viewer.games.OtbGame20260809
import dev.martianzoo.viewer.games.OtbGame20260818
import dev.martianzoo.viewer.games.OtbGame20260825
import dev.martianzoo.viewer.games.OtbGame20260828

public object SavedGames {
  public val all: List<SavedGame> =
      listOf(
          SavedGame("August 9, 2026", ::OtbGame20260809),
          SavedGame("August 18, 2026", ::OtbGame20260818),
          SavedGame("August 25, 2026", ::OtbGame20260825),
          SavedGame("August 28, 2026 (partial)", ::OtbGame20260828),
      )
}
