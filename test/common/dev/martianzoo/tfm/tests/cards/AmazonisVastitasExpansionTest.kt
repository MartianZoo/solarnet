package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.Vastitas
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AmazonisVastitasExpansionTest : CardTest() {
  @Test
  internal fun `Amazonis defaults prefer its Merchant variant and reuse matching goals`() {
    val table = newGame(Amazonis).classTable

    table.isInhabited(cn("Merchant3")) shouldBe true
    table.isInhabited(cn("Merchant")) shouldBe false
    table.isInhabited(cn("Manufacturer")) shouldBe true
    table.isInhabited(cn("Manufacturer2")) shouldBe false
    table.isInhabited(cn("Terran")) shouldBe true
    table.isInhabited(cn("Collector")) shouldBe true
  }

  @Test
  internal fun `Amazonis Merchant needs three of each resource after paying the claim cost`() {
    newGameWithAutoWorkflow(Amazonis)
    playUntilFirstActionPhase(startingMc = 127)

    p1.turn {
      playProject(MineralDeposit, 5)
      playProject(AsteroidCard, 14)
    }
    requireP2().pass()
    p1.playProject(ImportedHydrogen, 16) {
      doTask("3 Plant")
      placeTile(2, 1)
    }
    p1.stdProject("CityProject") { placeTile(7, 5) }
    p1.stdProject("CityProject") { placeTile(7, 7) }
    p1.stdProject("CityProject") { placeTile(11, 7) }
    p1.playProject(ImportedGhg, 7)

    shouldThrow<RequirementException> { p1.claimMilestone(cn("Merchant3")) }
    p1.count("MC") shouldBe 10

    p1.sellPatents(1).expect("MC")
    p1.claimMilestone(cn("Merchant3")).expect("-8 MC, Merchant3")
  }

  @Test
  internal fun `Amazonis Manufacturer uses the corrected production metric`() {
    newGameWithAutoWorkflow(Amazonis, PreludeExpansion)
    val p2 = requireP2()
    playUntilPreludePhase()
    p1.turn {
      playPrelude(MiningOperations)
      playPrelude(MoholeExcavation)
    }
    p2.turn {
      playPrelude(Mohole)
      playPrelude(Donation)
    }
    p1.fundAward(cn("Manufacturer"), 8).expect("Manufacturer")

    shutdownWorkflow()
    TfmWorkflow.Stepwise(agents).endPhase()

    p1.count("PROD[Steel OR Heat]") shouldBe 5
    p2.count("PROD[Steel OR Heat]") shouldBe 4
    p1.count("FirstPlace<Player1, Manufacturer>") shouldBe 1
    p2.count("FirstPlace<Player2, Manufacturer>") shouldBe 0
  }

  @Test
  internal fun `Amazonis delegate bonuses are ignored without Turmoil`() {
    newGameWithAutoWorkflow(Amazonis)
    playUntilFirstActionPhase()

    p1.turn {
      stdProject("CityProject") { placeTile(1, 4) }.expect("ProjectCard")
      stdProject("CityProject") {
            placeTile(5, 3)
            doTask("Titanium")
          }
          .expect("Titanium")
    }
    requireP2().pass()

    p1.stdProject("CityProject") { placeTile(2, 2) }.expect("0 ProjectCard, 0 Titanium")
  }

  @Test
  internal fun `both single Amazonis delegate spaces place one delegate with Turmoil`() {
    listOf("Amazonis_02_02", "Amazonis_07_11").forEach { area ->
      newGame(Amazonis, TurmoilExpansion)

      p1.runOperation("CityTile<$area>") { doTask("PartyDelegate<Scientists>") }

      p1.count("PartyDelegate<Scientists>") shouldBe 1
      p1.count("PartyDelegate OR Chairman") shouldBe 1
      p1.count("LobbyActionAvailable") shouldBe 1
    }
  }

  @Test
  internal fun `Olympus Mons places two delegates in one chosen party`() {
    newGame(Amazonis, TurmoilExpansion)

    p1.runOperation("CityTile<Amazonis_08_09>") {
      doTask("2 PartyDelegate<Scientists>")
    }

    p1.count("PartyDelegate<Scientists>") shouldBe 2
    p1.count("PartyDelegate") shouldBe 2
    p1.count("PartyLeader<Scientists>") shouldBe 1
    admin.count("Dominant<Scientists>") shouldBe 1
    p1.count("PartyDelegate OR Chairman") shouldBe 2
  }

  @Test
  internal fun `Olympus Mons cannot be occupied without two available delegates`() {
    newGame(Amazonis, TurmoilExpansion)
    repeat(6) { p1.runOperation("PartyDelegate<Unity>") }

    shouldThrow<DeadEndException> {
      p1.runOperation("CityTile<Amazonis_08_09>") {
        doTask("2 PartyDelegate<MarsFirst>")
      }
    }

    p1.count("CityTile<Amazonis_08_09>") shouldBe 0
    p1.count("PartyDelegate OR Chairman") shouldBe 6
  }

  @Test
  internal fun `both Vastitas delegate spaces place one delegate with Turmoil`() {
    listOf("Vastitas_4_8", "Vastitas_9_5").forEach { area ->
      newGame(Vastitas, TurmoilExpansion)

      p1.runOperation("CityTile<$area>") { doTask("PartyDelegate<Greens>") }

      p1.count("PartyDelegate<Greens>") shouldBe 1
      p1.count("PartyDelegate OR Chairman") shouldBe 1
    }
  }

  @Test
  internal fun `Vastitas delegate bonuses are ignored without Turmoil`() {
    newGame(Vastitas)

    p1.runOperation("CityTile<Vastitas_4_8>")

    p1.count("CityTile<Vastitas_4_8>") shouldBe 1
    p1.count("PartyDelegate") shouldBe 0
  }

  @Test
  internal fun `Vastitas delegate spaces cannot be occupied without an available delegate`() {
    newGame(Vastitas, TurmoilExpansion)
    repeat(7) { p1.runOperation("PartyDelegate<Unity>") }

    shouldThrow<DeadEndException> {
      p1.runOperation("CityTile<Vastitas_4_8>") {
        doTask("PartyDelegate<Greens>")
      }
    }

    p1.count("CityTile<Vastitas_4_8>") shouldBe 0
  }

  @Test
  internal fun `Vastitas Geologist counts owned tiles with owned neighbors`() {
    newGameWithAutoWorkflow(Vastitas)
    playUntilFirstActionPhase()
    p1.turn {
      stdProject("PowerPlantProject")
      playProject(LavaFlows, 18) { placeTile(4, 1) }
    }
    requireP2().pass()
    p1.playProject(RestrictedArea, 11) { placeTile(3, 1) }

    shouldThrow<RequirementException> { p1.claimMilestone(cn("Geologist")) }

    p1.playProject(CommercialDistrict, 16) { placeTile(4, 2) }
    p1.claimMilestone(cn("Geologist")).expect("Geologist")
  }

  @Test
  internal fun `Vastitas Landscaper counts only the largest contiguous map group`() {
    val game = newGameWithAutoWorkflow(Vastitas)
    game.classTable.isInhabited(cn("Landscaper")) shouldBe true
    playUntilFirstActionPhase()
    p1.turn {
      stdProject("PowerPlantProject")
      playProject(LavaFlows, 18) { placeTile(4, 1) }
    }
    requireP2().pass()
    p1.playProject(RestrictedArea, 11) { placeTile(3, 1) }
    p1.playProject(CommercialDistrict, 16) { placeTile(4, 2) }
    p1.stdProject("CityProject") { placeTile(8, 7) }

    p1.count("OwnedTile") shouldBe 4
    p1.count("TileInLargestGroup") shouldBe 3

    p1.fundAward(cn("Landscaper"), 8).expect("Landscaper")
  }

  @Test
  internal fun `Vastitas defaults reuse its supported printed goals`() {
    val table = newGame(Vastitas).classTable

    table.isInhabited(cn("Engineer")) shouldBe true
    table.isInhabited(cn("Geologist")) shouldBe true
    table.isInhabited(cn("Traveller")) shouldBe true
    table.isInhabited(cn("Promoter")) shouldBe true
  }

  @Test
  internal fun `Vastitas north pole costs four MC and raises temperature`() {
    newGameWithAutoWorkflow(Vastitas)
    playUntilFirstActionPhase()

    p1.stdProject("CityProject") { placeTile(5, 5) }.expect("-29 MC, TemperatureStep")
  }
}
