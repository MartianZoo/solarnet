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
    kim.exMachina("PlayedEvent<Class<$MineralDeposit>>, PlayedEvent<Class<$InvestmentLoan>>")

    kim.playProject(AstraMechanica, 7) {
          doTask("ProjectCard FROM PlayedEvent<Class<$MineralDeposit>>")
        }
        .expect(
            "$AstraMechanica, ProjectCard, " +
                "-PlayedEvent<Class<$MineralDeposit>>, -PlayedEvent<Class<$InvestmentLoan>>"
        )
  }

  @Test
  internal fun `Cannot recover an event that placed a special tile`() {
    kim.exMachina(
        "PlayedEvent<Class<$LavaFlows>>, PlayedEvent<Class<$MineralDeposit>>, " +
            "PlayedEvent<Class<$InvestmentLoan>>"
    )

    shouldThrow<NarrowingException> {
      kim.playProject(AstraMechanica, 7) {
        doTask("ProjectCard FROM PlayedEvent<Class<$LavaFlows>>")
      }
    }
  }

  @Test
  internal fun `Cannot recover flipped Pharmacy Union`() {
    kim.exMachina(
        "PlayedEvent<Class<$PharmacyUnion>>, PlayedEvent<Class<$MineralDeposit>>, " +
            "PlayedEvent<Class<$InvestmentLoan>>"
    )

    shouldThrow<TaskException> {
      kim.playProject(AstraMechanica, 7) {
        doTask("ProjectCard FROM PlayedEvent<Class<$PharmacyUnion>>")
      }
    }
  }

  @Test
  internal fun `Cannot recover another player's event`() {
    stan.exMachina("PlayedEvent<Class<$MineralDeposit>>")
    kim.exMachina("PlayedEvent<Class<$InvestmentLoan>>, PlayedEvent<Class<$BribedCommittee>>")

    shouldThrow<TaskException> {
      kim.playProject(AstraMechanica, 7) {
        doTask("ProjectCard FROM PlayedEvent<Stan, Class<$MineralDeposit>>")
      }
    }
  }

  @Test
  internal fun `Cannot play with only one eligible event`() {
    kim.exMachina("PlayedEvent<Class<$Flooding>>")

    shouldThrow<LimitsException> {
      kim.playProject(AstraMechanica, 7) {
        doTask("ProjectCard FROM PlayedEvent<Class<$Flooding>>")
      }
    }
  }
}
