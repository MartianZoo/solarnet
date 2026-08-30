package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AsteroidDeflectionSystemTest : CardTest() {
  @Test
  internal fun `Reveals cards through Asteroid Deflection System when plants are protected`() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    engine.phase("Action")
    p1.manual("13 MC, ProjectCard, Plant, $Tardigrades, Microbe<$Tardigrades>, PROD[Energy]")

    p1.playProject(AsteroidDeflectionSystem, 13).expect("PROD[-Energy]")

    shouldThrow<DeadEndException> { p2.manual("-Plant<Player1>") }
    p2.manual("-Microbe<Player1, $Tardigrades<Player1>>")
        .expect("-Microbe<Player1, $Tardigrades<Player1>>")
    p1.manual("-Plant<Player1>").expect("-Plant")

    p1.cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
    engine.phase("End")
    p1.assertCounts(21 to "VictoryPoint")
  }
}
