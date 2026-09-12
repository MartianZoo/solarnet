package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class FakeThawerTest : CardTest() {
  @Test
  internal fun `Thawer credits each player step but not other players or Admin`() {
    newGame(GameConfig("FakeStuffBundle, FakeThawer, Builder, Engineer", "Player1", "Player2"))
    p1.runOperation("8 MC, 4 TemperatureStep")
    requireP2().runOperation("TemperatureStep")
    admin.runOperation("TemperatureStep")
    admin.phase("Action")
    shouldThrow<RequirementException> { p1.claimMilestone(cn("FakeThawer")) }
    p1.runOperation("TemperatureStep")
    p1.claimMilestone(cn("FakeThawer")).expect("-8 MC, FakeThawer")
  }
}
