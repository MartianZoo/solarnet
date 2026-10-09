package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class ConstructorTest : CardTest() {

  // Absent colonies should contribute zero to city-and-colony scoring.
  // https://boardgamegeek.com/thread/3242831/article/43755615#43755615
  @Ignore // The absent colony category makes Constructor unavailable.
  @Test
  internal fun `Can be funded without Colonies`() {
    fundWithoutColonies().expect("Constructor")
  }

  @Test
  internal fun `BUG - Cannot be funded without Colonies`() {
    shouldThrow<DeadEndException> { fundWithoutColonies() }
    p1.count("MC") shouldBe 36
    admin.count("Award") shouldBe 0
  }

  private fun fundWithoutColonies(): TaskResult {
    newGame(Amazonis)
    p1.playCorp(CrediCor, 0)
    admin.phase("Action")
    p1.stdProject("CityProject") { placeTile(5, 1) }
    return p1.fundAward(cn("Constructor"), 8)
  }
}
