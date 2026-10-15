package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class HiredRaidersTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `An unavailable self-steal does not block stealing from an opponent`() {
    kim.setToExMachina(2, "Steel")
    kim.setToExMachina(1, "MC")
    stan.setToExMachina(3, "MC")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<HiredRaiders>>",
              "-1 MC",
              "HiredRaiders FROM ProjectCard",
              "3 MC<Kim FROM Stan>",
          )
          doTask("PlayedEvent<Class<HiredRaiders>> FROM HiredRaiders")
        }
        .expect("0 Steel<Kim>")
  }

  // Unresolved adjudication (question 17, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3415045/article/48261869#48261869
  @Test
  internal fun `Cannot decline when an opponent has resources`() {
    kim.setToExMachina(1, "MC")
    stan.setToExMachina(3, "MC")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<HiredRaiders>>")

        doTasks(
            "-1 MC",
            "HiredRaiders FROM ProjectCard",
        )
        doTask("Ok")
      }
    }
  }

  @Test
  internal fun `Can steal less than the offered maximum`() {
    kim.setToExMachina(2, "MC")
    stan.setToExMachina(2, "Steel")
    rob.setToExMachina(2, "Steel")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<HiredRaiders>>",
              "-1 MC",
              "HiredRaiders FROM ProjectCard",
              "Steel<Kim FROM Rob>",
          )
          doTask("PlayedEvent<Class<HiredRaiders>> FROM HiredRaiders")
        }
        .expect("0 Steel<Stan>")
  }
}
