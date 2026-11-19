package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.engine.CardTracking
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.withTestTurmoilDeck
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ParadigmBreakdownTest : TfmGameplayTest() {
  @Test
  fun `Each player chooses two cards to discard and keeps the rest of their hand`() {
    val config = GameConfig("TurmoilExpansion, BeginnerVariant", "Kim", "Stan")
    initializeGame(
        withTestTurmoilDeck(
            canonicalCatalog(config).gamePremise(config),
            listOf("$ScientificConsensus", "$RedResistance", "$ScientificProgress"),
        )
    )
    val cards = CardTracking.start(agents)
    val kimHand =
        listOf(
            Mine,
            TitaniumMine,
            PowerPlant,
            Sponsors,
            EarthOffice,
            Research,
            AdaptedLichen,
            Archaebacteria,
            Tardigrades,
            IndustrialMicrobes,
        )
    val stanHand =
        listOf(
            VestaShipyard,
            Cartel,
            AcquiredCompany,
            InvestmentLoan,
            BribedCommittee,
            Birds,
            Fish,
            Livestock,
            Predators,
            SmallAnimals,
        )
    val kimPlayer = kim.actor as Player
    val stanPlayer = stan.actor as Player
    cards.expectArrivals(kimPlayer, *kimHand.toTypedArray())
    cards.expectArrivals(stanPlayer, *stanHand.toTypedArray())
    players.forEachIndexed { index, player ->
      cards.expectArrivals(
          player.actor as Player,
          cn("BeginnerCorporation${index + 1}"),
          back = cn("BeginnerCard"),
      )
    }
    admin.beginOperation("SetupPhase FROM BootstrapPhase")
    players.forEach {
      // Select the setup alternatives before narrowing the beginner path.
      it.selectTask("BeginnerOption OR StandardOption")
      it.narrowTask("BeginnerOption")
    }
    players.forEach { it.doTasks("20 TerraformRating", "BeginnerCard") }
    players.forEachIndexed { index, player ->
      player.doTasks(
          "PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation${index + 1}>, Hand>",
          "BeginnerCorporation${index + 1} FROM BeginnerCard",
          "42 MC",
          "10 ProjectCard",
      )
    }

    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }

    // These are the players' discard decisions, supplied before the event moves their cards.
    cards.expectDepartures(kimPlayer, TitaniumMine, Research)
    cards.expectDepartures(stanPlayer, Birds, Predators)
    expectChanges("-2 ProjectCard<Kim>, -2 ProjectCard<Stan>") {
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.doTask("19 MC") }
      players.forEach { it.doTasks("-2 ProjectCard", "0 MC") }
    }
    cards.hand(kimPlayer) shouldBe kimHand.toSet() - setOf(TitaniumMine, Research)
    cards.hand(stanPlayer) shouldBe stanHand.toSet() - setOf(Birds, Predators)
    cards.requireComplete()
  }

  @Test
  fun `Empty hand does not block the event or its influence reward`() {
    newTestGame("TurmoilExpansion, -QuickStartVariant, -ColoniesExpansion", playerCount = 2)
    kim.exMachina("-10 ProjectCard<Hand>, $EventAnalysts")
    // Prepare the event, then resolve it through the real Solar phase.
    admin.exMachina(
        "$ScientificConsensus, Current<$ScientificConsensus>, " +
            "PendingGlobalEffect<Current<$ScientificConsensus>, Class<ParadigmBreakdown>>"
    )
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    expectChanges("0 ProjectCard<Kim, Hand>, -2 ProjectCard<Stan, Hand>, 2 MC<Kim>") {
      kim.doTasks(
          "VenusStep! BY Admin",
          "-0 ProjectCard",
          "2 MC",
      )
      stan.doTasks("-2 ProjectCard", "0 MC")
    }
    kim.count("ProjectCard<Hand>") shouldBe 0
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }
}
