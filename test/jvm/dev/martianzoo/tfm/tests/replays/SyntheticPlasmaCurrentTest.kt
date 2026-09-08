package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val syntheticPlasmaCurrentDeclarations =
    parseClasses("CLASS RecordedTableau : TagHolder { HAS MAX 1 This }").toSet()

// Complete Turmoil replay from all 203 retained database saves:
// Synthetic Plasma Current (g5625e0252c7c), generations 1 through 12.
// Source: _local/replays/Game20260908/game-g5625e0252c7c.sqlite
// http://newazure.local:8080/the-end?id=p145be84b9959
internal class SyntheticPlasmaCurrentTest :
    CardTest(additionalClassDeclarations = syntheticPlasmaCurrentDeclarations) {
  private val workflow: TfmWorkflow.Manual
    get() = TfmWorkflow.Manual(game)

  @Test
  internal fun `complete solo turmoil timeline`() {
    newGame(
        GameConfig(
            """
            TharsisMap
            VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack, TurmoilExpansion
            Tr63SoloObjective
            """,
            "Bloo",
        )
    )
    replaceDefaultEventQueue()
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 1
    admin.count("Dominant<MarsFirst>") shouldBe 1
    admin.phase("Action")
    p1.manual("RecordedTableau, 200 MC")
    admin.count("TurmoilExpansion") shouldBe 1
    admin.count("SoloGenerationsLeft") shouldBe 11

    // Generation 1: free Scientists, then 5 M€ to Reds. No Current event or Mars First bonus.
    sendFreeDelegate("Scientists")
    sendPaidDelegate("Reds")
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 1
    admin.count("Dominant<MarsFirst>") shouldBe 1
    resolveSolar(
        "OceanTile<Tharsis_6_7>",
        "SponsoredProjects",
        ruling = "MarsFirst",
        dominant = "Reds",
        chairman = "Neutral",
    ) {
      p1.count("TerraformRating") shouldBe 13
    }

    // Generation 2: Riots sees no cities. Reds' solo bonus offsets the annual TR revision.
    sendFreeDelegate("Greens")
    val generation2Mc = p1.count("MC")
    val generation2Tr = p1.count("TerraformRating")
    resolveSolar(
        "OceanTile<Tharsis_5_5>",
        "StrongSociety",
        ruling = "Reds",
        dominant = "Scientists",
        chairman = "Bloo",
    ) {
      p1.count("MC") shouldBe generation2Mc
      p1.count("TerraformRating") shouldBe generation2Tr + 1
    }

    // Generation 3: Earth plus two influence protects Bloo from Revolution.
    p1.manual("EarthTag<RecordedTableau>, ScienceTag<RecordedTableau>")
    sendFreeDelegate("Kelvinists")
    val generation3Mc = p1.count("MC")
    val generation3Tr = p1.count("TerraformRating")
    resolveSolar(
        "OceanTile<Tharsis_6_9>",
        "SnowCover",
        ruling = "Scientists",
        dominant = "Greens",
        chairman = "Bloo",
    ) {
      p1.count("MC") shouldBe generation3Mc + 1
      p1.count("TerraformRating") shouldBe generation3Tr
    }

    // Generation 4: Sponsored Projects draws twice for influence; Greens pays for one bio tag.
    admin.count("ScientistsPolicy") shouldBe 1
    p1.manual("PlantTag<RecordedTableau>")
    sendFreeDelegate("Scientists")
    admin.count("ScientistsPolicy") shouldBe 1
    val generation4Mc = p1.count("MC")
    resolveSolar(
        "TemperatureStep",
        "ScientificCommunity",
        ruling = "Greens",
        dominant = "Kelvinists",
        chairman = "Bloo",
    ) {
      p1.count("ProjectCard") shouldBe 2
      p1.count("MC") shouldBe generation4Mc + 1
    }

    // Generation 5: zero cities plus two influence pays 4 M€; Kelvinists has no heat bonus yet.
    sendFreeDelegate("Greens")
    val generation5Mc = p1.count("MC")
    resolveSolar(
        "VenusStep",
        "HomeworldSupport",
        ruling = "Kelvinists",
        dominant = "Reds",
        chairman = "Bloo",
    ) {
      p1.count("MC") shouldBe generation5Mc + 4
    }

    // Generation 6: Snow Cover draws once and drops temperature twice. Reds again restores the TR.
    sendFreeDelegate("Scientists")
    val generation6Tr = p1.count("TerraformRating")
    resolveSolar(
        "VenusStep",
        "Pandemic",
        ruling = "Reds",
        dominant = "Scientists",
        chairman = "Neutral",
    ) {
      p1.count("ProjectCard") shouldBe 3
      p1.count("TerraformRating") shouldBe generation6Tr
      admin.count("TemperatureStep") shouldBe 0
    }

    // Generation 7: the archive has 16 cards and two influence, then four Science tags pay again.
    p1.manual("13 ProjectCard, 3 ScienceTag<RecordedTableau>")
    sendFreeDelegate("Reds")
    val generation7Mc = p1.count("MC")
    resolveSolar(
        "VenusStep",
        "CelebrityLeaders",
        ruling = "Scientists",
        dominant = "Unity",
        chairman = "Bloo",
    ) {
      p1.count("MC") shouldBe generation7Mc + 22
    }

    // Generation 8: Envoys from Venus supplied two Kelvinist delegates before the free Scientists
    // delegate. The replay uses the public delegate action for those equivalent physical moves.
    sendFreeDelegate("Kelvinists")
    sendPaidDelegate("Kelvinists")
    sendPaidDelegate("Scientists")
    p1.manual("EarthTag<RecordedTableau>, 8 VenusTag<RecordedTableau>, 5 SpaceTag<RecordedTableau>")
    val generation8Mc = p1.count("MC")
    resolveSolar(
        "VenusStep",
        "InterplanetaryTradeGlobalEvent",
        ruling = "Unity",
        dominant = "Kelvinists",
        chairman = "Neutral",
    ) {
      p1.count("MC") shouldBe generation8Mc + 16
    }

    // Generation 9: Pandemic charges three times after two influence; Kelvinists pays five.
    p1.manual("PROD[5 Heat], 5 BuildingTag<RecordedTableau>")
    sendFreeDelegate("Greens")
    sendPaidDelegate("Unity")
    val generation9Mc = p1.count("MC")
    resolveSolar(
        "OxygenStep",
        "SpinOffProducts",
        ruling = "Kelvinists",
        dominant = "Greens",
        chairman = "Bloo",
    ) {
      p1.count("MC") shouldBe generation9Mc - 4
    }

    // Generation 10: five played events and two influence pay 14 M€; seven bio tags pay 7 more.
    p1.manual("5 PlayedEvent<Class<Comet>>, 6 PlantTag<RecordedTableau>")
    sendFreeDelegate("Kelvinists")
    val generation10Mc = p1.count("MC")
    resolveSolar(
        "TemperatureStep",
        "SuccessfulOrganisms",
        ruling = "Greens",
        dominant = "MarsFirst",
        chairman = "Neutral",
    ) {
      p1.count("MC") shouldBe generation10Mc + 21
    }

    // Generation 11: Interplanetary Trade pays 10 M€ and 18 Building tags pay 18 more.
    p1.manual("13 BuildingTag<RecordedTableau>")
    sendFreeDelegate("Greens")
    sendPaidDelegate("Greens")
    val generation11Mc = p1.count("MC")
    resolveSolar(
        "TemperatureStep",
        "VolcanicEruptions",
        ruling = "MarsFirst",
        dominant = "Unity",
        chairman = "Neutral",
    ) {
      p1.count("MC") shouldBe generation11Mc + 28
    }

    // Generation 12 ended before another Solar phase.
    sendFreeDelegate("MarsFirst")
    admin.assertCounts(
        1 to "Current<Class<SpinOffProducts>>",
        1 to "Coming<Class<SuccessfulOrganisms>>",
        1 to "Distant<Class<VolcanicEruptions>>",
        1 to "Ruling<MarsFirst>",
        1 to "Dominant<Unity>",
        1 to "Chairman<Neutral>",
    )
    p1.count("PartyDelegate<MarsFirst>") shouldBe 1
    admin.count("Generation") shouldBe 12
  }

  private fun replaceDefaultEventQueue() {
    admin.manual(
        "-Coming<Class<AquiferReleasedByPublicCouncil>>!, -AquiferReleasedByPublicCouncil!, " +
            "-Distant<Class<DryDeserts>>!, -DryDeserts!, " +
            "ReserveDelegate<Neutral> FROM PartyDelegate<Reds, Neutral>, " +
            "-PartyLeader<Reds, Neutral>!, " +
            "Riots, Coming<Class<Riots>>, Revolution, Distant<Class<Revolution>>"
    )
  }

  private fun sendFreeDelegate(party: String) {
    p1.stdAction("SendDelegateSA", 1) {
      doTask("PartyDelegate<$party> FROM LobbyDelegate")
    }
  }

  private fun sendPaidDelegate(party: String) {
    p1.stdAction("SendDelegateSA", 2) {
      doTask("PartyDelegate<$party> FROM ReserveDelegate")
    }
  }

  private fun resolveSolar(
      worldGovernmentChoice: String,
      nextDistantEvent: String,
      ruling: String,
      dominant: String,
      chairman: String,
      assertions: () -> Unit,
  ) {
    workflow.solarPhase()
    p1.wgt(worldGovernmentChoice)
    assertions()
    admin.doTask(nextDistantEvent)
    admin.assertCounts(
        1 to "Ruling<$ruling>",
        1 to "Dominant<$dominant>",
        1 to "Chairman<$chairman>",
    )
    workflow.researchPhase { p1.buyCards(0) }
    workflow.actionPhase()
  }
}
