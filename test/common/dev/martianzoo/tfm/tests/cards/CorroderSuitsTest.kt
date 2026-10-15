package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.VenusianAnimals
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CorroderSuitsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can be played without an eligible Venus card`() {
    kim.inTurn {
          doTasks(
              "PlayProject<Class<CorroderSuits>>",
              "8 Pay<> FROM MC",
              "CorroderSuits FROM ProjectCard",
              "PROD[2 MC]",
          )
          // Resolve the resource offer: no eligible Venus card can hold it.
          selectTask("CardResource")
        }
        .expect("0 CardResource")
  }

  @Test
  internal fun `Cannot decline when an eligible Venus resource exists`() {
    kim.exMachina("$VenusianAnimals")

    // Unlike Venusian Plants, this one resource instruction has no unavailable arm to choose.
    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<CorroderSuits>>")
        doTasks("8 Pay<> FROM MC", "CorroderSuits FROM ProjectCard")
        doTask("PROD[2 MC]")
        doTask("Ok")
      }
    }
  }
}
