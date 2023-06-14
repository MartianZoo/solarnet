package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test

class ManutechTest {

  @Test
  fun manutech() {
    val game = Engine.newGame(GameSetup(Canon, "BMV", 2))
    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("Manutech", 5)
      assertCounts(1 to "PROD[Steel]", 1 to "Steel")

      godMode().manual("PROD[8, 6T, 7P, 5E, 3H]")
      assertThat(production().values).containsExactly(8, 1, 6, 7, 5, 3).inOrder()
      assertCounts(28 to "M", 1 to "S", 6 to "T", 7 to "P", 5 to "E", 3 to "H")

      godMode().manual("-7 Plant")
      assertCounts(0 to "Plant")

      godMode().manual("Moss")
      assertThat(production().values).containsExactly(8, 1, 6, 8, 5, 3).inOrder()
      assertCounts(28 to "M", 1 to "S", 6 to "T", 0 to "P", 5 to "E", 3 to "H")
    }
  }
}
