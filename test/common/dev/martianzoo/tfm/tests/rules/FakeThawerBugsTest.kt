package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.cards.CardTest
import kotlin.test.Test

internal class FakeThawerBugsTest : CardTest() {
  @Test
  internal fun `temperature reductions leave all fake credits available`() {
    newGame(GameConfig("FakeStuffBundle, FakeThawer, Builder, Engineer", "Player1", "Player2"))
    p1.runOperation("8 MC, 5 TemperatureStep")
    admin.runOperation("-TemperatureStep")
    admin.phase("Action")
    // Unlike markers on the printed track, these credits cannot identify the removed step.
    p1.claimMilestone(cn("FakeThawer")).expect("-8 MC, FakeThawer")
  }
}
