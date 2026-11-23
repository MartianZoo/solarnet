package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Terraforming Mars Comprehensive FAQ 1.8, Self-Replicating Robots entry, pages 72–74.
internal class SelfReplicatingRobotsTest : TfmSandboxTest() {
  private val robotsOptions = "$SelfReplicatingRobots"

  @Test
  internal fun `A staged project stays stored through payment and is played from that location`() {
    initialize(1)
    kim.setToExMachina(2, "MC")
    stage("$Mine")
    kim.assertCounts(0 to "ProjectCard<Hand>", 1 to "ProjectCard<StagedProject<Class<$Mine>>>")

    kim.inTurn {
      doTasks(*playCardTasks("$Mine", "StagedProject<Class<$Mine>>"))
      kim.assertCounts(
          0 to "ProjectCard<Hand>",
          1 to "ProjectCard<StagedProject<Class<$Mine>>>",
          1 to "StagedProject<Class<$Mine>>",
          2 to "RobotUnit",
          2 to "Owed",
      )
      doTask("2 Pay<> FROM MC")
      kim.count("ProjectCard<StagedProject<Class<$Mine>>>") shouldBe 0
      doTasks("PROD[Steel]")
    }

    kim.assertCounts(1 to "$Mine", 0 to "ProjectCard<Hand>", 0 to "StagedProject", 0 to "RobotUnit")
  }

  @Test
  internal fun `A staged Mine cannot be played as Titanium Mine`() {
    initialize(1)
    stage("$Mine")
    shouldThrow<DependencyException> {
      kim.inTurn {
        doTasks(*playCardTasks("$TitaniumMine", "StagedProject<Class<$TitaniumMine>>"))
      }
    }
    shouldThrow<NotNowException> {
      kim.inTurn {
        doTasks(*playCardTasks("$TitaniumMine", "StagedProject<Class<$Mine>>"))
      }
    }
    kim.assertCounts(
        1 to "ProjectCard<StagedProject<Class<$Mine>>>",
        2 to "RobotUnit",
        0 to "$TitaniumMine",
        0 to "Owed",
    )
  }

  @Test
  internal fun `Action can be used only once per generation`() {
    initialize(2)

    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *cardActionTasks("$SelfReplicatingRobots"),
            "StageForReplicatedProject<Class<$TitaniumMine>>",
            "ProjectCard<StagedProject<Class<$TitaniumMine>> FROM Hand>",
            "2 RobotUnit<StagedProject<Class<$TitaniumMine>>>",
        )
      }
    }

    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$TitaniumMine>>",
          "ProjectCard<StagedProject<Class<$TitaniumMine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$TitaniumMine>>>",
      )
    }
  }

  @Test
  internal fun `Action can stage a card with two resources or double its resources`() {
    initialize(1)

    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    kim.assertCounts(2 to "RobotUnit<StagedProject<Class<$Mine>>>")

    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$Mine>>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    kim.assertCounts(4 to "RobotUnit<StagedProject<Class<$Mine>>>")
  }

  @Test
  internal fun `Staged cards retain independent robot unit counts`() {
    initialize(5)

    stage("$Mine")
    advanceUntilActionPhase()
    stage("$TitaniumMine")
    advanceUntilActionPhase()
    stage("$MartianRails")
    advanceUntilActionPhase()
    stage("$SpaceStation")
    advanceUntilActionPhase()
    stage("$PowerPlant")

    kim.assertCounts(
        0 to "ProjectCard",
        10 to "RobotUnit",
        2 to "RobotUnit<StagedProject<Class<$Mine>>>",
        2 to "RobotUnit<StagedProject<Class<$TitaniumMine>>>",
        2 to "RobotUnit<StagedProject<Class<$MartianRails>>>",
        2 to "RobotUnit<StagedProject<Class<$SpaceStation>>>",
        2 to "RobotUnit<StagedProject<Class<$PowerPlant>>>",
    )
  }

  @Test
  internal fun `Doubling chooses one card rather than every card`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$TitaniumMine>>",
          "ProjectCard<StagedProject<Class<$TitaniumMine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$TitaniumMine>>>",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$Mine>>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.assertCounts(
        4 to "RobotUnit<StagedProject<Class<$Mine>>>",
        2 to "RobotUnit<StagedProject<Class<$TitaniumMine>>>",
    )
  }

  @Test
  internal fun `More than five cards can be staged`() {
    initialize(6)
    stage("$Mine")
    advanceUntilActionPhase()
    stage("$TitaniumMine")
    advanceUntilActionPhase()
    stage("$MartianRails")
    advanceUntilActionPhase()
    stage("$SpaceStation")
    advanceUntilActionPhase()
    stage("$PowerPlant")
    advanceUntilActionPhase()
    stage("$VestaShipyard")

    kim.assertCounts(0 to "ProjectCard", 12 to "RobotUnit")
  }

  @Test
  internal fun `Staged cards remain outside hand for Planner`() {
    newTestGame(addOptions = robotsOptions)
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(16, "ProjectCard")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.count("ProjectCard") shouldBe 15
    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            "UseAction<ClaimMilestoneAction, Action1>",
            "8 Pay<> FROM MC",
            "Planner",
        )
      }
    }
  }

  @Test
  internal fun `Staged cards remain outside hand for Visionary`() {
    newTestGame(addOptions = "${robotsOptions}, Visionary, Landlord, Banker", playerCount = 2)
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(2, "ProjectCard")
    stan.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "8 Pay<> FROM MC",
          "Visionary",
      )
    }
    victoryPoints() shouldBe listOf(20, 25)
  }

  @Test
  internal fun `Sell Patents cannot sell a staged card`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
          doTasks(*standardProjectTasks("SellPatentsProject"), "1! MC FROM ProjectCard")
        }
        .expect("0 RobotUnit")

    kim.count("ProjectCard") shouldBe 0
    kim.count("RobotUnit<StagedProject<Class<$Mine>>>") shouldBe 2
  }

  @Test
  internal fun `Excentric ignores resources on a card that is not in play`() {
    newTestGame(addOptions = "HellasMap, $robotsOptions", playerCount = 2)
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    stan.exMachina("$SearchForLife, Science<$SearchForLife>")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "8 Pay<> FROM MC",
          "Excentric",
      )
    }
    victoryPoints() shouldBe listOf(20, 28)
  }

  @Test
  internal fun `A card may be staged before its play requirement is met`() {
    initialize(1)
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$BeamFromAThoriumAsteroid>>",
          "ProjectCard<StagedProject<Class<$BeamFromAThoriumAsteroid>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$BeamFromAThoriumAsteroid>>>",
      )
    }

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks(
            *playCardTasks(
                "$BeamFromAThoriumAsteroid",
                "StagedProject<Class<$BeamFromAThoriumAsteroid>>",
            )
        )
      }
    }

    kim.assertCounts(
        0 to "$BeamFromAThoriumAsteroid",
        2 to "RobotUnit<StagedProject<Class<$BeamFromAThoriumAsteroid>>>",
    )
  }

  @Test
  internal fun `Staging a card does not fire its play effects or triggers`() {
    newTestGame(addOptions = robotsOptions)
    kim.exMachina("$SelfReplicatingRobots, PROD[2 MC, Energy]")
    kim.setToExMachina(1, "ProjectCard")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$ImmigrantCity>>",
          "ProjectCard<StagedProject<Class<$ImmigrantCity>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$ImmigrantCity>>>",
      )
    }
    advanceUntilActionPhase()
    replicateImmigrantCity()
    advanceUntilActionPhase()
    replicateImmigrantCity()
    advanceUntilActionPhase()
    replicateImmigrantCity()

    kim.assertProds(3 to "MC", 2 to "Energy")
    kim.count("CityTile") shouldBe 0

    kim.inTurn {
      doTasks(
          *playCardTasks("$ImmigrantCity", "StagedProject<Class<$ImmigrantCity>>"),
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Tharsis_7_4>",
          "PROD[MC]",
      )
    }

    kim.assertProds(2 to "MC", 1 to "Energy")
    kim.count("CityTile") shouldBe 1
  }

  @Test
  internal fun `Resources reduce a staged cards play cost one MC each`() {
    initialize(1)
    kim.setToExMachina(2, "MC")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
      doTasks(
          *playCardTasks("$Mine", "StagedProject<Class<$Mine>>"),
          "2 Pay<> FROM MC",
          "PROD[Steel]",
      )
    }

    kim.assertCounts(0 to "ProjectCard", 1 to "$Mine")
  }

  @Test
  internal fun `A staged card discount cannot reduce its cost below zero`() {
    initialize(1)
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$Mine>>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$Mine>>",
          "4 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
          doTasks(
              *playCardTasks("$Mine", "StagedProject<Class<$Mine>>"),
              "PROD[Steel]",
          )
        }
        .expect("0 MC")

    kim.assertCounts(
        1 to "$Mine",
        0 to "RobotUnit<StagedProject<Class<$Mine>>>",
    )
  }

  @Test
  internal fun `Playing one staged card discards only that cards resources`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$TitaniumMine>>",
          "ProjectCard<StagedProject<Class<$TitaniumMine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$TitaniumMine>>>",
      )
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$Mine>>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
          doTasks(
              *playCardTasks("$Mine", "StagedProject<Class<$Mine>>"),
              "PROD[Steel]",
          )
        }
        .expect("0 MC")

    kim.assertCounts(
        1 to "$Mine",
        0 to "ProjectCard",
        0 to "RobotUnit<StagedProject<Class<$Mine>>>",
        2 to "RobotUnit<StagedProject<Class<$TitaniumMine>>>",
    )
  }

  @Test
  internal fun `Viron can stage a second card in the same generation`() {
    initialize(2)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$SelfReplicatingRobots, Action1>",
          "StageForReplicatedProject<Class<$TitaniumMine>>",
          "ProjectCard<StagedProject<Class<$TitaniumMine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$TitaniumMine>>>",
      )
    }

    kim.assertCounts(
        0 to "ProjectCard",
        2 to "RobotUnit<StagedProject<Class<$Mine>>>",
        2 to "RobotUnit<StagedProject<Class<$TitaniumMine>>>",
    )
  }

  @Test
  internal fun `Viron can stage and then double that card in the same generation`() {
    initialize(1)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.inTurn {
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$SelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<$Mine>>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.count("RobotUnit<StagedProject<Class<$Mine>>>") shouldBe 4
  }

  @Test
  internal fun `Viron can double a staged card twice in one generation`() {
    initialize(1)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$Mine>>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    kim.inTurn {
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$SelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<$Mine>>",
          "4 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }

    kim.count("RobotUnit<StagedProject<Class<$Mine>>>") shouldBe 8
  }

  @Test
  internal fun `Viron can double two different staged cards`() {
    initialize(2)
    kim.exMachina("$Viron")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$TitaniumMine>>",
          "ProjectCard<StagedProject<Class<$TitaniumMine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$TitaniumMine>>>",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$Mine>>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    kim.inTurn {
      doTasks(
          *cardActionTasks("$Viron"),
          "UseAction<$SelfReplicatingRobots, Action2>",
          "ReplicateForStagedProject<Class<$TitaniumMine>>",
          "2 RobotUnit<StagedProject<Class<$TitaniumMine>>>",
      )
    }

    kim.assertCounts(
        4 to "RobotUnit<StagedProject<Class<$Mine>>>",
        4 to "RobotUnit<StagedProject<Class<$TitaniumMine>>>",
    )
  }

  @Test
  internal fun `Playing Robots requires two science tags`() {
    newTestGame(addOptions = robotsOptions)
    kim.setToExMachina(7, "MC")
    kim.setToExMachina(1, "ProjectCard")
    shouldThrow<RequirementException> {
      kim.inTurn { doTasks(*playCardTasks("$SelfReplicatingRobots")) }
    }
    kim.exMachina("$Research")
    kim.inTurn {
      doTasks(
          *playCardTasks("$SelfReplicatingRobots"),
          "7 Pay<> FROM MC",
      )
    }
    kim.assertCounts(1 to "$SelfReplicatingRobots", 0 to "ProjectCard", 0 to "MC")
  }

  @Test
  internal fun `Stored building card can be paid with steel and cash`() {
    initialize(1)
    stage("$TitaniumMine")
    kim.setToExMachina(2, "Steel")
    kim.setToExMachina(1, "MC")
    kim.inTurn {
      doTasks(
          *playCardTasks("$TitaniumMine", "StagedProject<Class<$TitaniumMine>>"),
          "2 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "PROD[Titanium]",
      )
    }
    kim.assertCounts(0 to "MC", 0 to "Steel", 0 to "ProjectCard", 0 to "RobotUnit")
    kim.assertProds(2 to "Titanium")
  }

  @Test
  internal fun `Stored space card combines counters with other discounts and titanium`() {
    initialize(1)
    stage("$SpaceStation")
    kim.exMachina("$EarthCatapult")
    kim.setToExMachina(2, "Titanium")
    kim.setToExMachina(0, "MC")
    kim.inTurn {
      doTasks(
          *playCardTasks("$SpaceStation", "StagedProject<Class<$SpaceStation>>"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "0 Pay<> FROM MC",
      )
    }
    kim.assertCounts(0 to "MC", 0 to "Titanium", 0 to "ProjectCard", 0 to "RobotUnit")
  }

  @Test
  internal fun `A stored event enters the played-event pile after its effects`() {
    initialize(1)
    stage("$AsteroidCard")
    kim.setToExMachina(12, "MC")
    kim.inTurn {
      doTasks(
          *playCardTasks("$AsteroidCard", "StagedProject<Class<$AsteroidCard>>"),
          "12 Pay<> FROM MC",
          "TemperatureStep",
          "2 Titanium",
          "-0 Plant<Anyone>",
          "TerraformRating",
      )
    }
    kim.assertCounts(
        1 to "PlayedEvent<Class<$AsteroidCard>>",
        0 to "$AsteroidCard",
        0 to "ProjectCard",
        0 to "RobotUnit",
        0 to "MC",
    )
  }

  @Test
  internal fun `Printed wild tag cannot qualify a stored card`() {
    newTestGame(addOptions = "$robotsOptions, $ResearchCoordination")
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    shouldThrow<RequirementException> { stage("$ResearchCoordination") }
    kim.assertCounts(1 to "ProjectCard", 0 to "RobotUnit")
  }

  @Test
  internal fun `A corporation cannot be stored even if it has an eligible tag`() {
    initialize(1)
    shouldThrow<RequirementException> { stage("$MiningGuild") }
    kim.assertCounts(1 to "ProjectCard", 0 to "RobotUnit")
  }

  private fun initialize(cards: Int) {
    newTestGame(addOptions = robotsOptions)
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(cards, "ProjectCard")
  }

  private fun stage(card: String) {
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$card>>",
          "ProjectCard<StagedProject<Class<$card>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$card>>>",
      )
    }
  }

  private fun replicateImmigrantCity() {
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots", "Action2"),
          "ReplicateForStagedProject<Class<$ImmigrantCity>>",
          "RobotUnit<StagedProject<Class<$ImmigrantCity>>> / RobotUnit<StagedProject<Class<$ImmigrantCity>>>",
      )
    }
  }

  @Test
  internal fun `Sponsored Academies cannot discard a card hosted on Self-Replicating Robots`() {
    initialize(2)
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    kim.count("ProjectCard") shouldBe 1

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("$SponsoredAcademies"),
            "9 Pay<> FROM MC",
        )
        // Expand the reward into separate tasks for each other player.
        doTasks(
            "EACH Other@Player(NOT Kim) { ProjectCard<Other@Player, Hand>! }",
            "ProjectCard<Stan, Hand>",
            "ProjectCard<Rob, Hand>",
            "-ProjectCard<Hand>",
        )
      }
    }
    kim.count("ProjectCard") shouldBe 1
    kim.count("RobotUnit<StagedProject<Class<$Mine>>>") shouldBe 2
  }

  @Test
  internal fun `Mars University cannot discard a hosted card when a staged science card is played`() {
    initialize(2)
    kim.exMachina("$MarsUniversity")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    advanceUntilActionPhase()
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$ResearchOutpost>>",
          "ProjectCard<StagedProject<Class<$ResearchOutpost>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$ResearchOutpost>>>",
      )
    }

    kim.inTurn {
          doTasks(
              *playCardTasks("$ResearchOutpost", "StagedProject<Class<$ResearchOutpost>>"),
              "16 Pay<> FROM MC",
          )
          // Resolve the exchange offer: the empty hand leaves no card to discard.
          doTasks(
              "Ok",
              "CityTile<Tharsis_4_2>",
              "Plant",
          )
        }
        .expect("0 ProjectCard, 0 RobotUnit<StagedProject<Class<$Mine>>>")
    kim.count("RobotUnit<StagedProject<Class<$Mine>>>") shouldBe 2
  }

  @Test
  internal fun `Corroder Suits adds a resource to a hosted Venus card`() {
    playCorroderWithHostedVenusCard().expect("RobotUnit<StagedProject<Class<$VenusWaystation>>>")
  }

  private fun playCorroderWithHostedVenusCard(): TaskResult {
    newTestGame(addOptions = "$SelfReplicatingRobots")
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$VenusWaystation>>",
          "ProjectCard<StagedProject<Class<$VenusWaystation>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$VenusWaystation>>>",
      )
    }
    return kim.inTurn {
      doTasks(
          *playCardTasks("$CorroderSuits"),
          "8 Pay<> FROM MC",
          "PROD[2 MC]",
          "RobotUnit<StagedProject<Class<$VenusWaystation>>>",
      )
    }
  }

  @Test
  internal fun `Robot units do not satisfy Diversity Support's ninth resource type`() {
    shouldThrow<RequirementException> { playDiversityWithRobotUnits() }
    kim.assertCounts(10 to "MC", 1 to "ProjectCard", 0 to "$DiversitySupport", 2 to "RobotUnit")
  }

  private fun setUpEightResourceTypesWithRobots() {
    newTestGame(addOptions = "AmazonisMap, $robotsOptions")
    val standardResources = "MC, Steel, Titanium, Plant, Energy, Heat"
    kim.setToExMachina(2, "ProjectCard")
    kim.exMachina(
        "$SelfReplicatingRobots, $standardResources, " +
            "$Pets, $Decomposers, Animal<$Pets>, Microbe<$Decomposers>"
    )
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$VenusWaystation>>",
          "ProjectCard<StagedProject<Class<$VenusWaystation>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$VenusWaystation>>>",
      )
    }
    kim.setToExMachina(10, "MC")
  }

  private fun playDiversityWithRobotUnits(): TaskResult {
    setUpEightResourceTypesWithRobots()
    return kim.inTurn {
      doTasks(
          *playCardTasks("$DiversitySupport"),
          "1 Pay<> FROM MC",
          "TerraformRating",
      )
    }
  }

  @Test
  internal fun `Robot units do not break a tie for Collector`() {
    scoreCollectorWithRobotUnits() shouldBe listOf(25, 26, 20)
  }

  @Test
  internal fun `Second-place Collector ignores robot counters without an Audit`() {
    fundCollectorWithRobotUnits()
    kim.exMachina("-Plant, -Heat")
    val checkpoint = game.events.nextOrdinal

    victoryPoints {
      kim.auditGainsSince(checkpoint) shouldBe 0
      stan.auditGainsSince(checkpoint) shouldBe 0
    } shouldBe listOf(22, 26, 20)
  }

  private fun scoreCollectorWithRobotUnits(): List<Int> {
    fundCollectorWithRobotUnits()
    return victoryPoints()
  }

  private fun fundCollectorWithRobotUnits() {
    setUpEightResourceTypesWithRobots()
    stan.exMachina(
        "MC, Steel, Titanium, Plant, Energy, Heat, $Predators, $RegolithEaters, " +
            "Animal<$Predators>, Microbe<$RegolithEaters>"
    )
    kim.inTurn {
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "8 Pay<> FROM MC",
          "Collector",
      )
    }
  }

  @Test
  internal fun `Maxwell Base can add to a hosted Venus card`() {
    useMaxwellWithHostedVenusCard { doTask("RobotUnit<StagedProject<Class<$VenusWaystation>>>") }
        .expect("RobotUnit<StagedProject<Class<$VenusWaystation>>>, 0 Floater<$AerialMappers>")
  }

  private fun useMaxwellWithHostedVenusCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "$SelfReplicatingRobots")
    kim.exMachina("$SelfReplicatingRobots, " + "$AerialMappers, Floater<$AerialMappers>")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$VenusWaystation>>",
          "ProjectCard<StagedProject<Class<$VenusWaystation>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$VenusWaystation>>>",
      )
    }
    kim.exMachina("$MaxwellBase")
    return kim.inTurn {
      doTasks(*cardActionTasks("$MaxwellBase"))
      choice()
    }
  }

  @Test
  internal fun `CEO's Favorite Project can add to a hosted card`() {
    playCeosFavoriteWithHostedCard { doTask("RobotUnit<StagedProject<Class<$Mine>>>") }
        .expect("RobotUnit<StagedProject<Class<$Mine>>>, 0 Animal<$Pets>")
  }

  private fun playCeosFavoriteWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "$SelfReplicatingRobots")
    kim.exMachina("$SelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    return kim.inTurn {
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "1 Pay<> FROM MC",
      )
      // Select the eligible-holder choice before choosing the hosted card's resource.
      selectTask("CardResource<ResourceHolder(HAS CardResource)>*")
      choice()
    }
  }

  @Test
  internal fun `Applied Science can add to a hosted card`() {
    useAppliedScienceWithHostedCard { doTask("RobotUnit<StagedProject<Class<$Mine>>>") }
        .expect("RobotUnit<StagedProject<Class<$Mine>>>")
  }

  private fun useAppliedScienceWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "$SelfReplicatingRobots, $AppliedScience")
    kim.exMachina("$SelfReplicatingRobots, $AppliedScience, Science<$AppliedScience>")
    kim.setToExMachina(1, "ProjectCard")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    return kim.inTurn {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
      )
      choice()
    }
  }

  @Test
  internal fun `L1 Trade Terminal can add to a hosted card`() {
    playTerminalWithHostedCard {
          doTasks("RobotUnit<StagedProject<Class<$Mine>>>", "Animal<$Pets>")
        }
        .expect("RobotUnit<StagedProject<Class<$Mine>>>, Animal<$Pets>")
  }

  private fun playTerminalWithHostedCard(choice: OperationBlock): TaskResult {
    newTestGame(addOptions = "prelude2:cards, ColoniesExpansion, $SelfReplicatingRobots")
    kim.exMachina("$SelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(2, "ProjectCard")
    kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$Mine>>",
          "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$Mine>>>",
      )
    }
    return kim.inTurn {
      doTasks(
          *playCardTasks("$L1TradeTerminal"),
          "25 Pay<> FROM MC",
      )
      // Expand the bonus into a separate resource choice for each eligible card.
      doTask("EACH @ResourceHolder(HAS CardResource) { CardResource<@ResourceHolder>? }")
      choice()
    }
  }

  @Test
  internal fun `L1 Trade Terminal adds to at most three distinct stored or played cards`() {
    newTestGame(addOptions = "$SelfReplicatingRobots, $L1TradeTerminal")
    kim.exMachina("$SelfReplicatingRobots, $Pets, Animal<$Pets>")
    kim.setToExMachina(4, "ProjectCard")
    stage("$Mine")
    advanceUntilActionPhase()
    stage("$TitaniumMine")
    advanceUntilActionPhase()
    stage("$VenusWaystation")

    kim.inTurn {
          doTasks(
              *playCardTasks("$L1TradeTerminal"),
              "25 Pay<> FROM MC",
              "EACH @ResourceHolder(HAS CardResource) { CardResource<@ResourceHolder>? }",
              "RobotUnit<StagedProject<Class<$Mine>>>",
          )
          shouldThrow<TaskException> { doTask("RobotUnit<StagedProject<Class<$Mine>>>") }
          doTasks(
              "RobotUnit<StagedProject<Class<$TitaniumMine>>>",
              "Animal<$Pets>",
          )
          shouldThrow<LimitsException> {
            doTask("RobotUnit<StagedProject<Class<$VenusWaystation>>>")
          }
          doTask("0 RobotUnit<StagedProject<Class<$VenusWaystation>>>")
        }
        .expect(
            "RobotUnit<StagedProject<Class<$Mine>>>, " +
                "RobotUnit<StagedProject<Class<$TitaniumMine>>>, Animal<$Pets>, " +
                "0 RobotUnit<StagedProject<Class<$VenusWaystation>>>"
        )
  }

  @Test
  internal fun `Trader excludes robots from its three card resource types`() {
    newTestGame(addOptions = "UtopiaMap, $SelfReplicatingRobots")
    kim.exMachina(
        "$SelfReplicatingRobots, $Pets, Animal<$Pets>, $RegolithEaters, Microbe<$RegolithEaters>"
    )
    stage("$Mine")

    shouldThrow<RequirementException> {
      kim.inTurn {
        doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Trader")
      }
    }
    kim.exMachina("$SearchForLife, Science<$SearchForLife>")
    kim.inTurn {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Trader")
    }
  }

  @Test
  internal fun `A stored science holder receives robots without activating its science resources`() {
    initialize(2)
    stage("$PhysicsComplex")
    kim.inTurn {
          doTasks(
              *playCardTasks("$CeosFavoriteProject"),
              "1 Pay<> FROM MC",
              "RobotUnit<StagedProject<Class<$PhysicsComplex>>>",
          )
        }
        .expect("RobotUnit<StagedProject<Class<$PhysicsComplex>>>, 0 Science, 0 ScienceTag")
    kim.assertCounts(0 to "$PhysicsComplex", 0 to "BuildingTag")
  }

  @Test
  internal fun `Maxwell Base cannot add to a stored card without a printed Venus tag`() {
    initialize(1)
    stage("$Mine")
    kim.exMachina("$MaxwellBase, $AerialMappers, Floater<$AerialMappers>")
    kim.inTurn {
          doTasks(*cardActionTasks("$MaxwellBase"))
          shouldThrow<NarrowingException> { doTask("RobotUnit<StagedProject<Class<$Mine>>>") }
          doTask("Floater<$AerialMappers>")
        }
        .expect("0 RobotUnit, Floater<$AerialMappers>")
  }

  @Test
  internal fun `Maxwell Base cannot skip a played destination by choosing absent stored resources`() {
    newTestGame(addOptions = "$SelfReplicatingRobots")
    kim.exMachina("$MaxwellBase, $AerialMappers, Floater<$AerialMappers>")
    kim.inTurn {
          doTasks(*cardActionTasks("$MaxwellBase"))
          shouldThrow<TaskException> {
            doTask("Ok")
          }
          doTask("Floater<$AerialMappers>")
        }
        .expect("Floater<$AerialMappers>")
  }

  @Test
  internal fun `Stored projects work in solo games`() {
    newTestGame(addOptions = "$SelfReplicatingRobots", playerCount = 1)
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    kim.setToExMachina(2, "MC")
    stage("$Mine")
    kim.inTurn {
          doTasks(
              *playCardTasks("$Mine", "StagedProject<Class<$Mine>>"),
              "2 Pay<> FROM MC",
              "PROD[Steel]",
          )
        }
        .expect("-StagedProject<Class<$Mine>>, -2 RobotUnit<StagedProject<Class<$Mine>>>")
    kim.assertCounts(0 to "ProjectCard", 0 to "MC")
  }

  // https://boardgamegeek.com/thread/1861808/article/28914878#28914878
  @Test
  internal fun `Cannot host a card without a Building or Space tag`() {
    shouldThrow<RequirementException> { stageCardWithoutEligibleTag() }
    kim.assertCounts(1 to "ProjectCard", 0 to "RobotUnit")
  }

  private fun stageCardWithoutEligibleTag(): TaskResult {
    newTestGame(addOptions = robotsOptions)
    kim.exMachina("$SelfReplicatingRobots")
    kim.setToExMachina(1, "ProjectCard")
    return kim.inTurn {
      doTasks(
          *cardActionTasks("$SelfReplicatingRobots"),
          "StageForReplicatedProject<Class<$MicroMills>>",
          "ProjectCard<StagedProject<Class<$MicroMills>> FROM Hand>",
          "2 RobotUnit<StagedProject<Class<$MicroMills>>>",
      )
    }
  }

  internal class Gameplay : TfmGameplayTest() {
    // https://boardgamegeek.com/thread/2334454/article/33634574#33634574
    @Test
    internal fun `Sponsored Projects adds to stored and played cards across players`() {
      newTestGame("TurmoilExpansion, $SelfReplicatingRobots, -QuickStartVariant", playerCount = 2)
      // Prior card purchases and event reveals are incidental; stage the hosted card normally.
      kim.exMachina("$SelfReplicatingRobots, $Pets, Animal<$Pets>")
      stan.exMachina("$Tardigrades, Microbe<$Tardigrades>")
      admin.exMachina(
          "$ScienceSummit, Current<$ScienceSummit>, " +
              "PendingGlobalEffect<Current<$ScienceSummit>, Class<SponsoredProjects>>"
      )
      kim.turn {
        doTasks(
            *cardActionTasks("$SelfReplicatingRobots"),
            "StageForReplicatedProject<Class<$Mine>>",
            "ProjectCard<StagedProject<Class<$Mine>> FROM Hand>",
            "2 RobotUnit<StagedProject<Class<$Mine>>>",
        )
      }
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.doTask("20 MC") }
      kim.doTask("VenusStep! BY Admin")
      kim.doTask(
          "EACH"
      ) // Admin adds a resource to every occupied holder, including the staged card.
      players.forEach { it.doTask("0 ProjectCard") }
      kim.count("RobotUnit<StagedProject<Class<$Mine>>>") shouldBe 3
      stan.count("Microbe<$Tardigrades>") shouldBe 2
      kim.count("Animal<$Pets>") shouldBe 2
      players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    }
  }
}
