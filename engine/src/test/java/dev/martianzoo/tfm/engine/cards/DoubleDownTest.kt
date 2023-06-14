package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DoubleDownTest {

  @Test
  fun doubleDown() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))

    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("PharmacyUnion", 5)
      phase("Prelude")
      playPrelude("BiosphereSupport")
      assertThat(production().values).containsExactly(-1, 0, 0, 2, 0, 0).inOrder()

      asPlayer(PLAYER2).playPrelude("UnmiContractor")

      playPrelude("DoubleDown") {
        assertThrows<DependencyException>("exist") { doFirstTask("CopyPrelude<MartianIndustries>") }
        assertThrows<DependencyException>("mine") { doFirstTask("CopyPrelude<UnmiContractor>") }
        assertThrows<NarrowingException>("prelude") { doFirstTask("CopyPrelude<PharmacyUnion>") }
        assertThrows<NarrowingException>("other") { doFirstTask("CopyPrelude<DoubleDown>") }

        doFirstTask("CopyPrelude<BiosphereSupport>")
        assertThat(production().values).containsExactly(-2, 0, 0, 4, 0, 0).inOrder()
      }
    }
  }
}
