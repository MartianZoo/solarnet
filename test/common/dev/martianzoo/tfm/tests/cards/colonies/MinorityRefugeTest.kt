package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MinorityRefugeTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Luna, Io, Triton, Europa, Titan")

  @Test
  internal fun `Cannot place its colony on Io at minimum MC production`() {
    kim.setToExMachina(0, "PROD[MC]")
    kim.exMachina("PROD[-5 MC]")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("$MinorityRefuge"),
            "5 Pay<> FROM MC",
            "$MinorityRefuge FROM ProjectCard",
        )

        doTasks(
            "Colony<Io>",
            "PROD[Heat]",
            "PROD[-2 MC]",
        )
      }
    }
  }

  @Test
  internal fun `A Luna placement can enable its production decrease`() {
    kim.setToExMachina(0, "PROD[MC]")
    kim.exMachina("PROD[-5 MC]")

    kim.inTurn {
          doTasks(
              *playCardTasks("$MinorityRefuge"),
              "5 Pay<> FROM MC",
              "$MinorityRefuge FROM ProjectCard",
              "Colony<Luna>",
              "PROD[2 MC]",
              "PROD[-2 MC]",
          )
        }
        .expect("PROD[0 MC]")
  }
}
