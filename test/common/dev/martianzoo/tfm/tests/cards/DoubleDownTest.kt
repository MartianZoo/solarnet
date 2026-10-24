package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
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
      doTask("4 Director<BoardOfDirectors>")
    }

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
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
      // Expand the increase into separate track changes for each active colony.
      selectTask("EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }")
      doTasks("Colony<Luna>", "PROD[2 MC]")
    }

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
          doTasks("CopyPrelude<EarlyColonization>", "3 Energy")
          // Expand the increase into separate track changes for each active colony.
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
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
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
      doTask("ProjectCard<Stan, Hand>")
    }

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
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
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
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
        doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
        doTask("4 CorporationCard<Kim, Selecting>")

        doTask("PlayCard<Class<CorporationCard>, Class<NirgalEnterprises>>")
        doTask("NirgalEnterprises<Kim> FROM CorporationCard<Kim, Selecting>!")
        doTask("30 MC")
        doTask("PROD[Energy]")
        doTask("PROD[Plant]")
        doTask("PROD[Steel]")
        // Resolve Merger's payment to expose the unaffordable corporation.
        doTask("-42 MC")
      }
    }
    kim.inTurn { doTasks("-PreludeCard", "Audit", "15 MC") }.expect("0 $Merger")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
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
          doTask("ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard, EarthTag")

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<DoubleDown>>", "DoubleDown FROM PreludeCard")
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
      // Expand the copied production box into its separate resource increases.
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
          doTasks("CopyPrelude<IndustrialComplex>", "-18 MC")
          // Expand the copied production box into its separate resource increases.
          selectOnlyTask()
          doTask("PROD[Steel]")
        }
        .expect("PROD[0 MC, Steel, 0 Titanium, 0 Plant, 0 Energy, 0 Heat]")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Copies a played New Partner without Prelude setup`() {
      newTestGame(
          addOptions = "ValleyTrust, prelude:cards",
          kimCorporation = ValleyTrust,
      )
      admin.count("PreludeExpansion") shouldBe 0
      kim.count("PreludeCard") shouldBe 0

      kim.completeOperation {
            doTask("DoRequiredActions")
            doTask("PlayCard<Class<PreludeCard>, Class<$NewPartner>>")
            autoExecNow(EAGER)
            doTask("PlayCard<Class<PreludeCard>, Class<$DoubleDown>>")
            doTask("CopyPrelude<$NewPartner>")
            autoExecNow(EAGER)
            doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
            autoExecNow(EAGER)
          }
          .expect("PROD[2 MC], 21 MC, $NewPartner, $DoubleDown, $Donation, -RequiredAction")
      kim.count("PreludeCard") shouldBe 0
    }

    @Test
    internal fun `Copying Preservation Program adds TR without another Action phase skip`() {
      newTestGame("PreludeExpansion, PreservationProgram", playerCount = 2)
      kim.doTask("PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>")
      kim.completeOperation {
            doTask("PlayCard<Class<PreludeCard>, Class<$DoubleDown>>")
            doTask("CopyPrelude<$PreservationProgram>")
            autoExecNow(EAGER)
          }
          .expect("5 TerraformRating")
      stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
      stan.autoExecNow(EAGER)

      stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
      stan.autoExecNow(EAGER)

      kim.completeOperation {
            doTasks(*playCardTasks("$Comet"), "Ok" /* No Titanium payment. */)
            doTask("OceanTile<Tharsis_1_2>")
            autoExecNow(EAGER)
          }
          .expect("TerraformRating")
    }
  }
}
