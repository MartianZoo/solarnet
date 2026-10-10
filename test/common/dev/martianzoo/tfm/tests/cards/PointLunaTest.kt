package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.fakeWildTags
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class PointLunaTest : CardTest() {

  // https://boardgamegeek.com/thread/2030851/article/29611733#29611733
  @Ignore // The temporary tag holder triggers Point Luna.
  @Test
  internal fun `A temporary wild Earth tag adds no draw beyond Cartel's printed tag`() {
    playCartelWithWildEarthTag().expect("0 ProjectCard, PROD[3 MC]")
  }

  @Test
  internal fun `BUG - A temporary wild Earth tag grants an extra draw`() {
    playCartelWithWildEarthTag().expect("ProjectCard, PROD[3 MC]")
  }

  private fun playCartelWithWildEarthTag(): TaskResult {
    newGame(PreludeExpansion, CorporateEraExpansion, FakeStuffBundle)
    p1.playCorp(PointLuna, 1)
    admin.phase("Action")
    p1.playProject(FakeResearchCoordination, 4)
    val result =
        with(p1) {
          runOperation("${fakeWildTags("EarthTag")}, NewTurn") {
            playProject(Cartel, 8)
          }
        }
    p1.count("FakeWildTagUse") shouldBe 0
    return result
  }
}
