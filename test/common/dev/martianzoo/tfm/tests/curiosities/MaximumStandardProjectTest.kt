package dev.martianzoo.tfm.tests.curiosities

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.replays.AbstractSoloTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** A legal solo opening built to maximize the MC gained during one standard-project action. */
internal class MaximumStandardProjectTest : AbstractSoloTest() {
  override val config =
      GameConfig(
          """
          HellasMap
          CorporateEraExpansion, PreludeExpansion, Prelude2CardPack, PromoCardPack
          TurmoilExpansion
          """,
          "Me",
      )

  override fun cityAreas(): Pair<String, String> = "Hellas_5_1" to "Hellas_8_4"

  override fun greeneryAreas(): Pair<String, String> = "Hellas_6_2" to "Hellas_9_5"

  override fun resolveExpansionSetupTasks() {
    admin.doTask("AquiferReleasedByPublicCouncil")
    admin.doTask("DryDeserts")
  }

  @Test
  internal fun `one greenery standard project can gain fifty one mc`() {
    me.playCorp(Spire, 10)

    me.turn {
      playPrelude(Merger) { me.playCorp(LakefrontResorts) }
      playPrelude(NewPartner) { playPrelude(BoardOfDirectors) }
    }

    me.count("MC") shouldBe 32

    me.turn {
      stdAction("DoRequiredActionsAction")
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(DoubleDown) {
          doTask("CopyPrelude<$Merger>")
          me.playCorp(CrediCor)
        }
      }
      playProject(MediaGroup, 6)
      playProject(OptimalAerobraking, 7)
      playProject(InvestmentLoan, 3)
      playProject(InventionContest, 2)
      playProject(TechnologyDemonstration, 5)
      playProject(BusinessContacts, 7)
      playProject(Virus, 1) { doTask("-5 Plant<SoloOpponent>") }
      playProject(Research, 11)
      playProject(StandardTechnology, 6)
    }

    me.pass()
    admin.doTask("Diversity")
    me.buyCards(3)

    me.turn {
      playProject(AsteroidCard, 14) { doTask("-3 Plant<SoloOpponent>") }
      playProject(EarthOffice, 1)
      playProject(SpaceHotels, 3, titanium = 2)
    }
    me.pass()
    me.doTask("OceanTile<Hellas_1_1> BY Admin")
    admin.doTask("VolcanicEruptions")
    me.buyCards(3)

    me.turn {
      playProject(AcquiredCompany, 7)
    }
    me.pass()
    admin.doTask("SponsoredProjects")
    me.buyCards(4)

    me.turn {
      playProject(Sponsors, 3)
      playProject(MediaArchives, 5)
    }
    me.pass()
    admin.doTask("GenerousFunding")
    me.buyCards(4)

    me.turn {
      playProject(HomeostasisBureau, 16)
    }
    me.pass()
    admin.doTask("InterplanetaryTradeGlobalEvent")
    me.buyCards(4)

    me.turn {
      playProject(ArcticAlgae, 12)
      playProject(ImportedGhg, 4)
      convertHeat()
      playProject(LavaFlows, 18) { doTask("LavaFlows_SpecialTile<Hellas_2_2>") }
    }
    me.pass()
    admin.doTask("CelebrityLeaders")
    me.buyCards(3)

    me.turn {
      convertHeat()
      playProject(BigAsteroid, 27) { doTask("-4 Plant<SoloOpponent>") }
      playProject(GiantIceAsteroid, 24, titanium = 4) {
        doTask("-Plant<SoloOpponent>")
        placeTile(4, 6)
        placeTile(4, 7)
      }
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(TerraformingDeal)
      }
    }
    me.pass()
    admin.doTask("SpinOffProducts")
    me.buyCards(4)

    me.turn {
      convertHeat()
      playProject(IceAsteroid, 23) {
        placeTile(5, 6)
        placeTile(5, 8)
      }
      playProject(Comet, 21) {
        doTask("-3 Plant<Me>")
        placeTile(6, 7)
      }
      playProject(TowingAComet, 23) { placeTile(6, 8) }
      convertHeat()
      convertPlants { placeTile(1, 2) }
      convertPlants { placeTile(1, 3) }
      cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        playPrelude(SuitableInfrastructure)
      }
    }
    me.pass()
    admin.doTask("Election")
    me.buyCards(4)

    me.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Greens>") }
      playProject(ProtectedValley, 11, steel = 6) { placeTile(2, 1) }
      playProject(Plantation, 15) { placeTile(1, 4) }
      playProject(Algae, 10)
      convertPlants { placeTile(2, 3) }
      playProject(Heather, 6)
      playProject(ArtificialLake, 13, steel = 1) { placeTile(2, 5) }
      stdProject(
          "AquiferProject",
          payment = {
            doTask("6 PayFromCard<Spire> FROM Science<Spire>")
            doTask("6 Pay<Class<MC>> FROM MC")
          },
      ) {
        placeTile(1, 1)
      }
      convertPlants { placeTile(1, 5) }
    }
    me.pass()
    admin.doTask("HomeworldSupport")
    me.buyCards(4)

    me.turn {
      playProject(MeatIndustry, 5)
      playProject(SpecialDesign, 4)
      playProject(Herbivores, 12) { doTask("PROD[-Plant<SoloOpponent>]") }
      playProject(RestrictedArea, 11) { placeTile(2, 6) }
    }

    me.count("Science<Spire>") shouldBe 12
    admin.count("TemperatureStep") shouldBe 14
    admin.count("OxygenStep") shouldBe 7
    admin.count("OceanTile") shouldBe 8
    admin.count("Ruling<Greens>") shouldBe 1
    val mcBefore = me.count("MC")

    // 27 ocean adjacency + 6 Terraforming Deal + 4 CrediCor + 4 Greens + 3 each
    // from Standard Technology and Homeostasis Bureau + 2 each from Suitable Infrastructure and
    // Meat Industry.
    me.autoExecPolicy = NONE
    me.stdProject(
            "GreeneryProject",
            payment = {
              doTask("4 MC", CrediCor)
              doTask("23 Owed<Class<MC>>", cn("GreeneryProject"))
              doTask(
                  "ActionBilling<GreeneryProject, Action1, Class<MC>>",
                  cn("GreeneryProject"),
              )
              doTask("12 PayFromCard<Spire> FROM Science<Spire>")
              // Twelve science are worth 24 MC; decline the unused MC tender after overpaying by
              // one.
              declineTask()
            },
        ) {
          doTask("3 MC", StandardTechnology)
          doTask("DefaultGreeneryTile")
          doTask("GreeneryTile<Hellas_3_6>")
          doTask("4 MC", cn("GreensPolicy"))
          doTask("2 Plant", cn("Hellas_3_6"))
          doTask("3 MC", LakefrontResorts)
          doTask("2 MC", cn("OceanTile"))
          doTask("2 MC", cn("OceanTile"))
          doTask("2 MC", cn("OceanTile"))
          doTask("Animal<Herbivores>", Herbivores)
          doTask("2 MC", MeatIndustry)
          doTask("OxygenStep", cn("GreeneryTile"))
          doTask("TerraformRating", cn("OxygenStep"))
          doTask("2 MC", TerraformingDeal)
          doTask("TemperatureStep", cn("StandardGpTrackRules"))
          doTask("3 MC", HomeostasisBureau)
          doTask("TerraformRating", cn("TemperatureStep"))
          doTask("2 MC", TerraformingDeal)
          doTask("OceanTile<Hellas_5_7>")
          doTask("3 Heat", cn("Hellas_5_7"))
          doTask("6 MC", LakefrontResorts)
          doTask("2 MC", cn("OceanTile"))
          doTask("2 MC", cn("OceanTile"))
          doTask("2 MC", cn("OceanTile"))
          doTask("2 MC", cn("OceanTile"))
          doTask("2 MC", cn("OceanTile"))
          doTask("2 MC", cn("OceanTile"))
          doTask("2 Plant", ArcticAlgae)
          doTask("TerraformRating", cn("OceanTile"))
          doTask("2 MC", TerraformingDeal)
          doTask("PROD[1 MC]", LakefrontResorts)
          // Suitable Infrastructure: 2 MC (automatic)
        }
        .expect("51 MC, OxygenStep, TemperatureStep, OceanTile, 3 TerraformRating")

    me.count("MC") shouldBe mcBefore + 51
  }
}
