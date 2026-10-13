package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Ignore
import kotlin.test.Test

internal class SagittaFrontierServicesTest : TfmSandboxTest() {
  @Test
  internal fun `Its own tagless corporation play earns the rebate`() {
    newTestGame(
        "-promos:cards",
        kimCorporation = SagittaFrontierServices,
        startAtCorporation = true,
    )

    // 31 starting MC plus the 4 MC rebate, minus the ten retained projects.
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<SagittaFrontierServices>>",
          "SagittaFrontierServices FROM CorporationCard",
          "31 MC",
          "PROD[Energy]",
          "PROD[2 MC]",
      )
      selectTask("SearchForCard<Kim, NoTagsFilter>!")
      doTasks("4 MC", "-30 MC", "10 ProjectCard<Hand FROM Selecting>")
    }
  }

  @Test
  internal fun `Pays four MC for a tagless project`() {
    newTestGame("-promos:cards", kimCorporation = SagittaFrontierServices)
    kim.setToExMachina(15, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<AtmoCollectors>>",
          "-15 MC",
          "AtmoCollectors FROM ProjectCard",
          "4 MC",
      )
      selectTask("2 Floater.")
      doTask("2 Floater<AtmoCollectors>.")
    }
  }

  // https://boardgamegeek.com/thread/3154781/do-event-tags-count-for-sagitta
  @Test
  internal fun `An event with no other tags earns the single-tag rebate`() {
    newTestGame("-promos:cards", kimCorporation = SagittaFrontierServices)

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Sabotage>>",
              "-1 MC",
              "Sabotage FROM ProjectCard",
              "MC",
              "-7 MC<Stan>",
          )
          doTask("PlayedEvent<Class<Sabotage>> FROM Sabotage")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `An event with another printed tag earns no rebate`() {
    newTestGame("-promos:cards, SmallAsteroid", kimCorporation = SagittaFrontierServices)
    kim.setToExMachina(10, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<SmallAsteroid>>", "-10 MC")
      selectTask("-X Titanium?")
      doTasks(
          "Ok",
          "SmallAsteroid FROM ProjectCard",
      )
      selectTask("TemperatureStep.")
      selectTask("-2 Plant<Anyone>?")
      doTask("TerraformRating")
      doTask("PlayedEvent<Class<SmallAsteroid>> FROM SmallAsteroid")
    }
  }

  @Test
  internal fun `A single building tag earns the single-tag rebate`() {
    newTestGame("-promos:cards", kimCorporation = SagittaFrontierServices)

    kim.inTurn {
      doTasks("PlayProject<Class<Mine>>", "-4 MC")
      selectTask("-X Steel?")
      doTasks("Ok", "Mine FROM ProjectCard", "PROD[Steel]", "MC")
    }
  }

  @Test
  internal fun `Repeated tags still count separately for the rebate threshold`() {
    newTestGame("-promos:cards", kimCorporation = SagittaFrontierServices)
    kim.setToExMachina(11, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<Research>>", "-11 MC", "Research FROM ProjectCard")
      selectTask("2 ProjectCard<Kim, Hand>!")
    }
  }

  @Test
  internal fun `Ignores another players card even when it has a single tag`() {
    newTestGame("-promos:cards", kimCorporation = SagittaFrontierServices)

    stan
        .inTurn {
          doTasks("PlayProject<Class<Mine>>", "-4 MC")
          selectTask("-X Steel?")
          doTasks("Ok", "Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("0 MC<Kim>")
  }

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Ignore // Sagitta misses its enclosing tagless Merger.
  @Test
  internal fun `Pays for Merger when acquired during Prelude`() {
    acquireDuringPrelude().expect("-3 MC")
  }

  @Test
  internal fun `BUG - Misses Merger when acquired during Prelude`() {
    acquireDuringPrelude().expect("-7 MC")
  }

  private fun acquireDuringPrelude(): TaskResult {
    newTestGame("PreludeExpansion, SagittaFrontierServices, Unsafe")
    kim.setToExMachina(54, "MC")
    return kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      selectTask("4 CorporationCard<Kim, Selecting>!")
      doTask("-42 MC")
      selectTask("-3 CorporationCard<Kim, Selecting>!")
      doTask("PlayCard<Class<CorporationCard>, Class<SagittaFrontierServices>>")
      doTask("SagittaFrontierServices<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTasks("31 MC", "PROD[Energy]", "PROD[2 MC]")
      selectTask("SearchForCard<Kim, NoTagsFilter>!")
      doTask("4 MC")
    }
  }
}
