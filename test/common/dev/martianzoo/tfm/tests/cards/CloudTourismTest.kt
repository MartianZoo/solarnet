package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CloudTourismTest : TfmSandboxTest() {
  @Test
  internal fun `Uses Earth tags when they are fewer than Venus tags`() {
    newTestGame("CloudTourism")
    kim.exMachina("$Sponsors, $EarthOffice, $VenusGovernor, $VenusWaystation, $ForcedPrecipitation")

    kim.inTurn {
          doTasks("PlayProject<Class<CloudTourism>>", "-9 MC", "CloudTourism FROM ProjectCard")
          selectOnlyTask()
        }
        .expect("PROD[2 MC]")
  }

  @Test
  internal fun `Uses Venus tags including its own when they are fewer than Earth tags`() {
    newTestGame("CloudTourism")
    kim.exMachina(
        "$Sponsors, $EarthOffice, $EarthCatapult, $AcquiredCompany, $MediaGroup, $ForcedPrecipitation"
    )

    kim.inTurn {
          doTasks("PlayProject<Class<CloudTourism>>", "-9 MC", "CloudTourism FROM ProjectCard")
          selectOnlyTask()
        }
        .expect("PROD[2 MC]")
  }
}
