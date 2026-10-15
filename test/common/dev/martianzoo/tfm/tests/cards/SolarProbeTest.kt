package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.CarbonNanosystems
import dev.martianzoo.tfm.tests.cards.cardnames.PhysicsComplex
import dev.martianzoo.tfm.tests.cards.cardnames.SolarProbe
import dev.martianzoo.tfm.tests.cards.cardnames.TransNeptuneProbe
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class SolarProbeTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Must be paid before its science tag supplies Carbon Nanosystems graphene`() {
    kim.exMachina("$CarbonNanosystems")
    kim.setToExMachina(8, "MC")
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<SolarProbe>>")
        doTask("9 Pay<> FROM MC")
      }
    }
    kim.assertCounts(
        8 to "MC",
        1 to "ProjectCard",
        0 to "Graphene<$CarbonNanosystems>",
        0 to "PlayedEvent<Class<$SolarProbe>>",
    )
  }

  @Test
  internal fun `Counts its own science tag before entering the played-event pile`() {
    kim.exMachina("$TransNeptuneProbe, $PhysicsComplex")
    kim.setToExMachina(9, "MC")

    kim.inTurn {
          doTasks("PlayProject<Class<SolarProbe>>", "9 Pay<> FROM MC")
          doTasks(
              "SolarProbe FROM ProjectCard",
          )
          doTask("ProjectCard")
          doTask("PlayedEvent<Class<$SolarProbe>> FROM $SolarProbe")
        }
        .expect("0 ProjectCard")

    kim.assertCounts(
        0 to "$SolarProbe",
        1 to "PlayedEvent<Class<$SolarProbe>>",
    )
  }
}
