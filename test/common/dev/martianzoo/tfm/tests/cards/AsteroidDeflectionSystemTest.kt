package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AsteroidDeflectionSystemTest : CardTest() {
  @Test
  internal fun `Reveals cards through Asteroid Deflection System when plants are protected`() {
    newGameWithAutoWorkflow(PromoCardPack)
    val p2 = requireP2()
    playUntilFirstActionPhase()

    p1.turn {
      stdProject("PowerPlantSP")
      playProject(AsteroidDeflectionSystem, 13).expect("PROD[-Energy]")
    }
    p2.turn {
      sellPatents(1)
      sellPatents(1)
    }
    p1.turn {
      stdProject("CitySP") { placeTile(4, 2) }.expect("Plant")
      sellPatents(1)
    }

    shouldThrow<DeadEndException> {
      p2.playProject(Virus, 1) { doTask("-Plant<Player1>") }
    }
    p2.turn {
      stdProject("PowerPlantSP")
      sellPatents(1)
    }

    p1.cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
        .expect("Asteroid<$AsteroidDeflectionSystem>")

    shutdownWorkflow()
    TfmWorkflow.Manual(game).endPhase()
    p1.count("VictoryPoint") shouldBe 21
  }
}
