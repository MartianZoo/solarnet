package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PoliticalAllianceTest : TfmSandboxTest() {
  @Test
  internal fun `Cannot be played without Turmoil`() {
    newTestGame()

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTasks(*playCardTasks("PoliticalAlliance"))
      }
    }
  }

  @Test
  internal fun `Cannot be played without party leadership`() {
    newTestGame(addOptions = "TurmoilExpansion")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(*playCardTasks("PoliticalAlliance"))
      }
    }
  }

  @Test
  internal fun `Cannot substitute the chairmanship for a second party leader`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("-Chairman<Neutral>, Chairman, PartyLeader<Scientists>")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(*playCardTasks("PoliticalAlliance"))
      }
    }
  }

  @Test
  internal fun `Can be played with two party leaders and no chairmanship`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Scientists>, PartyLeader<Unity>")

    kim.inTurn {
      doTasks(
          *playCardTasks("PoliticalAlliance"),
          "4 Pay<> FROM MC",
          "PoliticalAlliance FROM ProjectCard",
          "TerraformRating",
      )
    }
  }
}
