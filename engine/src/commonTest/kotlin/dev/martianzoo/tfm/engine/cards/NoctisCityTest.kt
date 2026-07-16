package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class NoctisCityTest : CardTest() {
  @Test
  fun `uses an ordinary land area on a map without Noctis`() {
    val game = newGame(GameSetup(Canon, "BRH", 2))
    with(game.tfm(PLAYER1)) {
      godMode().manual("PROD[Energy]")

      godMode()
          .manual("NoctisCity") {
            doTask("CityTile<Hellas_1_3>")
          }
          .expect("PROD[3 Megacredit, -Energy]")
    }
  }

  @Test
  fun `the same coordinate is not eligible on Tharsis`() {
    val game = newGame(GameSetup(Canon, "BMT", 2))
    val p1 = game.tfm(PLAYER1)
    p1.godMode().manual("PROD[Energy]")

    // Without this, the sole NoctisArea is selected before the operation body can try a bad space.
    val manual = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    manual.manual("NoctisCity") {
      shouldThrow<TaskException> { doTask("CityTile<Tharsis_1_3>") }
      doTask("CityTile<Tharsis_5_3>")
      doTask("PROD[-Energy]")
      doTask("PROD[3 Megacredit]")
      doTask("2 Plant")
    }
  }
}
