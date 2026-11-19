package dev.martianzoo.tfm.tests.cards

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
          "PlayCard<Class<PreludeCard>, Class<$BiosphereSupport>>",
          "$BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
              "$DoubleDown FROM PreludeCard",
              "CopyPrelude<$BiosphereSupport>",
              "PROD[-MC]",
              "PROD[2 Plant]",
          )
        }
        .expect("PROD[0 Steel, 0 Titanium, 0 Energy, 0 Heat]")
  }

  @Test
  internal fun `Copies Board of Directors without becoming a holder for its directors`() {
    newTestGame("PreludeExpansion, $BoardOfDirectors")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "$BoardOfDirectors FROM PreludeCard",
          "4 Director<$BoardOfDirectors>",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
              "$DoubleDown FROM PreludeCard",
              "CopyPrelude<$BoardOfDirectors>",
          )
        }
        .expect("0 Director")
  }

  @Test
  internal fun `Copies Early Colonizations benefit for every active colony track`() {
    newTestGame("PreludeExpansion, $EarlyColonization, Luna, Ceres")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EarlyColonization>>",
          "$EarlyColonization FROM PreludeCard",
          "3 Energy",
      )
      // Expand the increase into separate track changes for each active colony.
      doTasks(
          "EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }",
          "Colony<Luna>",
          "PROD[2 MC]",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
              "$DoubleDown FROM PreludeCard",
              "CopyPrelude<$EarlyColonization>",
              "3 Energy",
          )
          // Expand the increase into separate track changes for each active colony.
          doTasks(
              "EACH @ColonyTile { 2! ColonyProduction<@ColonyTile> }",
              "Colony<Ceres>",
              "PROD[Steel]",
          )
        }
        .expect("2 ColonyProduction<Luna>, 2 ColonyProduction<Ceres>")
  }

  @Test
  internal fun `Cannot copy an absent Prelude`() {
    newTestGame("PreludeExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BiosphereSupport>>",
          "$BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
            "$DoubleDown FROM PreludeCard",
            "CopyPrelude<$MartianIndustries>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot copy another players Prelude`() {
    newTestGame("PreludeExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BiosphereSupport>>",
          "$BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }
    stan.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard",
          "3 TerraformRating",
          "ProjectCard<Hand>",
      )
    }

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
            "$DoubleDown FROM PreludeCard",
            "CopyPrelude<$UnmiContractor>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot copy a corporation`() {
    newTestGame("PreludeExpansion", kimCorporation = CrediCor)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BiosphereSupport>>",
          "$BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
    }

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
            "$DoubleDown FROM PreludeCard",
            "CopyPrelude<$CrediCor>",
        )
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
    newTestGame("PreludeExpansion, $NirgalEnterprises")
    kim.setToExMachina(7, "MC")
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$Merger>>",
            "$Merger FROM PreludeCard",
            "4 CorporationCard<Selecting>",
        )

        doTasks(
            "PlayCard<Class<CorporationCard>, Class<$NirgalEnterprises>>",
            "$NirgalEnterprises FROM CorporationCard<Selecting>!",
            "30 MC",
            "PROD[Energy]",
            "PROD[Plant]",
            "PROD[Steel]",
        )
        // Resolve Merger's payment to expose the unaffordable corporation.
        doTask("-42 MC")
      }
    }
    kim.inTurn { doTasks("-PreludeCard", "Audit", "15 MC") }.expect("0 $Merger")

    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
            "$DoubleDown FROM PreludeCard",
            "CopyPrelude<$Merger>",
        )
      }
    }
    kim.inTurn { doTasks("-PreludeCard", "Audit", "15 MC") }.expect("0 $DoubleDown")
  }

  @Test
  internal fun `Repeats the benefit without adding tags or triggering Point Luna`() {
    newTestGame("PreludeExpansion, $PointLuna", kimCorporation = PointLuna)
    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$AlliedBank>>",
              "$AlliedBank FROM PreludeCard",
              "PROD[4 MC]",
              "3 MC",
              "ProjectCard<Hand>",
          )
        }
        .expect("ProjectCard, EarthTag")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
              "$DoubleDown FROM PreludeCard",
              "CopyPrelude<$AlliedBank>",
              "PROD[4 MC]",
              "3 MC",
          )
        }
        .expect("0 ProjectCard, 0 EarthTag")
  }

  @Test
  internal fun `Industrial Complex recalculates which tracks need raising when copied`() {
    newTestGame("PreludeExpansion, $IndustrialComplex")
    kim.setToExMachina(36, "MC")
    kim.setToExMachina(-5, "PROD[MC]")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$IndustrialComplex>>",
          "$IndustrialComplex FROM PreludeCard",
          "-18 MC",
      )
      // Expand the copied production box into its separate resource increases.
      doTasks(
          "EACH",
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
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
              "$DoubleDown FROM PreludeCard",
              "CopyPrelude<$IndustrialComplex>",
              "-18 MC",
          )
          // Expand the copied production box into its separate resource increases.
          doTasks(
              "EACH",
              "PROD[Steel]",
          )
        }
        .expect("PROD[0 MC, Steel, 0 Titanium, 0 Plant, 0 Energy, 0 Heat]")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Copies a played New Partner without Prelude setup`() {
      newTestGame(addOptions = "$ValleyTrust, prelude:cards", kimCorporation = ValleyTrust)
      admin.count("PreludeExpansion") shouldBe 0
      kim.count("PreludeCard") shouldBe 0
      kim.completeOperation {
            doTasks(
                "DoRequiredActions",
                "3 PreludeCard<Selecting>",
                "PlayCard<Class<PreludeCard>, Class<$NewPartner>>",
                "$NewPartner FROM PreludeCard<Selecting>",
                "PROD[MC]",
                "2 PreludeCard<Selecting>",
            )

            doTasks(
                "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
                "$DoubleDown FROM PreludeCard<Selecting>",
                "CopyPrelude<$NewPartner>",
                "PROD[MC]",
                "2 PreludeCard<Selecting>",
            )

            doTasks(
                "PlayCard<Class<PreludeCard>, Class<$Donation>>",
                "$Donation FROM PreludeCard<Selecting>",
                "21 MC",
            )
          }
          .expect("-RequiredAction, 0 PreludeCard<Selecting>")
      kim.count("PreludeCard") shouldBe 0
    }

    @Test
    internal fun `Copying Preservation Program adds TR without another Action phase skip`() {
      newTestGame("PreludeExpansion, $PreservationProgram", playerCount = 2)
      kim.completeOperation {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$PreservationProgram>>",
            "$PreservationProgram FROM PreludeCard",
            "5 TerraformRating",
        )

        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
            "$DoubleDown FROM PreludeCard",
            "CopyPrelude<$PreservationProgram>",
            "5 TerraformRating",
        )
      }
      stan.completeOperation {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
            "$Supplier FROM PreludeCard",
            "PROD[2 Energy]",
            "4 Steel",
        )

        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>",
            "$MetalsCompany FROM PreludeCard",
            "PROD[MC]",
            "PROD[Steel]",
            "PROD[Titanium]",
        )
      }
      kim.turn {
        doTasks(
            *playCardTasks("$Comet"),
            "21 Pay<> FROM MC",
            "$Comet FROM ProjectCard",
            "TemperatureStep",
            "TerraformRating",
            "OceanTile<Tharsis_1_2>",
            "TerraformRating",
            "2 Steel",
            "Ok",
        ) // No plant attack.
      }
      kim.count("TerraformRating") shouldBe 31 // Of Comet's two TR, exactly the first was reversed.
    }
  }
}
