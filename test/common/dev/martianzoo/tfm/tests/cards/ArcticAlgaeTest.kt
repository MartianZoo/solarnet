package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ArcticAlgaeTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Gains plants when an opponent places an ocean`() {
    kim.exMachina("$ArcticAlgae")

    stan.stdProject("AquiferProject") { placeTile(1, 2) }.expect("2 Plant<Kim>")
  }

  @Test
  internal fun `Gains plants before an asteroid removes them when oceans resolve first`() {
    stan.exMachina("$ArcticAlgae, 2 Plant")

    kim.playProject(GiantIceAsteroid, 36) {
          shouldThrow<TaskException> { stan.doTask("2 Plant") }
          placeTile(1, 2)
          placeTile(1, 4)
          doTask("-6 Plant<Stan>")
        }
        .expect("-2 Plant<Stan>")
  }

  @Test
  internal fun `Gains plants after an asteroid removes them when the attack resolves first`() {
    stan.exMachina("$ArcticAlgae, 2 Plant")

    kim.playProject(GiantIceAsteroid, 36) {
          doTask("-2 Plant<Stan>")
          placeTile(1, 2)
          placeTile(1, 4)
        }
        .expect("2 Plant<Stan>")
  }
}
