package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class ConstructorTest : TfmSandboxTest() {

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
    kim.count("MC") shouldBe 42
    admin.count("Award") shouldBe 0
  }

  private fun fundWithoutColonies(): TaskResult {
    newTestGame(addOptions = "AmazonisMap, -ColoniesExpansion")
    kim.exMachina("NormalCityTile<Amazonis_05_01>")
    return kim.fundAward(cn("Constructor"), 8)
  }
}
