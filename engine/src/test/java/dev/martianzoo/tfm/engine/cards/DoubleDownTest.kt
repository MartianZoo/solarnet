package dev.martianzoo.tfm.engine.cards

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.Exceptions.DependencyException
import dev.martianzoo.tfm.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player.Companion.ENGINE
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.data.Player.Companion.PLAYER2
import dev.martianzoo.tfm.engine.Engine
import dev.martianzoo.tfm.execapi.PlayerSession.Companion.session
import dev.martianzoo.tfm.execapi.TerraformingMars.phase
import dev.martianzoo.tfm.execapi.TerraformingMars.playCorp
import dev.martianzoo.tfm.execapi.TerraformingMars.production
import dev.martianzoo.tfm.execapi.TerraformingMars.turn
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DoubleDownTest {

  @Test
  fun doubleDown() {
    val game = Engine.newGame(GameSetup(Canon, "BRHXP", 2))
    val eng = game.session(ENGINE)
    val p1 = game.session(PLAYER1)
    val p2 = game.session(PLAYER2)

    p1.playCorp("InterplanetaryCinematics", 7)
    p2.playCorp("PharmacyUnion", 5)

    eng.phase("Prelude")

    p1.turn("UnmiContractor")
    p1.turn("CorporateArchives")

    with(p2) {
      turn("BiosphereSupport")
      assertThat(production().values).containsExactly(-1, 0, 0, 2, 0, 0).inOrder()

      turn("DoubleDown") {
        assertThrows<DependencyException>("exist") { task("CopyPrelude<MartianIndustries>") }
        assertThrows<DependencyException>("mine") { task("CopyPrelude<UnmiContractor>") }
        assertThrows<NarrowingException>("prelude") { task("CopyPrelude<PharmacyUnion>") }
        assertThrows<NarrowingException>("other") { task("CopyPrelude<DoubleDown>") }

        task("CopyPrelude<BiosphereSupport>")
        assertThat(production().values).containsExactly(-2, 0, 0, 4, 0, 0).inOrder()
      }
    }
  }
}
