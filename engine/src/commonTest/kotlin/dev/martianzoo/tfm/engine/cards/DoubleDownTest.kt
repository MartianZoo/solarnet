package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import kotlin.test.Test

class DoubleDownTest {

  @Test
  fun doubleDown() {
    val game = setUpGame("BRHXP", 2)

    with(game.tfm(PLAYER1)) {
      phase("Corporation")
      playCorp("PharmacyUnion", 5)
      phase("Prelude")
      playPrelude("BiosphereSupport")
      production().values.shouldContainExactly(-1, 0, 0, 2, 0, 0)

      asPlayer(PLAYER2).playPrelude("UnmiContractor")

      playPrelude("DoubleDown") {
        shouldThrow<DependencyException> { doFirstTask("CopyPrelude<MartianIndustries>") }
        shouldThrow<DependencyException> { doFirstTask("CopyPrelude<UnmiContractor>") }
        shouldThrow<NarrowingException> { doFirstTask("CopyPrelude<PharmacyUnion>") }
        shouldThrow<NarrowingException> { doFirstTask("CopyPrelude<DoubleDown>") }

        doFirstTask("CopyPrelude<BiosphereSupport>")
        production().values.shouldContainExactly(-2, 0, 0, 4, 0, 0)
      }
    }
  }
}
