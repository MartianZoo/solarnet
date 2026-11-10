package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2Cards
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.cards.cardnames.MediaArchives
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchCoordination
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchNetwork
import dev.martianzoo.tfm.tests.cards.cardnames.SagittaFrontierServices
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ResearchWildTagTest : TfmSandboxTest() {
  private val player
    get() = agents[PLAYER1]

  @Test
  internal fun `A printed wild stays unassigned after its card is played`() {
    game = setUpGame(canonicalPremise(PreludeExpansion))
    player.autoExecPolicy = NONE
    player.runOperation("PreludeCard")
    admin.exMachina("PreludePhase FROM SetupPhase")
    player.beginOperation("OfferAction")

    player
        .inTurn {
          doTask("PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>")
          autoExecNow(EAGER)
        }
        .expect("PROD[1 MC], 3 ProjectCard, WildTag")

    player.count("WildTag<$ResearchNetwork>") shouldBe 1
    player.count("Tag<$ResearchNetwork>") shouldBe 0
  }

  @Test
  internal fun `An assigned wild Event tag does not count as a played event for Media Archives`() {
    game = setUpGame(canonicalPremise(PreludeExpansion, CorporateEraExpansion))
    player.autoExecPolicy = NONE
    player.runOperation("50 MC, 4 ProjectCard")
    admin.exMachina("ActionPhase FROM SetupPhase")
    player.inTurn {
      doTasks(*playCardTasks("$ResearchCoordination"), "4 Pay<> FROM MC")
      autoExecNow(EAGER)
    }
    player
        .inTurn {
          doTasks(*playCardTasks("$MediaArchives"))
          val assignment = "EventTag<WildTagElection<WildTag<$ResearchCoordination>>>"
          player.addTasks(assignment)
          doTask(assignment)
          player.count("EventTag") shouldBe 1
          doTask("8 Pay<> FROM MC")
          autoExecNow(EAGER)
        }
        .expect("-8 MC")
    player.count("PlayedEvent") shouldBe 0
    player.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `Sagitta rewards a wild-only card as tagless`() {
    game = setUpGame(canonicalPremise(PreludeExpansion, Prelude2Cards))
    player.autoExecPolicy = NONE
    player.runOperation("$SagittaFrontierServices, ProjectCard") { autoExecNow(EAGER) }
    admin.exMachina("ActionPhase FROM SetupPhase")

    player
        .inTurn {
          doTasks(*playCardTasks("$ResearchCoordination"), "4 Pay<> FROM MC")
          autoExecNow(EAGER)
        }
        .expect("0 MC")
  }

  @Test
  internal fun `An assigned wild science tag satisfies Excentric Sponsor during the Prelude phase`() {
    game = setUpGame(canonicalPremise(PreludeExpansion, VenusNextExpansion))
    player.autoExecPolicy = NONE
    player.runOperation("$Inventrix") { autoExecNow(EAGER) }
    admin.exMachina("PreludePhase FROM SetupPhase")
    shouldThrow<RequirementException> {
      player.inTurn {
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>",
            "$ExcentricSponsor FROM PreludeCard",
            "PlayCard<Class<ProjectCard>, Class<$FloatingHabs>>",
        )
        autoExecNow(EAGER)
      }
    }

    player.inTurn {
      doTask("PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>")
      autoExecNow(EAGER)
    }
    player
        .inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$ExcentricSponsor>>",
              "$ExcentricSponsor FROM PreludeCard",
              "PlayCard<Class<ProjectCard>, Class<$FloatingHabs>>",
          )
          autoExecNow(EAGER)
        }
        .expect("$FloatingHabs, 0 MC")
    player.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  @Test
  internal fun `An explicitly assigned wild Event tag raises Interplanetary Trade without Venus`() {
    interplanetaryTradeWithWildEvent(false)
  }

  @Test
  internal fun `An explicitly assigned wild Event tag raises Interplanetary Trade with Venus`() {
    interplanetaryTradeWithWildEvent(true)
  }

  private fun interplanetaryTradeWithWildEvent(venus: Boolean) {
    val config =
        GameConfig(
            "PreludeExpansion, promos:cards, CorporateEraExpansion, EcologyExperts, Unsafe" +
                if (venus) ", VenusNextExpansion" else "",
            "Player1",
            "Player2",
        )
    game = setUpGame(canonicalCatalog(config).gamePremise(config))
    player.autoExecPolicy = NONE
    player.runOperation("$SaturnSystems") { autoExecNow(EAGER) }
    player.runOperation("200 MC, 10 ProjectCard")
    admin.exMachina("PreludePhase FROM SetupPhase")
    player.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EcologyExperts>>",
          "$EcologyExperts FROM PreludeCard",
          "PROD[Plant]",
          "PlayCard<Class<ProjectCard>, Class<$Research>>",
          "-Required* / Required",
          "11 Pay<> FROM MC",
      )
      autoExecNow(EAGER)
    }
    player.inTurn {
      doTask("PlayCard<Class<PreludeCard>, Class<$ResearchNetwork>>")
      autoExecNow(EAGER)
    }
    admin.exMachina("ActionPhase FROM PreludePhase")
    player.inTurn {
      doTasks(*playCardTasks("$Pets"), "10 Pay<> FROM MC")
      autoExecNow(EAGER)
    }
    player.inTurn {
      doTasks(*playCardTasks("$PowerPlant"), "4 Pay<> FROM MC")
      autoExecNow(EAGER)
    }
    player.inTurn {
      doTasks(*playCardTasks("$ImmigrantCity"), "13 Pay<> FROM MC")
      autoExecNow(EAGER)
      doTask("CityTile<Tharsis_1_1>")
      autoExecNow(EAGER)
    }
    if (venus) {
      player.inTurn {
        doTasks(*playCardTasks("$FloatingHabs"), "5 Pay<> FROM MC")
        autoExecNow(EAGER)
      }
    }

    // Six supporting card faces cover the nine ordinary types; Trade supplies Space itself.
    player
        .inTurn {
          doTasks(*playCardTasks("$InterplanetaryTrade"))
          val assignment = "EventTag<WildTagElection<WildTag<$ResearchNetwork>>>"
          player.addTasks(assignment)
          doTask(assignment)
          doTask("27 Pay<> FROM MC")
          autoExecNow(EAGER)
          // The wild already means Event for this action; decline another assignment.
          doTask("0 EventTag<WildTagElection<WildTag<ResearchNetwork>>>")
          autoExecNow(EAGER)
        }
        .expect("PROD[${if (venus) 12 else 11} MC]")
    player.count("Tag<WildTagElection<WildTag>>") shouldBe 0
  }

  // https://boardgamegeek.com/thread/2030851/article/29611733#29611733
  @Test
  internal fun `A temporary wild Earth tag adds no draw beyond Cartel's printed tag`() {
    playCartelWithWildEarthTag().expect("0 ProjectCard, PROD[3 MC]")
  }

  private fun playCartelWithWildEarthTag(): TaskResult {
    game = setUpGame(canonicalPremise(PreludeExpansion, CorporateEraExpansion))
    player.autoExecPolicy = NONE
    player.runOperation("$PointLuna, ProjectCard") { autoExecNow(EAGER) }
    admin.exMachina("ActionPhase FROM SetupPhase")
    player.inTurn {
      doTasks(*playCardTasks("$ResearchCoordination"), "4 Pay<> FROM MC")
      autoExecNow(EAGER)
    }
    val result = player.inTurn {
      doTasks(*playCardTasks("$Cartel"), "8 Pay<> FROM MC")
      autoExecNow(EAGER)
    }
    player.count("Tag<WildTagElection<WildTag>>") shouldBe 0
    return result
  }
}
