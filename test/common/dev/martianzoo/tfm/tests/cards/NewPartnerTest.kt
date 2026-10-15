package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.Donation
import dev.martianzoo.tfm.tests.cards.cardnames.IndustrialComplex
import dev.martianzoo.tfm.tests.cards.cardnames.NewPartner
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class NewPartnerTest : TfmGameplayTest(NONE) {

  @Test
  internal fun `New Partner plays the offered Prelude and leaves the other hand card alone`() {
    newTestGame(addOptions = "PreludeExpansion")

    shouldThrow<TaskException> {
      kim.completeOperation {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
            "NewPartner FROM PreludeCard",
            "PROD[MC]",
            "2 PreludeCard<Selecting>",
        )
        kim.assertCounts(1 to "PreludeCard<Hand>")
        doTask("PlayCard<Class<PreludeCard>, Class<$Donation>, Hand>")
      }
    }

    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
          "NewPartner FROM PreludeCard",
          "PROD[MC]",
      )
      doTask("2 PreludeCard<Selecting>")
      doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      doTask("Donation<Kim> FROM PreludeCard<Kim, Selecting>!")
      doTask("21 MC")
    }
    kim.assertCounts(1 to "PreludeCard<Hand>", 0 to "PreludeCard<Selecting>", 1 to "$Donation")

    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<PowerGeneration>>",
          "PowerGeneration FROM PreludeCard",
          "PROD[3 Energy]",
      )
    }
    kim.assertCounts(0 to "PreludeCard<Hand>", 0 to "PreludeCard<Selecting>")
  }

  internal class Sandbox : TfmSandboxTest() {
    @Test
    internal fun `New Partner can fizzle an unaffordable Industrial Complex`() {
      newTestGame(addOptions = "PreludeExpansion, prelude2:cards")
      kim.setToExMachina(13, "MC")
      shouldThrow<LimitsException> {
        kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<NewPartner>>", "NewPartner FROM PreludeCard")
          doTask("PROD[MC]")
          doTask("2 PreludeCard<Selecting>")
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
              "IndustrialComplex FROM PreludeCard<Selecting>",
              "-18 MC",
          )
        }
      }

      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
            "NewPartner FROM PreludeCard",
            "PROD[MC]",
        )
        doTask("2 PreludeCard<Selecting>")
        doTask("-PreludeCard<Selecting>")
        doTask("Audit")
        doTask("15 MC")
      }
      kim.assertCounts(0 to "PreludeCard<Selecting>")
      kim.assertCounts(1 to "$NewPartner", 0 to "$IndustrialComplex", 1 to "PreludeCard")
    }
  }
}
