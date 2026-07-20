package dev.martianzoo.tfm.engine

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CustomMetricMilestonesTest {
  @Test
  fun diversifierCanBeClaimedWithEightDistinctTagTypes() {
    val p1 = Engine.newGame(Canon.fromOptionCodes("BRCHVX", 2, testColonyTiles(2))).tfm(PLAYER1)
    p1.godMode()
        .manual(
            "Ecoline, Thorgate, Phobolog, InventorsGuild, EarthOffice, " +
                "IoMiningIndustries, Pets"
        )

    p1.count("DistinctTagType<Player1>") shouldBe 7
    shouldThrow<RequirementException> { p1.godMode().manual("Diversifier") }

    p1.godMode().manual("Decomposers")
    p1.count("DistinctTagType<Player1>") shouldBe 8
    p1.godMode().manual("Diversifier")
    p1.count("Diversifier") shouldBe 1
  }

  @Test
  fun tacticianCanBeClaimedWithFiveCardsHavingRequirements() {
    val p1 = Engine.newGame(Canon.fromOptionCodes("BRCH", 2, testColonyTiles(2))).tfm(PLAYER1)
    p1.godMode().sneak("ArtificialLake, Birds, Algae, AsteroidMiningConsortium")

    p1.count("CardFront(HAS CardRequirement)") shouldBe 4
    shouldThrow<RequirementException> { p1.godMode().manual("Tactician") }

    p1.godMode().sneak("BreathingFilters")
    p1.count("CardFront(HAS CardRequirement)") shouldBe 5
    p1.godMode().manual("Tactician")
    p1.count("Tactician") shouldBe 1
  }
}
