package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class AsteroidDeflectionSystemTest : CardTest() {
  @Test
  fun `with protected plants, reveals cards using Asteroid Deflection System`() {
    newGame("TerraformingMars,TharsisMapOption,CorporateEraExpansion,PromoCardPack")
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("13, ProjectCard, Plant, Tardigrades, Microbe<Tardigrades>, PROD[Energy]")

    p1.playProject("AsteroidDeflectionSystem", 13).expect("PROD[-Energy]")

    shouldThrow<DeadEndException> { p2.manual("-Plant<Player1>") }
    p2.manual("-Microbe<Player1, Tardigrades<Player1>>").expect("-Microbe")
    p1.manual("-Plant<Player1>").expect("-Plant")

    p1.cardAction1("AsteroidDeflectionSystem") {
      doTask("Asteroid<AsteroidDeflectionSystem>")
    }
    engine.phase("End")
    p1.assertCounts(21 to "VictoryPoint")
  }
}
