package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class OldMiningColonyTest : TfmSandboxTest() {
  @Test
  internal fun `Old Mining Colony needs Pluto's cards when the hand is empty`() {
    newTestGame(addOptions = "PreludeExpansion, $OldMiningColony, Pluto, Io, Luna, Ceres, Triton")
    kim.setToExMachina(0, "ProjectCard")
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$OldMiningColony>>",
            "$OldMiningColony FROM PreludeCard",
            "PROD[Titanium]",
        )

        doTasks(
            "Colony<Io>",
            "PROD[Heat]",
            "-ProjectCard<Hand>",
        )
      }
    }
    kim.count("$OldMiningColony") shouldBe 0
    kim.count("Colony") shouldBe 0
    kim.count("PreludeCard") shouldBe 2

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$OldMiningColony>>",
              "$OldMiningColony FROM PreludeCard",
              "PROD[Titanium]",
              "Colony<Pluto>",
              "2 ProjectCard<Hand>",
              "-ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard")
  }
}
