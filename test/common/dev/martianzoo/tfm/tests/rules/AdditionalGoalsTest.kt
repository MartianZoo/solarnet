package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AdditionalGoalsTest : CardTest() {
  @Test
  internal fun `Terraformer26 requires twenty six TR and can coexist with Terraformer35`() {
    newGame(
        GameConfig("TurmoilExpansion, Terraformer26, Terraformer35, Builder", "Player1", "Player2")
    )
    p1.runOperation("5 TerraformRating, 8 MC")
    admin.phase("Action")
    shouldThrow<RequirementException> { p1.claimMilestone(cn("Terraformer26")) }
    p1.runOperation("TerraformRating")
    p1.claimMilestone(cn("Terraformer26")).expect("-8 MC, Terraformer26")
    game.classTable.isActive(cn("Terraformer35")) shouldBe true
  }

  @Test
  internal fun `Lobbyist counts a chairman and six party delegates without double counting leaders`() {
    newGame(GameConfig("TurmoilExpansion, Lobbyist, Builder, Engineer", "Player1", "Player2"))
    p1.runOperation("8 MC")
    repeat(5) { p1.runOperation("PartyDelegate<Scientists>") }
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman")
    admin.phase("Action")
    shouldThrow<RequirementException> { p1.claimMilestone(cn("Lobbyist")) }
    p1.stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }
    p1.claimMilestone(cn("Lobbyist")).expect("-8 MC, Lobbyist")
  }

  @Test
  internal fun `Politician measures final politics including Event Analysts without forming government`() {
    newGame(GameConfig("TurmoilExpansion, Politician, Thermalist, Miner", "Player1", "Player2"))
    val p2 = requireP2()
    repeat(3) { p1.runOperation("PartyDelegate<Scientists>") }
    admin.runOperation("MeasureInfluence<Player1>")
    p1.count("Influence") shouldBe 2

    // Player 2 takes dominance after the snapshot. The old influence must not survive scoring.
    repeat(4) { p2.runOperation("PartyDelegate<Unity>") }
    p1.runOperation("EventAnalysts, 8 MC")
    admin.phase("Action")
    p1.fundAward(cn("Politician"), 8)
    admin.runOperation("End FROM Phase")

    p1.count("Influence") shouldBe 1
    p2.count("Influence") shouldBe 2
    p1.count("FirstPlace<Politician>") shouldBe 0
    p2.count("FirstPlace<Politician>") shouldBe 1
    p2.count("SecondPlace<Politician>") shouldBe 0
    admin.count("Ruling<Greens>") shouldBe 1
    admin.count("Chairman<Neutral>") shouldBe 1
    p2.count("PartyDelegate<Unity>") shouldBe 4
  }

  @Test
  internal fun `Politician awards friendly first place ties`() {
    newGame(GameConfig("TurmoilExpansion, Politician, Thermalist, Miner", "Player1", "Player2"))
    p1.runOperation("8 MC")
    admin.phase("Action")
    p1.fundAward(cn("Politician"), 8)
    admin.runOperation("End FROM Phase")
    p1.count("FirstPlace<Politician>") shouldBe 1
    requireP2().count("FirstPlace<Politician>") shouldBe 1
  }
}
