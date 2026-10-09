package dev.martianzoo.tfm.tests

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.cards.cardnames.Donation
import dev.martianzoo.tfm.tests.cards.cardnames.GiantIceAsteroid
import dev.martianzoo.tfm.tests.cards.cardnames.Manutech
import dev.martianzoo.tfm.tests.cards.cardnames.MineralDeposit
import dev.martianzoo.tfm.tests.cards.cardnames.Pets
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TfmSandboxTestTest : TfmSandboxTest() {
  @Test
  internal fun `Prelude games allow a Prelude before explicitly starting Action phase`() {
    newTestGame(addOptions = "PreludeExpansion")

    kim.playPrelude(Donation).expect("21 MC")
    startActionPhase()
    kim.playProject(MineralDeposit, 5).expect("5 Steel")
  }

  @Test
  internal fun `Corporation entry is independent of the cached game with corporations played`() {
    newTestGame(kimCorporation = Manutech)
    newTestGame(kimCorporation = Manutech, startAtCorporation = true)

    kim.count("$Manutech") shouldBe 0
    kim.playCorp(Manutech).expect("Steel")
    stan.count("BeginnerCorporation2") shouldBe 0

    newTestGame(kimCorporation = Manutech)
    kim.playProject(MineralDeposit, 5).expect("5 Steel")
  }

  @Test
  internal fun `Scoring queries preserve state and history for later gameplay`() {
    newTestGame()
    kim.exMachina("$Pets, 4 Animal<$Pets>")
    val checkpoint = game.timeline.checkpoint()

    victoryPoints() shouldBe listOf(22, 20, 20)
    victoryPoints() shouldBe listOf(22, 20, 20)
    game.timeline.checkpoint() shouldBe checkpoint

    kim.playProject(MineralDeposit, 5).expect("5 Steel")
  }

  @Test
  internal fun `A rejected scoring query preserves the pending player choice`() {
    newTestGame()

    kim.playProject(GiantIceAsteroid, 36) {
          val checkpoint = game.timeline.checkpoint()
          shouldThrow<IllegalStateException> { victoryPoints() }
          game.timeline.checkpoint() shouldBe checkpoint
          placeTile(1, 2)
          placeTile(1, 4)
        }
        .expect("2 OceanTile")
  }

  @Test
  internal fun `A scoring failure restores state and history`() {
    newTestGame()
    val checkpoint = game.timeline.checkpoint()
    val policy = admin.autoExecPolicy
    admin.autoExecPolicy = NONE
    try {
      shouldThrow<NotFullySpecifiedException> { victoryPoints() }
      game.timeline.checkpoint() shouldBe checkpoint
    } finally {
      admin.autoExecPolicy = policy
    }
    victoryPoints() shouldBe listOf(20, 20, 20)
  }

  @Test
  internal fun `Forked games remain isolated`() {
    newTestGame()
    val firstKim = kim
    firstKim.exMachina("Plant")

    newTestGame()
    kim.count("Plant") shouldBe 0
    kim.exMachina("2 Plant")

    firstKim.count("Plant") shouldBe 1
    kim.count("Plant") shouldBe 2
  }

  @Test
  internal fun `Kim can use a live corporation while the other players remain beginners`() {
    newTestGame(kimCorporation = Manutech)

    kim.count("$Manutech") shouldBe 1
    kim.count("BeginnerCorporation") shouldBe 0
    kim.production(cn("Steel")) shouldBe 2
    kim.count("Steel") shouldBe 1
    stan.count("BeginnerCorporation2") shouldBe 1
    rob.count("BeginnerCorporation3") shouldBe 1
  }

  @Test
  internal fun `Absolute correction accepts production types`() {
    newTestGame()

    kim.setToExMachina(0, "PROD[Energy]")
    kim.setToExMachina(3, "PROD[Heat]")

    kim.production(cn("Energy")) shouldBe 0
    kim.production(cn("Heat")) shouldBe 3
  }

  @Test
  internal fun `Two-player games fail clearly only when Rob is used`() {
    newTestGame(playerCount = 2)

    players.size shouldBe 2
    kim.actor.toString() shouldBe "Kim"
    stan.actor.toString() shouldBe "Stan"
    shouldThrow<IllegalStateException> { rob }.message shouldBe "Rob is sitting this game out"
  }

  @Test
  internal fun `Four-player games prepare a fourth beginner corporation`() {
    newTestGame(playerCount = 4)

    players.size shouldBe 4
    players[3].actor.toString() shouldBe "Maya"
    players[3].count("BeginnerCorporation4") shouldBe 1
  }

  @Test
  internal fun `Five-player games prepare a fifth beginner corporation`() {
    newTestGame(playerCount = 5)

    players.size shouldBe 5
    players[4].actor.toString() shouldBe "Nadia"
    players[4].count("BeginnerCorporation5") shouldBe 1
  }

  @Test
  internal fun `Solo games support play and report absent seats clearly`() {
    newTestGame(playerCount = 1)

    shouldThrow<IllegalStateException> { stan }.message shouldBe "Stan is sitting this game out"
    shouldThrow<IllegalStateException> { rob }.message shouldBe "Rob is sitting this game out"
    kim.stdProject("AsteroidProject").expect("TerraformRating")
  }

  internal class TfmGameplayTestTest : TfmGameplayTest() {
    @Test
    internal fun `Corporation entry lets the players finish setup through the workflow`() {
      newTestGame(playerCount = 2, kimCorporation = Manutech, startAtCorporation = true)

      kim.count("$Manutech") shouldBe 0
      kim.playCorp(Manutech).expect("Steel")
      stan.count("BeginnerCorporation2") shouldBe 0
      stan.inTurn {
        doTask("PlayCard<Class<BeginnerCard>, Class<BeginnerCorporation2>, Hand>")
        stan.pay()
      }
      kim.playProject(MineralDeposit, 5).expect("5 Steel")
    }

    @Test
    internal fun `Starting another game clears the previous workflows unfinished turn`() {
      newTestGame()
      val previousGame = game
      previousGame.tasks.isEmpty() shouldBe false

      newTestGame()

      previousGame.tasks.isEmpty() shouldBe true
      kim.playProject(MineralDeposit, 5).expect("5 Steel")
    }
  }
}
