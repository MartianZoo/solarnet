package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PoliticianTest : TfmSandboxTest() {
  @Test
  internal fun `two player games award no second place points even to the funder`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner", playerCount = 2)
    kim.exMachina("2 PartyDelegate<Scientists>")
    stan.exMachina("PartyDelegate<Unity>")

    // Kim scores 3, Stan 1. Both also receive their ordinary party-leader VP.
    stan.fundAward(cn("Politician"), 8)
    victoryPoints() shouldBe listOf(26, 21)
  }

  @Test
  internal fun `Event Analysts breaks a tie even outside the dominant party`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("2 PartyDelegate<Scientists>")
    stan.exMachina(
        "PartyDelegate<Unity>, PartyDelegate<Kelvinists>, PartyDelegate<Greens>, EventAnalysts"
    )

    // Kim scores 3 (leader + dominant leader + delegate); Stan scores 4 (three leaders + Analysts).
    // Before funding, only their ordinary party-leader VP count.
    victoryPoints() shouldBe listOf(21, 23, 20)
    kim.fundAward(cn("Politician"), 8)
    victoryPoints() shouldBe listOf(23, 28, 20)
  }

  @Test
  internal fun `chairman contributes influence but is not another party leader`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("Chairman FROM Chairman<Neutral>")
    kim.exMachina("3 PartyDelegate<Scientists, Neutral>")
    stan.exMachina("PartyDelegate<Unity>")

    // Kim's chairman influence ties Stan's party leadership at 1.
    stan.fundAward(cn("Politician"), 8)
    victoryPoints() shouldBe listOf(26, 26, 20)
  }

  @Test
  internal fun `several dominant party delegates give a single influence and second place can tie`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("4 PartyDelegate<Scientists>")
    stan.exMachina("3 PartyDelegate<Scientists>")
    rob.exMachina("PartyDelegate<Unity>")

    // Kim scores 3; Stan's three delegates score 1, tying Rob's single party leadership.
    rob.fundAward(cn("Politician"), 8)
    victoryPoints() shouldBe listOf(26, 22, 23)
  }

  @Test
  internal fun `delegates under a neutral dominant leader can tie for first without a second prize`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("3 PartyDelegate<Scientists, Neutral>")
    kim.exMachina("PartyDelegate<Scientists>")
    stan.exMachina("PartyDelegate<Unity>")

    // Kim's delegate influence and Stan's leadership each score 1. Rob scores 0.
    rob.fundAward(cn("Politician"), 8)
    victoryPoints() shouldBe listOf(25, 26, 20)
  }

  @Test
  internal fun `delegates outside the dominant party add no influence`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("6 PartyDelegate<Scientists, Neutral>")
    kim.exMachina("3 PartyDelegate<Unity>")
    stan.exMachina("2 PartyDelegate<Unity>")
    rob.exMachina("Chairman FROM Chairman<Neutral>")

    // Kim's leadership ties Rob's chairman influence at 1; Stan's delegates score 0.
    stan.fundAward(cn("Politician"), 8)
    victoryPoints() shouldBe listOf(26, 20, 26)
  }

  @Test
  internal fun `lobbying after funding changes the winner at final scoring`() {
    newTestGame(addOptions = "TurmoilExpansion, Politician, Thermalist, Miner")
    kim.exMachina("2 PartyDelegate<Scientists>")
    stan.exMachina("2 PartyDelegate<Unity>")
    kim.fundAward(cn("Politician"), 8)

    // Unity overtakes Scientists: Kim drops from 3 to 1, Stan rises from 1 to 3.
    stan
        .stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }
        .expect("Dominant<Unity>, -Dominant<Scientists>")
    victoryPoints() shouldBe listOf(23, 26, 20)
  }
}
