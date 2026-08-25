package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class NoctisCityTest : CardTest() {
  @Test
  internal fun `Can be placed anywhere on Hellas`() {
    newGame(Hellas)
    p1.manual("PROD[Energy]")
    p1.manual("$NoctisCity") { placeTile(1, 3) }.expect("PROD[3 Megacredit, -Energy]")
  }

  @Test
  internal fun `Must be placed on Noctis on Tharsis`() {
    newGame()
    p1.manual("PROD[Energy]")

    // Without this, the sole NoctisArea is selected before the operation body can try a bad space.
    p1.autoExecMode = NONE
    p1.manual("$NoctisCity") {
      shouldThrow<TaskException> { doTask("CityTile<Tharsis_1_3>") }
      placeTile(5, 3)
      doTask("PROD[-Energy]")
      doTask("PROD[3 Megacredit]")
      doTask("2 Plant")
    }
  }
}
