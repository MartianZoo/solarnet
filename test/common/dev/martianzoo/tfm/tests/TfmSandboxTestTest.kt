package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.cards.cardnames.CorporateStronghold
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

    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Donation>>", "Donation FROM PreludeCard", "21 MC")
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          "PlayProject<Class<MineralDeposit>>",
          "-5 MC",
          "MineralDeposit FROM ProjectCard",
          "LATER { PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit. }",
          "5 Steel",
      )
      doTask("PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit")
    }
  }

  @Test
  internal fun `Corporation entry is independent of the cached game with corporations played`() {
    newTestGame(kimCorporation = Manutech)
    newTestGame(kimCorporation = Manutech, startAtCorporation = true)

    kim.count("$Manutech") shouldBe 0
    kim.inTurn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<Manutech>>",
          "Manutech FROM CorporationCard",
          "35 MC",
          "PROD[Steel]",
          "Steel",
          "-30 MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
    }
    stan.count("BeginnerCorporation2") shouldBe 0

    newTestGame(kimCorporation = Manutech)
    kim.inTurn {
      doTasks(
          "PlayProject<Class<MineralDeposit>>",
          "-5 MC",
          "MineralDeposit FROM ProjectCard",
          "LATER { PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit. }",
          "5 Steel",
      )
      doTask("PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit")
    }
  }

  @Test
  internal fun `Scoring queries preserve state and history for later gameplay`() {
    newTestGame()
    kim.exMachina("$Pets, 4 Animal<$Pets>")
    val checkpoint = game.timeline.checkpoint()

    victoryPoints() shouldBe listOf(22, 20, 20)
    victoryPoints() shouldBe listOf(22, 20, 20)
    game.timeline.checkpoint() shouldBe checkpoint

    kim.inTurn {
      doTasks(
          "PlayProject<Class<MineralDeposit>>",
          "-5 MC",
          "MineralDeposit FROM ProjectCard",
          "LATER { PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit. }",
          "5 Steel",
      )
      doTask("PlayedEvent<Class<MineralDeposit>> FROM MineralDeposit")
    }
  }

  @Test
  internal fun `A rejected scoring query preserves the pending player choice`() {
    newTestGame()

    kim.inTurn {
          doTasks(
              "PlayProject<Class<GiantIceAsteroid>>",
              "-36 MC",
              "GiantIceAsteroid FROM ProjectCard",
              "LATER { PlayedEvent<Class<GiantIceAsteroid>> FROM GiantIceAsteroid. }",
              "TemperatureStep",
              "TemperatureStep",
              "TerraformRating",
              "TerraformRating",
          )
          selectTask("-6 Plant<Anyone>?")
          doTask("Ok")
          val checkpoint = game.timeline.checkpoint()
          shouldThrow<IllegalStateException> { victoryPoints() }
          game.timeline.checkpoint() shouldBe checkpoint
          doTasks("OceanTile<Tharsis_1_2>", "TerraformRating", "2 Steel")
          doTasks("OceanTile<Tharsis_1_4>", "TerraformRating", "ProjectCard")
          doTask("PlayedEvent<Class<GiantIceAsteroid>> FROM GiantIceAsteroid")
        }
        .expect("2 OceanTile")
  }

  @Test
  internal fun `A scoring failure restores state and history`() {
    newTestGame()
    kim.exMachina("$CorporateStronghold, -20 TerraformRating")
    val checkpoint = game.timeline.checkpoint()

    shouldThrow<LimitsException> { victoryPoints() }
    game.timeline.checkpoint() shouldBe checkpoint

    kim.exMachina("20 TerraformRating")
    victoryPoints() shouldBe listOf(18, 20, 20)
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
    kim.inTurn {
      doTasks("UseStandardProject<AsteroidProject>", "-14 MC")
      selectTask("TemperatureStep.")
      doTask("TerraformRating")
    }
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
