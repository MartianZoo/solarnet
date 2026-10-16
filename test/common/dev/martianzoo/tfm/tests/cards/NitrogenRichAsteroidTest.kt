package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class NitrogenRichAsteroidTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `CORRECT BEHAVIOR - May choose lesser production with three plant tags`() {
    kim.exMachina("$AdaptedLichen, $Lichen, $ArcticAlgae")

    kim.inTurn {
      doTasks("PlayProject<Class<NitrogenRichAsteroid>>", "31 Pay<> FROM MC")
      doTasks(
          "NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("TemperatureStep")
      doTasks("TerraformRating", "PROD[Plant]")
    }
  }
}
