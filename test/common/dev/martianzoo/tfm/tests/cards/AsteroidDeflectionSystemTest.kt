package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.SystemClasses.AUDIT
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
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

    val result = kim.cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidDeflectionSystem>") }
    result.expect("Asteroid<$AsteroidDeflectionSystem>")
    result.changes.count { it.change.gaining?.className == AUDIT } shouldBe 0
  }

  @Test
  internal fun `Protects plants from another player's project card`() {
    kim.exMachina("$AsteroidDeflectionSystem")
    kim.setToExMachina(1, "Plant")

    shouldThrow<DeadEndException> {
      stan.playProject(Virus, 1) { doTask("-Plant<Kim>") }
    }
    kim.assertCounts(1 to "Plant")
  }

  @Test
  internal fun `Its action claims a revealed space card`() {
    kim.exMachina("$AsteroidDeflectionSystem")

    val result =
        kim.cardAction1(AsteroidDeflectionSystem) {
          doTask("ClaimCardReward<TagFilter<Class<SpaceTag>>, AsteroidDeflectionSystem>")
        }
    result.expect("Asteroid<$AsteroidDeflectionSystem>, 0 ProjectCard<Revealed>")
    result.changes.count { it.change.gaining?.className == AUDIT } shouldBe 1
  }
}
