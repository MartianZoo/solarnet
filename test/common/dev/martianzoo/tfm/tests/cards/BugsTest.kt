package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior. */
internal class BugsTest : CardTest() {
  @Test
  internal fun `Ecology Experts incorrectly does not trigger Viral Enhancers with its own tags`() {
    newGame(PreludeExpansion, CorporateEraExpansion)
    engine.phase("Prelude")
    p1.manual("9 MC, ProjectCard, PreludeCard")

    p1.playPrelude(EcologyExperts) { p1.playProject(ViralEnhancers, 9) }

    p1.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Ecology Experts incorrectly does not trigger Ecological Zone with its plant tag`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    p1.manual("12 MC, ProjectCard, PreludeCard, GreeneryTile<Tharsis_4_4>")

    p1.playPrelude(EcologyExperts) { p1.playProject(EcologicalZone, 12) { placeTile(4, 5) } }

    p1.assertCounts(2 to "Animal<$EcologicalZone>")
  }

  @Test
  internal fun `Mars University incorrectly allows two discards before either draw`() {
    newGame(CorporateEraExpansion)
    p1.manual(
        "5 ProjectCard, $MarsUniversity"
    ) { /* Decline Mars University's discard-and-draw effect. */
      declineTask()
    }
    val manual = p1.also { it.autoExecMode = NONE }

    manual
        .manual("$Research") {
          doTask("2 ProjectCard")
          doTask("-ProjectCard")
          doTask("-ProjectCard")
          doTask("ProjectCard")
          doTask("ProjectCard")
        }
        .expect("2 ProjectCard")
  }

  // https://boardgamegeek.com/thread/3361875/questions-about-the-head-start
  @Test
  internal fun `Head Start incorrectly allows its two actions to interleave`() {
    newGame(PreludeExpansion, TurmoilCardPack, PromoCardPack)
    p1.phase("Prelude")
    p1.manual("4 MC, 10 ProjectCard, PreludeCard, 10 Heat")

    p1.playPrelude(HeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "MC")
      doTask("UseAction<ConvertHeat, Action1>")
      doTask("8 Pay<Class<Heat>> FROM Heat")
      doTask("UseAction<AquiferSP, Action1>")
      doTask("18 Pay<Class<MC>> FROM MC")
      placeTile(5, 5)
    }
  }

  @Test
  internal fun `Prelude incorrectly allows discarding a playable card`() {
    newGame(PreludeExpansion)
    engine.phase("Prelude")
    val moneyBefore = p1.count("MC")

    p1.startTurn()
    p1.doTask("-PreludeCard")
    p1.startTurn()
    p1.doTask("PlayCard<Class<PreludeCard>, Class<$DomeFarming>>")

    p1.assertCounts(1 to "$DomeFarming", 0 to "PreludeCard")
    p1.count("MC") shouldBe moneyBefore + 15
  }

  @Test
  internal fun `Law Suit incorrectly rejects a funded attacker when its owner has only two mc`() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.autoExecMode = NONE
    p1.manual("2 MC, ProjectCard, PROD[Plant]")
    val p2 = requireP2()
    p2.manual("5 MC, PROD[-Plant<Player1>]")

    shouldThrow<TaskException> {
      p1.playProject(LawSuit, 2) {
        doTask("3 MC<Player1> FROM MC<Player2>")
      }
    }

    p1.assertCounts(2 to "MC", 1 to "ProjectCard")
    p2.assertCounts(5 to "MC")
  }

  @Test
  internal fun `Space Elevator incorrectly accepts payment that wastes one steel`() {
    newGame()
    engine.phase("Action")
    p1.manual("10 Steel, 10 Titanium, ProjectCard")

    p1.inTurn {
      doTask("UseAction<PlayCardFromHand, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<$SpaceElevator>>")
      doTask("7 Pay<Class<Steel>> FROM Steel")
      doTask("5 Pay<Class<Titanium>> FROM Titanium")
      doTask("Ok")
    }

    p1.assertCounts(
        3 to "Steel",
        5 to "Titanium",
        0 to "ProjectCard",
        1 to "$SpaceElevator",
    )
  }

  @Test
  internal fun `Two colonies on one tile incorrectly merge their bonuses into one instruction`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2, "Titan"))
    engine.phase("Action")
    p1.manual("$AtmoCollectors") { addCardResources(AtmoCollectors) }
    p1.manual("Colony<Titan>") { addCardResources(AtmoCollectors) }
    p1.manual("Colony<Titan>") { addCardResources(AtmoCollectors) }

    // Each colony should request its own Floater, so the two could go on different cards.
    p1.manual("$ProductiveOutpost") {
      tasks.extract { "${it.instruction}" } shouldBe listOf("2 Floater<Player1>.")
      addCardResources(AtmoCollectors)
    }
  }
}
