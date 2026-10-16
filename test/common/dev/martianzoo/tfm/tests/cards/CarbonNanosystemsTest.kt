package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CarbonNanosystemsTest : TfmSandboxTest() {
  @Test
  internal fun `Its graphene payment resource works without the Promo card pack`() {
    newTestGame(addOptions = "-promos:cards, CarbonNanosystems")
    kim.exMachina("$CarbonNanosystems, 2 Graphene<$CarbonNanosystems>")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<AsteroidCard>>",
          "2 PayFromCard<CarbonNanosystems> FROM Graphene<CarbonNanosystems>",
          "6 Pay<> FROM MC",
          "AsteroidCard FROM ProjectCard",
      )
      doTask("TemperatureStep")
      doTask("2 Titanium")
      // Resolve the optional attack: no unprotected plant target is available.
      selectTask("-3? Plant<Anyone>")
      doTask("TerraformRating")
      doTask("PlayedEvent<Class<AsteroidCard>> FROM AsteroidCard")
    }
  }
}
