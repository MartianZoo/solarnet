package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class TurnCompletionTest : TfmGameplayTest() {
  @Test
  internal fun `After two actions a player must let their opponent act`() {
    newTestGame("$ResearchCoordination", playerCount = 2)
    kim.completeOperation {
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "4 Pay<> FROM MC",
      )
    }
    kim.completeOperation {
      doTasks(
          *playCardTasks("$Cartel"),
          "8 Pay<> FROM MC",
          "PROD[2 MC]",
      )
    }
    shouldThrow<TaskException> { kim.doTasks(*standardProjectTasks("PowerPlantProject")) }

    stan.doTask("Pass")
    kim.completeOperation {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
  }

  @Test
  internal fun `An opponent finishes paying Philares before taking another action`() {
    newTestGame(playerCount = 2, kimCorporation = Philares)
    kim.completeOperation {
      doTasks(
          "DoRequiredActions",
          "GreeneryTile<Tharsis_4_2>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
    }
    kim.doTask("Ok")

    stan.doTasks(
        *standardProjectTasks("GreeneryProject"),
        "23 Pay<> FROM MC",
        "GreeneryTile<Tharsis_3_2>",
        "OxygenStep",
        "TerraformRating",
    )
    shouldThrow<TaskException> { stan.doTasks(*standardProjectTasks("PowerPlantProject")) }

    stan.giveTask(kim, "Titanium")
    stan.completeOperation {
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
  }
}
