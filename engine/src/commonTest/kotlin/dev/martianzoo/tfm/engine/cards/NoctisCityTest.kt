package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.engine.AutoExecMode.NONE
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class NoctisCityTest : CardTest() {
  @Test
  fun `on Hellas, plays Noctis City`() {
    newGame("HellasMapOption FROM TharsisMapOption")
    p1.manual("PROD[Energy]")
    p1.manual("NoctisCity") {
          doTask("CityTile<Hellas_1_3>")
        }
        .expect("PROD[3 Megacredit, -Energy]")
  }

  @Test
  fun `on Tharsis, tries to place Noctis City off Noctis`() {
    newGame()
    p1.manual("PROD[Energy]")

    // Without this, the sole NoctisArea is selected before the operation body can try a bad space.
    p1.autoExecMode = NONE
    p1.manual("NoctisCity") {
      shouldThrow<TaskException> { doTask("CityTile<Tharsis_1_3>") }
      doTask("CityTile<Tharsis_5_3>")
      doTask("PROD[-Energy]")
      doTask("PROD[3 Megacredit]")
      doTask("2 Plant")
    }
  }
}
