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
  internal fun `Asteroids added by another card do not claim a revealed card`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("PROD[Energy]")
    p1.runOperation("$AsteroidDeflectionSystem, $AsteroidRights, MC")

    val checkpoint = game.timeline.checkpoint()
    p1.cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidDeflectionSystem>") }

    p1.count("Asteroid<$AsteroidDeflectionSystem>") shouldBe 1
    p1.auditGainsSince(checkpoint) shouldBe 0
  }

  @Test
  internal fun `Reveals cards through Asteroid Deflection System when plants are protected`() {
    newGameWithAutoWorkflow(PromoCardPack)
    val p2 = requireP2()
    playUntilFirstActionPhase()

    p1.turn {
      stdProject("PowerPlantProject")
      playProject(AsteroidDeflectionSystem, 13).expect("PROD[-Energy]")
    }
    p2.turn {
      sellPatents(1)
      sellPatents(1)
    }
    p1.turn {
      stdProject("CityProject") { placeTile(4, 2) }.expect("Plant")
      sellPatents(1)
    }

    shouldThrow<DeadEndException> {
      p2.playProject(Virus, 1) { doTask("-Plant<Player1>") }
    }
    p2.turn {
      stdProject("PowerPlantProject")
      sellPatents(1)
    }

    val checkpoint = game.timeline.checkpoint()
    val reveal =
        p1.cardAction1(AsteroidDeflectionSystem) {
          doTask("ClaimCardReward<TagFilter<Class<SpaceTag>>, AsteroidDeflectionSystem>")
        }
    reveal.expect("Asteroid<$AsteroidDeflectionSystem>")
    reveal.changes
        .filter { it.change.gaining?.type == p1.resolve("ProjectCard<Revealed>") }
        .sumOf { it.change.count } shouldBe 1
    p1.count("ProjectCard<Revealed>") shouldBe 0
    p1.auditGainsSince(checkpoint) shouldBe 1

    shutdownWorkflow()
    p1.declineSecondAction()
    TfmWorkflow.Stepwise(agents).endPhase()
    p1.count("VictoryPoint") shouldBe 21
  }
}
