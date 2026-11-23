package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WildTagTest : TfmSandboxTest() {
  @Test
  internal fun `Three played wilds enable Advanced Ecosystems then five enable Diversifier`() {
    newTestGame(
        "PreludeExpansion, TurmoilExpansion, Diversifier, $AppliedScience, $NobelPrize, $ResearchNetwork, $ResearchCoordination",
        kimCorporation = SeptemTribus,
    )
    // The sandbox supplies an extra Prelude back; the subject is allocation across actual card
    // plays.
    kim.exMachina("PreludeCard")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>",
          "PROD[MC]",
          "3 ProjectCard",
      )
    }
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AppliedScience>>",
          "6 Science<$AppliedScience>",
      )
    }
    kim.count("WildTag") shouldBe 3
    kim.count("Tag") shouldBe 0
    // Invoke the plays directly: this scenario tests card effects, not phase offers.
    kim.runOperation("UseAction<PlayCardAction, Action1>") {
      doTask("PlayCard<Class<ProjectCard>, Class<$AdvancedEcosystems>, Hand>")

      kim.count("PlantTag<WildTagElection<WildTag<$AppliedScience>>>") shouldBe 1
      kim.count("MicrobeTag<WildTagElection<WildTag<$ResearchNetwork>>>") shouldBe 1
      kim.count("AnimalTag<WildTagElection<WildTag<$SeptemTribus>>>") shouldBe 1
      doTasks("11 Pay<> FROM MC")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("Tag<$AdvancedEcosystems>") shouldBe 3
    kim.runOperation("UseAction<PlayCardAction, Action1>") {
      doTask("PlayCard<Class<ProjectCard>, Class<$ResearchCoordination>, Hand>")

      doTasks("4 Pay<> FROM MC")
    }
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NobelPrize>, Hand>",
          "5 MC",
          "2 SearchForCard<RequirementFilter>",
      )
    }
    kim.count("WildTag") shouldBe 5
    kim.runOperation("UseAction<ClaimMilestoneAction, Action1>") {
          doTasks("8 Pay<> FROM MC", "Diversifier")
        }
        .expect("0 Tag<WildTagElection<WildTag>>, 0 WildTag")
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("WildTag") shouldBe 5
  }

  @Test
  internal fun `All five Canon wild cards satisfy a science requirement and can be reused as Earth`() {
    val cards =
        listOf(
            "$ResearchCoordination",
            "$ResearchNetwork",
            "$AppliedScience",
            "$NobelPrize",
            "$SeptemTribus",
        )
    newTestGame("TurmoilExpansion, " + cards.joinToString())
    kim.exMachina(cards.joinToString())
    kim.inTurn {
      doTasks(*playCardTasks("$MassConverter"))
      cards.forEach { kim.count("ScienceTag<WildTagElection<WildTag<$it>>>") shouldBe 1 }
      doTasks("8 Pay<> FROM MC", "PROD[6 Energy]")
    }
    kim.inTurn {
      doTasks(*playCardTasks("$Cartel"), "8 Pay<> FROM MC")
      cards.forEach { kim.count("EarthTag<WildTagElection<WildTag<$it>>>") shouldBe 1 }
      doTask("PROD[6 MC]")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("WildTag") shouldBe 5
  }

  @Test
  internal fun `science requirement uses only its shortfall and cleans up after the benefit`() {
    newTestGame("$ResearchCoordination, $ResearchNetwork")
    kim.exMachina(
        "$ResearchCoordination, $ResearchNetwork, $Research, $SearchForLife, $DevelopmentCenter"
    )
    kim.inTurn {
      kim.doTasks(*playCardTasks("$MassConverter"))
          .expect("ScienceTag<WildTagElection<WildTag<$ResearchCoordination>>>")
      kim.count("Tag<WildTagElection<WildTag>>") shouldBe 1
      doTasks(
          "8 Pay<> FROM MC",
          "PROD[6 Energy]",
      )
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("WildTag") shouldBe 2
  }

  @Test
  internal fun `a mixed requirement assigns distinct meanings`() {
    newTestGame("$ResearchCoordination, $ResearchNetwork")
    kim.exMachina("$ResearchCoordination, $ResearchNetwork, $Ecoline")
    kim.inTurn {
      kim.doTasks(*playCardTasks("$AdvancedEcosystems"))
          .expect(
              "MicrobeTag<WildTagElection<WildTag<$ResearchCoordination>>>, AnimalTag<WildTagElection<WildTag<$ResearchNetwork>>>"
          )
      kim.count("Tag<WildTagElection<WildTag>>") shouldBe 2
      doTasks("11 Pay<> FROM MC")
    }
    stan.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `a missing requirement cannot count a wild twice`() {
    newTestGame("$ResearchCoordination")
    kim.exMachina("$ResearchCoordination, $Ecoline")
    shouldThrow<RequirementException> {
      kim.inTurn { doTasks(*playCardTasks("$AdvancedEcosystems")) }
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `greedy benefits reuse wilds on later actions without extra Point Luna draws`() {
    newTestGame("$ResearchCoordination, $ResearchNetwork, $PointLuna")
    kim.exMachina("$ResearchNetwork, $PointLuna")
    kim.inTurn {
      kim.doTasks(*playCardTasks("$ResearchCoordination"), "4 Pay<> FROM MC")
          .expect("WildTag<$ResearchCoordination>")
    }
    kim.inTurn {
      kim.doTasks(*playCardTasks("$Cartel"), "8 Pay<> FROM MC")
          .expect(
              "EarthTag<WildTagElection<WildTag<$ResearchCoordination>>>, EarthTag<WildTagElection<WildTag<$ResearchNetwork>>>"
          )
      doTasks("ProjectCard", "PROD[4 MC]")
    }
    kim.inTurn {
      kim.doTasks(*playCardTasks("$Satellites"), "10 Pay<> FROM MC")
          .expect(
              "SpaceTag<WildTagElection<WildTag<$ResearchCoordination>>>, SpaceTag<WildTagElection<WildTag<$ResearchNetwork>>>"
          )
      doTask("PROD[4 MC]")
    }
  }

  @Test
  internal fun `Gyropolis chooses Earth by convention`() {
    newTestGame("$ResearchCoordination")
    kim.exMachina("$ResearchCoordination, PROD[2 Energy]")
    kim.inTurn {
      kim.doTasks(*playCardTasks("$Gyropolis"), "20 Pay<> FROM MC")
          .expect("EarthTag<WildTagElection<WildTag<$ResearchCoordination>>>")
      doTasks("PROD[-2 Energy]", "PROD[0 MC]", "PROD[1 MC]", "CityTile<Tharsis_1_1>", "2 Steel")
    }
  }

  @Test
  internal fun `an opponents wild cannot satisfy a requirement or take a greedy assignment`() {
    newTestGame("$ResearchCoordination, $ResearchNetwork")
    kim.exMachina("$ResearchCoordination, $Ecoline")
    stan.exMachina("$ResearchNetwork")
    shouldThrow<RequirementException> {
      kim.inTurn { doTasks(*playCardTasks("$AdvancedEcosystems")) }
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$Cartel"),
          "8 Pay<> FROM MC",
          "PROD[2 MC]",
      )
    }
    stan.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Saturn Systems sees another players printed Jovian but not their assignment`() {
    newTestGame("$ResearchCoordination")
    kim.exMachina("$SaturnSystems")
    stan.exMachina("$ResearchCoordination")
    stan.inTurn {
      doTasks(
          *playCardTasks("$TerraformingGanymede"),
          "33 Pay<> FROM MC",
      )
      giveTask(kim, "PROD[1 MC]")
      doTask("2 TerraformRating")
    }
  }

  @Test
  internal fun `Cloud Tourism splits wilds between Earth and Venus without firing Point Luna`() {
    newTestGame("$CloudTourism, $ResearchCoordination, $ResearchNetwork, $PointLuna")
    kim.exMachina("$ResearchCoordination, $ResearchNetwork, $PointLuna")
    kim.inTurn {
      doTasks(
          *playCardTasks("$CloudTourism"),
          "11 Pay<> FROM MC",
          "EarthTag<WildTagElection<WildTag<$ResearchCoordination>>>",
          "VenusTag<WildTagElection<WildTag<$ResearchNetwork>>>",
          "PROD[2 MC]",
      )
    }
  }

  @Test
  internal fun `Cloud Tourism leaves a spare wild unused when it cannot improve production`() {
    newTestGame("$CloudTourism, $ResearchCoordination, $ResearchNetwork")
    kim.exMachina("$ResearchCoordination, $ResearchNetwork")
    kim.inTurn {
      doTasks(
          *playCardTasks("$CloudTourism"),
          "11 Pay<> FROM MC",
          "EarthTag<WildTagElection<WildTag<$ResearchCoordination>>>",
      )
      kim.count("Tag<WildTagElection<WildTag<$ResearchNetwork>>>") shouldBe 0
      doTask("PROD[1 MC]")
    }
  }

  @Test
  internal fun `Interplanetary Trade accepts distinct tag choices and declining a wild`() {
    newTestGame("$ResearchCoordination, $ResearchNetwork")
    kim.exMachina("$ResearchCoordination, $ResearchNetwork, $Ecoline")
    kim.inTurn {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "27 Pay<> FROM MC",
          "EACH @WildTagElection { Tag<@WildTagElection>? }",
          "EventTag<WildTagElection<WildTag<$ResearchCoordination>>>",
          "0 EarthTag<WildTagElection<WildTag<$ResearchNetwork>>>",
          "PROD[3 MC]",
      )
    }
  }

  @Test
  internal fun `Interplanetary Trade lets the player take production before allocating`() {
    newTestGame("$ResearchCoordination")
    kim.exMachina("$ResearchCoordination")
    kim.inTurn {
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "27 Pay<> FROM MC",
          "PROD[1 MC]",
          "EACH @WildTagElection { Tag<@WildTagElection>? }",
          "EventTag<WildTagElection<WildTag<$ResearchCoordination>>>",
      )
    }
  }

  @Test
  internal fun `Ecologist accepts wild tags without permanently assigning their types`() {
    newTestGame("ElysiumMap, $ResearchCoordination, $ResearchNetwork")
    kim.exMachina("$ResearchCoordination, $ResearchNetwork, $AdvancedEcosystems")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC")
      doTask("Ecologist")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Planetologist fills separate planetary shortfalls`() {
    newTestGame("CimmeriaMap, $ResearchCoordination, $ResearchNetwork")
    kim.exMachina(
        "$ResearchCoordination, $ResearchNetwork, $Sponsors, $VenusGovernor, $IoMiningIndustries"
    )
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC")
      doTask("Planetologist")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Diversifier counts unused wilds without assigning tag types`() {
    newTestGame("TharsisMap, Diversifier, $ResearchCoordination, $ResearchNetwork")
    kim.exMachina(
        "$AdvancedEcosystems, $ResearchOutpost, $Satellites, $ResearchCoordination, $ResearchNetwork"
    )
    kim.inTurn {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Diversifier",
      )
    }
  }

  @Test
  internal fun `Diversifier still needs eight distinct types including unused wilds`() {
    newTestGame("Diversifier, $ResearchCoordination, $ResearchNetwork")
    kim.exMachina("$AdvancedEcosystems, $ResearchOutpost, $Mine, $ResearchCoordination")
    stan.exMachina("$ResearchNetwork")
    kim.setToExMachina(8, "MC")
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
      }
    }
    kim.count("MC") shouldBe 8
    kim.count("Diversifier") shouldBe 0
    kim.exMachina("$Satellites")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
    }
  }

  @Test
  internal fun `Claiming Energizer with wilds requires no tag assignment`() {
    newTestGame("HellasMap, $ResearchCoordination")
    kim.exMachina("$ResearchCoordination")
    kim.setToExMachina(6, "PROD[Energy]")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Energizer")
    }
  }
}
