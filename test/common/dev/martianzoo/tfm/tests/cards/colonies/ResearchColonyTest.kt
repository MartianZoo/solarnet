package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchColony
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ResearchColonyTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Can be played when its owner already has a colony on Luna`() {
    kim.exMachina("Colony<Luna>")

    kim.inTurn {
      doTasks("PlayProject<Class<ResearchColony>>", "-20 MC")
      doTasks("ResearchColony FROM ProjectCard")
      doTask("2 ProjectCard<Kim, Hand>")

      doTasks("Colony<Luna>", "PROD[2 MC]")
    }
  }

  @Test
  internal fun `Cannot be played on a colony tile that already has three colonies`() {
    kim.exMachina("Colony<Kim, Luna>, Colony<Stan, Luna>, Colony<Rob, Luna>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            "PlayProject<Class<$ResearchColony>>",
            "-20 MC",
            "$ResearchColony FROM ProjectCard",
            "Colony<Luna>",
        )
      }
    }
  }
}
