package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.data.Player.Companion.PLAYER3
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class CrashSiteCleanupTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PromoCardPack)
    engine.phase("Action")
    p1.manual("4, ProjectCard")
    requireP2().manual("Plant")
  }

  @Test
  fun `Can be played after removing an opponent's plant`() {
    p1.manual("-Plant<Player2>")
    p1.playProject(CrashSiteCleanup, 4) { doTask("Titanium") }.expect("Titanium")
  }

  @Test
  fun `Cannot be played without removing a plant`() {
    shouldThrow<RequirementException> { p1.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  fun `Cannot be played after losing one of its own plants`() {
    p1.manual("Plant, -Plant")
    shouldThrow<RequirementException> { p1.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  fun `Cannot be played after an opponent removes its own plant`() {
    requireP2().manual("-Plant")
    shouldThrow<RequirementException> { p1.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  fun `Cannot be played if the plant removal was in a previous generation`() {
    p1.manual("-Plant<Player2>")
    engine.manual("Generation")
    shouldThrow<RequirementException> { p1.playProject(CrashSiteCleanup, 4) }
  }

  @Test
  fun `Only the player who removed the plant qualifies`() {
    newGame(PromoCardPack, players = 3)
    val p3 = game.tfm(PLAYER3)
    engine.phase("Action")
    p1.manual("4, ProjectCard")
    requireP2().manual("Plant")
    p3.manual("4, ProjectCard")

    p1.manual("-Plant<Player2>")

    shouldThrow<RequirementException> { p3.playProject(CrashSiteCleanup, 4) }
    p1.playProject(CrashSiteCleanup, 4) { doTask("2 Steel") }.expect("2 Steel")
  }
}
