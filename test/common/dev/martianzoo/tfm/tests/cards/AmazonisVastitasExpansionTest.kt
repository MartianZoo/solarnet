package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.Vastitas
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AmazonisVastitasExpansionTest : CardTest() {
  @Test
  internal fun `Amazonis defaults prefer its Merchant variant and reuse matching goals`() {
    val table = newGame(Amazonis).classTable

    table.isActive(cn("Merchant3")) shouldBe true
    table.isActive(cn("Merchant")) shouldBe false
    table.isActive(cn("Manufacturer")) shouldBe true
    table.isActive(cn("Manufacturer2")) shouldBe false
    table.isActive(cn("Terran")) shouldBe true
    table.isActive(cn("Collector")) shouldBe true
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
    p1.stdProject("CitySP") { placeTile(7, 5) }
    p1.stdProject("CitySP") { placeTile(7, 7) }
    p1.stdProject("CitySP") { placeTile(11, 7) }
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
    TfmWorkflow.Manual(game).endPhase()

    p1.count("AwardTally<Player1, Manufacturer>") shouldBe 5
    p2.count("AwardTally<Player2, Manufacturer>") shouldBe 4
  }

  @Test
  internal fun `Amazonis card and wild resource bonuses work while delegates are inert`() {
    newGameWithAutoWorkflow(Amazonis)
    playUntilFirstActionPhase()

    p1.turn {
      stdProject("CitySP") { placeTile(1, 4) }.expect("ProjectCard")
      stdProject("CitySP") {
            placeTile(5, 3)
            doTask("Titanium")
          }
          .expect("Titanium")
    }
    requireP2().pass()

    p1.stdProject("CitySP") { placeTile(2, 2) }.expect("0 ProjectCard, 0 Titanium")
  }

  @Test
  internal fun `Vastitas Geologist counts owned tiles with owned neighbors`() {
    newGameWithAutoWorkflow(Vastitas)
    playUntilFirstActionPhase()
    p1.turn {
      stdProject("PowerPlantSP")
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
    game.classTable.isActive(cn("Landscaper")) shouldBe true
    playUntilFirstActionPhase()
    p1.turn {
      stdProject("PowerPlantSP")
      playProject(LavaFlows, 18) { placeTile(4, 1) }
    }
    requireP2().pass()
    p1.playProject(RestrictedArea, 11) { placeTile(3, 1) }
    p1.playProject(CommercialDistrict, 16) { placeTile(4, 2) }
    p1.stdProject("CitySP") { placeTile(8, 7) }

    p1.count("OwnedTile") shouldBe 4
    p1.count("TileInLargestGroup") shouldBe 3

    p1.fundAward(cn("Landscaper"), 8).expect("Landscaper")
  }

  @Test
  internal fun `Vastitas defaults reuse its supported printed goals`() {
    val table = newGame(Vastitas).classTable

    table.isActive(cn("Engineer")) shouldBe true
    table.isActive(cn("Geologist")) shouldBe true
    table.isActive(cn("Traveller")) shouldBe true
    table.isActive(cn("Promoter")) shouldBe true
  }

  @Test
  internal fun `Vastitas north pole costs four MC and raises temperature`() {
    newGameWithAutoWorkflow(Vastitas)
    playUntilFirstActionPhase()

    p1.stdProject("CitySP") { placeTile(5, 5) }.expect("-29 MC, TemperatureStep")
  }
}
