package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RequiredFirstActionTest : TfmGameplayTest(NONE) {
  @Test
  internal fun `Can pass the first generation and draw three cards in the second`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    kim.doTask("OxygenStep! BY Admin")
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTask("Pass")

    kim.completeOperation { doTasks("DoRequiredActions", "3 ProjectCard") }
  }

  @Test
  internal fun `Can play a project after resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    kim.doTask("OxygenStep! BY Admin")
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTask("Pass")
    kim.doTasks("DoRequiredActions", "3 ProjectCard")

    kim.completeOperation {
      doTasks(
          *playCardTasks("$Mine"),
          "Ok" /* No Steel payment. */,
          "4 Pay<> FROM MC",
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
      )
    }
  }

  @Test
  internal fun `Cannot play a project before resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)

    shouldThrow<TaskException> { kim.doTask("UseAction<PlayCardAction, Action1>") }
    kim.count("$Mine") shouldBe 0
    kim.count("RequiredAction") shouldBe 1
    kim.completeOperation { doTasks("DoRequiredActions", "3 ProjectCard") }
  }

  @Test
  internal fun `Cannot buy a standard project before resolving its mandatory card draw`() {
    newTestGame(playerCount = 2, kimCorporation = Inventrix)
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "Heat")
    kim.doTask("OxygenStep! BY Admin")
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTask("Pass")

    shouldThrow<TaskException> { kim.doTask("UseAction<UseStandardProjectAction, Action1>") }
    admin.count("TemperatureStep") shouldBe 0
    kim.count("RequiredAction") shouldBe 1
    kim.completeOperation { doTasks("DoRequiredActions", "3 ProjectCard") }
  }

  @Test
  internal fun `Cannot convert available heat before resolving its mandatory card draw`() {
    newTestGame(addOptions = "PreludeExpansion", playerCount = 2, kimCorporation = Inventrix)
    kim.completeOperation {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>",
          "$ExcentricSponsor FROM PreludeCard",
          "PlayCard<Class<ProjectCard>, Class<$Soletta>>",
      )
      // Hand the discount to Admin before the remaining payment is offered.
      selectTask("-25* Owed")
      doTasks("10 Pay<> FROM MC", "$Soletta FROM ProjectCard", "PROD[7 Heat]")
    }
    kim.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Donation>>",
        "$Donation FROM PreludeCard",
        "21 MC",
    )
    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$Biolab>>",
        "$Biolab FROM PreludeCard",
        "PROD[Plant]",
        "3 ProjectCard",
    )
    stan.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$SupplyDrop>>",
        "$SupplyDrop FROM PreludeCard",
        "3 Titanium",
        "8 Steel",
        "3 Plant",
    )

    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTasks("21 MC", "Steel", "Titanium", "Plant", "Energy", "8 Heat")
    stan.doTasks("21 MC", "Steel", "Titanium", "2 Plant", "Energy", "Heat")
    kim.doTask("OxygenStep! BY Admin")
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    stan.doTask("Pass")
    kim.count("Heat") shouldBe 8

    shouldThrow<TaskException> { kim.doTask("UseAction<ConvertHeatAction, Action1>") }
    kim.count("Heat") shouldBe 8
    kim.count("RequiredAction") shouldBe 1
    kim.completeOperation { doTasks("DoRequiredActions", "3 ProjectCard") }
  }
}
