package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption.FakeBundle
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val fakeResearchNetwork = cn("FakeResearchNetwork")

internal class FakeWildTagTest : CardTest() {
  @Test
  internal fun `Fake wild tag stays inert on its card`() {
    newGame(PreludeExpansion, FakeBundle)
    p1.manual("PreludeCard")
    engine.phase("Prelude")
    p1.startTurn()

    p1.playPrelude(fakeResearchNetwork).expect("PROD[1 MC], 3 ProjectCard, FakeWildTag")

    p1.count("FakeWildTag<$fakeResearchNetwork>") shouldBe 1
    p1.count("Tag<$fakeResearchNetwork>") shouldBe 0
  }
}
