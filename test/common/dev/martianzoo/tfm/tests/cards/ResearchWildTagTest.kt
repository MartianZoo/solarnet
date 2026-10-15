package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2Cards
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.TfmTest
import dev.martianzoo.tfm.tests.canonicalCatalog
import dev.martianzoo.tfm.tests.canonicalPremise
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.cards.cardnames.MediaArchives
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchCoordination
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchNetwork
import dev.martianzoo.tfm.tests.cards.cardnames.SagittaFrontierServices
import dev.martianzoo.tfm.tests.setUpGame
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ResearchWildTagTest : TfmTest() {
  private val kim
    get() = game.testTfm(PLAYER1)

  @Test
  internal fun `A printed wild stays unassigned after its card is played`() {
    game = setUpGame(canonicalPremise(PreludeExpansion))
    kim.runOperation("PreludeCard")
    admin.phase("Prelude")
    kim.startTurn()

    kim.playPrelude(ResearchNetwork).expect("PROD[1 MC], 3 ProjectCard, WildTag")

    kim.count("WildTag<$ResearchNetwork>") shouldBe 1
    kim.count("Tag<$ResearchNetwork>") shouldBe 0
  }

  @Test
  internal fun `An assigned wild Event tag does not count as a played event for Media Archives`() {
    game = setUpGame(canonicalPremise(PreludeExpansion, CorporateEraExpansion))
    kim.runOperation("50 MC, 4 ProjectCard")
    admin.phase("Action")
    kim.playProject(ResearchCoordination, 4)
    with(kim) {
      runOperation("EventTag<WildTag<$ResearchCoordination>>, NewTurn") {
            kim.count("EventTag") shouldBe 1
            playProject(MediaArchives, 8)
          }
          .expect("-8 MC")
    }
    kim.count("PlayedEvent") shouldBe 0
    kim.count("Tag<WildTag>") shouldBe 0
  }

  @Test
  internal fun `Sagitta rewards a wild-only card as tagless`() {
    game = setUpGame(canonicalPremise(PreludeExpansion, Prelude2Cards))
    kim.runOperation("$SagittaFrontierServices, ProjectCard")
    admin.phase("Action")

    kim.playProject(ResearchCoordination, 4).expect("0 MC")
  }

  @Test
  internal fun `An assigned wild science tag satisfies Excentric Sponsor during the Prelude phase`() {
    game = setUpGame(canonicalPremise(PreludeExpansion, VenusNextExpansion))
    kim.runOperation("$Inventrix")
    admin.phase("Prelude")
    shouldThrow<RequirementException> {
      with(kim) { playPrelude(ExcentricSponsor) { playProject(FloatingHabs, 0) } }
    }

    kim.playPrelude(ResearchNetwork)
    with(kim) {
      runOperation("NewTurn") {
            playPrelude(ExcentricSponsor) { playProject(FloatingHabs, 0) }
          }
          .expect("$FloatingHabs, 0 MC")
    }
    kim.count("Tag<WildTag>") shouldBe 0
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
    kim.runOperation("$SaturnSystems")
    kim.runOperation("200 MC, 10 ProjectCard")
    admin.phase("Prelude")
    with(kim) { playPrelude(EcologyExperts) { playProject(Research, 11) } }
    kim.playPrelude(ResearchNetwork)
    admin.phase("Action")
    kim.playProject(Pets, 10)
    kim.playProject(PowerPlant, 4)
    kim.playProject(ImmigrantCity, 13) { placeTile(1, 1) }
    if (venus) kim.playProject(FloatingHabs, 5)

    // Six supporting card faces cover the nine ordinary types; Trade supplies Space itself.
    with(kim) {
      runOperation("EventTag<WildTag<$ResearchNetwork>>, NewTurn") {
            playProject(InterplanetaryTrade, 27) {
              // The wild already means Event for this action; decline another assignment.
              doTask("0 EventTag<WildTag<ResearchNetwork>>")
            }
          }
          .expect("PROD[${if (venus) 12 else 11} MC]")
    }
    kim.count("Tag<WildTag>") shouldBe 0
  }

  // https://boardgamegeek.com/thread/2030851/article/29611733#29611733
  @Test
  internal fun `A temporary wild Earth tag adds no draw beyond Cartel's printed tag`() {
    playCartelWithWildEarthTag().expect("0 ProjectCard, PROD[3 MC]")
  }

  private fun playCartelWithWildEarthTag(): TaskResult {
    game = setUpGame(canonicalPremise(PreludeExpansion, CorporateEraExpansion))
    kim.runOperation("$PointLuna, ProjectCard")
    admin.phase("Action")
    kim.playProject(ResearchCoordination, 4)
    val result =
        with(kim) {
          runOperation("NewTurn") {
            playProject(Cartel, 8)
          }
        }
    kim.count("Tag<WildTag>") shouldBe 0
    return result
  }
}
