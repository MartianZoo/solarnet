package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test
import io.kotest.assertions.throwables.shouldThrow

class PhilaresTest {
  @Test
  fun test() {
    val game = Engine.newGame(GameSetup(Canon, "BMX", 2))
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    p1.phase("Corporation")
    p1.playCorp("TharsisRepublic", 0)
    p2.playCorp("Philares", 0)

    p1.assertCounts(40 to "Megacredit", 1 to "Mandate", 0 to "CityTile")
    p2.assertCounts(47 to "Megacredit", 1 to "Mandate", 0 to "GreeneryTile")

    p1.phase("Action")
    p1.stdAction("HandleMandates") { doTask("CityTile<M42>") }

    p2.stdAction("HandleMandates") {
      doTask("GreeneryTile<M32>")
      doTask("Steel")
    }

    p2.assertCounts(47 to "Megacredit", 1 to "Steel", 0 to "Mandate", 1 to "GreeneryTile")

    shouldThrow<IllegalArgumentException> {
      p1.stdProject("GreenerySP") { doTask("GreeneryTile<M43>") }
    }

    p1.stdProject("GreenerySP") {
      doTask("GreeneryTile<M43>")
      p2.doTask("Titanium")
    }
    p2.assertCounts(1 to "Steel", 1 to "Titanium")
  }

  @Test
  fun doesNotTriggerBetweenOwnTiles() {
    val game = Engine.newGame(GameSetup(Canon, "BMX", 2))
    val p1 = game.tfm(PLAYER1)

    p1.phase("Corporation")
    p1.playCorp("Philares", 0)

    p1.phase("Action")
    p1.stdAction("HandleMandates") { doTask("GreeneryTile<M42>") }

    p1.stdProject("GreenerySP") { doTask("GreeneryTile<M32>") }
    shouldThrow<TaskException> { p1.doTask("Megacredit") }
  }
}
