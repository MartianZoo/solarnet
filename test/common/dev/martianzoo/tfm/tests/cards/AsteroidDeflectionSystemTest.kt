package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AsteroidDeflectionSystemTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Asteroids added by another card do not claim a revealed card`() {
    kim.exMachina("$AsteroidDeflectionSystem, $AsteroidRights")

    val checkpoint = game.timeline.checkpoint()
    kim.cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidDeflectionSystem>") }

    kim.count("Asteroid<$AsteroidDeflectionSystem>") shouldBe 1
    kim.auditGainsSince(checkpoint) shouldBe 0
  }

  @Test
  internal fun `Protects plants from another player's project card`() {
    kim.exMachina("$AsteroidDeflectionSystem")
    kim.setToExMachina(1, "Plant")

    shouldThrow<DeadEndException> {
      stan.playProject(Virus, 1) { doTask("-Plant<Kim>") }
    }
    kim.count("Plant") shouldBe 1
  }

  @Test
  internal fun `Its action claims a revealed space card`() {
    kim.exMachina("$AsteroidDeflectionSystem")

    val checkpoint = game.timeline.checkpoint()
    val reveal =
        kim.cardAction1(AsteroidDeflectionSystem) {
          doTask("ClaimCardReward<TagFilter<Class<SpaceTag>>, AsteroidDeflectionSystem>")
        }
    reveal.expect("Asteroid<$AsteroidDeflectionSystem>")
    reveal.changes
        .filter { it.change.gaining?.type == kim.resolve("ProjectCard<Revealed>") }
        .sumOf { it.change.count } shouldBe 1
    kim.count("ProjectCard<Revealed>") shouldBe 0
    kim.auditGainsSince(checkpoint) shouldBe 1
  }
}
