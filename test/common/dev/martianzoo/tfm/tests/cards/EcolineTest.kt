package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EcolineTest : TfmSandboxTest() {

  @Test
  internal fun `plant conversion preserves Ecolines discount and leaves tile choice to the player`() {
    newTestGame(kimCorporation = Ecoline)
    kim.exMachina("4 Plant")

    kim.inTurn {
      doTask("UseAction<ConvertPlantsAction, Action1>")
      shouldThrow<TaskException> { doTask("PlaceGreeneryTile") }
      doTasks(
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Tharsis_1_3>",
          "OxygenStep",
          "TerraformRating",
      )

      kim.count("Plant") shouldBe 0
    }
    game.isIdle() shouldBe true
  }
}
