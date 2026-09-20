package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val globalEventProbeDeclarations =
    parseClasses(
            """
            CLASS GlobalEventProbe : TagHolder { HAS MAX 1 This }
            CLASS PlayedEventProbe : EventCard { cost = 0 }
            CLASS ActiveEventProbe : ActiveCard, ResourceCard<Class<Animal>> {
              cost = 0
              MeasureInfluence:: Ok
            }
            CLASS EmptyResourceProbe : ActiveCard, ResourceCard<Class<Microbe>> {
              cost = 0
              MeasureInfluence:: Ok
            }
            CLASS FloaterEventProbe : ActiveCard, ResourceCard<Class<Floater>> {
              cost = 0
              MeasureInfluence:: Ok
            }
            CLASS OtherFloaterEventProbe : ActiveCard, ResourceCard<Class<Floater>> {
              cost = 0
              MeasureInfluence:: Ok
            }
            """
                .trimIndent()
        )
        .toSet()

internal class TurmoilEventsTest :
    CardTest(additionalClassDeclarations = globalEventProbeDeclarations) {
  @Test
  internal fun `setup reveals coming and distant events with their neutral delegates`() {
    newGame(TurmoilExpansion)

    admin.count("GlobalEvent") shouldBe 2
    admin.count("Coming<Class<AquiferReleasedByPublicCouncil>>") shouldBe 1
    admin.count("Distant<Class<DryDeserts>>") shouldBe 1
    admin.count("Current") shouldBe 0
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Reds, Neutral>") shouldBe 1
    admin.count("Dominant<MarsFirst>") shouldBe 1
    admin.count("Delegate<Neutral>") shouldBe 3
  }

  @Test
  internal fun `changing times advances events discards current and reveals the next card`() {
    newGame(TurmoilExpansion)

    admin.runOperation("ChangingTimes") { doTask("CelebrityLeaders") }

    admin.count("GlobalEvent") shouldBe 3
    admin.count("Current<Class<AquiferReleasedByPublicCouncil>>") shouldBe 1
    admin.count("Coming<Class<DryDeserts>>") shouldBe 1
    admin.count("Distant<Class<CelebrityLeaders>>") shouldBe 1
    admin.count("PartyDelegate<Greens, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 1
    admin.count("Delegate<Neutral>") shouldBe 5

    admin.runOperation("ChangingTimes") { doTask("Diversity") }

    admin.count("GlobalEvent") shouldBe 3
    admin.count("AquiferReleasedByPublicCouncil") shouldBe 0
    admin.count("Current<Class<DryDeserts>>") shouldBe 1
    admin.count("Coming<Class<CelebrityLeaders>>") shouldBe 1
    admin.count("Distant<Class<Diversity>>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 2
    admin.count("PartyDelegate<Scientists, Neutral>") shouldBe 1
    admin.count("Delegate<Neutral>") shouldBe 7
  }

  @Test
  internal fun `global event delegate placements are ignored when all neutral delegates are placed`() {
    newGame(TurmoilExpansion)
    repeat(11) {
      admin.runOperation("PartyDelegate<Scientists, Neutral>")
    }
    val placedBefore = admin.count("PartyDelegate<Neutral>")

    admin.runOperation("ChangingTimes") { doTask("CelebrityLeaders") }

    admin.count("Delegate<Neutral>") shouldBe 14
    admin.count("PartyDelegate<Neutral>") shouldBe placedBefore
    admin.count("Distant<Class<CelebrityLeaders>>") shouldBe 1
  }

  @Test
  internal fun `current event resolution measures influence before dispatching to that card`() {
    newGame(TurmoilExpansion)
    makeCurrent("AsteroidMiningGlobalEvent")
    seatPlayerOneAsChairman()
    p1.runOperation("GlobalEventProbe, 7 JovianTag<GlobalEventProbe>")

    admin.beginOperation("TurmoilSolarOperation")
    admin.completeOperation { doTask("CelebrityLeaders") }

    p1.count("Influence") shouldBe 1
    p1.count("Titanium") shouldBe 6
    requireP2().count("Titanium") shouldBe 0
  }

  @Test
  internal fun `tag payouts cap their printed count before adding influence`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation(
        "GlobalEventProbe, 7 EarthTag<GlobalEventProbe>, 4 SpaceTag<GlobalEventProbe>, " +
            "6 ScienceTag<GlobalEventProbe>"
    )
    admin.runOperation("MeasureInfluence<Player1>")

    resolve("HomeworldSupport")
    resolve("InterplanetaryTradeGlobalEvent")
    resolve("SpinOffProducts")

    p1.count("MC") shouldBe 34
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `resource and card payouts use player state plus influence`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation(
        "GlobalEventProbe, 7 JovianTag<GlobalEventProbe>, PROD[7 Steel, 4 Plant], 3 ProjectCard"
    )
    admin.runOperation("MeasureInfluence<Player1>")

    resolve("AsteroidMiningGlobalEvent")
    resolve("Productivity")
    resolve("SuccessfulOrganisms")
    resolve("GenerousFunding")
    resolve("ScientificCommunity")

    p1.count("Titanium") shouldBe 6
    p1.count("Steel") shouldBe 6
    p1.count("Plant") shouldBe 5
    p1.count("MC") shouldBe 8
    requireP2().count("MC") shouldBe 2
  }

  @Test
  internal fun `played events and owned cities pay only their owner`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation(
        "7 PlayedEvent<Class<PlayedEventProbe>>, " + "CityTile<Tharsis_1_1>, CityTile<Tharsis_1_3>"
    )
    admin.runOperation("MeasureInfluence<Player1>")

    resolve("CelebrityLeaders")
    resolve("StrongSociety")

    p1.count("MC") shouldBe 18
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `money penalties cap holdings then subtract influence and available money`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation(
        "100 MC, 5 Heat, GlobalEventProbe, 7 BuildingTag<GlobalEventProbe>, " +
            "2 SpaceTag<GlobalEventProbe>, ActiveEventProbe, " +
            "CityTile<Tharsis_1_1>, CityTile<Tharsis_1_3>, CityTile<Tharsis_2_5>"
    )
    admin.runOperation("MeasureInfluence<Player1>")

    resolve("GlobalDustStorm")
    resolve("Pandemic")
    resolve("Riots")
    resolve("SolarFlare")
    resolve("SolarnetShutdown")

    p1.count("Heat") shouldBe 0
    p1.count("MC") shouldBe 69
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `resource production card and rating losses do as much as the player can`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation(
        "10 MC, 10 Plant, 3 Titanium, ProjectCard, GlobalEventProbe, " +
            "7 JovianTag<GlobalEventProbe>, PROD[Steel]"
    )
    admin.runOperation("MeasureInfluence<Player1>")

    resolve("EcoSabotage")
    resolve("MinersOnStrike")
    resolve("SabotageGlobalEvent")
    resolve("ParadigmBreakdown")
    resolve("RedInfluence")
    resolve("WarOnEarth")

    p1.count("Plant") shouldBe 4
    p1.count("Titanium") shouldBe 0
    p1.count("Steel") shouldBe 1
    p1.count("PROD[Steel]") shouldBe 0
    p1.count("PROD[Energy]") shouldBe 0
    p1.count("ProjectCard") shouldBe 0
    p1.count("MC") shouldBe 6
    p1.count("PROD[MC] - ProdOffset<Class<MC>>") shouldBe 1
    p1.count("TerraformRating") shouldBe 17
    requireP2().count("TerraformRating") shouldBe 16
  }

  @Test
  internal fun `mud slides counts each owned coastal tile once`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation("50 MC, CityTile<Tharsis_4_4>, GreeneryTile<Tharsis_4_5>")
    admin.runOperation("OceanTile<Tharsis_5_4>, OceanTile<Tharsis_5_5>")
    admin.runOperation("MeasureInfluence<Player1>")

    p1.count("OwnedTile<MarsArea(HAS Neighbor<OceanTile>)>") shouldBe 2
    resolve("MudSlides")

    p1.count("MC") shouldBe 46
  }

  @Test
  internal fun `public aquifer and dry deserts use neutral ocean changes and player resource choices`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    admin.runOperation("MeasureInfluence<Player1>")

    admin.runOperation("ResolveGlobalEvent<Class<AquiferReleasedByPublicCouncil>>") {
      p1.doTask("OceanTile<Tharsis_1_2> BY Admin")
    }

    admin.count("OceanTile") shouldBe 1
    p1.count("TerraformRating") shouldBe 20
    p1.count("Plant") shouldBe 1
    p1.count("Steel") shouldBe 1

    p1.runOperation("PartyLeaderInfluence")
    admin.runOperation("ResolveGlobalEvent<Class<DryDeserts>>") {
      val resourceChoices = game.tasks.extract { it }
      resourceChoices.size shouldBe 2
      p1.doTask("Heat", resourceChoices[0].id)
      p1.doTask("Plant", resourceChoices[1].id)
    }

    admin.count("OceanTile") shouldBe 0
    p1.count("TerraformRating") shouldBe 20
    p1.count("Heat") shouldBe 1
    p1.count("Plant") shouldBe 2
  }

  @Test
  internal fun `diversity and energy templates combine distinct state with influence`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation(
        "GlobalEventProbe, BuildingTag<GlobalEventProbe>, SpaceTag<GlobalEventProbe>, " +
            "ScienceTag<GlobalEventProbe>, 3 PowerTag<GlobalEventProbe>, " +
            "CityTag<GlobalEventProbe>, PlantTag<GlobalEventProbe>, " +
            "EarthTag<GlobalEventProbe>, JovianTag<GlobalEventProbe>"
    )
    admin.runOperation("MeasureInfluence<Player1>")

    resolve("Diversity")
    resolve("ImprovedEnergyTemplates")

    p1.count("Class<Tag>(HAS Tag<Player1>)") shouldBe 8
    p1.count("MC") shouldBe 10
    p1.count("PROD[Energy]") shouldBe 2
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `temperature events change an incomplete track but never a completed one`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    admin.runOperation("MeasureInfluence<Player1>")
    admin.runOperation("5 TemperatureStep")

    resolve("SnowCover")
    resolve("VolcanicEruptions")

    admin.count("TemperatureStep") shouldBe 5
    p1.count("ProjectCard") shouldBe 1
    p1.count("PROD[Heat]") shouldBe 1

    admin.runOperation("13 TemperatureStep") { p1.doTask("OceanTile<Tharsis_1_2> BY Admin") }
    admin.runOperation("ResolveGlobalEvent<Class<VolcanicEruptions>>")
    admin.runOperation("ResolveGlobalEvent<Class<SnowCover>>")
    admin.runOperation("ResolveGlobalEvent<Class<VolcanicEruptions>>")

    admin.count("TemperatureStep") shouldBe 19
  }

  @Test
  internal fun `volcanic eruptions lets the first player place its threshold ocean for Admin`() {
    newGame(TurmoilExpansion)
    admin.runOperation("13 TemperatureStep")
    admin.runOperation("VolcanicEruptions")

    admin.runOperation("ResolveGlobalEvent<Class<VolcanicEruptions>>") {
      p1.doTask("OceanTile<Tharsis_1_2> BY Admin")
    }

    admin.count("TemperatureStep") shouldBe 15
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
    p1.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `sponsored projects adds to every compatible resource card then draws for influence`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.runOperation("ActiveEventProbe, Animal<ActiveEventProbe>, EmptyResourceProbe")
    admin.runOperation("MeasureInfluence<Player1>")

    resolve("SponsoredProjects")

    p1.count("Animal<ActiveEventProbe>") shouldBe 2
    p1.count("Microbe<EmptyResourceProbe>") shouldBe 1
    p1.count("ProjectCard") shouldBe 1
    requireP2().count("ProjectCard") shouldBe 0
  }

  @Test
  internal fun `ranking events award friendly places and protect zero revolution scores`() {
    newGame(TurmoilExpansion)
    p1.runOperation(
        "GlobalEventProbe, 2 BuildingTag<GlobalEventProbe>, 2 EarthTag<GlobalEventProbe>"
    )

    resolve("Election")

    p1.count("TerraformRating") shouldBe 22
    requireP2().count("TerraformRating") shouldBe 21

    resolve("Revolution")

    p1.count("TerraformRating") shouldBe 20
    requireP2().count("TerraformRating") shouldBe 21

    newGame(TurmoilExpansion)
    p1.runOperation("GlobalEventProbe, BuildingTag<GlobalEventProbe>, EarthTag<GlobalEventProbe>")
    requireP2()
        .runOperation("GlobalEventProbe, BuildingTag<GlobalEventProbe>, EarthTag<GlobalEventProbe>")

    resolve("Election")
    resolve("Revolution")

    p1.count("TerraformRating") shouldBe 20
    requireP2().count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `solo ranking events use their printed thresholds`() {
    newGame(TurmoilExpansion, players = 1)
    val startingRating = p1.count("TerraformRating")
    p1.runOperation(
        "GlobalEventProbe, 9 BuildingTag<GlobalEventProbe>, " +
            "3 EarthTag<GlobalEventProbe>, ChairmanInfluence"
    )

    resolve("Election")

    p1.count("TerraformRating") shouldBe startingRating + 2

    resolve("Revolution")

    p1.count("TerraformRating") shouldBe startingRating
  }

  @Test
  internal fun `promo events require the promo pack and their companion expansions`() {
    newGame(TurmoilExpansion)

    shouldThrow<DeadEndException> { admin.runOperation("VenusInfrastructure") }
    shouldThrow<DeadEndException> { admin.runOperation("JovianTaxRights") }
    shouldThrow<DeadEndException> { admin.runOperation("CloudSocieties") }

    newGame(TurmoilExpansion, PromoCardPack)

    shouldThrow<DeadEndException> { admin.runOperation("VenusInfrastructure") }
    shouldThrow<DeadEndException> { admin.runOperation("JovianTaxRights") }
    shouldThrow<DeadEndException> { admin.runOperation("CloudSocieties") }

    newGame(TurmoilExpansion, PromoCardPack, VenusNextExpansion)

    admin.runOperation("VenusInfrastructure")
    shouldThrow<DeadEndException> { admin.runOperation("CloudSocieties") }

    newGame(
        TurmoilExpansion,
        PromoCardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )

    admin.runOperation("JovianTaxRights")
    shouldThrow<DeadEndException> { admin.runOperation("CloudSocieties") }

    newGame(
        TurmoilExpansion,
        PromoCardPack,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )

    admin.runOperation("CloudSocieties, CorrosiveRain")
  }

  @Test
  internal fun `floater events keep every multi floater change on one card`() {
    newGame(
        TurmoilExpansion,
        PromoCardPack,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.runOperation(
        "20 MC, FloaterEventProbe, OtherFloaterEventProbe, " +
            "ChairmanInfluence, PartyLeaderInfluence"
    )

    admin.runOperation("CloudSocieties")
    admin.runOperation("ResolveGlobalEvent<Class<CloudSocieties>>") {
      p1.doTask("2 Floater<FloaterEventProbe>")
    }

    p1.count("Floater<FloaterEventProbe>") shouldBe 3
    p1.count("Floater<OtherFloaterEventProbe>") shouldBe 1

    admin.runOperation("CorrosiveRain")
    admin.runOperation("ResolveGlobalEvent<Class<CorrosiveRain>>") {
      p1.count("ProjectCard") shouldBe 0
      p1.doTask("-2 Floater<FloaterEventProbe>")
    }

    p1.count("Floater<FloaterEventProbe>") shouldBe 1
    p1.count("Floater<OtherFloaterEventProbe>") shouldBe 1
    p1.count("MC") shouldBe 20
    p1.count("ProjectCard") shouldBe 2

    newGame(
        TurmoilExpansion,
        PromoCardPack,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.runOperation(
        "20 MC, FloaterEventProbe, Floater<FloaterEventProbe>, " +
            "OtherFloaterEventProbe, Floater<OtherFloaterEventProbe>"
    )

    resolve("CorrosiveRain")

    p1.count("MC") shouldBe 10
    p1.count("Floater<FloaterEventProbe>") shouldBe 1
    p1.count("Floater<OtherFloaterEventProbe>") shouldBe 1
  }

  @Test
  internal fun `optional tag and colony events use owned counts caps and influence`() {
    newGame(
        TurmoilExpansion,
        PromoCardPack,
        VenusNextExpansion,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2, "Luna", "Io"),
    )
    p1.runOperation(
        "20 MC, GlobalEventProbe, 7 VenusTag<GlobalEventProbe>, " +
            "ChairmanInfluence, PartyLeaderInfluence"
    )
    repeat(3) {
      admin.runOperation("Colony<Player1, Luna>")
      admin.runOperation("Colony<Player1, Io>")
    }
    val moneyProduction = p1.count("PROD[MC]")

    resolve("JovianTaxRights")

    p1.count("PROD[MC]") shouldBe moneyProduction + 5
    p1.count("Titanium") shouldBe 2

    resolve("MicrogravityHealthProblems")

    p1.count("MC") shouldBe 11

    resolve("VenusInfrastructure")

    p1.count("MC") shouldBe 25
  }

  private fun resolve(event: String) {
    admin.runOperation(event)
    admin.runOperation("ResolveGlobalEvent<Class<$event>>")
  }

  private fun makeCurrent(event: String) {
    admin.runOperation(
        "-Coming<Class<AquiferReleasedByPublicCouncil>>!, -AquiferReleasedByPublicCouncil!"
    )
    admin.runOperation(event)
    admin.runOperation("Current<Class<$event>>")
  }

  private fun seatPlayerOneAsChairman() {
    admin.runOperation("-Chairman<Neutral>")
    p1.runOperation("Chairman")
  }
}
