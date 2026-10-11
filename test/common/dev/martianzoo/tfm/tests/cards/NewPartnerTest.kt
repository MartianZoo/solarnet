package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.Donation
import dev.martianzoo.tfm.tests.cards.cardnames.IndustrialComplex
import dev.martianzoo.tfm.tests.cards.cardnames.NewPartner
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class NewPartnerTest : TfmSandboxTest() {

  @Test
  internal fun `New Partner plays the offered Prelude and leaves the other hand card alone`() {
    newTestGame(addOptions = "PreludeExpansion")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
            "NewPartner FROM PreludeCard",
            "PROD[MC]",
            "2 PreludeCard<Selecting>",
            "-PreludeCard<Selecting>",
        )
        selectOnlyTask()
        kim.assertCounts(1 to "PreludeCard<Hand>")
        doTask("PlayCard<Class<PreludeCard>, Class<$Donation>, Hand>")
      }
    }

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
          "NewPartner FROM PreludeCard",
          "PROD[MC]",
      )
      selectOnlyTask()
      selectOnlyTask()
      doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      selectTask("MAX 0 Barrier: Donation FROM PreludeCard<Kim, Selecting>!")
      doTask("21 MC")
    }
    kim.assertCounts(1 to "PreludeCard<Hand>", 0 to "PreludeCard<Selecting>", 1 to "$Donation")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<PowerGeneration>>",
          "PowerGeneration FROM PreludeCard",
          "PROD[3 Energy]",
      )
    }
    kim.assertCounts(0 to "PreludeCard<Hand>", 0 to "PreludeCard<Selecting>")
  }

  @Test
  internal fun `New Partner can fizzle an unaffordable Industrial Complex`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack")
    kim.setToExMachina(13, "MC")
    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<NewPartner>>", "NewPartner FROM PreludeCard")
        selectTask("PROD[MC]")
        selectOnlyTask()
        selectOnlyTask()
        selectOnlyTask()
        doTask("PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>")
        selectOnlyTask()
        selectTask("-18 MC!")
      }
    }

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
          "NewPartner FROM PreludeCard",
          "PROD[MC]",
      )
      selectOnlyTask()
      selectOnlyTask()
      doTask("-PreludeCard<Selecting>")
      selectTask("Audit!")
      doTask("15 MC")
    }
    kim.assertCounts(0 to "PreludeCard<Selecting>")
    kim.assertCounts(1 to "$NewPartner", 0 to "$IndustrialComplex", 1 to "PreludeCard")
  }
}
