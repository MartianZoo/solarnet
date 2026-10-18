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
          // No resource task is created when neither kind of destination exists.
        }
        .expect("0 CardResource")
  }

  @Test
  internal fun `Cannot decline when an eligible Venus resource exists`() {
    kim.exMachina("$VenusianAnimals")

    // An empty stored-card alternative must not make the available addition optional.
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
