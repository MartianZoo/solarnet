package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SummitLogisticsTest : TfmSandboxTest() {
  @Test
  internal fun `Is unavailable when Turmoil is absent`() {
    newTestGame("prelude2:cards")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<SummitLogistics>>")
      }
    }
  }

  @Test
  internal fun `Counts Earth tags Jovian tags and colonies when Venus Next is absent`() {
    newTestGame("SummitLogistics, TurmoilExpansion, -VenusNextExpansion, Luna")
    kim.exMachina("$EarthOffice, $VestaShipyard, Colony<Luna>, 2 PartyDelegate<Scientists>")

    kim.inTurn {
          doTasks("PlayProject<Class<SummitLogistics>>", "-10 MC")
          selectTask("-X? Titanium")
          doTask("Ok")
          selectTask("-X? Steel")
          doTasks("Ok", "SummitLogistics FROM ProjectCard")
          selectTask("MC! / (PlanetaryTag OR Colony)")
          selectTask("2! ProjectCard<Kim, Hand>")
        }
        .expect("-7 MC, ProjectCard")
  }

  @Test
  internal fun `Two delegates satisfy its requirement without Scientists ruling`() {
    newTestGame("SummitLogistics, TurmoilExpansion")

    shouldThrow<RequirementException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<SummitLogistics>>")
      }
    }
    kim.exMachina("2 PartyDelegate<Scientists>")
    kim.inTurn {
          doTasks("PlayProject<Class<SummitLogistics>>", "-10 MC")
          selectTask("-X? Titanium")
          doTask("Ok")
          selectTask("-X? Steel")
          doTasks("Ok", "SummitLogistics FROM ProjectCard")
          selectTask("MC! / (PlanetaryTag OR Colony)")
          selectTask("2! ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")
  }

  @Test
  internal fun `Counts all planetary tags and each owned colony`() {
    newTestGame("SummitLogistics, TurmoilExpansion, Luna, Io")
    kim.exMachina(
        "$EarthOffice, $VestaShipyard, $VenusGovernor, Colony<Luna>, Colony<Io>, 2 PartyDelegate<Scientists>"
    )
    stan.exMachina("Colony<Luna>")

    kim.inTurn {
          doTasks("PlayProject<Class<SummitLogistics>>", "-10 MC")
          selectTask("-X? Titanium")
          doTask("Ok")
          selectTask("-X? Steel")
          doTasks("Ok", "SummitLogistics FROM ProjectCard")
          selectTask("MC! / (PlanetaryTag OR Colony)")
          selectTask("2! ProjectCard<Kim, Hand>")
        }
        .expect("-4 MC, ProjectCard")
  }
}
