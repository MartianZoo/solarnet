package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestonesAwardsExpansionTest : CardTest() {
  @Test
  internal fun `Briber costs twelve MC in addition to the normal claim cost`() {
    newGame(GameConfig("Briber, Builder, Engineer", "Player1", "Player2"))
    p1.manual("20 MC")
    admin.phase("Action")

    p1.claimMilestone(cn("Briber")).expect("-20 MC, Briber")
  }

  @Test
  internal fun `Briber claim is atomic when the player cannot pay the extra cost`() {
    newGame(GameConfig("Briber, Builder, Engineer", "Player1", "Player2"))
    p1.manual("19 MC")
    admin.phase("Action")

    shouldThrow<LimitsException> { p1.claimMilestone(cn("Briber")) }

    p1.count("MC") shouldBe 19
    p1.count("Milestone") shouldBe 0
  }

  @Test
  internal fun `Philantropist counts victory point gains but not Vitor's reference`() {
    newGame(
        GameConfig(
            "PreludeExpansion, Philantropist, Builder, Engineer",
            "Player1",
            "Player2",
        )
    )
    p1.manual("$Vitor, $SearchForLife, $Tardigrades, $ColonizerTrainingCamp, $DustSeals")

    shouldThrow<RequirementException> { p1.manual("Philantropist") }

    p1.manual("$SpaceElevator")
    p1.manual("Philantropist")
    p1.count("Philantropist") shouldBe 1
  }

  @Test
  internal fun `Merchant checks resources after the normal claim cost`() {
    val game =
        newGame(
            GameConfig(
                "Merchant, Builder, Engineer",
                "Player1",
                "Player2",
            )
        )
    game.classTable.isActive(cn("Merchant")) shouldBe true
    p1.manual("10 M, 2 S, 2 T, 2 P, 2 E, 2 H")
    admin.phase("Action")

    p1.stdAction("ClaimMilestone") { doTask("Merchant") }

    p1.count("Merchant") shouldBe 1
  }

  @Test
  internal fun `Hydrologist can be claimed after placing four oceans`() {
    newGame(GameConfig("Hydrologist, Builder, Engineer", "Player1", "Player2"))
    val p2 = requireP2()
    val oceans = p1.list("WaterArea").take(4)
    admin.count("HydrologistWatcher") shouldBe 1

    oceans.forEach { p1.manual("OceanTile<$it>") }

    shouldThrow<RequirementException> { p2.manual("Hydrologist") }
    p1.manual("Hydrologist")
    p1.count("Hydrologist") shouldBe 1
  }

  @Test
  internal fun `Removing an ocean removes its placement credit`() {
    newGame(GameConfig("Hydrologist, Builder, Engineer", "Player1", "Player2"))
    val oceans = p1.list("WaterArea").take(4)
    oceans.forEach { p1.manual("OceanTile<$it>") }
    p1.count("OceanCredit") shouldBe 4
    oceans.forEach { p1.count("OceanCredit<OceanTile<$it>>") shouldBe 1 }

    requireP2().manual("-OceanTile<${oceans.first()}>")

    p1.count("OceanCredit") shouldBe 3
    p1.count("OceanCredit<OceanTile<${oceans.first()}>>") shouldBe 0
    shouldThrow<RequirementException> { p1.manual("Hydrologist") }
  }

  @Test
  internal fun `OceanCredit and its watcher stay undefined without Hydrologist`() {
    val game = newGame(GameConfig("Builder, Legend, Merchant", "Player1", "Player2"))

    game.classTable.allClassNames.shouldNotContain(cn("OceanCredit"))
    game.classTable.allClassNames.shouldNotContain(cn("HydrologistWatcher"))
  }

  // Producer wants 16 printed production, and Producer22 wants 22 because QuickStartVariant hands
  // you 6 at setup. Both start one short of their threshold after these grants.
  private fun claimProducerOneProductionShortOfThreshold(milestone: String, modules: String) {
    newGame(GameConfig("$milestone, Builder, Engineer$modules", "Player1", "Player2"))
    p1.manual("8 M")
    p1.manual("PROD[5 Steel, 5 Titanium, 5 Plant]")
    admin.phase("Action")

    shouldThrow<RequirementException> { p1.manual(milestone) }

    p1.manual("PROD[Energy]")
    p1.stdAction("ClaimMilestone") { doTask(milestone) }

    p1.count(milestone) shouldBe 1
  }

  @Test
  internal fun `Producer requires sixteen printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer", "")

  @Test
  internal fun `Producer22 requires twenty two printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer22", ", -CorporateEraExpansion")

  @Test
  internal fun `Producer versions belong to opposite Quick Start modes`() {
    shouldThrow<LimitsException> {
      newGame(
          GameConfig(
              "Producer, Builder, Engineer, -CorporateEraExpansion",
              "Player1",
              "Player2",
          )
      )
    }
    shouldThrow<IllegalArgumentException> {
      newGame(
          GameConfig(
              "Producer22, Builder, Engineer",
              "Player1",
              "Player2",
          )
      )
    }
  }
}
