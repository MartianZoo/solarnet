package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CustomMetricMilestonesTest {
  @Test
  internal fun tycoonCanBeClaimedWithFifteenActiveAndAutomatedCards() {
    val p1 = Engine.newGame(canonicalPremise(Elysium, players = 2)).tfm(PLAYER1)
    p1.godMode()
        .sneak(
            "$ColonizerTrainingCamp, $DeepWellHeating, $CloudSeeding, $MartianRails, " +
                "$WaterImportFromEuropa, $EquatorialMagnetizer, $DomedCrater, $NoctisCity, " +
                "$MethaneFromTitan, $ResearchOutpost, $PhobosSpaceHaven, $BlackPolarDust, " +
                "$ArcticAlgae, $Predators"
        )

    p1.count("ActiveCard OR AutomatedCard") shouldBe 14
    shouldThrow<RequirementException> { p1.godMode().manual("Tycoon15") }

    p1.godMode().sneak("$EosChasmaNationalPark")
    p1.count("ActiveCard OR AutomatedCard") shouldBe 15
    p1.godMode().manual("Tycoon15")
    p1.count("Tycoon15") shouldBe 1
  }

  @Test
  internal fun diversifierCanBeClaimedWithEightDistinctTagTypes() {
    val game =
        Engine.newGame(
            canonicalPremise(
                ColoniesExpansion,
                Hellas,
                VenusNextExpansion,
                PromoCardPack,
                players = 2,
                colonyTiles = testColonyTiles(2),
            )
        )
    val p1 = game.tfm(PLAYER1)
    p1.godMode()
        .manual(
            "$Ecoline, $ThorGate, $Phobolog, $InventorsGuild, $EarthOffice, " +
                "$IoMiningIndustries, $Pets, 8 Plant, 6 Steel, 4 Heat, 3 ProjectCard"
        )
    game
        .tfm(PLAYER2)
        .godMode()
        .manual("$EarthCatapult, $Mine, $DeepWellHeating, 9 Plant, 7 Steel, 5 Heat")

    p1.count("Class<Tag>(HAS Tag<Player1>)") shouldBe 7
    p1.count("Class<Tag>(HAS Tag<Player2>)") shouldBe 3
    shouldThrow<RequirementException> { p1.godMode().manual("Diversifier") }

    p1.godMode().manual("$Decomposers")
    p1.count("Class<Tag>(HAS Tag<Player1>)") shouldBe 8
    p1.godMode().manual("Diversifier")
    p1.count("Diversifier") shouldBe 1
  }

  @Test
  internal fun tacticianCanBeClaimedWithFiveCardsHavingRequirements() {
    val p1 =
        Engine.newGame(
                canonicalPremise(
                    ColoniesExpansion,
                    Hellas,
                    players = 2,
                    colonyTiles = testColonyTiles(2),
                )
            )
            .tfm(PLAYER1)
    p1.godMode().sneak("$ArtificialLake, $Birds, $Algae, $AsteroidMiningConsortium")

    p1.count("CardFront(HAS requirement)") shouldBe 4
    shouldThrow<RequirementException> { p1.godMode().manual("Tactician5") }

    p1.godMode().sneak("$BreathingFilters")
    p1.count("CardFront(HAS requirement)") shouldBe 5
    p1.godMode().manual("Tactician5")
    p1.count("Tactician5") shouldBe 1
  }
}
