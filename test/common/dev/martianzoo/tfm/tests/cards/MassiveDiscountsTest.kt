package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MassiveDiscountsTest : TfmSandboxTest() {

  @Test
  internal fun `Stacks with other card discounts`() {
    newTestGame()

    kim.exMachina(
        "Steel, Titanium, $AntiGravityTechnology, $EarthCatapult, " +
            "$ResearchOutpost, $MassConverter, $QuantumExtractor, $Shuttles, $SpaceStation, " +
            "$AdvancedAlloys, $PhoboLog, $MercurianAlloys, $RegoPlastics"
    )
    kim.setToExMachina(4, "MC")
    kim.setToExMachina(1, "Titanium")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpaceElevator>>",
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `A discount applies once for each matching printed tag`() {
    newTestGame()
    kim.exMachina("$EarthOffice, $AcquiredCompany, $MediaGroup")

    kim.inTurn {
      doTasks("PlayProject<Class<LunaGovernor>>", "LunaGovernor FROM ProjectCard", "PROD[2 MC]")
    }
  }
}
