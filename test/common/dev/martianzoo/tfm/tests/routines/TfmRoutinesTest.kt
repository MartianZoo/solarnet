package dev.martianzoo.tfm.tests.routines

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.tfm.engine.TfmRoutines
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TfmRoutinesTest : TfmSandboxTest() {
  @Test
  internal fun explicitPaymentLeavesOrdinaryCardEffectsForTheCaller() {
    newTestGame()
    kim.exMachina("2 Steel")
    kim.setToExMachina(10, "MC")
    val routines = TfmRoutines(agents)
    kim.beginOperation("OfferAction")
    routines.execute(kim, "PlayCard", "$Mine")
    kim.count("$Mine") shouldBe 0
    val productionBefore = kim.count("PROD[Steel]")

    routines.execute(kim, "Pay", "1 Steel", "2 MC")

    kim.count("$Mine") shouldBe 1
    kim.count("Steel") shouldBe 1
    kim.count("MC") shouldBe 8
    kim.count("Owed") shouldBe 0
    kim.count("PROD[Steel]") shouldBe productionBefore
    kim.autoExecPolicy shouldBe NONE
    kim.doTask("PROD[Steel]")
    game.isIdle() shouldBe true
  }

  @Test
  internal fun explicitAllocationRunsBeforeConfiguredAutomaticWork() {
    newTestGame()
    kim.exMachina("2 Steel")
    kim.setToExMachina(10, "MC")
    kim.autoExecPolicy = EAGER
    val routines = TfmRoutines(agents)
    kim.beginOperation("OfferAction")
    routines.execute(kim, "PlayCard", "$Mine")
    val productionBefore = kim.count("PROD[Steel]")
    val eventsBefore = game.events.nextOrdinal

    shouldThrowAny { routines.execute(kim, "Pay", "1 Steel", "1 MC") }

    game.events.nextOrdinal shouldBe eventsBefore
    kim.count("Steel") shouldBe 2
    kim.count("MC") shouldBe 10
    kim.count("Owed") shouldBe 4
    kim.autoExecPolicy shouldBe EAGER

    routines.execute(kim, "Pay", "1 Steel", "2 MC")

    kim.count("Steel") shouldBe 1
    kim.count("MC") shouldBe 8
    kim.count("$Mine") shouldBe 1
    kim.count("PROD[Steel]") shouldBe productionBefore + 1
    kim.autoExecPolicy shouldBe EAGER
    game.isIdle() shouldBe true
  }

  @Test
  internal fun failedPaymentRestoresItsAllocationAndRetainsTheEarlierPlayRequest() {
    newTestGame()
    kim.exMachina("2 Steel")
    kim.setToExMachina(10, "MC")
    val routines = TfmRoutines(agents)
    kim.beginOperation("OfferAction")
    routines.execute(kim, "PlayCard", "$Mine")
    val eventsBefore = game.events.nextOrdinal
    val tasksBefore = game.tasks.ids()

    shouldThrowAny { routines.execute(kim, "Pay", "1 Steel", "1 MC") }

    game.events.nextOrdinal shouldBe eventsBefore
    game.tasks.ids() shouldBe tasksBefore
    kim.count("Steel") shouldBe 2
    kim.count("MC") shouldBe 10
    kim.count("Owed") shouldBe 4
    kim.count("$Mine") shouldBe 0
    routines.execute(kim, "Pay", "2 Steel")
    kim.count("$Mine") shouldBe 1
    kim.doTask("PROD[Steel]")
  }

  @Test
  internal fun payingCashKeepsUnusedAlternativeSources() {
    newTestGame(addOptions = "$Psychrophiles")
    kim.exMachina("$Psychrophiles, 2 Microbe<$Psychrophiles>, 2 Steel")
    kim.setToExMachina(23, "MC")
    val routines = TfmRoutines(agents)
    kim.beginOperation("OfferAction")
    routines.execute(kim, "PlayCard", "$ProtectedValley")

    routines.execute(kim, "Pay", "23 MC")

    kim.count("$ProtectedValley") shouldBe 1
    kim.count("MC") shouldBe 0
    kim.count("Steel") shouldBe 2
    kim.count("Microbe<$Psychrophiles>") shouldBe 2
    kim.count("Owed") shouldBe 0
  }

  @Test
  internal fun cardHeldResourcesCanPayTheWholeBill() {
    newTestGame(addOptions = "$Psychrophiles")
    kim.exMachina("$Psychrophiles, 5 Microbe<$Psychrophiles>")
    val routines = TfmRoutines(agents)
    kim.beginOperation("OfferAction")
    routines.execute(kim, "PlayCard", "$AdaptedLichen")
    val cashBefore = kim.count("MC")

    routines.execute(kim, "Pay", "5 Microbe<$Psychrophiles>")

    kim.count("$AdaptedLichen") shouldBe 1
    kim.count("Microbe<$Psychrophiles>") shouldBe 0
    kim.count("MC") shouldBe cashBefore
    kim.count("Owed") shouldBe 0
  }

  @Test
  internal fun cardActionInvocationLeavesItsExplicitDrawForTheCaller() {
    newTestGame()
    kim.exMachina("$AiCentral")
    val routines = TfmRoutines(agents)
    kim.beginOperation("OfferAction")
    val cardsBefore = kim.count("ProjectCard")

    routines.execute(kim, "UseAction1", "$AiCentral")

    kim.count("ActionUsedMarker<$AiCentral>") shouldBe 1
    kim.count("ProjectCard") shouldBe cardsBefore
    kim.doTask("2 ProjectCard")
    kim.count("ProjectCard") shouldBe cardsBefore + 2
  }

  @Test
  internal fun aGrantedActionDoesNotConsumeTheOrdinaryGenerationUse() {
    newTestGame()
    kim.exMachina("$AiCentral")
    kim.beginOperation("UseAction<ActionCard>")
    val routines = TfmRoutines(agents)

    routines.execute(kim, "UseAction1", "$AiCentral")

    kim.count("ActionUsedMarker<$AiCentral>") shouldBe 0
    kim.doTask("2 ProjectCard")
  }

  @Test
  internal fun standardProjectInvocationSharesThePaymentRoutine() {
    newTestGame()
    kim.setToExMachina(11, "MC")
    val productionBefore = kim.count("PROD[Energy]")
    val routines = TfmRoutines(agents)
    kim.beginOperation("OfferAction")

    routines.execute(kim, "UseStandardProject", "PowerPlantProject")
    routines.execute(kim, "Pay", "11 MC")
    kim.count("PROD[Energy]") shouldBe productionBefore
    kim.doTask("PROD[Energy]")

    kim.count("PROD[Energy]") shouldBe productionBefore + 1
    kim.count("MC") shouldBe 0
  }
}
