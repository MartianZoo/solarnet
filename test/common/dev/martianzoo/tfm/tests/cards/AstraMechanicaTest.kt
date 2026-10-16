package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AstraMechanicaTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can return two differently typed played events`() {
    kim.runOperation("PlayedEvent<Class<$MineralDeposit>>, PlayedEvent<Class<$InvestmentLoan>>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<AstraMechanica>>",
              "7 Pay<> FROM MC",
              "AstraMechanica FROM ProjectCard",
              "ProjectCard FROM PlayedEvent<Class<MineralDeposit>>",
          )
          doTask("ProjectCard FROM PlayedEvent<Class<InvestmentLoan>>")
        }
        .expect(
            "$AstraMechanica, ProjectCard, " +
                "-PlayedEvent<Class<$MineralDeposit>>, -PlayedEvent<Class<$InvestmentLoan>>"
        )
  }

  @Test
  internal fun `Cannot recover an event that placed a special tile`() {
    kim.runOperation(
        "PlayedEvent<Class<$LavaFlows>>, PlayedEvent<Class<$MineralDeposit>>, " +
            "PlayedEvent<Class<$InvestmentLoan>>"
    )

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTask("PlayProject<Class<AstraMechanica>>")

        doTasks(
            "7 Pay<> FROM MC",
            "AstraMechanica FROM ProjectCard",
            "ProjectCard FROM PlayedEvent<Class<LavaFlows>>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot recover flipped Pharmacy Union`() {
    kim.runOperation(
        "PlayedEvent<Class<$PharmacyUnion>>, PlayedEvent<Class<$MineralDeposit>>, " +
            "PlayedEvent<Class<$InvestmentLoan>>"
    )

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<AstraMechanica>>")

        doTasks(
            "7 Pay<> FROM MC",
            "AstraMechanica FROM ProjectCard",
            "ProjectCard FROM PlayedEvent<Class<PharmacyUnion>>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot recover another player's event`() {
    stan.runOperation("PlayedEvent<Class<$MineralDeposit>>")
    kim.runOperation("PlayedEvent<Class<$InvestmentLoan>>, PlayedEvent<Class<$BribedCommittee>>")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<AstraMechanica>>")

        doTasks(
            "7 Pay<> FROM MC",
            "AstraMechanica FROM ProjectCard",
            "ProjectCard FROM PlayedEvent<Stan, Class<MineralDeposit>>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot play with only one eligible event`() {
    kim.runOperation("PlayedEvent<Class<$Flooding>>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<AstraMechanica>>")
        doTasks("7 Pay<> FROM MC", "AstraMechanica FROM ProjectCard")
        doTask("ProjectCard FROM PlayedEvent<Class<Flooding>>")
        doTask("ProjectCard FROM PlayedEvent<Class<Flooding>>")
      }
    }
  }
}
