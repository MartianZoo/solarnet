package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val globalEventProbeDeclarations =
    parseClasses(
            """
            CLASS GlobalEventProbe : TagHolder { HAS MAX 1 This }
            CLASS PlayedEventProbe : EventCard<Class<ProjectCard>> { cost = 0 }
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
    admin.count("ReserveDelegate<Neutral>") shouldBe 11
  }

  @Test
  internal fun `changing times advances events discards current and reveals the next card`() {
    newGame(TurmoilExpansion)

    admin.manual("ChangingTimes") { doTask("CelebrityLeaders") }

    admin.count("GlobalEvent") shouldBe 3
    admin.count("Current<Class<AquiferReleasedByPublicCouncil>>") shouldBe 1
    admin.count("Coming<Class<DryDeserts>>") shouldBe 1
    admin.count("Distant<Class<CelebrityLeaders>>") shouldBe 1
    admin.count("PartyDelegate<Greens, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 1
    admin.count("ReserveDelegate<Neutral>") shouldBe 9

    admin.manual("ChangingTimes") { doTask("Diversity") }

    admin.count("GlobalEvent") shouldBe 3
    admin.count("AquiferReleasedByPublicCouncil") shouldBe 0
    admin.count("Current<Class<DryDeserts>>") shouldBe 1
    admin.count("Coming<Class<CelebrityLeaders>>") shouldBe 1
    admin.count("Distant<Class<Diversity>>") shouldBe 1
    admin.count("PartyDelegate<Unity, Neutral>") shouldBe 2
    admin.count("PartyDelegate<Scientists, Neutral>") shouldBe 1
    admin.count("ReserveDelegate<Neutral>") shouldBe 7
  }

  @Test
  internal fun `current event resolution measures influence before dispatching to that card`() {
    newGame(TurmoilExpansion)
    makeCurrent("AsteroidMiningGlobalEvent")
    seatPlayerOneAsChairman()
    p1.manual("GlobalEventProbe, 7 JovianTag<GlobalEventProbe>")

    admin.manual("ResolveCurrentGlobalEvent")

    p1.count("Influence") shouldBe 1
    p1.count("Titanium") shouldBe 6
    requireP2().count("Titanium") shouldBe 0
  }

  @Test
  internal fun `tag payouts cap their printed count before adding influence`() {
    newGame(TurmoilExpansion)
    seatPlayerOneAsChairman()
    p1.manual(
        "GlobalEventProbe, 7 EarthTag<GlobalEventProbe>, 4 SpaceTag<GlobalEventProbe>, " +
            "6 ScienceTag<GlobalEventProbe>"
    )
    admin.manual("MeasureInfluence<Player1>")

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
    p1.manual(
        "GlobalEventProbe, 7 JovianTag<GlobalEventProbe>, PROD[7 Steel, 4 Plant], 3 ProjectCard"
    )
    admin.manual("MeasureInfluence<Player1>")

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
    p1.manual(
        "7 PlayedEvent<Class<PlayedEventProbe>>, " + "CityTile<Tharsis_1_1>, CityTile<Tharsis_1_3>"
    )
    admin.manual("MeasureInfluence<Player1>")

    resolve("CelebrityLeaders")
    resolve("StrongSociety")

    p1.count("MC") shouldBe 18
    requireP2().count("MC") shouldBe 0
  }

  private fun resolve(event: String) {
    admin.manual(event)
    admin.manual("ResolveGlobalEvent<Class<$event>>")
  }

  private fun makeCurrent(event: String) {
    admin.manual(
        "-Coming<Class<AquiferReleasedByPublicCouncil>>!, -AquiferReleasedByPublicCouncil!"
    )
    admin.manual(event)
    admin.manual("Current<Class<$event>>")
  }

  private fun seatPlayerOneAsChairman() {
    admin.manual("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p1.manual("Chairman FROM ReserveDelegate")
  }
}
