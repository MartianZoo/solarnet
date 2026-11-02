package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Complete database replay: Cosmic Proton Trace (g26b33667e4ba).
// Source log and retained saves: _local/replays/CosmicProtonTrace/.
// http://newazure.local:8080/the-end?id=pc6e0fc6151eb
internal class CosmicProtonTraceTest : CardTrackingFullGameTest() {
  // Ecology Experts is excluded by default; its three incompatible cards never enter a saved hand.
  override val config =
      GameConfig(
          """
      AmazonisMap, VenusNextExpansion, PreludeExpansion, promos:cards
      EcologyExperts, -ViralEnhancers, -EcologicalZone, -Decomposers
      1 SelectableCorporationCount
      Fundraiser, Thawer, Sponsor, Researcher, Metallurgist, Planetologist
      Thermalist, Highlander, Industrialist, Forecaster, Administrator, Celebrity
      """
      )

  override val playerClassPets =
      """
      CLASS Blue : Player { SetupPhase: 3 TerraformRating<This> }
      CLASS Yellow : Player
      CLASS Green : Player { SetupPhase: TerraformRating<This> }
      CLASS Purple : Player { SetupPhase: 5 TerraformRating<This> }
      """
          .trimIndent()

  override val cardArrivalOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(
                  cn("Blue") to listOf(UtopiaInvest, Inventrix, Ecoline),
                  cn("Yellow") to listOf(PhoboLog, Recyclon, CrediCor),
                  cn("Green") to listOf(AstroDrill, Viron, ThorGate),
                  cn("Purple") to listOf(RobinsonIndustries, Vitor, TychoMagnetics),
              ),
          cn("PreludeCard") to
              mapOf(
                  cn("Blue") to
                      listOf(
                          SelfSufficientSettlement,
                          HugeAsteroid,
                          EcologyExperts,
                          PolarIndustries,
                      ),
                  cn("Yellow") to
                      listOf(
                          GiantSolarCollector,
                          SocietySupport,
                          AcquiredSpaceAgency,
                          GreatAquifer,
                      ),
                  cn("Green") to
                      listOf(
                          EarlySettlement,
                          ExperimentalForest,
                          MiningOperations,
                          MetalRichAsteroid,
                      ),
                  cn("Purple") to
                      listOf(
                          AntiDesertificationTechniques,
                          EstablishedMethods,
                          UnmiContractor,
                          Biofuels,
                      ),
              ),
      )
  override val cardDepartureOrder =
      mapOf(
          cn("CorporationCard") to
              mapOf(
                  cn("Blue") to listOf(UtopiaInvest, Inventrix),
                  cn("Yellow") to listOf(PhoboLog, Recyclon),
                  cn("Green") to listOf(Viron, ThorGate),
                  cn("Purple") to listOf(Vitor, TychoMagnetics),
              ),
          cn("PreludeCard") to
              mapOf(
                  cn("Blue") to listOf(SelfSufficientSettlement, PolarIndustries),
                  cn("Yellow") to listOf(GiantSolarCollector, AcquiredSpaceAgency),
                  cn("Green") to listOf(EarlySettlement, ExperimentalForest),
                  cn("Purple") to listOf(EstablishedMethods, Biofuels),
              ),
      )

  private val purple
    get() = player(4)

  @Test
  fun completeGame() {
    startGame()
    listOf(p1, p2, p3, purple).forEach { it.doTask("20 TerraformRating") }
    p1.doTask("3 TerraformRating")
    p3.doTask("TerraformRating")
    purple.doTask("5 TerraformRating")
    p1.doTasks(
        "3 CorporationCard",
        "-2 CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-6 ProjectCard<Selecting>",
    )
    p2.doTasks(
        "3 CorporationCard",
        "-2 CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-6 ProjectCard<Selecting>",
    )
    p3.doTasks(
        "3 CorporationCard",
        "-2 CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-4 ProjectCard<Selecting>",
    )
    purple.doTasks(
        "3 CorporationCard",
        "-2 CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-3 ProjectCard<Selecting>",
    )
    with(p1) {
      draw(MarsNomads, AdvancedAlloys, Algae, ArtificialPhotosynthesis)
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Ecoline>>",
          "$Ecoline FROM CorporationCard",
          "36 MC",
          "PROD[2 Plant]",
          "3 Plant",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p2) {
      draw(ReleaseOfInertGases, MoholeArea, ProtectedValley, GhgImportFromVenus)
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$CrediCor>>",
          "$CrediCor FROM CorporationCard",
          "57 MC",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      draw(
          HomeostasisBureau,
          IndustrialCenter,
          FieldCappedCity,
          ExtractorBalloons,
          OreProcessor,
          MercurianAlloys,
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$AstroDrill>>",
          "$AstroDrill FROM CorporationCard",
          "35 MC",
          "3 Asteroid<$AstroDrill>",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(
          ColonizerTrainingCamp,
          StaticHarvesting,
          TransNeptuneProbe,
          ConvoyFromEuropa,
          Lichen,
          ForcedPrecipitation,
          FoodFactory,
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$RobinsonIndustries>>",
          "$RobinsonIndustries FROM CorporationCard",
          "47 MC",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p1) {
      doTask("PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>")
      doTask("$EcologyExperts FROM PreludeCard")
      doTask("PROD[Plant]")
      doTask("PlayCard<Class<$Algae>>")
      selectTask("-Required* / Required")
      doTasks("10 Pay<> FROM MC", "$Algae FROM ProjectCard", "PROD[2 Plant]", "Plant")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$HugeAsteroid>>",
          "$HugeAsteroid FROM PreludeCard",
          "3 TemperatureStep",
          "3 TerraformRating",
          "PROD[Heat]",
          "-5 MC",
      )
      repeat(3) { doTask("ThawerCredit") }
    }
    with(p2) {
      draw(Capital, PublicBaths)
      doTask("PlayCard<Class<PreludeCard>, Class<$GreatAquifer>>")
      doTask("$GreatAquifer FROM PreludeCard")
      doTask("OceanTile<Amazonis_08_04>")
      doTask("2 ProjectCard")
      doTask("TerraformRating")
      doTask("OceanTile<Amazonis_07_04>")
      doTask("2 Plant")
      doTask("2 MC")
      doTask("TerraformRating")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SocietySupport>>",
          "$SocietySupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[Plant]",
          "PROD[Energy]",
          "PROD[Heat]",
      )
    }
    with(p3) {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MetalRichAsteroid>>",
          "$MetalRichAsteroid FROM PreludeCard",
          "4 Steel",
          "4 Titanium",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("ThawerCredit")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MiningOperations>>",
          "$MiningOperations FROM PreludeCard",
          "PROD[2 Steel]",
          "4 Steel",
      )
    }
    with(purple) {
      draw(SpaceMirrors)
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard",
          "ProjectCard",
          "3 TerraformRating",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AntiDesertificationTechniques>>",
          "$AntiDesertificationTechniques FROM PreludeCard",
          "PROD[Steel]",
          "PROD[Plant]",
          "3 MC",
      )
    }
    generation1()
    generation2()
    generation3()
    generation4()
    generation5()
    generation6()
    generation7()
    generation8()
  }

  private fun generation1() {
    p1.doTask("Pass")
    with(p2) {
      doTasks(
          *playCardTasks("$MoholeArea"),
          "20 Pay<> FROM MC",
          "$MoholeArea FROM ProjectCard",
          "PROD[4 Heat]",
          "4 MC",
          "MoholeArea_SpecialTile<Amazonis_09_04>",
          "Steel",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*playCardTasks("$HomeostasisBureau"), "8 Pay<Class<Steel>> FROM Steel")
      doTask("Ok") // no MC paid
      doTasks("$HomeostasisBureau FROM ProjectCard", "PROD[2 Heat]")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$FoodFactory"),
          "12 Pay<> FROM MC",
          "$FoodFactory FROM ProjectCard",
          "PROD[4 MC]",
          "PROD[-Plant]",
      )
      doTasks(
          *playCardTasks("$ColonizerTrainingCamp"),
          "8 Pay<> FROM MC",
          "$ColonizerTrainingCamp FROM ProjectCard",
      )
    }
    with(p2) {
      doTasks(
          *playCardTasks("$ProtectedValley"),
          "Pay<Class<Steel>> FROM Steel",
          "21 Pay<> FROM MC",
          "$ProtectedValley FROM ProjectCard",
          "PROD[2 MC]",
          "4 MC",
          "GreeneryTile<Amazonis_02_07>",
          "2 Titanium",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      doTask("ThawerCredit")
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$StaticHarvesting"),
          "5 Pay<> FROM MC",
          "$StaticHarvesting FROM ProjectCard",
          "PROD[Energy]",
          "2 MC",
      )
      doTask("PassBaton")
    }
    p2.doTask("Pass")
    p3.doTask("Pass")
    with(purple) {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Titanium]")
      doTask("Pass")
    }
    p1.doTasks("26 MC", "5 Plant", "Heat")
    p2.doTasks("25 MC", "Plant", "Energy", "6 Heat")
    p3.doTasks("22 MC", "2 Steel", "2 Heat")
    purple.doTasks("32 MC", "Steel", "Energy", "Titanium")
    p1.doTask("VenusStep! BY Admin")
  }

  private fun generation2() {
    with(p2) {
      draw(NuclearPower, AquiferPumping)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      draw(AsteroidCard, Research)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(BusinessNetwork, Fish, Grass)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p1) {
      draw(FuelFactory, GeothermalPower, Bushes)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p2) {
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("PassBaton")
    }
    with(p3) {
      draw(EosChasmaNationalPark, Omnicourt)
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$ForcedPrecipitation"),
          "8 Pay<> FROM MC",
          "$ForcedPrecipitation FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_08_05>",
          "Plant",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    p2.doTask("Pass")
    with(p3) {
      doTasks(
          *playCardTasks("$MercurianAlloys"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$MercurianAlloys FROM ProjectCard",
      ) // no MC paid
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$Lichen"),
          "7 Pay<> FROM MC",
          "$Lichen FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$MarsNomads"),
          "13 Pay<> FROM MC",
          "$MarsNomads FROM ProjectCard",
          "NomadsMarker<Amazonis_11_11>",
      )
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Amazonis_10_11 FROM Amazonis_11_11>",
          "2 Steel",
      )
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Heat]")
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$GeothermalPower"),
          "2 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "2 Titanium",
          "3 MC",
          "-3 Plant<Blue>",
      ) // no MC paid
      doTask("ThawerCredit")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$SpaceMirrors"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$SpaceMirrors FROM ProjectCard",
      ) // no MC paid
      doTask("PassBaton")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$FuelFactory"),
          "6 Pay<> FROM MC",
          "$FuelFactory FROM ProjectCard",
          "PROD[MC]",
          "PROD[Titanium]",
          "PROD[-Energy]",
      )
      doTask("PassBaton")
    }
    p3.doTask("Pass")
    purple.doTask("Pass")
    p1.doTask("Pass")
    p1.doTasks("28 MC", "5 Plant", "Heat", "Energy", "Titanium")
    p2.doTasks("27 MC", "Plant", "Heat FROM Energy", "Energy", "6 Heat")
    p3.doTasks("23 MC", "2 Steel", "2 Heat")
    purple.doTasks("32 MC", "Steel", "Heat FROM Energy", "Energy", "Titanium", "Plant", "Heat")
    p2.doTask("VenusStep! BY Admin")
    // Database save 108: generation 3, before Research purchases.
    p1.assertResources(m = 32, s = 0, t = 1, p = 5, e = 1, h = 2)
    p1.assertProduction(m = 1, s = 0, t = 1, p = 5, e = 1, h = 1)
    p2.assertResources(m = 34, s = 0, t = 3, p = 4, e = 1, h = 13)
    p2.assertProduction(m = 1, s = 0, t = 0, p = 1, e = 1, h = 6)
    p3.assertResources(m = 48, s = 4, t = 7, p = 0, e = 0, h = 4)
    p3.assertProduction(m = 0, s = 2, t = 0, p = 0, e = 0, h = 2)
    purple.assertResources(m = 36, s = 2, t = 1, p = 1, e = 1, h = 2)
    purple.assertProduction(m = 4, s = 1, t = 1, p = 1, e = 1, h = 1)
  }

  private fun generation3() {
    with(p2) {
      draw(LocalShading, DirectedImpactors)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p1) {
      draw(ProjectInspection, VestaShipyard)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      draw(ImportedNutrients, ArcticAlgae)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(Comet, WaterSplittingPlant)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      doTasks(
          *playCardTasks("$ArcticAlgae"),
          "12 Pay<> FROM MC",
          "$ArcticAlgae FROM ProjectCard",
          "Plant",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Titanium]")
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Amazonis_09_11 FROM Amazonis_10_11>",
          "Titanium",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$DirectedImpactors"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$DirectedImpactors FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill"), "Steel")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$Comet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "18 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Amazonis_06_05>",
          "2 Plant",
          "TerraformRating",
          "-3 Plant<Blue>",
      )
      doTask("ThawerCredit")
      p3.doTask("2 Plant") // Arctic Algae after Comet's ocean
      doTask("PassBaton")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "Asteroid<$DirectedImpactors>",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *playCardTasks("$FieldCappedCity"),
          "5 Pay<Class<Steel>> FROM Steel",
          "19 Pay<> FROM MC",
          "$FieldCappedCity FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[Energy]",
          "3 Plant",
          "CityTile<Amazonis_07_03>",
          "2 Plant",
          "4 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_06_03>",
          "2 Plant",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
      )
    }
    with(purple) {
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "4 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
      )
      doTask("PassBaton")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(*playCardTasks("$LocalShading"), "4 Pay<> FROM MC", "$LocalShading FROM ProjectCard")
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *playCardTasks("$IndustrialCenter"),
          "4 Pay<> FROM MC",
          "$IndustrialCenter FROM ProjectCard",
          "IndustrialCenter_SpecialTile<Amazonis_08_03>",
          "Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }
    purple.doTask("Pass")
    p1.doTask("Pass")
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*cardActionTasks("$IndustrialCenter"), "7 Pay<> FROM MC", "PROD[Steel]")
      doTask("PassBaton")
    }
    p2.doTask("Pass")
    p3.doTask("Pass")
    p1.doTasks("28 MC", "5 Plant", "Heat FROM Energy", "Heat", "Energy", "2 Titanium")
    p2.doTasks("27 MC", "Plant", "Heat FROM Energy", "Energy", "6 Heat")
    p3.doTasks("26 MC", "3 Steel", "2 Heat", "Energy")
    purple.doTasks("33 MC", "Steel", "Heat FROM Energy", "Energy", "2 Titanium", "Plant", "Heat")
    p3.doTasks("OceanTile<Amazonis_04_02>! BY Admin", "2 Plant")
    // Database save 174: generation 4, before Research purchases.
    p1.assertResources(m = 38, s = 0, t = 2, p = 7, e = 1, h = 4)
    p1.assertProduction(m = 1, s = 0, t = 2, p = 5, e = 1, h = 1)
    p2.assertResources(m = 46, s = 0, t = 0, p = 5, e = 1, h = 20)
    p2.assertProduction(m = 1, s = 0, t = 0, p = 1, e = 1, h = 6)
    p3.assertResources(m = 34, s = 3, t = 7, p = 5, e = 1, h = 6)
    p3.assertProduction(m = 2, s = 3, t = 0, p = 0, e = 1, h = 2)
    purple.assertResources(m = 35, s = 3, t = 2, p = 4, e = 1, h = 4)
    purple.assertProduction(m = 3, s = 1, t = 2, p = 1, e = 1, h = 1)
  }

  private fun generation4() {
    with(p2) {
      draw(MarsUniversity, IceAsteroid)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      draw(QuantumExtractor, IoMiningIndustries)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p1) {
      draw(BigAsteroid, RegoPlastics, MiningArea)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(NaturalPreserve, SubCrustMeasurements)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      draw(DeimosDownPromo)
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // keep the revealed card
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "19 Pay<> FROM MC",
          "$BigAsteroid FROM ProjectCard",
          "2 TemperatureStep",
          "2 TerraformRating",
          "4 Titanium",
          "PROD[Plant]",
          "-4 Plant<Green>",
      )
      repeat(2) { doTask("ThawerCredit") }
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Thawer")
    }
    with(p2) {
      draw(CarbonNanosystems)
      doTasks(
          *playCardTasks("$GhgImportFromVenus"),
          "23 Pay<> FROM MC",
          "$GhgImportFromVenus FROM ProjectCard",
          "PROD[3 Heat]",
          "VenusStep",
          "ProjectCard",
          "TerraformRating",
          "4 MC",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$NaturalPreserve"),
          "3 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$NaturalPreserve FROM ProjectCard",
          "PROD[MC]",
          "NaturalPreserve_SpecialTile<Amazonis_10_09>",
          "3 Plant",
      )
      doTask("PassBaton")
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_05>",
          "2 Energy",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *playCardTasks("$IoMiningIndustries"),
          "10 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$IoMiningIndustries FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Titanium]",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Steel]")
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$TransNeptuneProbe FROM ProjectCard",
      ) // no MC paid
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Amazonis_10_11 FROM Amazonis_09_11>",
          "2 Steel",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*cardActionTasks("$IndustrialCenter"), "7 Pay<> FROM MC", "PROD[Steel]")
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Metallurgist")
    }
    with(purple) {
      draw(CarbonateProcessing)
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "15 Pay<> FROM MC",
          "$ConvoyFromEuropa FROM ProjectCard",
          "ProjectCard",
          "OceanTile<Amazonis_06_06>",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
      p3.doTask("2 Plant") // Arctic Algae
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_10_10>",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
      )
    }
    p1.doTask("Pass")
    with(p2) {
      doTasks(
          *cardActionTasks("$DirectedImpactors"),
          "6 Pay<> FROM MC",
          "Asteroid<$DirectedImpactors>",
      )
      doTask("PassBaton")
    }
    p3.doTask("Pass")
    purple.doTask("Pass")
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTasks(
          *playCardTasks("$NuclearPower"),
          "10 Pay<> FROM MC",
          "$NuclearPower FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Energy]",
      )
      doTask("Pass")
    }
    p1.doTasks("31 MC", "6 Plant", "3 Heat FROM Energy", "Heat", "Energy", "2 Titanium")
    p2.doTasks("29 MC", "Plant", "Heat FROM Energy", "4 Energy", "9 Heat")
    p3.doTasks("28 MC", "4 Steel", "Heat FROM Energy", "2 Heat", "Energy", "2 Titanium")
    purple.doTasks("37 MC", "2 Steel", "Heat FROM Energy", "Energy", "2 Titanium", "Plant", "Heat")
    purple.doTask("TemperatureStep! BY Admin")
    // Database save 239: generation 5, before Research purchases.
    p1.assertResources(m = 37, s = 2, t = 6, p = 6, e = 1, h = 8)
    p1.assertProduction(m = 1, s = 0, t = 2, p = 6, e = 1, h = 1)
    p2.assertResources(m = 34, s = 0, t = 0, p = 6, e = 4, h = 14)
    p2.assertProduction(m = 0, s = 0, t = 0, p = 1, e = 4, h = 9)
    p3.assertResources(m = 40, s = 7, t = 2, p = 3, e = 1, h = 9)
    p3.assertProduction(m = 4, s = 4, t = 2, p = 0, e = 1, h = 2)
    purple.assertResources(m = 43, s = 2, t = 2, p = 4, e = 1, h = 6)
    purple.assertProduction(m = 4, s = 2, t = 2, p = 1, e = 1, h = 1)
  }

  private fun generation5() {
    with(p2) {
      draw(InventionContest)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(TerraformingContract, CeosFavoriteProject, Hackers)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p1) {
      draw(AsteroidDeflectionSystem, BribedCommittee, RobotPollinators)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      draw(FueledGenerators, GiantIceAsteroid)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$RegoPlastics"),
          "2 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$RegoPlastics FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
              *playCardTasks("$Capital"),
              "26 Pay<> FROM MC",
              "$Capital FROM ProjectCard",
              "PROD[5 MC]",
              "PROD[-2 Energy]",
              "4 MC",
              "CapitalTile<Amazonis_06_04>",
              "2 Plant",
              "4 MC",
          )
          .expect("-18 MC") // payment, CrediCor rebate, and ocean adjacency
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Sponsor")
    }
    with(p3) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
      )
      draw(TundraFarming, Shuttles)
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "28 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "2 TemperatureStep",
          "2 TerraformRating",
          "6 MC",
          "OceanTile<Amazonis_05_05>",
          "ProjectCard",
          "4 MC",
          "OceanTile<Amazonis_03_01>",
          "2 Steel",
          "2 MC",
          "OceanTile<Amazonis_06_11>",
          "ProjectCard",
          "-6 Plant<Yellow>",
      )
      doTask("2 Plant")
      doTask("2 Plant")
      doTask("2 Plant")
      repeat(3) { doTask("TerraformRating") }
    }
    with(purple) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      ) // no cards bought
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$AsteroidDeflectionSystem"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$AsteroidDeflectionSystem FROM ProjectCard",
          "PROD[-Energy]",
      )
    }
    with(p2) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    with(p3) {
      doTasks(
          *playCardTasks("$EosChasmaNationalPark"),
          "8 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$EosChasmaNationalPark FROM ProjectCard",
          "Ok", // no animal holder
          "3 Plant",
          "PROD[2 MC]",
      ) // no MC paid; no animal holder
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "14 Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "Ok", // no microbe holder
      ) // no microbe holder
    }
    with(purple) {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Energy]")
      doTasks(
          *playCardTasks("$Grass"),
          "11 Pay<> FROM MC",
          "$Grass FROM ProjectCard",
          "PROD[Plant]",
          "3 Plant",
      )
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "Ok", // no space-card reward
          "-ProjectCard<Revealed>",
      ) // Harvest is not a space card
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }
    with(p3) {
      draw(PowerInfrastructure)
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_02>",
          "Plant",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_06_02>",
          "Plant",
          "OxygenStep",
          "TerraformRating",
          "ProjectCard",
      )
    }
    with(purple) {
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "1 Pay<> FROM MC",
          "$CeosFavoriteProject FROM ProjectCard",
          "Floater<$ForcedPrecipitation>",
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$Bushes"),
          "10 Pay<> FROM MC",
          "$Bushes FROM ProjectCard",
          "PROD[2 Plant]",
          "2 Plant",
      )
      doTask("PassBaton")
    }
    p2.doTask("Pass")
    with(p3) {
      doTasks(
          *playCardTasks("$FueledGenerators"),
          "Ok", // no steel paid
          "1 Pay<> FROM MC",
          "$FueledGenerators FROM ProjectCard",
          "PROD[-MC]",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$Fish"),
          "9 Pay<> FROM MC",
          "$Fish FROM ProjectCard",
          "PROD[-Plant<Blue>]",
      )
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_06>",
          "Energy",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill"), "Asteroid<$AstroDrill>")
      doTask("PassBaton")
    }
    purple.doTask("Pass")
    with(p1) {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Amazonis_09_11 FROM Amazonis_10_11>",
          "Titanium",
      )
      doTask("PassBaton")
    }
    p3.doTask("Pass")
    with(p1) {
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "9 Pay<> FROM MC",
          "$RobotPollinators FROM ProjectCard",
          "PROD[Plant]",
          "4 Plant",
      )
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("Pass")
    }
    p1.doTasks("35 MC", "8 Plant", "2 Heat FROM Energy", "Heat", "2 Titanium")
    p2.doTasks("36 MC", "Plant", "4 Heat FROM Energy", "2 Energy", "9 Heat")
    p3.doTasks("37 MC", "4 Steel", "Heat FROM Energy", "2 Heat", "2 Energy", "2 Titanium")
    purple.doTasks(
        "37 MC",
        "2 Steel",
        "Heat FROM Energy",
        "3 Energy",
        "2 Titanium",
        "2 Plant",
        "Heat",
    )
    p1.doTask("VenusStep! BY Admin")
    // Database save 315: generation 6, before Research purchases.
    p1.assertResources(m = 36, s = 0, t = 6, p = 13, e = 0, h = 3)
    p1.assertProduction(m = 1, s = 0, t = 2, p = 8, e = 0, h = 1)
    p2.assertResources(m = 41, s = 0, t = 0, p = 3, e = 2, h = 19)
    p2.assertProduction(m = 5, s = 0, t = 0, p = 1, e = 2, h = 9)
    p3.assertResources(m = 43, s = 5, t = 2, p = 2, e = 2, h = 4)
    p3.assertProduction(m = 5, s = 4, t = 2, p = 0, e = 2, h = 2)
    purple.assertResources(m = 37, s = 4, t = 4, p = 9, e = 3, h = 8)
    purple.assertProduction(m = 4, s = 2, t = 2, p = 2, e = 3, h = 1)
  }

  private fun generation6() {
    with(p2) {
      draw(UrbanizedArea, CuttingEdgeTechnology, DomedCrater, Cartel)
      doTasks(
          "4 ProjectCard<Selecting>",
          "Ok", // no cards discarded
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      ) // no cards discarded
    }
    p1.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    with(p3) {
      draw(KelpFarming, ImportedNitrogen, SolarLogistics)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(RedShips, IshtarMining)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p2) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill", "Action2"), "-Asteroid<$AstroDrill>", "3 Titanium")
      doTasks(
          *playCardTasks("$Shuttles"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$Shuttles FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[-Energy]",
      )
    }
    with(purple) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_08_06>",
          "PROD[MC]",
          "Energy",
      )
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "Ok", // no space-card reward
          "-ProjectCard<Revealed>",
      ) // Vermin is not a space card
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$DirectedImpactors", "Action2"),
          "-Asteroid<$DirectedImpactors>",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$IceAsteroid"),
          "23 Pay<> FROM MC",
          "$IceAsteroid FROM ProjectCard",
          "4 MC",
          "OceanTile<Amazonis_05_10>",
          "2 Plant",
          "2 MC",
          "TerraformRating",
          "OceanTile<Amazonis_02_06>",
          "2 Plant",
          "TerraformRating",
      )
      p3.doTask("2 Plant") // Arctic Algae
      p3.doTask("2 Plant") // Arctic Algae
    }
    with(p3) {
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_05_02>",
          "PROD[MC]",
          "Plant",
          "2 MC",
      )
    }
    with(purple) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_09_06>",
          "Titanium",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Heat]")
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_07_07>",
          "PROD[MC]",
          "2 Energy",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_06_07>",
          "Steel",
          "2 Plant",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
      )
    }
    with(p2) {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Thermalist")
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *playCardTasks("$PowerInfrastructure"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$PowerInfrastructure FROM ProjectCard",
      ) // no MC paid
      doTasks(*cardActionTasks("$PowerInfrastructure"), "-2 Energy", "2 MC")
    }
    with(purple) {
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
      doTasks(*playCardTasks("$RedShips"), "2 Pay<> FROM MC", "$RedShips FROM ProjectCard")
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_08_08>",
          "Plant",
          "OxygenStep",
          "TerraformRating",
          "TemperatureStep",
      )
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Amazonis_08_10 FROM Amazonis_09_11>",
          "Steel",
      )
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }
    p3.doTask("Pass")
    with(purple) {
      doTasks(*cardActionTasks("$RedShips"), "6 MC")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$ArtificialPhotosynthesis"),
          "12 Pay<> FROM MC",
          "$ArtificialPhotosynthesis FROM ProjectCard",
          "PROD[2 Energy]",
      )
      doTask("PassBaton")
    }
    p2.doTask("Pass")
    with(purple) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$IshtarMining"),
          "5 Pay<> FROM MC",
          "$IshtarMining FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
    p1.doTask("Pass")
    purple.doTask("Pass")
    p1.doTasks("38 MC", "8 Plant", "2 Heat FROM Energy", "Heat", "2 Titanium", "2 Energy")
    p2.doTasks("42 MC", "Plant", "2 Heat FROM Energy", "2 Energy", "9 Heat")
    p3.doTasks("40 MC", "4 Steel", "2 Heat", "Energy", "2 Titanium")
    purple.doTasks(
        "41 MC",
        "2 Steel",
        "4 Heat FROM Energy",
        "3 Energy",
        "3 Titanium",
        "2 Plant",
        "2 Heat",
    )
    p2.doTask("OceanTile<Amazonis_03_06>! BY Admin")
    p3.doTask("2 Plant") // Arctic Algae
    // Database save 384: generation 7, before Research purchases.
    p1.assertResources(m = 41, s = 2, t = 8, p = 10, e = 2, h = 6)
    p1.assertProduction(m = 2, s = 0, t = 2, p = 8, e = 2, h = 1)
    p2.assertResources(m = 46, s = 0, t = 0, p = 8, e = 2, h = 14)
    p2.assertProduction(m = 6, s = 0, t = 0, p = 1, e = 2, h = 9)
    p3.assertResources(m = 45, s = 7, t = 4, p = 9, e = 1, h = 6)
    p3.assertProduction(m = 8, s = 4, t = 2, p = 0, e = 1, h = 2)
    purple.assertResources(m = 42, s = 6, t = 8, p = 3, e = 3, h = 6)
    purple.assertProduction(m = 5, s = 2, t = 3, p = 2, e = 3, h = 2)
  }

  private fun generation7() {
    with(p3) {
      draw(DustSeals)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p2) {
      draw(LandClaim, SpaceStation)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-2 ProjectCard<Selecting>",
          "6 Pay<> FROM MC",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    p1.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    with(purple) {
      draw(AdvancedEcosystems, Dirigibles, MartianLumberCorp)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
          "9 Pay<> FROM MC",
          "3 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_04_01>",
          "Titanium",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_05_07>",
          "PROD[MC]",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_06>",
          "Plant",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
    }
    with(p2) {
      doTasks(
          *playCardTasks("$LandClaim"),
          "1 Pay<> FROM MC",
          "$LandClaim FROM ProjectCard",
          "Community<Amazonis_09_09>",
      )
      doTasks(
          *playCardTasks("$CarbonNanosystems"),
          "14 Pay<> FROM MC",
          "$CarbonNanosystems FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
      )
    }
    with(p3) {
      doTasks(*cardActionTasks("$PowerInfrastructure"), "-Energy", "MC")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Steel]")
      doTasks(*cardActionTasks("$RedShips"), "6 MC")
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "Ok", // no space-card reward
          "-ProjectCard<Revealed>",
      ) // Immigrant City is not a space card
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Amazonis_07_10 FROM Amazonis_08_10>",
          "Plant",
      )
    }
    with(p2) {
      discard(DomedCrater)
      draw(Casinos)
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "8 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$UrbanizedArea"),
          "2 PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>",
          "2 Pay<> FROM MC",
          "$UrbanizedArea FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[-Energy]",
          "NormalCityTile<Amazonis_05_03>",
          "Titanium",
          "2 MC",
      )
    }
    with(p3) {
      doTasks(
          *playCardTasks("$KelpFarming"),
          "17 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(
          *playCardTasks("$TerraformingContract"),
          "8 Pay<> FROM MC",
          "$TerraformingContract FROM ProjectCard",
          "PROD[4 MC]",
      )
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_04_07>",
          "2 Plant",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$MarsNomads, Action1>",
          "NomadsMarker<Amazonis_06_10 FROM Amazonis_07_10>",
          "Plant",
          "4 MC",
      )
    }
    with(p2) {
      discard(Casinos)
      draw(StandardTechnology, EnergyMarket)
      doTasks(
          *playCardTasks("$InventionContest"),
          "2 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
          "Graphene<$CarbonNanosystems>",
          "-ProjectCard",
          "ProjectCard",
      )
      discard(AquiferPumping)
      draw(Pets)
      doTasks(
          *playCardTasks("$CuttingEdgeTechnology"),
          "12 Pay<> FROM MC",
          "$CuttingEdgeTechnology FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
          "-ProjectCard",
          "ProjectCard",
      )
    }
    with(p3) {
      draw(NeutralizerFactory)
      doTasks(
          *playCardTasks("$ImportedNitrogen"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$ImportedNitrogen FROM ProjectCard",
          "TerraformRating",
          "4 Plant",
          "ProjectCard",
      ) // no MC paid
      selectTask("3* Microbe") // no eligible holder
      selectTask("2* Animal") // no eligible holder
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$MartianLumberCorp"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$MartianLumberCorp FROM ProjectCard",
          "PROD[Plant]",
      ) // no MC paid
      doTasks(
          *playCardTasks("$SubCrustMeasurements"),
          "3 Pay<Class<Plant>> FROM Plant",
          "3 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$SubCrustMeasurements FROM ProjectCard",
      )
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_04_05>",
          "PROD[MC]",
          "Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_04>",
          "Plant",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill"), "Plant")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_01>",
          "2 Steel",
          "OxygenStep",
          "TerraformRating",
      )
    }
    with(purple) {
      draw(CityParks)
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      discard(DeimosDownPromo, CarbonateProcessing)
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
    }
    p1.doTask("Pass")
    with(p2) {
      doTasks(
              *playCardTasks("$PublicBaths"),
              "4 Pay<> FROM MC",
              "$PublicBaths FROM ProjectCard",
              "6 MC",
          )
          .expect("2 MC") // discounted cost and card reward
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_06_01>",
          "PROD[MC]",
          "Plant",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Forecaster")
      doTasks(
          *playCardTasks("$Hackers"),
          "3 Pay<> FROM MC",
          "$Hackers FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[-Energy]",
          "PROD[-2 MC<Green>]",
      )
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }
    p3.doTask("Pass")
    purple.doTask("Pass")
    with(p2) {
      discard(Cartel)
      draw(Tardigrades)
      doTasks(
          *playCardTasks("$StandardTechnology"),
          "6 Pay<> FROM MC",
          "$StandardTechnology FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
          "-ProjectCard",
          "ProjectCard",
      )
      doTasks(*playCardTasks("$EnergyMarket"), "3 Pay<> FROM MC", "$EnergyMarket FROM ProjectCard")
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("Pass")
    }
    p1.doTasks("42 MC", "8 Plant", "2 Heat FROM Energy", "Heat", "2 Titanium", "2 Energy")
    p2.doTasks("45 MC", "Plant", "2 Heat FROM Energy", "9 Heat")
    p3.doTasks("44 MC", "4 Steel", "2 Heat", "Energy", "2 Titanium", "3 Plant")
    purple.doTasks(
        "47 MC",
        "3 Steel",
        "3 Heat FROM Energy",
        "2 Energy",
        "3 Titanium",
        "3 Plant",
        "2 Heat",
    )
    p3.doTask("OxygenStep! BY Admin")
    // Database save 474: generation 8, before Research purchases.
    p1.assertResources(m = 45, s = 2, t = 10, p = 12, e = 2, h = 9)
    p1.assertProduction(m = 4, s = 0, t = 2, p = 8, e = 2, h = 1)
    p2.assertResources(m = 53, s = 0, t = 1, p = 2, e = 0, h = 25)
    p2.assertProduction(m = 8, s = 0, t = 0, p = 1, e = 0, h = 9)
    p3.assertResources(m = 49, s = 13, t = 2, p = 4, e = 1, h = 8)
    p3.assertProduction(m = 9, s = 4, t = 2, p = 3, e = 1, h = 2)
    purple.assertResources(m = 52, s = 3, t = 11, p = 3, e = 2, h = 11)
    purple.assertProduction(m = 11, s = 3, t = 3, p = 3, e = 2, h = 2)
  }

  private fun generation8() {
    p1.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    with(p2) {
      draw(LavaTubeSettlement)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    with(p3) {
      draw(AqueductSystems)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(CallistoPenalMines)
      doTasks(
          "4 ProjectCard<Selecting>",
          "-3 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    with(purple) {
      draw(Meltworks)
      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_04_06>",
          "Plant",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "Ok", // no space-card reward
          "-ProjectCard<Revealed>",
      ) // Supercapacitors is not a space card
    }
    with(p2) {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Administrator")
      doTasks(
          *playCardTasks("$Pets"),
          "10 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
      )
    }
    with(p3) {
      draw(HiTechLab, Steelworks, UndergroundCity)
      doTasks(
          *playCardTasks("$AqueductSystems"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$AqueductSystems FROM ProjectCard",
          "3 SearchForCard<TagFilter<Class<BuildingTag>>>",
      )
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$CallistoPenalMines"),
          "8 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$CallistoPenalMines FROM ProjectCard",
          "PROD[3 MC]",
      ) // no MC paid
      doTask("PassBaton")
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Amazonis_05_09 FROM Amazonis_06_10>",
          "Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_03_05>",
          "Plant",
          "2 MC",
          "OxygenStep",
      )
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *playCardTasks("$UndergroundCity"),
          "9 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$UndergroundCity FROM ProjectCard",
          "CityTile<Amazonis_03_07>",
          "4 MC",
          "PROD[2 Steel]",
          "PROD[-2 Energy]",
      ) // no MC paid
      p2.doTask("Animal<$Pets>")
      doTask("PassBaton")
    }
    with(purple) {
      discard(WaterSplittingPlant, Meltworks)
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_06_09>",
          "PROD[MC]",
      )
      p2.doTask("Animal<$Pets>")
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "PROD[Energy]",
          "3 MC",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *playCardTasks("$NeutralizerFactory"),
          "7 Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(*cardActionTasks("$RedShips"), "9 MC")
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Amazonis_09_05>",
          "PROD[MC]",
          "2 MC",
      )
      p2.doTask("Animal<$Pets>")
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_06_08>",
          "Plant",
          "OxygenStep",
      )
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_04_08>",
          "Plant",
          "OxygenStep",
      )
      doTasks(*cardActionTasks("$PowerInfrastructure"), "-Energy", "MC")
    }
    with(purple) {
      doTasks(
          *playCardTasks("$AdvancedEcosystems"),
          "11 Pay<> FROM MC",
          "$AdvancedEcosystems FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    p1.doTask("Pass")
    with(p2) {
      discard(Tardigrades)
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(p3) {
      discard(
          ExtractorBalloons,
          OreProcessor,
          QuantumExtractor,
          TundraFarming,
          DustSeals,
          HiTechLab,
          Steelworks,
      )
      doTasks(*standardProjectTasks("SellPatentsProject"), "7 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$Omnicourt"),
          "11 Pay<> FROM MC",
          "$Omnicourt FROM ProjectCard",
          "2 TerraformRating",
      )
    }
    with(purple) {
      doTasks(*playCardTasks("$Dirigibles"), "11 Pay<> FROM MC", "$Dirigibles FROM ProjectCard")
      doTasks(*cardActionTasks("$Dirigibles"), "Floater<$ForcedPrecipitation>")
    }
    with(p2) {
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "PROD[Energy]",
          "3 MC",
      )
      doTask("PassBaton")
    }
    with(p3) {
      doTasks(*cardActionTasks("$AstroDrill"), "MC")
      doTask("PassBaton")
    }
    with(purple) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RobinsonIndustries"), "4 Pay<> FROM MC", "PROD[Heat]")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$SpaceStation"),
          "PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>",
          "Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$SpaceStation FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    p3.doTask("Pass")
    with(purple) {
      discard(CityParks)
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$LavaTubeSettlement"),
          "2 PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>",
          "7 Pay<> FROM MC",
          "$LavaTubeSettlement FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[-Energy]",
          "NormalCityTile<Amazonis_08_09>",
          "Animal<$Pets>",
      )
      doTask("PassBaton")
    }
    purple.doTask("Pass")
    p2.doTask("Pass")
    p1.doTasks("44 MC", "8 Plant", "2 Heat FROM Energy", "Heat", "2 Titanium", "2 Energy")
    p2.doTasks("48 MC", "Plant", "9 Heat")
    p3.doTasks("47 MC", "6 Steel", "2 Heat", "2 Titanium", "3 Plant")
    purple.doTasks(
        "52 MC",
        "3 Steel",
        "2 Heat FROM Energy",
        "2 Energy",
        "3 Titanium",
        "3 Plant",
        "3 Heat",
    )
    purple.selectTask("PassBaton") // no final greenery
    p1.doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "7 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Amazonis_05_08>",
        "Plant",
    )
    p1.selectTask("PassBaton") // no more greenery
    p2.selectTask("PassBaton") // no final greenery
    p3.doTasks(
        "UseAction<ConvertPlantsAction, Action1>",
        "8 Pay<Class<Plant>> FROM Plant",
        "GreeneryTile<Amazonis_03_08>",
    )
    p3.selectTask("PassBaton") // no more greenery
    p1.cardsHand shouldBe setOf(MiningArea)
    p2.cardsHand shouldBe emptySet()
    p3.cardsHand shouldBe emptySet()
    purple.cardsHand shouldBe emptySet()
    assertCardTrackingComplete()
    // Player-record evidence: final resources, production, rating, and score.
    p1.assertResources(m = 47, s = 2, t = 12, p = 4, e = 2, h = 12)
    p1.assertProduction(m = 5, s = 0, t = 2, p = 8, e = 2, h = 1)
    p1.assertCounts(39 to "TerraformRating", 74 to "VictoryPoint", 1 to "Victory")
    p2.assertResources(m = 51, s = 0, t = 0, p = 3, e = 0, h = 34)
    p2.assertProduction(m = 11, s = 0, t = 0, p = 1, e = 0, h = 9)
    p2.assertCounts(37 to "TerraformRating", 69 to "VictoryPoint", 0 to "Victory")
    p3.assertResources(m = 53, s = 6, t = 4, p = 0, e = 0, h = 10)
    p3.assertProduction(m = 9, s = 6, t = 2, p = 3, e = 0, h = 2)
    p3.assertCounts(38 to "TerraformRating", 73 to "VictoryPoint", 0 to "Victory")
    purple.assertResources(m = 64, s = 6, t = 6, p = 6, e = 2, h = 16)
    purple.assertProduction(m = 15, s = 3, t = 3, p = 3, e = 2, h = 3)
    purple.assertCounts(37 to "TerraformRating", 72 to "VictoryPoint", 0 to "Victory")
    // Player-record evidence: every occupied Mars area and the final global parameters.
    assertSidebar(gen = 8, temp = 14, oxygen = 18, oceans = 11, venus = 16)
    admin.assertCounts(
        1 to "OceanTile<Amazonis_02_06>",
        1 to "GreeneryTile<Yellow, Amazonis_02_07>",
        1 to "OceanTile<Amazonis_03_01>",
        1 to "GreeneryTile<Blue, Amazonis_03_05>",
        1 to "OceanTile<Amazonis_03_06>",
        1 to "CityTile<Green, Amazonis_03_07>",
        1 to "GreeneryTile<Green, Amazonis_03_08>",
        1 to "GreeneryTile<Green, Amazonis_04_01>",
        1 to "OceanTile<Amazonis_04_02>",
        1 to "CityTile<Blue, Amazonis_04_05>",
        1 to "GreeneryTile<Blue, Amazonis_04_06>",
        1 to "GreeneryTile<Blue, Amazonis_04_07>",
        1 to "GreeneryTile<Green, Amazonis_04_08>",
        1 to "GreeneryTile<Green, Amazonis_05_01>",
        1 to "CityTile<Green, Amazonis_05_02>",
        1 to "CityTile<Yellow, Amazonis_05_03>",
        1 to "GreeneryTile<Yellow, Amazonis_05_04>",
        1 to "OceanTile<Amazonis_05_05>",
        1 to "GreeneryTile<Blue, Amazonis_05_06>",
        1 to "CityTile<Blue, Amazonis_05_07>",
        1 to "GreeneryTile<Blue, Amazonis_05_08>",
        1 to "OceanTile<Amazonis_05_10>",
        1 to "CityTile<Green, Amazonis_06_01>",
        1 to "GreeneryTile<Green, Amazonis_06_02>",
        1 to "GreeneryTile<Green, Amazonis_06_03>",
        1 to "CapitalTile<Amazonis_06_04>",
        1 to "OceanTile<Amazonis_06_05>",
        1 to "OceanTile<Amazonis_06_06>",
        1 to "GreeneryTile<Blue, Amazonis_06_07>",
        1 to "GreeneryTile<Blue, Amazonis_06_08>",
        1 to "CityTile<Blue, Amazonis_06_09>",
        1 to "OceanTile<Amazonis_06_11>",
        1 to "GreeneryTile<Green, Amazonis_07_02>",
        1 to "CityTile<Green, Amazonis_07_03>",
        1 to "OceanTile<Amazonis_07_04>",
        1 to "GreeneryTile<Blue, Amazonis_07_05>",
        1 to "GreeneryTile<Blue, Amazonis_07_06>",
        1 to "CityTile<Blue, Amazonis_07_07>",
        1 to "IndustrialCenter_SpecialTile<Amazonis_08_03>",
        1 to "OceanTile<Amazonis_08_04>",
        1 to "GreeneryTile<Blue, Amazonis_08_05>",
        1 to "CityTile<Purple, Amazonis_08_06>",
        1 to "GreeneryTile<Blue, Amazonis_08_08>",
        1 to "CityTile<Yellow, Amazonis_08_09>",
        1 to "MoholeArea_SpecialTile<Amazonis_09_04>",
        1 to "CityTile<Purple, Amazonis_09_05>",
        1 to "GreeneryTile<Purple, Amazonis_09_06>",
        1 to "NaturalPreserve_SpecialTile<Amazonis_10_09>",
        1 to "GreeneryTile<Purple, Amazonis_10_10>",
    )
    p1.assertCounts(1 to "NomadsMarker<Amazonis_05_09>")
    p2.assertCounts(5 to "Animal<$Pets>")
    purple.assertCounts(4 to "Animal<$Fish>")
    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Blue>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Yellow>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Green>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Purple>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Blue>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Yellow>") shouldBe 10
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Purple>") shouldBe 10
    score.net("SecondPlace", "VictoryPoint<Blue>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Yellow>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Green>") shouldBe 2
    score.net("SecondPlace", "VictoryPoint<Purple>") shouldBe 2
    score.net("GreeneryTile", "VictoryPoint<Blue>") shouldBe 11
    score.net("GreeneryTile", "VictoryPoint<Yellow>") shouldBe 2
    score.net("GreeneryTile", "VictoryPoint<Green>") shouldBe 7
    score.net("GreeneryTile", "VictoryPoint<Purple>") shouldBe 2
    score.net("CityTile", "VictoryPoint<Blue>") shouldBe 14
    score.net("CityTile", "VictoryPoint<Yellow>") shouldBe 6
    score.net("CityTile", "VictoryPoint<Green>") shouldBe 14
    score.net("CityTile", "VictoryPoint<Purple>") shouldBe 6
    score.net("Card", "VictoryPoint<Blue>") shouldBe 0
    score.net("Card", "VictoryPoint<Yellow>") shouldBe 9
    score.net("Card", "VictoryPoint<Green>") shouldBe 7
    score.net("Card", "VictoryPoint<Purple>") shouldBe 15
  }
}
