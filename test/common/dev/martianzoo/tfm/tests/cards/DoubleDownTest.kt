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
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BiosphereSupport>>",
          "BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
          selectOnlyTask()
          doTasks("CopyPrelude<BiosphereSupport>", "PROD[-MC]", "PROD[2 Plant]")
        }
        .expect("PROD[0 Steel, 0 Titanium, 0 Energy, 0 Heat]")
  }

  @Test
  internal fun `Copies Board of Directors without becoming a holder for its directors`() {
    newTestGame("PreludeExpansion, BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      selectOnlyTask()
    }

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
          selectOnlyTask()
          doTask("CopyPrelude<BoardOfDirectors>")
        }
        .expect("0 Director")
  }

  @Test
  internal fun `Copies Early Colonizations benefit for every active colony track`() {
    newTestGame("PreludeExpansion, EarlyColonization, Luna, Ceres")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<EarlyColonization>>",
          "EarlyColonization FROM PreludeCard",
          "3 Energy",
      )
      selectTask("EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }")
      doTasks("Colony<Luna>", "PROD[2 MC]")
    }

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
          selectOnlyTask()
          doTasks("CopyPrelude<EarlyColonization>", "3 Energy")
          selectTask("EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }")
          doTasks("Colony<Ceres>", "PROD[Steel]")
        }
        .expect("2 ColonyProduction<Luna>, 2 ColonyProduction<Ceres>")
  }

  @Test
  internal fun `Cannot copy an absent Prelude`() {
    newTestGame("PreludeExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BiosphereSupport>>",
          "BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }

    shouldThrow<DependencyException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
        selectOnlyTask()
        doTask("CopyPrelude<MartianIndustries>")
      }
    }
  }

  @Test
  internal fun `Cannot copy another players Prelude`() {
    newTestGame("PreludeExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BiosphereSupport>>",
          "BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }
    stan.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<UnmiContractor>>",
          "UnmiContractor FROM PreludeCard",
          "3 TerraformRating",
      )
      selectTask("ProjectCard<Stan, Hand>!")
    }

    shouldThrow<DependencyException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
        selectOnlyTask()
        doTask("CopyPrelude<UnmiContractor>")
      }
    }
  }

  @Test
  internal fun `Cannot copy a corporation`() {
    newTestGame("PreludeExpansion", kimCorporation = CrediCor)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BiosphereSupport>>",
          "BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
        selectOnlyTask()
        doTask("CopyPrelude<CrediCor>")
      }
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    newTestGame("PreludeExpansion")
    kim.exMachina("$BiosphereSupport")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
            "$DoubleDown FROM PreludeCard",
            "CopyPrelude<$DoubleDown>",
        )
      }
    }
  }

  @Test
  internal fun `Fizzles when a fizzled Merger leaves no Prelude to copy`() {
    newTestGame("PreludeExpansion, NirgalEnterprises")
    kim.setToExMachina(7, "MC")
    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
        selectTask("4! CorporationCard<Kim, Selecting>")
        selectTask("-3! CorporationCard<Kim, Selecting>")
        selectTask("PlayCard<Kim, Class<CorporationCard>, Class<CardFront>, Selecting>!")
        doTask("PlayCard<Class<CorporationCard>, Class<NirgalEnterprises>>")
        selectTask("MAX 0 Barrier: NirgalEnterprises FROM CorporationCard<Kim, Selecting>!")
        selectTask("30! MC")
        selectTask("PROD[Energy]")
        selectTask("PROD[Plant]")
        selectTask("PROD[Steel]")
        selectOnlyTask()
      }
    }
    kim.inTurn { doTasks("-PreludeCard", "Audit", "15 MC") }.expect("0 $Merger")

    shouldThrow<DependencyException> {
      kim.inTurn {
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
        selectOnlyTask()
        doTask("CopyPrelude<Merger>")
      }
    }
    kim.inTurn { doTasks("-PreludeCard", "Audit", "15 MC") }.expect("0 $DoubleDown")
  }

  @Test
  internal fun `Repeats the benefit without adding tags or triggering Point Luna`() {
    newTestGame("PreludeExpansion, PointLuna", kimCorporation = PointLuna)
    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<AlliedBank>>",
              "AlliedBank FROM PreludeCard",
              "PROD[4 MC]",
              "3 MC",
          )
          selectTask("ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard, EarthTag")

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
          selectOnlyTask()
          doTasks("CopyPrelude<AlliedBank>", "PROD[4 MC]", "3 MC")
        }
        .expect("0 ProjectCard, 0 EarthTag")
  }

  @Test
  internal fun `Industrial Complex recalculates which tracks need raising when copied`() {
    newTestGame("PreludeExpansion, IndustrialComplex")
    kim.setToExMachina(36, "MC")
    kim.setToExMachina(-5, "PROD[MC]")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>",
          "IndustrialComplex FROM PreludeCard",
          "-18 MC",
      )
      selectOnlyTask()
      doTasks(
          "PROD[7 MC]",
          "PROD[Titanium]",
          "PROD[Energy]",
          "PROD[Plant]",
          "PROD[Heat]",
          "PROD[Steel]",
      )
    }
    kim.exMachina("PROD[-Steel]")

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
          selectOnlyTask()
          doTasks("CopyPrelude<IndustrialComplex>", "-18 MC")
          selectOnlyTask()
          doTask("PROD[Steel]")
        }
        .expect("PROD[0 MC, Steel, 0 Titanium, 0 Plant, 0 Energy, 0 Heat]")
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
