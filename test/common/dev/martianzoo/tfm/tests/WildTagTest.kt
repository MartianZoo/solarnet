package dev.martianzoo.tfm.tests

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.cards.cardnames.SeptemTribus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WildTagTest : TfmSandboxTest() {
  @Test
  internal fun `Three played wilds enable Advanced Ecosystems then five enable Diversifier`() {
    newTestGame(
        "PreludeExpansion, TurmoilExpansion, Diversifier, AppliedScience, NobelPrize, ResearchNetwork, ResearchCoordination",
        kimCorporation = SeptemTribus,
    )
    // The sandbox supplies an extra Prelude back; the subject is allocation across actual card
    // plays.
    kim.exMachina("PreludeCard")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<ResearchNetwork>>",
          "ResearchNetwork FROM PreludeCard",
          "PROD[MC]",
          "3 ProjectCard",
      )
    }
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<AppliedScience>>",
          "AppliedScience FROM PreludeCard",
          "6 Science<AppliedScience>",
      )
    }
    kim.count("WildTag") shouldBe 3
    kim.count("Tag") shouldBe 0
    // Invoke the plays directly: this scenario tests card effects, not phase offers.
    kim.runOperation("PlayProject<Class<AdvancedEcosystems>>") {
      kim.count("PlantTag<WildTagElection<WildTag<AppliedScience>>>") shouldBe 1
      kim.count("MicrobeTag<WildTagElection<WildTag<ResearchNetwork>>>") shouldBe 1
      kim.count("AnimalTag<WildTagElection<WildTag<SeptemTribus>>>") shouldBe 1
      doTasks("11 Pay<> FROM MC", "AdvancedEcosystems FROM ProjectCard")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("Tag<AdvancedEcosystems>") shouldBe 3
    kim.runOperation("PlayProject<Class<ResearchCoordination>>") {
      doTasks("4 Pay<> FROM MC", "ResearchCoordination FROM ProjectCard")
    }
    kim.inTurn {
      doTask("PlayCard<Class<PreludeCard>, Class<NobelPrize>, Hand>")
      doTasks("NobelPrize FROM PreludeCard", "5 MC", "2 SearchForCard<RequirementFilter>")
    }
    kim.count("WildTag") shouldBe 5
    kim.runOperation("ClaimMilestone<Class<Diversifier>>") {
          selectTask("EACH @WildTagElection { Tag<@WildTagElection>? }")
          doTasks(
              "BuildingTag<WildTagElection<WildTag<AppliedScience>>>",
              "SpaceTag<WildTagElection<WildTag<NobelPrize>>>",
              "ScienceTag<WildTagElection<WildTag<ResearchCoordination>>>",
              "EarthTag<WildTagElection<WildTag<ResearchNetwork>>>",
              "JovianTag<WildTagElection<WildTag<SeptemTribus>>>",
          )
          kim.count("Class<@Tag>(HAS @Tag)") shouldBe 8
          doTasks("8 Pay<> FROM MC", "Diversifier")
        }
        .expect("Diversifier, 0 Tag<WildTagElection<WildTag>>, 0 WildTag")
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("WildTag") shouldBe 5
  }

  @Test
  internal fun `All five Canon wild cards satisfy a science requirement and can be reused as Earth`() {
    val cards =
        listOf(
            "ResearchCoordination",
            "ResearchNetwork",
            "AppliedScience",
            "NobelPrize",
            "SeptemTribus",
        )
    newTestGame("TurmoilExpansion, " + cards.joinToString())
    kim.exMachina(cards.joinToString())
    kim.inTurn {
      doTask("PlayProject<Class<MassConverter>>")
      cards.forEach { kim.count("ScienceTag<WildTagElection<WildTag<$it>>>") shouldBe 1 }
      doTasks("8 Pay<> FROM MC", "MassConverter FROM ProjectCard", "PROD[6 Energy]")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<Cartel>>", "8 Pay<> FROM MC", "Cartel FROM ProjectCard")
      cards.forEach { kim.count("EarthTag<WildTagElection<WildTag<$it>>>") shouldBe 1 }
      doTask("PROD[6 MC]")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("WildTag") shouldBe 5
  }

  @Test
  internal fun `science requirement uses only its shortfall and cleans up after the benefit`() {
    newTestGame("ResearchCoordination, ResearchNetwork")
    kim.exMachina(
        "ResearchCoordination, ResearchNetwork, Research, SearchForLife, DevelopmentCenter"
    )
    kim.inTurn {
      kim.doTasks("PlayProject<Class<MassConverter>>")
          .expect("ScienceTag<WildTagElection<WildTag<ResearchCoordination>>>")
      kim.count("Tag<WildTagElection<WildTag>>") shouldBe 1
      doTask("8 Pay<> FROM MC")
      doTask("MassConverter FROM ProjectCard")
      doTask("PROD[6 Energy]")
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    kim.count("WildTag") shouldBe 2
  }

  @Test
  internal fun `a mixed requirement assigns distinct meanings`() {
    newTestGame("ResearchCoordination, ResearchNetwork")
    kim.exMachina("ResearchCoordination, ResearchNetwork, Ecoline")
    kim.inTurn {
      kim.doTasks("PlayProject<Class<AdvancedEcosystems>>")
          .expect(
              "MicrobeTag<WildTagElection<WildTag<ResearchCoordination>>>, AnimalTag<WildTagElection<WildTag<ResearchNetwork>>>"
          )
      kim.count("Tag<WildTagElection<WildTag>>") shouldBe 2
      doTasks("11 Pay<> FROM MC", "AdvancedEcosystems FROM ProjectCard")
    }
    stan.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `a missing requirement cannot count a wild twice`() {
    newTestGame("ResearchCoordination")
    kim.exMachina("ResearchCoordination, Ecoline")
    shouldThrow<RequirementException> {
      kim.inTurn { doTask("PlayProject<Class<AdvancedEcosystems>>") }
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `greedy benefits reuse wilds on later actions without extra Point Luna draws`() {
    newTestGame("ResearchCoordination, ResearchNetwork, PointLuna")
    kim.exMachina("ResearchNetwork, PointLuna")
    kim.inTurn {
      doTasks("PlayProject<Class<ResearchCoordination>>", "4 Pay<> FROM MC")
      kim.doTasks("ResearchCoordination FROM ProjectCard").expect("WildTag<ResearchCoordination>")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<Cartel>>", "8 Pay<> FROM MC")
      kim.doTasks("Cartel FROM ProjectCard")
          .expect(
              "EarthTag<WildTagElection<WildTag<ResearchCoordination>>>, EarthTag<WildTagElection<WildTag<ResearchNetwork>>>"
          )
      doTasks("ProjectCard", "PROD[4 MC]")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<Satellites>>", "10 Pay<> FROM MC")
      kim.doTasks("Satellites FROM ProjectCard")
          .expect(
              "SpaceTag<WildTagElection<WildTag<ResearchCoordination>>>, SpaceTag<WildTagElection<WildTag<ResearchNetwork>>>"
          )
      doTask("PROD[4 MC]")
    }
  }

  @Test
  internal fun `Gyropolis chooses Earth by convention`() {
    newTestGame("ResearchCoordination")
    kim.exMachina("ResearchCoordination, PROD[2 Energy]")
    kim.inTurn {
      doTasks("PlayProject<Class<Gyropolis>>", "20 Pay<> FROM MC")
      kim.doTasks("Gyropolis FROM ProjectCard")
          .expect("EarthTag<WildTagElection<WildTag<ResearchCoordination>>>")
      doTasks("PROD[-2 Energy]", "PROD[0 MC]", "PROD[1 MC]", "CityTile<Tharsis_1_1>", "2 Steel")
    }
  }

  @Test
  internal fun `an opponents wild cannot satisfy a requirement or take a greedy assignment`() {
    newTestGame("ResearchCoordination, ResearchNetwork")
    kim.exMachina("ResearchCoordination, Ecoline")
    stan.exMachina("ResearchNetwork")
    shouldThrow<RequirementException> {
      kim.inTurn { doTask("PlayProject<Class<AdvancedEcosystems>>") }
    }
    kim.inTurn {
      doTasks(
          "PlayProject<Class<Cartel>>",
          "8 Pay<> FROM MC",
          "Cartel FROM ProjectCard",
          "PROD[2 MC]",
      )
    }
    stan.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Saturn Systems sees another players printed Jovian but not their assignment`() {
    newTestGame("ResearchCoordination")
    kim.exMachina("SaturnSystems")
    stan.exMachina("ResearchCoordination")
    stan.inTurn {
      doTasks(
          "PlayProject<Class<TerraformingGanymede>>",
          "33 Pay<> FROM MC",
          "TerraformingGanymede FROM ProjectCard",
      )
      kim.doTask("PROD[1 MC]")
      doTask("2 TerraformRating")
    }
  }

  @Test
  internal fun `Cloud Tourism splits wilds between Earth and Venus without firing Point Luna`() {
    newTestGame("CloudTourism, ResearchCoordination, ResearchNetwork, PointLuna")
    kim.exMachina("ResearchCoordination, ResearchNetwork, PointLuna")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CloudTourism>>",
          "11 Pay<> FROM MC",
          "CloudTourism FROM ProjectCard",
      )
      doTasks(
          "EarthTag<WildTagElection<WildTag<ResearchCoordination>>>",
          "VenusTag<WildTagElection<WildTag<ResearchNetwork>>>",
      )
      doTask("PROD[2 MC]")
    }
  }

  @Test
  internal fun `Cloud Tourism leaves a spare wild unused when it cannot improve production`() {
    newTestGame("CloudTourism, ResearchCoordination, ResearchNetwork")
    kim.exMachina("ResearchCoordination, ResearchNetwork")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<CloudTourism>>",
          "11 Pay<> FROM MC",
          "CloudTourism FROM ProjectCard",
      )
      doTask("EarthTag<WildTagElection<WildTag<ResearchCoordination>>>")
      kim.count("Tag<WildTagElection<WildTag<ResearchNetwork>>>") shouldBe 0
      doTask("PROD[1 MC]")
    }
  }

  @Test
  internal fun `Interplanetary Trade accepts distinct tag choices and declining a wild`() {
    newTestGame("ResearchCoordination, ResearchNetwork")
    kim.exMachina("ResearchCoordination, ResearchNetwork, Ecoline")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<InterplanetaryTrade>>",
          "27 Pay<> FROM MC",
          "InterplanetaryTrade FROM ProjectCard",
      )
      selectTask("EACH @WildTagElection { Tag<@WildTagElection>? }")
      doTasks(
          "EventTag<WildTagElection<WildTag<ResearchCoordination>>>",
          "0 EarthTag<WildTagElection<WildTag<ResearchNetwork>>>",
      )
      doTask("PROD[3 MC]")
    }
  }

  @Test
  internal fun `Interplanetary Trade lets the player take production before allocating`() {
    newTestGame("ResearchCoordination")
    kim.exMachina("ResearchCoordination")
    kim.inTurn {
      doTasks(
          "PlayProject<Class<InterplanetaryTrade>>",
          "27 Pay<> FROM MC",
          "InterplanetaryTrade FROM ProjectCard",
      )
      doTask("PROD[1 MC]")
      selectTask("EACH @WildTagElection { Tag<@WildTagElection>? }")
      doTask("EventTag<WildTagElection<WildTag<ResearchCoordination>>>")
    }
  }

  @Test
  internal fun `Ecologist uses Plant by convention and leaves an unneeded wild unused`() {
    newTestGame("ElysiumMap, ResearchCoordination, ResearchNetwork")
    kim.exMachina("ResearchCoordination, ResearchNetwork, AdvancedEcosystems")
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Ecologist>>", "8 Pay<> FROM MC")
      val result = kim.doTask("Ecologist")
      result.expect("0 Tag<WildTagElection<WildTag>>")
      val assigned = kim.resolve("PlantTag<WildTagElection<WildTag<ResearchCoordination>>>")
      kotlin.test.assertTrue(
          result.changes.any { it.change.gaining?.type?.isSubtypeOf(assigned) == true },
          result.changes.joinToString("\n"),
      )
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Planetologist fills separate planetary shortfalls`() {
    newTestGame("CimmeriaMap, ResearchCoordination, ResearchNetwork")
    kim.exMachina(
        "ResearchCoordination, ResearchNetwork, Sponsors, VenusGovernor, IoMiningIndustries"
    )
    kim.inTurn {
      doTasks("ClaimMilestone<Class<Planetologist>>", "8 Pay<> FROM MC")
      val result = kim.doTask("Planetologist")
      result.expect("0 Tag<WildTagElection<WildTag>>")
      for (tag in
          listOf(
              "EarthTag<WildTagElection<WildTag<ResearchCoordination>>>",
              "JovianTag<WildTagElection<WildTag<ResearchNetwork>>>",
          )) {
        val assigned = kim.resolve(tag)
        kotlin.test.assertTrue(
            result.changes.any { it.change.gaining?.type?.isSubtypeOf(assigned) == true },
            result.changes.joinToString("\n"),
        )
      }
    }
    kim.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Diversifier offers wild tag choices before claiming`() {
    newTestGame("TharsisMap, Diversifier, ResearchCoordination, ResearchNetwork")
    kim.exMachina(
        "AdvancedEcosystems, ResearchOutpost, Satellites, ResearchCoordination, ResearchNetwork"
    )
    kim.inTurn {
      doTask("ClaimMilestone<Class<Diversifier>>")
      selectTask("EACH @WildTagElection { Tag<@WildTagElection>? }")
      doTask("EarthTag<WildTagElection<WildTag<ResearchCoordination>>>")
      doTask("0 EventTag<WildTagElection<WildTag<ResearchNetwork>>>")
      doTasks("8 Pay<> FROM MC", "Diversifier")
    }
  }
}
