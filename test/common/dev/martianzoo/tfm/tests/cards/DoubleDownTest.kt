package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class DoubleDownTest : TfmSandboxTest() {
  @Test
  internal fun `Copies both the production gain and loss of Biosphere Support`() {
    newTestGame("PreludeExpansion")
    kim.playPrelude(BiosphereSupport)

    kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$BiosphereSupport>") }
        .expect("PROD[-MC, 0 Steel, 0 Titanium, 2 Plant, 0 Energy, 0 Heat]")
  }

  @Test
  internal fun `Copies Board of Directors without becoming a holder for its directors`() {
    newTestGame("PreludeExpansion, BoardOfDirectors")
    kim.playPrelude(BoardOfDirectors)

    kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$BoardOfDirectors>") }
        .expect("0 Director, $DoubleDown")
  }

  @Test
  internal fun `Copies Early Colonizations benefit for every active colony track`() {
    newTestGame("PreludeExpansion, EarlyColonization, Luna, Ceres")
    kim.playPrelude(EarlyColonization) { doTask("Colony<Luna>") }

    kim.playPrelude(DoubleDown) {
          doTask("CopyPrelude<$EarlyColonization>")
          doTask("Colony<Ceres>")
        }
        .expect("3 Energy, Colony<Ceres>, 2 ColonyProduction<Luna>, 2 ColonyProduction<Ceres>")
  }

  @Test
  internal fun `Cannot copy an absent Prelude`() {
    newTestGame("PreludeExpansion")
    kim.playPrelude(BiosphereSupport)

    shouldThrow<DependencyException> {
      kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$MartianIndustries>") }
    }
  }

  @Test
  internal fun `Cannot copy another players Prelude`() {
    newTestGame("PreludeExpansion")
    kim.playPrelude(BiosphereSupport)
    stan.playPrelude(UnmiContractor)

    shouldThrow<DependencyException> {
      kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$UnmiContractor>") }
    }
  }

  @Test
  internal fun `Cannot copy a corporation`() {
    newTestGame("PreludeExpansion", kimCorporation = CrediCor)
    kim.playPrelude(BiosphereSupport)

    shouldThrow<NarrowingException> {
      kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$CrediCor>") }
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    newTestGame("PreludeExpansion")
    kim.playPrelude(BiosphereSupport)

    shouldThrow<NarrowingException> {
      kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$DoubleDown>") }
    }
  }

  @Test
  internal fun `Fizzles when a fizzled Merger leaves no Prelude to copy`() {
    newTestGame("PreludeExpansion, NirgalEnterprises")
    kim.setToExMachina(7, "MC")
    shouldThrow<LimitsException> {
      kim.playPrelude(Merger) { kim.playCorp(NirgalEnterprises) { doTask("-42 MC") } }
    }
    kim.inTurn { doTask("-PreludeCard") }.expect("15 MC, 0 $Merger")

    shouldThrow<DependencyException> {
      kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$Merger>") }
    }
    kim.inTurn { doTask("-PreludeCard") }.expect("15 MC, -PreludeCard, 0 $DoubleDown")
  }

  @Test
  internal fun `Repeats the benefit without adding tags or triggering Point Luna`() {
    newTestGame("PreludeExpansion", kimCorporation = PointLuna)
    kim.playPrelude(AlliedBank).expect("3 MC, PROD[4 MC], ProjectCard, EarthTag")

    kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$AlliedBank>") }
        .expect("3 MC, PROD[4 MC], 0 ProjectCard, 0 EarthTag")
  }

  @Test
  internal fun `Industrial Complex recalculates which tracks need raising when copied`() {
    newTestGame("PreludeExpansion, IndustrialComplex")
    kim.setToExMachina(36, "MC")
    kim.setToExMachina(-5, "PROD[MC]")
    kim.playPrelude(IndustrialComplex)
    kim.exMachina("PROD[-Steel]")

    kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$IndustrialComplex>") }
        .expect("-18 MC, PROD[0 MC, Steel, 0 Titanium, 0 Plant, 0 Energy, 0 Heat]")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Copying Preservation Program adds TR without another Action phase skip`() {
      newTestGame("PreludeExpansion, PreservationProgram", playerCount = 2)
      kim.playPrelude(PreservationProgram)
      kim.playPrelude(DoubleDown) { doTask("CopyPrelude<$PreservationProgram>") }
          .expect("5 TerraformRating")
      stan.playPrelude(Supplier)
      stan.playPrelude(MetalsCompany)

      kim.playProject(Comet, 21) { placeTile(1, 2) }.expect("TerraformRating")
    }
  }
}
