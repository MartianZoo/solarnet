package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class NitrogenRichAsteroidTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can choose the lesser production despite three plant tags`() {
    kim.exMachina("$AdaptedLichen, $Lichen, $ArcticAlgae")

    kim.inTurn {
      doTasks("PlayProject<Class<NitrogenRichAsteroid>>", "-31 MC")
      doTasks(
          "Ok" /* No Titanium payment. */,
          "NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("TemperatureStep")
      doTasks("TerraformRating", "PROD[Plant]")
      doTask("PlayedEvent<Class<NitrogenRichAsteroid>> FROM NitrogenRichAsteroid")
    }
  }
}
