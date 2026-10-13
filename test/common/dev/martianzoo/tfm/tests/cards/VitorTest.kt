package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VitorTest : TfmSandboxTest() {
  @Test
  internal fun `Funds its starting award for free`() {
    newTestGame(addOptions = "Vitor", kimCorporation = Vitor)

    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
          doTask("Landlord")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `In solo play its corporation turn creates no award-funding action`() {
    newTestGameBeforeCorporationPlay(addOptions = "Vitor", playerCount = 1)

    kim.inTurn {
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<Vitor>>",
              "Vitor FROM CorporationCard",
              "45 MC",
              "3 MC",
              "-15 MC",
              "5 ProjectCard<Hand FROM Selecting>",
          )
        }
        .expect("0 RequiredAction, 0 Award")
  }

  @Test
  internal fun `Rebates Search for Life before it has earned any victory points`() {
    newTestGame(addOptions = "Vitor", kimCorporation = Vitor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Landlord")
    }

    kim.inTurn {
          doTasks(
              "PlayProject<Class<SearchForLife>>",
              "-3 MC",
              "SearchForLife FROM ProjectCard",
              "3 MC",
          )
        }
        .expect("0 MC")
  }

  @Test
  internal fun `Does not rebate a card without victory points`() {
    newTestGame(addOptions = "Vitor", kimCorporation = Vitor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Landlord")
    }

    kim.inTurn {
      doTasks("PlayProject<Class<Mine>>", "-4 MC")
      selectTask("-X? Steel")
      doTasks("Ok", "Mine FROM ProjectCard", "PROD[Steel]")
    }
  }

  @Test
  internal fun `Does not rebate a card with negative victory points`() {
    newTestGame(addOptions = "Vitor", kimCorporation = Vitor)
    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectOnlyTask()
      doTask("Landlord")
    }

    kim.inTurn {
      doTasks(
          "PlayProject<Class<BribedCommittee>>",
          "-7 MC",
          "BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("PlayedEvent<Class<BribedCommittee>> FROM BribedCommittee")
    }
  }

  // https://boardgamegeek.com/thread/2993276/article/41548927#41548927
  @Test
  internal fun `Vitor acquired through Merger funds a fourth award`() {
    newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    stan.inTurn {
      doTasks("FundAward<Class<Landlord>>", "-8 MC")
      selectOnlyTask()
    }
    stan.inTurn {
      doTasks("FundAward<Class<Banker>>", "-14 MC")
      selectOnlyTask()
    }
    stan.inTurn {
      doTasks("FundAward<Class<Scientist>>", "-20 MC")
      selectOnlyTask()
    }
    kim.inTurn {
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      selectTask("PreludeCard<Kim, Hand>!")
      selectOnlyTask()
      doTask("-12 MC")
      selectTask("-Director<BoardOfDirectors>!")
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      selectTask("4! CorporationCard<Kim, Selecting>")
      doTask("PlayCard<Class<CorporationCard>, Class<Vitor>>")
      doTask("Vitor<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTasks("45 MC", "-42 MC")
      selectTask("Vitor_RequiredAction!")
      doTask("3 MC")
    }

    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectOnlyTask()
          doTask("Thermalist")
        }
        .expect("0 MC")
    admin.count("Award") shouldBe 4
    stan.setToExMachina(20, "MC")
    shouldThrow<DeadEndException> {
      stan.inTurn {
        selectOnlyTask()
        doTask("FundAward<Class<Miner>>")
      }
    }
  }
}
