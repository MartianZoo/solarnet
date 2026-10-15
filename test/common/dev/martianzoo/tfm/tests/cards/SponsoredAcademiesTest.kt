package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SponsoredAcademiesTest : TfmSandboxTest() {
  @Test
  internal fun `Point Luna draw supplies the mandatory discard when it is the only hand card`() {
    newTestGame(addOptions = "PointLuna", kimCorporation = PointLuna)
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(9, "MC")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<SponsoredAcademies>>",
              "9 Pay<> FROM MC",
              "SponsoredAcademies FROM ProjectCard",
          )
          // Expand the reward into separate tasks for each other player.
          selectTask("EACH Other@Player(NOT Kim) { ProjectCard<Other@Player, Hand>! }")
          doTask("ProjectCard<Kim, Hand>")
          doTask("-ProjectCard<Kim, Hand>")
          doTask("ProjectCard<Stan, Hand>")
          doTask("ProjectCard<Rob, Hand>")
          doTask("3 ProjectCard")
        }
        .expect("2 ProjectCard<Kim>, ProjectCard<Stan>, ProjectCard<Rob>")
  }

  @Test
  internal fun `Cannot be played with only one card in hand`() {
    newTestGame()
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(9, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<SponsoredAcademies>>")
        doTasks("9 Pay<> FROM MC", "SponsoredAcademies FROM ProjectCard")
        // Expand the reward into separate tasks for each other player.
        selectTask("EACH Other@Player(NOT Kim) { ProjectCard<Other@Player, Hand>! }")
        doTask("ProjectCard<Stan, Hand>")
        doTask("ProjectCard<Rob, Hand>")
        doTask("-ProjectCard<Kim, Hand>")
      }
    }

    kim.assertCounts(9 to "MC", 1 to "ProjectCard", 0 to "$SponsoredAcademies")
  }
}
