package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CyberiaSystemsTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    p1.runOperation("$Mine, $IndustrialMicrobes")
  }

  @Test
  internal fun `Copies production boxes from two different building cards`() {
    p1.runOperation("$CyberiaSystems") {
          doTask("CopyProductionBox<$Mine>")
          doTask("CopyProductionBox<$IndustrialMicrobes>")
        }
        .expect("PROD[3 Steel, Energy]")
  }

  @Test
  internal fun `Cannot copy the same card twice`() {
    p1.runOperation("$CyberiaSystems") {
      doTask("CopyProductionBox<$Mine>")
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<$Mine>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    p1.runOperation("$CyberiaSystems") {
      shouldThrow<NarrowingException> { doTask("CopyProductionBox<$CyberiaSystems>") }
      abort()
    }
  }

  @Test
  internal fun `Cyberia repeats Magnetic Field Generators before Industrial Complex to power Development Center`() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        PromoCardPack,
        VenusNextExpansion,
        CorporateEraExpansion,
    )
    p1.playCorp(Manutech, 0)
    p1.runOperation("76 MC, 7 ProjectCard")
    admin.phase("Prelude")
    p1.playPrelude(IndustrialComplex)
    p1.playPrelude(PowerGeneration)
    admin.phase("Action")
    // Magnetic Field Generators has the largest building production decrease: four energy.
    p1.playProject(MagneticFieldGeneratorsPromo, 22) { placeTile(1, 1) }
    p1.playProject(PowerPlant, 4)
    p1.playProject(GiantSpaceMirror, 17)
    p1.playProject(Ironworks, 11)
    p1.playProject(OreProcessor, 13)
    p1.playProject(DevelopmentCenter, 11)
    p1.cardAction1(Ironworks)
    p1.cardAction1(OreProcessor)
    p1.assertCounts(0 to "Energy", 1 to "ProjectCard")
    p1.assertProds(4 to "Energy")
    shouldThrow<LimitsException> { p1.cardAction1(DevelopmentCenter) }

    // Lower energy production to zero first: restoring it to one gives Manutech one energy now.
    p1.playProject(CyberiaSystems, 16) {
          doTask("CopyProductionBox<$MagneticFieldGeneratorsPromo>")
          doTask("CopyProductionBox<$IndustrialComplex>")
        }
        .expect("Energy, 2 Plant, PROD[-3 Energy, 2 Plant]")
    p1.cardAction1(DevelopmentCenter).expect("-Energy, ProjectCard")
    p1.assertCounts(0 to "Energy", 0 to "MC")
  }
}
