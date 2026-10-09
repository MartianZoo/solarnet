package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class RoboticWorkforceTest : CardTest() {
  @Test
  internal fun `Can copy Strip Mine's production box`() {
    newGame()
    p1.runOperation("PROD[4 Energy], $StripMine")
    p1.assertProds(2 to "Steel", 1 to "Titanium", 2 to "Energy")
    p1.runOperation("$RoboticWorkforce") { doTask("CopyProductionBox<$StripMine>") }
    p1.assertProds(4 to "Steel", 2 to "Titanium", 0 to "Energy")
  }

  @Test
  internal fun `Cannot copy a non-building card`() {
    newGame()
    p1.runOperation("PROD[Energy], $Mine, $MassConverter")
    shouldThrow<NarrowingException> {
      p1.runOperation("$RoboticWorkforce") { doTask("CopyProductionBox<$MassConverter>") }
    }
  }

  @Test
  internal fun `Cannot copy another player's building card`() {
    newGame()
    val p2 = requireP2()
    p1.runOperation("$IndustrialMicrobes")
    p2.runOperation("$Mine")

    shouldThrow<NarrowingException> {
      p1.runOperation("$RoboticWorkforce") { doTask("CopyProductionBox<$Mine<Player2>>") }
    }
  }

  @Test
  internal fun `Cannot copy a building card its player does not own`() {
    newGame()
    p1.runOperation("$IndustrialMicrobes")
    shouldThrow<NarrowingException> {
      p1.runOperation("$RoboticWorkforce") { doTask("CopyProductionBox<$Mine>") }
    }
  }

  // https://boardgamegeek.com/thread/3430226/article/45396575#45396575
  @Test
  internal fun `Robotic Workforce does Industrial Complex production adjustment again without its cost`() {
    newGame(PreludeExpansion, Prelude2CardPack)
    p1.playCorp(ThorGate, 3)
    admin.phase("Prelude")
    p1.playPrelude(IndustrialComplex)
    p1.playPrelude(Donation)
    admin.phase("Action")
    p1.playProject(ImmigrantCity, 13) { placeTile(4, 4) }
    p1.playProject(PowerPlant, 1)

    p1.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$IndustrialComplex>") }
        .expect("-9 MC, PROD[MC, 0 Steel, 0 Energy]")
  }

  @Test
  internal fun `Copying Medical Lab counts building tags present when copied`() {
    newGame(CorporateEraExpansion)
    p1.runOperation("50 MC, 5 ProjectCard")
    admin.phase("Action")
    p1.playProject(MedicalLab, 13)
    p1.playProject(Mine, 4)
    p1.playProject(IndustrialMicrobes, 12)
    p1.playProject(TitaniumMine, 7)
    p1.assertProds(0 to "MC")

    p1.playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$MedicalLab>") }
        .expect("PROD[2 MC]")
  }

  @Test
  internal fun `Copying Heat Trappers may target a different player`() {
    newGame(CorporateEraExpansion)
    val p2 = requireP2()
    p1.runOperation("PROD[2 Heat], 30 MC, 2 ProjectCard")
    p2.runOperation("PROD[2 Heat]")
    admin.phase("Action")
    p1.playProject(HeatTrappers, 6) { doTask("PROD[-2 Heat<Player2>]") }
    p2.assertProds(0 to "Heat")
    p2.runOperation("PROD[2 Heat]")

    p1.playProject(RoboticWorkforce, 9) {
          doTask("CopyProductionBox<$HeatTrappers>")
          doTask("PROD[-2 Heat<Player1>]")
        }
        .expect("PROD[-2 Heat<Player1>, Energy<Player1>, 0 Heat<Player2>]")

    p1.assertProds(0 to "Heat", 2 to "Energy")
    p2.assertProds(2 to "Heat")
  }
}
