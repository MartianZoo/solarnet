package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RequiredFirstActionTest : TfmGameplayTest() {
  @Test
  internal fun `Can pass the first generation and draw three cards in the second`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    passFirstGeneration()

    kim.doTask("DoRequiredActions").expect("3 ProjectCard")
  }

  @Test
  internal fun `Can play a project after resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    passFirstGeneration()
    kim.doTask("DoRequiredActions")

    kim.doTasks(*playCardTasks("$Mine"), "Ok" /* No Steel payment. */).expect("PROD[Steel]")
  }

  @Test
  internal fun `Cannot play a project before resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)

    shouldThrow<NarrowingException> {
      kim.doTasks(*playCardTasks("$Mine"), "Ok" /* No Steel payment. */)
    }
    kim.count("$Mine") shouldBe 0
    kim.count("RequiredAction") shouldBe 1
  }

  @Test
  internal fun `Cannot buy a standard project before resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    passFirstGeneration()

    shouldThrow<NarrowingException> {
      kim.completeOperation {
        doTasks(*standardProjectTasks("AsteroidProject"))
        autoExecNow(EAGER)
      }
    }
    admin.count("TemperatureStep") shouldBe 0
    kim.count("RequiredAction") shouldBe 1
  }

  @Test
  internal fun `Cannot convert available heat before resolving its mandatory card draw`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 2, kimCorporation = Inventrix)
    kim.completeOperation {
      doTask("PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>")
      doTasks(
          "PlayCard<Class<ProjectCard>, Class<$Soletta>>",
      )
      autoExecNow(EAGER)
    }
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$Biolab>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$SupplyDrop>>")
    stan.autoExecNow(EAGER)

    passFirstGeneration()
    kim.count("Heat") shouldBe 8

    shouldThrow<NarrowingException> {
      kim.doTasks("UseAction<ConvertHeatAction, Action1>", "8 Pay<Class<Heat>> FROM Heat")
    }
    kim.count("Heat") shouldBe 8
    kim.count("RequiredAction") shouldBe 1
  }

  private fun passFirstGeneration() {
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("OxygenStep! BY Admin")

    kim.doTask("-4 ProjectCard<Selecting>")
    stan.doTask("-4 ProjectCard<Selecting>")
    stan.doTask("Pass")
  }
}
